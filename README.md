# Janelia Workstation

## Build instructions

Create a keystore:
```
keytool -genkey -validity 360 -storepass <password> -alias janeliaws -keystore private/keystore -dname "C=US, ST=VA, L=Ashburn, O=Janelia, CN=localhost"
```

Build all modules:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -Dkeystorepass=<password> clean install
```

Run application:
```
cd modules/application
nbm:cluster-app nbm:run-platform
```

Build installers and update center:
```
cd modules/application
mvn --batch-mode -T 8 -Djava.awt.headless=true -Dkeystorepass=<password> package -P deployment
```

