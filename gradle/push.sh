#!/bin/bash
# ----------------------------------------------------------
# Automatically push back the generated JavaDocs to gh-pages
# ----------------------------------------------------------
# based on https://gist.github.com/willprice/e07efd73fb7f13f917ea

# specify the common address for the repository
targetRepo=github.com/ReactiveX/RxJava.git
# =======================================================================

# only for main pushes, for now
 if [ "$TRAVIS_PULL_REQUEST" == "true" ]; then
	echo -e "Pull request detected, skipping JavaDocs pushback."
	exit 0
fi

// get the current build tag if any
buildTag="$TRAVIS_TAG"

if [ "$buildTag" == ""]; then
   buildTag = "snapshot"
else
   buildTag = "${buildTag:1}"
fi

echo -e "JavaDocs pushback for tag: $buildTag"

# check if the token is actually there
if [ "$GITHUB_TOKEN" == "" ]; then
	echo -e "No access to GitHub, skipping JavaDocs pushback."
	exit 0
fi

# prepare the git information
git config --global user.email "travis@travis-ci.org"
git config --global user.name "Travis CI"

# setup the remote
git remote add origin-pages https://${GITHUB_TOKEN}@${targetRepo} > /dev/null 2>&1

# stash changes due to chmod
git stash

# get the gh-pages
git fetch --all
git branch -a
git checkout -b gh-pages origin-pages/gh-pages

# for snapshots, only replace the 2.x/javadoc/snapshot directory
#if [ "$buildTag" != "snapshot" ]; then
# for now
if [ "$buildTag" != "" ]; then
	# for releases, add a new directory with the new version
	# and carefully replace the others
	
	# 1.) main javadoc
	# ----------------
	# remove the io subdir
	rm -r javadoc/io
	# remove the html files
	rm javadoc/*.html
	# copy the new doc
	yes | cp -rf ./build/docs/javadoc/ javadoc/
	
	# 2.) 2.x javadoc
	# remove the io subdir
	rm -r 2.x/javadoc/io
	# remove the html files
	rm 2.x/javadoc/*.html
	# copy the new doc
	yes | cp -rf ./build/docs/javadoc/ 2.x/javadoc/
fi

# 3.) create a version/snapshot specific copy of the docs
# clear the existing tag
rm -r 2.x/javadoc/${buildTag}
# copy the new doc
yes | cp -rf ./build/docs/javadoc/ 2.x/javadoc/${buildTag}/


# stage all changed and new files
git add *.html
git add *.css
git add *.js
git add javadoc/package-list

# remove tracked but deleted files
git add -u

# commit all
git commit --message "Travis build: $TRAVIS_BUILD_NUMBER for $buildTag"


# push it
#git push --quiet --set-upstream origin-pages gh-pages
# just print the result for now
find .

# we are done
