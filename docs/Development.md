# Development

## Adding a Module

To add a module, create a new directory for your module under modules/ and create the following files:

* `pom.xml`
* `src/main/nbm/manifest.mf`
* `src/main/resources/org/janelia/workstation/<module>/Bundle.properties`

You can follow any of the existing modules as a guide for the content of these files. Make sure to change the module's **name** and **artifact** in `pom.xml`!

Next, edit `pom.xml` at the top-level and add your module to the `<modules>` section at the bottom. This will make it part of the build.

Finally, edit the `pom.xml` for one or more applications (e.g. `modules/application/pom.xml` or `modules/application_horta/pom.xml`), and add your module as a dependency. This will include it into the default configuration for that application. 

