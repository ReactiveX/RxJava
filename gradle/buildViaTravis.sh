#!/bin/bash
# This script will build the project.

echo -e 'Build Script => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'

./gradlew -Prelease.useLastTag=true build
