from tornado import gen
import logging
import json
import datetime
import hashlib
from PIL import Image
import pytesseract
from unidecode import unidecode
from io import BytesIO
import db_safe
from bson.objectid import ObjectId

THUMBNAIL_SIZE = 128, 128


@gen.coroutine
def perform_ocr_and_store(fs, image, username):
    """
    Performs Tesseract OCR processing on the given image, creates a thumbnail and stores the image and its
    thumbnail to the database using GridFS.

    Returns OCR results and Object ID:s to the GridFS files.

    :param fs:
    :param username:
    :param image: Image to process as a Tornado File from a multipart/form-data request.
    :return: OCR result text, original image's ID in GridFS, thumbnail's ID in GridFS
    """
    logging.debug('Processing ' + image['filename'])
    pil_image = Image.open(BytesIO(image['body']))
    try:
        ocr_text = pytesseract.image_to_string(pil_image).replace('<', '')
    except UnicodeDecodeError:
        ocr_text = 'Error: OCR processing could not extract a valid string from image.'

    thumbnail = yield create_thumbnail(pil_image)
    image_fs_id = yield db_safe.fs_put(fs, image['body'],
                                       content_type=image['content_type'],
                                       filename=image['filename'],
                                       username=username,
                                       original=True)
    thumbnail_fs_id = yield db_safe.fs_put(fs, thumbnail,
                                           content_type='image/jpeg',
                                           filename='t_' + image['filename'],
                                           username=username,
                                           original=False)
    return ocr_text, image_fs_id, thumbnail_fs_id


@gen.coroutine
def create_thumbnail(image):
    """
    Creates a thumbnail of the PIL image given as a parameter, and returns it as a byte array in JPEG format.

    :param image: PIL image for thumbnail creation
    :return: Thumbnail as a byte array in JPEG format
    """
    image.thumbnail(THUMBNAIL_SIZE)
    thumbnail_bytes = BytesIO()
    image.save(thumbnail_bytes, format='JPEG')
    return thumbnail_bytes.getvalue()


@gen.coroutine
def get_next_seq(db, transaction_id):
    """
    Returns the sequence number of the next expected image for a transaction.

    :param transaction_id: The transaction document's ObjectID
    :return: Next expected seq number
    """
    transaction = yield db_safe.find_one(db.transactions, {'_id': transaction_id})
    return transaction['seq'] + 1


@gen.coroutine
def generate_json_message(message, transaction_id, next_seq, ocr_result=''):
    """
    Generates a JSON formatted string for responses.

    :param message:
    :param transaction_id:
    :param next_seq:
    :param ocr_result:
    :return:
    """
    msg = {
        'uid': str(transaction_id),
        'message': message,
        'next_seq': next_seq,
        'ocr_result': ocr_result
    }
    return msg


# Logs and responds with the given error code and message
def respond_and_log_error(request, error_code, msg):
    request.set_status(error_code)
    logging.debug('Error response: ' + str(msg))
    request.finish(msg)
    return


# JSONEncoder, which outputs date and datetime in ISO-format and ObjectID as hex string
class JSONDateTimeEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, (datetime.date, datetime.datetime)):
            return obj.isoformat()
        elif isinstance(obj, ObjectId):
            return str(obj)
        else:
            return json.JSONEncoder.default(self, obj)


# Creates and returns a user document from the given username and password
def create_user(username, password):
    user = {
        'username': username,
        'password': hashlib.sha256(password.encode('utf-8')).hexdigest(),
        'records': []
    }
    return user
