#!/bin/bash

SCRIPTS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ "$TARGET_PLATFORM" == "jdk" ]; then
    sh "${SCRIPTS_DIR}/buildViaTravis.sh"
elif [[ "$TARGET_PLATFORM" == android-* ]]; then
    sh "${SCRIPTS_DIR}/runTestsOnAndroid.sh"
fi