import tornado.ioloop
import tornado.web
import tornado.httpserver
from pymongo import MongoClient
from tornado.web import RequestHandler
from tornado import gen
from bson.objectid import ObjectId
from bson.errors import InvalidId
from bson.codec_options import CodecOptions
import hashlib
from gridfs import GridFS
import datetime
import tornado.options
import logging
from pytz import timezone
import functools
import base64
import json
from itsdangerous import (TimedJSONWebSignatureSerializer as Serializer, BadSignature, SignatureExpired)
import requests
import db_safe
from helpers import perform_ocr_and_store, get_next_seq, generate_json_message, respond_and_log_error, \
    JSONDateTimeEncoder, create_user

# App settings
SOURCE_IMAGE_LIFETIME = 7  # How many days source images are stored in DB
TRANSACTION_LIFETIME = 60  # How many minutes unfinished transactions are stored in DB
DB_CLEANUP_INTERVAL = 3600  # How many seconds to wait between database cleanup runs

DB_CONNECT_STRING = 'mongodb://mongo-1:27017,mongo-2:27017,mongo-3:27017'

SECRET_KEY = '5$4asRfg_thisAppIsAwesome:)'
TOKEN_EXPIRATION = 3600  # 60 minutes

APP_FB_TOKEN = '349946252046985|lJ9EY8Rs_63dP6I7ei0liQlEybQ'
FB_SUFFIX = '@facebook.com'
FB_NO_PASS = 'FB account'

# Global variables
mongo_client = None
db = None
fs = None


@gen.coroutine
def verify_password(user_token, password):
    user = yield verify_auth_token(user_token)
    if user:  # User from token
        return user
    else:
        if user_token.endswith(FB_SUFFIX):  # FB account; cannot be authorised this way
            return user_token

        user_entry = yield db_safe.find_one(db.users, {"username": user_token})

        if user_entry is not None:
            if hashlib.sha256(password.encode('utf-8')).hexdigest() == user_entry.get('password'):
                user = user_entry.get('username')
                return user

    return None


def generate_auth_token(user, expiration=TOKEN_EXPIRATION):
    logging.debug('Token generated for ' + str(user))
    s = Serializer(SECRET_KEY, expires_in=expiration)
    return s.dumps({'id': user})


@gen.coroutine
def verify_auth_token(token):
    s = Serializer(SECRET_KEY)
    try:
        data = s.loads(token)
    except SignatureExpired:
        return None  # valid token, but expired
    except BadSignature:
        return None  # invalid token
    username = data['id']

    user = yield db_safe.find_one(db.users, {"username": username})
    if user is not None or username.endswith(FB_SUFFIX):
        return username
    else:
        return None


def require_authentication(auth):
    def apply_authentication(func):
        def _authenticate(request_handler):
            request_handler.set_status(401)
            request_handler.set_header('WWW-Authenticate', 'Basic realm=temerariousRealm')
            request_handler.finish()
            return False

        @functools.wraps(func)
        @gen.coroutine
        def new_func(*args):
            handler = args[0]
            auth_header = handler.request.headers.get('Authorization')
            if auth_header is None:
                return _authenticate(handler)
            if auth_header[:6] != 'Basic ':
                return _authenticate(handler)
            auth_decoded = base64.b64decode(auth_header[6:])
            user_token, password = auth_decoded.decode().split(':', 2)

            authenticated_user = yield auth(user_token, password)
            if authenticated_user is None:
                _authenticate(handler)
            else:
                func(*args, username=authenticated_user)

        return new_func
    return apply_authentication


class TokenHandler(RequestHandler):
    @require_authentication(verify_password)
    @gen.coroutine
    def get(self, username):
        logging.debug('Token for user: ' + str(username))

        token = generate_auth_token(username)

        self.write(json.dumps({'token': token.decode('ascii')}))


class FBTokenHandler(RequestHandler):
    # TODO: Make this asynchronous
    @gen.coroutine
    def get(self):
        user_token = self.get_argument('token')

        # Verify user_token by FB
        r = requests.get('https://graph.facebook.com/debug_token?input_token='
                         + user_token + '&' + 'access_token=' + APP_FB_TOKEN)

        if r.status_code != 200:
            respond_and_log_error(self, 401, 'Authentication failed')
            return
        received_data = json.loads(r.text).get('data')

        if received_data.get('error') is not None:
            respond_and_log_error(self, 401, received_data.get('error').get('message'))
            return

        # Extract user id, use it as a username, concatenated with FB_SUFFIX,
        # so that it does not interfere with locally registered users
        user_id = received_data.get('user_id')
        username = user_id + FB_SUFFIX

        # FB user still needs to be in the local database. Check if this account
        # is already there; if not, add it.
        user = yield db_safe.find_one(db.users, {'username': username})
        if user is None:
            logging.debug('Adding Facebook user to DB: ' + username)
            yield db_safe.insert(db.users, create_user(username, FB_NO_PASS))
            logging.debug('Added to DB')

        token = generate_auth_token(username)

        self.write(json.dumps({'token': token.decode('ascii')}))


class OtherHandler(RequestHandler):
    @require_authentication(verify_password)
    def get(self, username):
        self.write('You are authorised')


class MainHandler(RequestHandler):
    def get(self):
        self.write('Backend is running!')


# Test function which displays MongoDB info
class DbTestHandler(RequestHandler):
    def get(self):
        logging.debug(self.request)
        response = str(mongo_client.server_info()) + '\n' + str(mongo_client.admin.command('replSetGetStatus'))
        self.write(response)


class GetRecordsHandler(RequestHandler):
    """
    Gets a given amount of OCR records from the database and returns the data as JSON
    """

    @require_authentication(verify_password)
    @gen.coroutine
    def get(self, username):
        logging.debug('User: ' + username)
        logging.debug(self.request)

        try:
            amount = int(self.get_query_argument('amount'))
        except tornado.web.MissingArgumentError:
            amount = 1000

        # Get timezone-aware users-collection
        users_tz = db.users.with_options(codec_options=CodecOptions(
            tz_aware=True,
            tzinfo=timezone('Europe/Helsinki')))

        user = yield db_safe.find_one(users_tz, {'username': username})
        records = {'records': user['records'][-amount:]}
        self.write(json.dumps(records, cls=JSONDateTimeEncoder, ensure_ascii=False))


class GetImageHandler(RequestHandler):
    """
    Gets an image from the GridFS database.
    """

    @require_authentication(verify_password)
    @gen.coroutine
    def get(self, slug, username):
        logging.debug('User: ' + username)
        logging.debug(self.request)

        # Check that slug is a valid ObjectId
        try:
            fs_id = ObjectId(slug)
        except InvalidId:
            respond_and_log_error(self, 404, 'Malformed image ID')
            return

        # Check that image exists in GridFS
        image = yield db_safe.fs_find_one(fs, {'_id': fs_id})
        if image is None:
            respond_and_log_error(self, 404, 'No such image in database')
            return

        print(str(image))
        # Check that user has permission to access the image
        if username != image.username:
            print('wtf')
            respond_and_log_error(self, 403, 'No permission to access image')
            return
        self.set_header('Content-type', image.content_type)
        self.write(image.read())


class OCRHandler(RequestHandler):
    """
    Registers a new transaction in the database, which is used for monitoring image uploads.
    """

    @require_authentication(verify_password)
    @gen.coroutine
    def post(self, username):
        logging.debug('User: ' + username)
        logging.debug(self.request)

        images_total = int(self.get_body_argument('images_total'))
        transaction = {
            'username': username,
            'creation_time': datetime.datetime.utcnow(),
            'images_total': images_total,
            'seq': 0,
            'ocr': [],
            'image_fs_ids': []
        }
        result = yield db_safe.insert(db.transactions, transaction)
        response = yield generate_json_message('Ready for upload', str(result.inserted_id), 1)
        self.write(response)


class UploadImageHandler(RequestHandler):
    """
    Handles individual image uploads and updates the user's records, when last image of a transaction is uploaded.
    """

    @require_authentication(verify_password)
    @gen.coroutine
    def post(self, username):
        logging.debug('User: ' + username)
        logging.debug(self.request)

        # Check that UID is a valid ObjectId
        try:
            uid = ObjectId(self.get_body_argument('uid'))
        except InvalidId:
            response = yield generate_json_message('Malformed UID', self.get_body_argument('uid'), 0)
            respond_and_log_error(self, 400, response)
            return

        seq = int(self.get_body_argument('seq'))
        next_seq = yield get_next_seq(db, uid)

        # Check that transaction exists in database
        transaction = yield db_safe.find_one(db.transactions, {'_id': uid})
        if transaction is None:
            response = yield generate_json_message('No such UID in database', uid, 0)
            respond_and_log_error(self, 400, response)
            return

        # Check that 'image' form element is in the request
        try:
            image = self.request.files['image'][0]
        except LookupError:
            response = yield generate_json_message('No image in request', uid, next_seq)
            respond_and_log_error(self, 400, response)
            return

        if len(transaction['image_fs_ids']) < transaction['images_total']:
            # Check that the uploaded image has the expected seq number
            if seq != next_seq:
                response = yield generate_json_message('Incorrect upload order', uid, next_seq)
                respond_and_log_error(self, 400, response)
                return

            # Perform OCR and store image and its thumbnail in GridFS
            ocr_text, image_fs_id, thumbnail_fs_id = yield perform_ocr_and_store(fs, image, username)
            image_fs_ids = {
                'image_fs_id': image_fs_id,
                'thumbnail_fs_id': thumbnail_fs_id
            }

            # Update transaction document in DB
            transaction = yield db_safe.find_one_and_update(db.transactions, {'_id': uid},
                                                            {'$set': {'seq': seq}, '$push': {
                                                                'ocr': ocr_text,
                                                                'image_fs_ids': image_fs_ids
                                                            }})

        # If this was the last expected image, do final processing
        if seq == transaction['images_total']:
            ocr_result = '\n'.join(transaction['ocr'])
            record = {
                'creation_time': datetime.datetime.utcnow(),
                'image_fs_ids': transaction['image_fs_ids'],
                'ocr_text': ocr_result
            }
            # TODO: Error handling and cleanup if database update fails
            # Update user document in DB
            yield db_safe.update(db.users, {'username': username}, {'$push': {'records': record}})
            # Delete transaction document from DB
            yield db_safe.delete(db.transactions, {'_id': uid})

            # Respond with the final combined text from OCR
            response = yield generate_json_message("OCR finished", uid, 0, ocr_result=ocr_result)
            logging.debug('Response: ' + str(response))
            self.write(response)
        else:
            # If more images are expected, respond with the expected seq number
            response = yield generate_json_message('Image processed', uid, seq + 1)
            logging.debug('Response: ' + str(response))
            self.write(response)


# Initializes the database by creating some users
def database_init():
    db.users.create_index('username', unique=True)
    users = [{'username': 'test1', 'password': 'secret1'},
             {'username': 'test2', 'password': 'secret2'},
             {'username': 'test3', 'password': 'secret3'}]
    for user in users:
        result = db.users.find_one({'username': user['username']})
        if result is None:
            db.users.insert(create_user(**user))
    logging.info("Database initialized")


# Removes source images older than SOURCE_IMAGE_LIFETIME from the database
@gen.coroutine
def database_cleanup():
    while True:
        logging.info(
            'Cleaning up source images older than ' + str(SOURCE_IMAGE_LIFETIME) + ' days from database...')
        now = datetime.datetime.utcnow()
        source_lifetime_limit = now - datetime.timedelta(days=SOURCE_IMAGE_LIFETIME)
        transaction_lifetime_limit = now - datetime.timedelta(minutes=TRANSACTION_LIFETIME)

        # Find users from db, who have outdated and uncleaned records
        users = yield db_safe.find(db.users, {
            '$and': [{'records.creation_time': {'$lt': source_lifetime_limit}},
                     {'records.cleaned': {'$in': [None, False]}}]
        })
        for user in users:
            for record in user['records']:
                # Find records which are older than limit
                if record['creation_time'] < source_lifetime_limit and 'cleaned' not in record:
                    for image in record['image_fs_ids']:
                        # Get source image ID and remove it from the record
                        image_fs_id = image.pop('image_fs_id', None)
                        if image_fs_id is not None:
                            # Delete image from GridFS
                            yield db_safe.fs_delete(fs, image_fs_id)
                            logging.info('Deleted image from GridFS: ' + str(image_fs_id))
                    record['cleaned'] = True
            # Update the user's records array
            yield db_safe.update(db.users, {'username': user['username']}, {'$set': {'records': user['records']}})
            logging.info("Cleaned up records for user: " + user['username'])

        # Find transactions which are older than limit
        logging.info(
            'Cleaning up transactions older than ' + str(TRANSACTION_LIFETIME) + ' minutes from database...')
        transactions = yield db_safe.find(db.transactions, {'creation_time': {'$lt': transaction_lifetime_limit}})
        for transaction in transactions:
            for images in transaction['image_fs_ids']:
                # Find GridFS ids for images in the transaction
                for key, image_fs_id in images.items():
                    # Delete image from GridFS
                    yield db_safe.fs_delete(fs, image_fs_id)
                    logging.info('Deleted image from GridFS: ' + str(image_fs_id))
            # Delete transaction document from DB
            yield db_safe.delete(db.transactions, {'_id': transaction['_id']})
            logging.info("Deleted transaction: " + str(transaction['_id']))

        logging.info('Database cleanup complete')

        # Wait in the background for the specified interval
        yield gen.sleep(DB_CLEANUP_INTERVAL)


# URL routes
def make_app():
    return tornado.web.Application([
        (r'/', MainHandler),
        (r'/token', TokenHandler),
        (r'/fb_token', FBTokenHandler),
        (r'/other', OtherHandler),
        (r'/db/', DbTestHandler),
        (r'/ocr/', OCRHandler),
        (r'/upload/', UploadImageHandler),
        (r'/records/', GetRecordsHandler),
        (r'/image/([^/]+)', GetImageHandler),
    ])


# Starts the HTTP server
def start_tornado():
    logging.info('Starting backend server')

    # Initialize database connection
    logging.info('MongoDB URI: %s' % DB_CONNECT_STRING)
    global mongo_client
    mongo_client = MongoClient(DB_CONNECT_STRING)
    if mongo_client is None:
        logging.error('Connection to MongoDB server failed, exiting')
        return

    # Initialize database
    global db
    db = mongo_client.userdata
    database_init()

    # Initialize GridFS
    try:
        global fs
        fs = GridFS(db)
    except TypeError:
        logging.error('Error loading database, exiting')
        return

    https_server = tornado.httpserver.HTTPServer(app, ssl_options={
        'certfile': 'cert/nopass_cert.pem',
        'keyfile': 'cert/nopass_key.pem',
    })
    https_server.listen(443)
    logging.info('Web server is waiting for requests...')
    tornado.ioloop.IOLoop.current().spawn_callback(database_cleanup)
    tornado.ioloop.IOLoop.instance().start()


# Main app
if __name__ == '__main__':
    app = make_app()

    # Set up logging
    tornado.options.parse_command_line()

    # Start server
    start_tornado()
