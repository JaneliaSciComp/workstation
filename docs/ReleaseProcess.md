# Release Process

The Workstation client is built and distributed using the [Apache NetBeans Platform](https://netbeans.apache.org/kb/docs/platform/). To create a release, ensure you are on the master branch, with all code checked in, then run the release script:

    ./release.sh <version>

The version number should be in [Semantic Versioning](https://semver.org/) style. This release process automatically increments all module versions, and sets the overall Workstation version. You can then proceed over to the [jacs-cm](https://github.com/JaneliaSciComp/jacs-cm) repository to rebuild the workstation-site container using your newly released Workstation version.

## Docker Build

The easiest way to distribute the Workstation is to use the [Docker Container](https://hub.docker.com/r/janeliascicomp/workstation-site). Running this container serves a static website where you can download the Workstation installer, and the Update Center for update distribution. You can build your own customized container by using the [jacs-cm](https://github.com/JaneliaSciComp/jacs-cm) tools. 

## Manual Release 

The release process above automates several things that can be run manually. These are documented below for completeness, but shouldn't be necessary for a normal release.

### Module Versioning

The modules are versioned with the NetBeans module system, which allows for updates of existing clients through the NetBeans Update Center mechanism.

To increment the global module version, edit the main pom.xml and increment **janeliaws.modules.specification.version**. Most modules use this version and will automatically updated for all users when this new version is deployed.

Some modules are rarely updated, and those are version pinned and need to changed independently. For example, to release a new version of the DarculaLAF module (normally pinned), edit ./modules/DarculaLAF/src/main/nbm/manifest.mf, and increment **OpenIDE-Module-Specification-Version**.


### Workstation Version

To increment the overall version of the Workstation artifacts, we use Maven. For example, to set the version to 9.0:
```
mvn versions:set -DnewVersion=9.0 -DgenerateBackupPoms=false
```

This version isn't actually used in the built application, but should be maintained anyway because it creates a version history of artifacts within Maven.


## Git Version Tagging

Tag the code base with your version number:
```
git tag 9.0
git push origin 9.0
```

Now you can use this version number to build and deploy the **workstation-site** container with the jacs-cm tools.

## Manual Production Build

The build can also be done manually, as follows:

Create a file ./modules/Core/src/main/resources/my.properties with the version number you want to display in the client, e.g.
```
client.versionNumber=8.0
```

Create a keystore with a self-signed certificate:
```
mkdir private
keytool -genkey -v -noprompt -validity 360 -storepass <password> -keypass <password> -alias janeliaws \
    -keystore private/keystore -dname "C=US, ST=VA, L=Ashburn, O=Janelia, CN=localhost"
```
Alternatively, you can use an existing certificate by following the steps in the Dockerfile. 

Build all modules:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -Dkeystorepass=<password> clean install
```

Build installers and update center:
```
mvn -f modules/applications/pom.xml --batch-mode -T 8 -Djava.awt.headless=true -Dkeystorepass=<password> package -P deployment
```

Once the build is complete, the installers and update center will be available under the **modules/application/target** directory.
