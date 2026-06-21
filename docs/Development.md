# Development

The Janelia Workstation is built on top of the [Apache NetBeans Platform](https://netbeans.apache.org/kb/docs/platform/). It is recommended for developers to get familiar with the concepts used in this framework before diving into Workstation development. The best starting point is _The Definitive Guide to NetBeans Platform_ by Heiko Bock. 

## Requirements

Use JDK 21 or newer to build, run, and package the Workstation. The project is built against Apache NetBeans Platform RELEASE300 and compiles with `--release 21`, so generated classes target the Java 21 runtime level.

The Maven enforcer accepts JDK 21 or newer. If multiple JDKs are installed, set `JAVA_HOME` to the JDK version you want Maven and `jpackage` to use.

## Building

To build the Janelia Workstation application for Janelia Research Campus, use the `janeliaws` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -P janeliaws clean install
```

To build the Janelia HortaCloud application, use the `horta` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -P horta clean install
```

To build the full project without running tests:
```
mvn -DskipTests compile
```

### Packaging

Native application images and installers are built with `jpackage`.

Run package builds with JDK 21 or newer. For example, on macOS, to select JDK 21 explicitly:

```
JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

To build the Janelia Workstation application image:
```
mvn -Pjaneliaws,deployment -pl modules/application -am -DskipTests -Djpackage.type=APP_IMAGE package
```

The generated application image is written under:
```
modules/application/target/jpackage/
```

To build the Horta application image:
```
mvn -Phorta,deployment -pl modules/application_horta -am -DskipTests -Djpackage.type=APP_IMAGE package
```

The generated application image is written under:
```
modules/application_horta/target/jpackage/
```

To build a native installer instead of an application image, change `jpackage.type` to the package type for the target operating system:
```
# macOS
-Djpackage.type=DMG
-Djpackage.type=PKG

# Windows
-Djpackage.type=MSI
-Djpackage.type=EXE

# Linux
-Djpackage.type=DEB
```

Native packages must be built on the target operating system. For example, build `DMG` or `PKG` on macOS, `MSI` or `EXE` on Windows, and `DEB` on Linux.

## Running 

To run the Janelia Workstation application, use the `janeliaws` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -f modules/application/pom.xml -P janeliaws nbm:run-platform
```

To run the Janelia HortaCloud application, use the `horta` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -f modules/application_horta/pom.xml  -P horta nbm:run-platform
```

## Debugging in IntelliJ

There are many ways to set up 
[debugging in IntelliJ](https://www.jetbrains.com/help/idea/attaching-to-local-process.html). Here's one way, where you can run the Workstation normally each time and add a debugger if you need it:

1) In your Run Configuration for the Workstation, under the "Runner" tab in "VM Options" add this: 

    ```"-Dnetbeans.run.params.debug=-J-Xdebug -J-Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=5005"```
    
    This will run a JDWP server each time you run the Workstation.
    
2)  Create a new Run Configuration of type "Remote" and set it to mode "Attach to remote JVM". The host and port should default to localhost:5005, as we configured above. Running this will begin debugging the Workstation session. 

3) Run the Workstation as normal, and any time you want to debug, just run the debugger configuration after starting the Workstation.

## Adding a Module

To add a module, create a new directory for your module under modules/ and create the following files:

* `pom.xml`
* `src/main/nbm/manifest.mf`
* `src/main/resources/org/janelia/workstation/<module>/Bundle.properties`

You can follow any of the existing modules as a guide for the content of these files. Make sure to change the module's **name** and **artifact** in `pom.xml`.

Next, edit `pom.xml` at the top-level and add your module to the `<modules>` section at the bottom. This will make it part of the build.

Finally, edit the `pom.xml` for one or more applications (e.g. `modules/application/pom.xml` or `modules/application_horta/pom.xml`), and add your module as a dependency. This will include it into the default configuration for that application. 

## DPI Settings in Windows 10+

If you set higher DPI ("Make everything bigger") in Windows options, sometimes this setting is ignored by the Workstation, especially during development. One workaround is to find your java.exe, right-click it and select Properties, then click on the Compatibility tab, and click "Change high DPI settings". In the second dialog, select the "Override high DPI scaling behavior" checkbox and select "System" below that.  
