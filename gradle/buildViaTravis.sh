#!/bin/bash
# This script will build the project.

buildTag="$TRAVIS_TAG"

if [ "$buildTag" != "" ] && [ "${buildTag:0:3}" != "v3." ]; then
   echo -e "Wrong tag on the 3.x brach: $buildTag : build stopped"
   exit 1
fi

export GRADLE_OPTS=-Xmx1024m

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo -e "Build Pull Request #$TRAVIS_PULL_REQUEST => Branch [$TRAVIS_BRANCH]"
  ./gradlew -PreleaseMode=pr build --stacktrace
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" == "" ]; then
  if [ "$TRAVIS_BRANCH" != "3.x" ]; then
     echo -e 'Build secondary Branch (no snapshot) => Branch ['$TRAVIS_BRANCH']'
     ./gradlew -PreleaseMode=pr build --stacktrace
  else
     echo -e 'Build Branch with Snapshot => Branch ['$TRAVIS_BRANCH']'
     ./gradlew -PreleaseMode=branch -PbintrayUser="${bintrayUser}" -PbintrayKey="${bintrayKey}" -PsonatypeUsername="${sonatypeUsername}" -PsonatypePassword="${sonatypePassword}" build --stacktrace
  fi
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then
  echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
  ./gradlew -PreleaseMode=full -PbintrayUser="${bintrayUser}" -PbintrayKey="${bintrayKey}" -PsonatypeUsername="${sonatypeUsername}" -PsonatypePassword="${sonatypePassword}" build --stacktrace
else
  echo -e 'WARN: Should not be here => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']  Pull Request ['$TRAVIS_PULL_REQUEST']'
fi
