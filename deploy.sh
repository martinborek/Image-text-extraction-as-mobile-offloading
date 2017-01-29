#!/usr/bin/env bash

PROJECT_ID="mcc-2016-g13-p2"
GCLOUD_ZONE="europe-west1-c"

echo 'Building Android application and deploying the backend service as a Google Container Engine cluster...'
echo 'NOTE: THIS WILL TAKE A WHILE!'

sleep 5

./build_apk.sh

sleep 10

echo 'Starting backend deployment...'

echo 'Installing dependencies...'

CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)"
echo "deb https://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
apt-get update && apt-get -y install google-cloud-sdk kubectl docker.io make

RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'Dependencies installed successfully.'
else
    (>&2 echo 'ERROR: Dependencies could not be installed.')
    exit $RESULT
fi


echo 'Building docker container...'

docker build -t eu.gcr.io/$PROJECT_ID/backend:v1 ./server

RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'Docker container set up successfully.'
else
    (>&2 echo 'ERROR: Docker container could not be built.')
    exit $RESULT
fi


echo 'Authenticating to Google Cloud and pushing Docker container...'

gcloud auth activate-service-account tt-822@mcc-2016-g13-p2.iam.gserviceaccount.com --key-file=./server/key/mcc-2016-g13-p2-94921abc7259.json
gcloud config set compute/zone $GCLOUD_ZONE
gcloud config set project $PROJECT_ID
gcloud docker -- push eu.gcr.io/$PROJECT_ID/backend:v1

RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'Docker container pushed successfully.'
else
    (>&2 echo 'ERROR: Docker container could not be pushed to Google Cloud.')
    exit $RESULT
fi


echo 'Setting up cluster...'

gcloud container clusters create backend
gcloud config set container/use_client_certificate True
gcloud container clusters get-credentials backend

RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'Initial cluster setup successful.'
else
    (>&2 echo 'ERROR: Initial cluster setup failed.')
    exit $RESULT
fi


echo 'Setting up MongoDB replica set...'

make -C server/cluster/sidecar/ add-replica
echo 'Waiting 90 seconds for DB container 1 to initialize...'
sleep 90
make -C server/cluster/sidecar/ add-replica
echo 'Waiting 90 seconds for DB container 2 to initialize...'
sleep 90
make -C server/cluster/sidecar/ add-replica
echo 'Waiting 90 seconds for DB container 3 to initialize...'
sleep 90


RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'MongoDB replica set created successfully.'
else
    (>&2 echo 'ERROR: MongoDB replica set creation failed.')
    exit $RESULT
fi


echo 'Setting up backend application cluster...'

kubectl create -f server/cluster/backend.yaml

RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'Cluster set up successfully.'
else
    (>&2 echo 'ERROR: Cluster could not be set up.')
    exit $RESULT
fi

echo 'Waiting 2 minutes for cluster to initialize...'
sleep 120
gcloud container clusters list
kubectl get pods
kubectl get services
echo 'Done.'