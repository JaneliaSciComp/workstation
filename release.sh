#!/bin/sh
#
# Production release script for creating a new release of the Workstation client.
#

# Exit after any error
set -o errexit

JWVER=$1
LOCAL=$2

if [ "$JWVER" == "" ]; then
    echo "Usage: release.sh <version> [--local]"
    echo "  If --local is enabled, no changes are pushed to the remote"
    exit 1
fi

CURR_BRANCH=`git rev-parse --abbrev-ref HEAD`
if [ "$CURR_BRANCH" != "master" ]; then
    echo "WARNING: Creating release from non-master branch: $CURR_BRANCH"
fi

if [[ `git status --porcelain` ]]; then
    echo "You have local changes. Clean them up before releasing."
    exit 1
fi

UNPUSHED=`git log origin/$CURR_BRANCH..$CURR_BRANCH`
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

MOD_PROP="janeliaws.modules.specification.version"
MOD_VER=`cat pom.xml | grep "$MOD_PROP" | awk -F '[<>]' '{print $3}'`
MOD_MAJOR=`echo $MOD_VER | awk 'BEGIN {FS="."}{print $1}'`
MOD_MINOR=`echo $MOD_VER | awk 'BEGIN {FS="."}{print $2}'`
NEW_MOD="${MOD_MAJOR}."`echo $MOD_MINOR + 1 | bc -l`

echo "Incrementing module version to $NEW_MOD"
mvn versions:set-property -Dproperty=$MOD_PROP -DnewVersion=$NEW_MOD -DgenerateBackupPoms=false
git commit -a -m "Updated module version to ${NEW_MOD}"

echo "Creating git tag for ${JWVER}"
git tag ${JWVER}

if [[ "$LOCAL" == "--local" ]]; then
    echo "Skipping remote push due to --local flag"
    exit 0
fi

echo "Pushing to Github..."
git push origin
git push origin ${JWVER}

echo ""
echo "Release $JWVER is ready to build"
echo ""

