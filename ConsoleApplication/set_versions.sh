#!/bin/sh

if [ "$(uname)" == "Darwin" ]; then
    find . -name manifest.mf -exec sed -i '' "s/\(OpenIDE-Module-Specification-Version: \).*$/\1${1}/" {} \;
else
    find . -name manifest.mf -exec sed -i "s/\(OpenIDE-Module-Specification-Version: \).*$/\1${1}/" {} \;
fi

