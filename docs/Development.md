# Development

The Janelia Workstation is built on top of the [Apache NetBeans Platform](https://netbeans.apache.org/kb/docs/platform/). It is recommended for developers to get familiar with the concepts used in this framework before diving into Workstation development. The best starting point is _The Definitive Guide to NetBeans Platform_ by Heiko Bock. 

## Building

To build the Janelia Workstation application for Janelia Research Campus, use the `janeliaws` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -P janeliaws clean install
```

To build the Janelia HortaCloud application, use the `horta` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -P horta clean install
```

## Running 

To run the Janelia Workstation application, use the `janeliaws` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -P janeliaws nbm:cluster-app nbm:run-platform
```

To run the Janelia HortaCloud application, use the `horta` profile:
```
mvn --batch-mode -T 8 -Djava.awt.headless=true -P horta nbm:cluster-app nbm:run-platform
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
