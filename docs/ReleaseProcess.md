# Release Process

## Module Versioning

The modules are versioned with the NetBeans module system, which allows for autoupdates of existing clients.

To increment the global module version, edit the main pom.xml and increment **janeliaws.modules.specification.version**. Most modules use this version and will automatically updated for all users when this new version is deployed.

Some modules are rarely updated, and those are version pinned and need to changed independently. For example, to release a new version of the DarculaLAF module (normally pinned), edit ./modules/DarculaLAF/src/main/nbm/manifest.mf, and increment **OpenIDE-Module-Specification-Version**.

## Workstation Version

To increment the overall version of the Workstation, you can use Maven, For example, to set the version to 9.0:
```
mvn versions:set -DnewVersion=8.0 -DgenerateBackupPoms=false
```

This version isn't actually used for anything currently, but should be maintained anyway because it creates a version history within Maven.

## Production Release

To release the code to production, first tag it remotely with a version number:
```
git tag 8.0-RC8
git push origin 8.0-RC8
```

Now you can use this version number to build and deploy the **workstation-site** container with the jacs-cm tools.

