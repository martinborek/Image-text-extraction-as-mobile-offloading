import tornado.ioloop
import tornado.web
from tornado.web import RequestHandler
from pymongo import MongoClient
import hashlib
from PIL import Image
import pytesseract
import tornado.httpserver

import functools
import base64
import json
from itsdangerous import (TimedJSONWebSignatureSerializer as Serializer, BadSignature, SignatureExpired)
#import tornado.web

users = {
    'test': 'secret',
    #'test': 'bd2b1aaf7ef4f09be9f52ce2d8d599674d81aa9d6a4421696dc4d93dd0619d682ce56b4d64a9ef097761ced99e0f67265b5f76085e5b0ee7ca4696b2ad6fe2b2',
    'newuser': '857b95a7e57b32249f27d1e425fbadab6b0d51adee4e251c50d7c6b5d137d775b7d053dacf778ffb19faf840ee20d511d7450262602d4e18d539addcf9c847cb'
}

SECRET_KEY = '5$4asRfg_thisAppIsAwesome:)'
TOKEN_EXPIRATION = 3600  # 60 minutes


def verify_password(userToken, password):
    user = verify_auth_token(userToken)
    if user:  # User from token
        return True
    else:
        userEntry = db.users.find_one({"user_name": userToken})
        if userEntry is not None:
            user = userEntry[user_name]
            return True
            #if password == users.get(userToken): #TODO:check password
            #    return True

    return False

def generate_auth_token(user, expiration=TOKEN_EXPIRATION):
    s = Serializer(SECRET_KEY, expires_in=expiration)
    return s.dumps({'id': user})

def verify_auth_token(token):
    s = Serializer(SECRET_KEY)
    try:
        data = s.loads(token)
    except SignatureExpired:
        return None  # valid token, but expired
    except BadSignature:
        return None  # invalid token

    #if data['id'] in users:
    if db.users.find_one({"user_name": data['id']}) is not None:
        return data['id']
    else:
        return None

def requireAuthentication(auth):
    def applyAuthentication(func):
        def _authenticate(requestHandler):
            requestHandler.set_status(401)
            requestHandler.set_header('WWW-Authenticate', 'Basic realm=temerariousRealm')
            requestHandler.finish()
            return False
        
        @functools.wraps(func)
        def newFunc(*args):
            handler = args[0]
 
            authHeader = handler.request.headers.get('Authorization')
            if authHeader is None: 
                return _authenticate(handler)
            if authHeader[:6] != 'Basic ': 
                return _authenticate(handler)
 
            authDecoded = base64.b64decode(authHeader[6:])
            userToken, password = authDecoded.decode().split(':', 2)
 
            if (auth(userToken, password)):
                func(*args, userToken)
            else:
                _authenticate(handler)
                    
        return newFunc
    return applyAuthentication

class TokenHandler(tornado.web.RequestHandler):
    @requireAuthentication(verify_password)
    def get(self, username):
        print('Token for user: ' + str(username))

        token = generate_auth_token(username)
        print(json.dumps({'token': token.decode('ascii')}))

        self.write(json.dumps({'token': token.decode('ascii')}))

class OtherHandler(tornado.web.RequestHandler):
    @requireAuthentication(verify_password)
    def get(self, username):
        self.write('You are authorised')


class MainHandler(RequestHandler):
    def get(self):
        self.write('Backend is running!')


# Test function which displays MongoDB info
class DbTestHandler(RequestHandler):
    def get(self):
        self.write(client.server_info())

class AddTestuserHandler(RequestHandler):
    def get(self):
        user = 'testuser'
        password = 'time2work'
        hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        user = {
            'user_name': user,
            'password': hashed_password,
            'records': []
        }
        self.write('Added')


class GetUserHandler(RequestHandler):
    def get(self):
        user = db.users.find_one({"user_name": 'testuser'})
        self.write(user)

# Test function for adding users to the DB
class AddUserHandler(RequestHandler):
    def post(self):
        user = self.get_body_argument('user')
        password = self.get_body_argument('password')
        hashed_password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        user = {
            'user_name': user,
            'password': hashed_password,
            'records': []
        }
        db.users.insert_one(user)

        self.write('OK')


# Test function for running OCR on test.jpg
class OCRTestHandler(RequestHandler):
    def get(self):
        self.write(pytesseract.image_to_string(Image.open('test.jpg')))


# URL routes etc.
def make_app():
    return tornado.web.Application([
        (r'/', MainHandler),
        (r'/token', TokenHandler),
        (r'/other', OtherHandler),
        (r'/db/', DbTestHandler),
        (r'/add_user/', AddUserHandler),
        (r'/add_testuser/', AddTestUserhandler),
        (r'/get_user/', GetUserHandler),
        (r'/test_ocr/', OCRTestHandler),
    ])

if __name__ == '__main__':
    app = make_app()

    client = MongoClient('mongodb://mongo:27017')
    db = client.userdata
    
    http_server = tornado.httpserver.HTTPServer(app, ssl_options={
        "certfile": "cert/nopass_cert.pem",
        "keyfile": "cert/nopass_key.pem",
    })
    http_server.listen(443)
    tornado.ioloop.IOLoop.instance().start()
