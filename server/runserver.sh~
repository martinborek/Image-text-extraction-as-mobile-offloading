#!/usr/bin/env bash
docker build -t backend .
docker run --name backend --link=mongo:mongo -p 80:80 -d backend

