#!/usr/bin/env bash
docker build -t eu.gcr.io/mcc-2016-g13-p2/backend .
gcloud docker -- push eu.gcr.io/mcc-2016-g13-p2/backend:latest
sleep 5
kubectl set image deployment/backend backend=eu.gcr.io/mcc-2016-g13-p2/backend:latest
echo 'Cluster updated.'
