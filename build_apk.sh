#!/usr/bin/env bash

echo 'Starting to build the APK...'

cd TOCR-mobileUI/

./gradlew assembleRelease

RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo 'APK saved in TOCR-mobileUI/app/build/outputs/apk/app-release.apk.'
else
    echo 'ERROR: APK build failed.'
fi

