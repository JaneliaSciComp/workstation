# Development

## Adding a Module

To add a module, create a new directory for your module under modules/ and create the following files:

* `pom.xml`
* `src/main/nbm/manifest.mf`
* `src/main/resources/org/janelia/workstation/<module>/Bundle.properties`

You can follow any of the existing modules as a guide for the content of these files. Make sure to change the module's **name** and **artifact** in `pom.xml`!

Next, edit `pom.xml` at the top-level and add your module to the `<modules>` section at the bottom. This will make it part of the build.

Finally, edit the `pom.xml` for one or more applications (e.g. `modules/application/pom.xml` or `modules/application_horta/pom.xml`), and add your module as a dependency. This will include it into the default configuration for that application. 

## IntelliJ Debugging

There are many ways to set up 
[debugging in IntelliJ](https://www.jetbrains.com/help/idea/attaching-to-local-process.html). Here's one way, where you can run the Workstation normally each time and add a debugger if you need it:

1) In your Run Configuration for the Workstation, under the "Runner" tab in "VM Options" add this: 

    ```"-Dnetbeans.run.params.debug=-J-Xdebug -J-Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=5005"```
    
    This will run a JDWP server each time you run the Workstation.
    
2)  Create a new Run Configuration of type "Remote" and set it to mode "Attach to remote JVM". The host and port should default to localhost:5005, as we configured above. Running this will begin debugging the Workstation session. 

3) Run the Workstation as normal, and any time you want to debug, just run the debugger configuration after starting the Workstation.

An alternative would be to run a persistent debugger server and set up the Workstation to connect to it each time you run it in "debug mode":

1) Create a Run Configuration of type "Remote" and set it to "Listen to remote VM". Make note of the *address* that is generated.

2) Create a copy of your Run Configuration for the Workstation, name it something like "Debug Workstation", and under the "Runner" tab in "VM Options" add this, filling in the `<address>` with the *address* you noted earlier: 

    ```"-Dnetbeans.run.params.debug=-J-Xdebug -J-Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=<address>"```
    
 3) Now you can run the debugger process once, and then choose when starting the Workstation whether or run it normally, or in debug mode. Note that (somewhat confusingly) you must use the regular run arrow when running the Workstation in debug mode, not the normal debug icon.
 