# Janelia Workstation

[![CircleCI](https://circleci.com/gh/JaneliaSciComp/workstation.svg?style=svg)](https://circleci.com/gh/JaneliaSciComp/workstation)

A neuroscience discovery platform that supporting processing, analysis, and annotation of large-scale 3d microscopy data. The aim of the Workstation project is to create a common framework for interaction among scientists working in different labs, and studying different organisms with diverse modalities and computational methods.

The Janelia Workstation currently supports two large-scale team projects at Janelia Research Campus: **FlyLight** and **MouseLight**:
* The **MouseLight** tools have been fully open sourced and made available here with complete documentation. These tools enable neuron tracing and connectomics on terabyte-scale Mouse brain volumes. 
* The **FlyLight** tools have been partially open sourced, and not yet officially supported. At Janelia, these tools will support search, browsing, and annotation of millions of confocal-imaged Fly nervous systems. In the future they will be fully released here.

## For Users

Read the [User Manual](docs/UserManual.md) to find out more about the Workstation's capabilities and how to use them.

## For System Administrators

### System Installation

The Workstation Client (this repository) is supported by a suite of back-end services. Deploying all of these services is accomplished through the use of Docker containers. Complete documentation about building and deploying the entire system is available in the [jacs-cm](https://github.com/JaneliaSciComp/jacs-cm) repository.

### MouseLight Deployment

The canonical two-server deployment of the MouseLight neuron tracing tools is described [here](https://github.com/JaneliaSciComp/jacs-cm/blob/master/docs/MouseLightDeployment.md). 

## For Developers

### Workstation Client Build 

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

