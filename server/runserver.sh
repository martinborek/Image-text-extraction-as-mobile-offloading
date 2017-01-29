#!/usr/bin/env bash
docker build -t backend .
docker run --name backend --link=mongo-1:mongo-1 -p 80:80 -p 443:443 -d backend

