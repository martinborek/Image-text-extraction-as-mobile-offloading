# Image text extraction as mobile offloading

Extracting text from images using Docker containers. Offloading from an Android device. Project for the course "Mobile cloud computing", taken at Aalto University in 2016.

Authors: Martin Borek, Fabiano Paiva Brito, Juho Kokko, Ivan Shabunin


## How to start

The application can be deployed with either of the following commands, run in the project root:

```sh
sudo ./deploy.sh
```
or
```sh
sudo bash deploy.sh
```

The deployment script will build the Android application as an APK file, which can then be installed to a device.
The APK file is saved to _TOCR-mobileUI/app/build/outputs/apk/app-release.apk_.

The script will also deploy the backend as a clustered application in Google Container Engine, under the project _mcc-2016-g13-p2_.
By default, the script deploys 3 backend application pods and 3 database pods.

Superuser permissions are required for the deployment. Note: Google Cloud SDK, Docker and Kubernetes are set up as the 
root user by the script. Thus, all following commands related to these features must be run with _sudo_, to be able to 
access needed configuration files.

### Login credentials for the application:

You can either authenticate with an internal username and password or using any Facebook account.
The precreated internal users are listed in the table below.

| Username  | Password  |
|-----------|-----------|
| test1     | secret1   |
| test2     | secret2   |
| test3     | secret3   |

### Re-deploying & removing backend

To re-deploy the backend in Google Cloud, the previous deployment must be removed. This can be done with the following commands:

```sh
cd server
sudo ./undeploy.sh
```

## Extras
Besides the Android application fully working according to the project description, we have implemented two of the proposed challenges:
* Facebook authentication.
* Context-awareness, in case the user loses the internet connection after login.

## In this repository you will find

### Server
The backend server is implemented with Python 3, using Tornado as the HTTP server framework. The main backend application is in _server.py_, 
and some helper functions are in _helpers.py_ and _db_safe.py_.

The backend runs in a Docker container, which is defined in _Dockerfile_. The container uses the official Python image. 
The Docker container is deployed in a Kubernetes cluster on Google Container Engine. The cluster components are defined 
in _server/cluster/backend.yaml_.

The backend uses MongoDB as a database. The MongoDB cluster deployment functionality is in _server/cluster/sidecar_.
The Makefile and .yaml definitions have been copied and slightly altered from https://github.com/cvallance/mongo-k8s-sidecar.
The official MongoDB image is used in the cluster containers, along with the mongo-k8s-sidecar image, which controls 
the replica set.

The certificate files used for HTTPS communication are stored in _server/cert_ and the key used for accessing the Google 
Cloud project is in _server/key_

### TOCR-mobileUI
This is where the Android application source code is. Under _app/src/main/java/com/temerarious/mccocr13/temerariousocr/_ 
you can find the classes. They are separated in four groups: *activities*, with the actual activities of the application; *fragments*, 
basically with the fragment to handle the Facebook authentication; *helpers*, with several classes and methods to support the 
activities; and *tasks*, with the local and async tasks to run the OCR and communicate with the server.

### Documentation
In this folder you can find the file _cluster_commands.txt_, which describes some commands for controlling the cluster, 
and the file _remote_ocr_processing.txt_ with detailed information on how some of the REST communication works in this application.
