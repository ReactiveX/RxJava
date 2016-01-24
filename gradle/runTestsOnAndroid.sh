#!/bin/bash
echo no | eval "android create avd --force -n test -t $TARGET_PLATFORM"
emulator -avd test -no-skin -no-audio -no-window &
android-wait-for-emulator
adb shell input keyevent 82
./gradlew connectedAndroidTest