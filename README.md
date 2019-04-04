# Janelia Workstation

[![CircleCI](https://circleci.com/gh/JaneliaSciComp/workstation.svg?style=svg)](https://circleci.com/gh/JaneliaSciComp/workstation)

A neuroscience discovery platform that supporting processing, analysis, and annotation of large-scale 3d microscopy data.

## System Installation

The Workstation Client (this repository) is supported by a suite of back-end services. Deploying these services is accomplished through the use of Docker containers. Complete documentation about building and deploying the entire system is available in the [jacs-cm](https://github.com/JaneliaSciComp/jacs-cm) repository.

### MouseLight Deployment

The canonical two-server deployment of the MouseLight neuron tracing tools is described [here](https://github.com/JaneliaSciComp/jacs-cm/blob/master/docs/MouseLightDeployment.md). 

## Workstation Client Build 

Create a keystore:
```
mkdir private
keytool -genkey -v -noprompt -validity 360 -storepass <password> -keypass <password> -alias janeliaws \
    -keystore private/keystore -dname "C=US, ST=VA, L=Ashburn, O=Janelia, CN=localhost"
```

Build all modules:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -Dkeystorepass=<password> clean install
```

Run application:
```
mvn -f modules/application/pom.xml nbm:cluster-app nbm:run-platform
```

Build installers and update center:
```
cd modules/application
mvn --batch-mode -T 8 -Djava.awt.headless=true -Dkeystorepass=<password> package -P deployment
```


