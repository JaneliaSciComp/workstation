Janelia Object Store Service (JOSS)

![Serenity](https://raw.github.com/JaneliaSciComp/janelia-workstation/master/images/serenity.jpg)

JOSS is a file/object management system with Scality as the storage backend. It exposes a REST API
for streaming data into and out of Scality, and manages asynchronous deletion of objects. 

It also keeps track of usage statistics and provides space usage reports.

-----
Build
-----

Just run Maven: 

    mvn package

----------
Deployment
----------

JOSS runs on the jacs-joss server, and requires Sproxyd to be running on the same server.

To deploy the latest jar file, run this and enter the jboss user password:

    scp target/microjoss-<VERSION>.jar jboss@jacs-joss:/opt/joss/microjoss.jar

