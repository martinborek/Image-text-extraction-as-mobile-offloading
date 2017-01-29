from pymongo.errors import AutoReconnect, NetworkTimeout
from pymongo.collection import Collection
from pymongo import ReturnDocument
from gridfs import GridFS
from tornado import gen
from socket import timeout
import logging

"""
Fail-over tolerant MongoDB database operations.

Each operation is retried 5 times with increasing delays between tries.
"""


# Decorator which runs a function again 5 times when an  AutoReconnect exception is thrown
def retry_on_autoreconnect(f):
    @gen.coroutine
    def f_retry(*args, **kwargs):
        for i in range(5):
            try:
                return f(*args, **kwargs)
            except AutoReconnect:
                logging.warning('MongoDB AutoReconnect: Could not connect to primary database, try # ' + str(i+1))
                yield gen.sleep(2 + pow(2, i))
                continue
        logging.error('Database fail-over timed out after 5 reconnect attempts, operation failed.')

    return f_retry


@retry_on_autoreconnect
def find(collection, query):
    if isinstance(collection, Collection):
        return collection.find(query)
    else:
        raise TypeError('collection must be of type Collection')


@retry_on_autoreconnect
def find_one(collection, query):
    if isinstance(collection, Collection):
        return collection.find_one(query)
    else:
        raise TypeError('collection must be of type Collection')


@retry_on_autoreconnect
def insert(collection, document):
    if isinstance(collection, Collection):
        return collection.insert_one(document)
    else:
        raise TypeError('collection must be of type Collection')


@retry_on_autoreconnect
def update(collection, query, update):
    if isinstance(collection, Collection):
        return collection.update_one(query, update)
    else:
        raise TypeError('collection must be of type Collection')


@retry_on_autoreconnect
def find_one_and_update(collection, query, update):
    if isinstance(collection, Collection):
        return collection.find_one_and_update(query, update, return_document=ReturnDocument.AFTER)
    else:
        raise TypeError('collection must be of type Collection')


@retry_on_autoreconnect
def delete(collection, query):
    if isinstance(collection, Collection):
        return collection.delete_one(query)
    else:
        raise TypeError('collection must be of type Collection')


@retry_on_autoreconnect
def fs_find_one(fs, query):
    if isinstance(fs, GridFS):
        return fs.find_one(query)
    else:
        raise TypeError('fs must be of type GridFS')


@retry_on_autoreconnect
def fs_put(fs, data, **kwargs):
    if isinstance(fs, GridFS):
        return fs.put(data, **kwargs)
    else:
        raise TypeError('fs must be of type GridFS')


@retry_on_autoreconnect
def fs_delete(fs, fs_id):
    if isinstance(fs, GridFS):
        return fs.delete(fs_id)
    else:
        raise TypeError('fs must be of type GridFS')
