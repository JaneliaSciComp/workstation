Janelia Object Store Service (JOSS)

![Serenity](http://i.imgur.com/vnHE9Xm.jpg)

JOSS is a file/object management system with Scality as the storage backend. It exposes a REST API for streaming data into and out of Scality, and manages asynchronous deletion of objects. It also keeps track of usage statistics and provides space usage reports.

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

-----
Usage
-----

This guide assumes that you are using the production JOSS instance. If you deploy JOSS locally, you'll need to modify the URLs below to use localhost instead of jacs-joss.

REST Documentation
* http://jacs-joss.int.janelia.org:8080/swagger

Space Usage Report
* http://jacs-joss.int.janelia.org:8080/jos/report/usage/owner

API Usage with CURL

Upload a file
    $ curl -v -u tester:jos -X PUT http://jacs-joss:8080/jos/object/test/README.md --data-binary @README.md

Download a file
    $ curl -v -u tester:jos -X GET http://jacs-joss:8080/jos/object/test/README.md -o README.md.download
    $ md5sum README.md*
    a0e45253c49423e4ac8ccc1c0566db47  README.md
    a0e45253c49423e4ac8ccc1c0566db47  README.md.download

Get metadata as HTTP headers
    $ curl -u tester:jos -I http://jacs-joss:8080/jos/object/test/README.md
    HTTP/1.1 204 No Content
    Date: Mon, 20 Jul 2015 21:25:13 GMT
    X-Joss-Name: README.md
    X-Joss-Owner: tester
    X-Joss-Size: 683
    Content-Type: application/octet-stream

Get metadata as JSON
    $ curl -u tester:jos -X GET http://jacs-joss:8080/jos/metadata/test/README.md
    {"_id":"55ad66aae4b0a422fd85610b","parentPath":"test","path":"test/README.md","name":"README.md","fileType":"md","owner":"tester","numBytes":"683","bzipped":false,"deleted":false}

Delete the file
    $ curl -v -u tester:jos -X DELETE http://jacs-joss:8080/jos/object/test/README.md
    ...
    < HTTP/1.1 204 No Content

