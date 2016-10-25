#!/bin/bash
BASEDIR=$(dirname "$0")
cd $BASEDIR
java -cp DataBrowserModule/build/classes org.janelia.it.workstation.browser.util.VersionNumberIncrementer $@

