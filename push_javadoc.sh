#!/bin/bash
# ----------------------------------------------------------
# Automatically push back the generated JavaDocs to gh-pages
# ----------------------------------------------------------
# based on https://gist.github.com/willprice/e07efd73fb7f13f917ea

# specify the common address for the repository
targetRepo=github.com/ReactiveX/RxJava.git
# =======================================================================

# get the current build tag if any
buildTag="$BUILD_TAG"
echo -e "Build tag: '$buildTag'"

if [ "$buildTag" == "" ]; then
   buildTag="snapshot"
else
   buildTag="${buildTag:1}"
fi

echo -e "JavaDocs pushback for tag: $buildTag"

# check if the token is actually there
if [ "$JAVADOCS_TOKEN" == "" ]; then
	echo -e "No access to GitHub, skipping JavaDocs pushback."
	exit 0
fi

# prepare the git information
git config --global user.email "akarnokd+ci@gmail.com"
git config --global user.name "akarnokd+ci"

# setup the remote
echo -e "Adding the target repository to git"
git remote add origin-pages https://${JAVADOCS_TOKEN}@${targetRepo} > /dev/null 2>&1

# stash changes due to chmod
echo -e "Stashing any local non-ignored changes"
git stash

# get the gh-pages
echo -e "Update branches and checking out gh-pages"
git fetch --all
git branch -a
git checkout -b gh-pages origin-pages/gh-pages

# releases should update 2 extra locations
if [ "$buildTag" != "snapshot" ]; then
	# for releases, add a new directory with the new version
	# and carefully replace the others
	
	# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    # enable once 3.x is mainstream
    # vvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    
	# 1.) main javadoc
	# ----------------
	# remove the io subdir
    #echo -e "Removing javadoc/io" 
	#rm -r javadoc/io

	# remove the html files
    #echo -e "Removing javadoc/*.html" 
	#rm javadoc/*.html

	# copy the new doc
    #echo -e "Copying to javadoc/" 
	#yes | cp -rf ./build/docs/javadoc/ .

    # ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    # enable once 3.x is mainstream
    # !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	
	# 2.) 3.x javadoc
	# remove the io subdir
    echo -e "Removing 3.x/javadoc/io" 
	rm -r 3.x/javadoc/io

	# remove the html files
    echo -e "Removing 3.x/javadoc/*.html" 
	rm 3.x/javadoc/*.html

	# copy the new doc
    echo -e "Copying to 3.x/javadoc/" 
	yes | cp -rf ./build/docs/javadoc/ 3.x/
fi

# 3.) create a version/snapshot specific copy of the docs
# clear the existing tag
echo -e "Removing to 3.x/javadoc/${buildTag}" 
rm -r 3.x/javadoc/${buildTag}

# copy the new doc
echo -e "Copying to 3.x/javadoc/${buildTag}" 
yes | cp -rf ./build/docs/javadoc/ 3.x/javadoc/${buildTag}/


# stage all changed and new files
echo -e "Staging new files" 
git add *.html
git add *.css
git add *.js
git add *package-list*

# remove tracked but deleted files
echo -e "Removing deleted files"
git add -u

# commit all
echo -e "commit CI build: $CI_BUILD_NUMBER for $buildTag"
git commit --message "CI build: $CI_BUILD_NUMBER for $buildTag"

# debug file list
#find -name "*.html"

# push it
echo -e "Pushing back changes."
git push --quiet --set-upstream origin-pages gh-pages


# we are done
echo -e "JavaDocs pushback complete."