#!/bin/sh
#
# Production release script for creating a new release of the Workstation client.
#

# Exit after any error
set -o errexit

JWVER=$1

if [ "$JWVER" == "" ]; then
    echo "Usage: release.sh <version>"
    exit 1
fi

CURR_BRANCH=`git rev-parse --abbrev-ref HEAD`
if [ "$CURR_BRANCH" != "master" ]; then
    echo "You must have the master branch checked out in order to run this script."
    #exit 1
fi

UNPUSHED=`git log origin/master..master`
if [ ! -z "$UNPUSHED" ]; then
    echo "You must push all your commits before running this script."
    exit 1
fi

CMD="git ls-files -v | grep \"^[[:lower:]]\""
IGNORED=`$CMD`
if [ ! -z "$IGNORED" ]; then
    echo "Some files are ignored (assume unchanged) which could interfere with this build."
    exit 1
fi

echo "Pulling latest updates from Github..."
git pull

echo "Changing version numbers to ${JWVER}"
mvn versions:set -DnewVersion=${JWVER} -DgenerateBackupPoms=false
git commit -a -m "Updated version to ${JWVER}"

echo "Creating git tag for ${JWVER}"
git tag ${JWVER}

echo "Pushing to Github..."
git push origin
git push origin ${JWVER}

echo ""
echo "Release $JWVER is ready to build"
echo ""

