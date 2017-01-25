# Setting up the development environment

To build the application you can either install gradle locally on your machine using the appropriate package manager for your OS
(brew or macports for OSX, yum for Centos based Linux distros, apt-get for Debian based Linux distros) or use gradle scripts packaged
with the project.

Because the build is configured to run the integration tests as part of the full build you also need to have access to a Mongo
database server. The default configuration expects the database server to be running on the development machine but it doesn't have to.

## Setup MongoDB

To install MongoDB on MacOS:

With Homebrew:
`brew install mongodb`

With macports:
`sudo port install mongodb`

On Centos based Linux distributions (Centos, Scientific Linux) you can use:
`yum install mongodb-org-server`

On Debian based Linux distributions (Debian, Ubuntu) you can use:
`sudo apt-get install mongodb-org`

Once MongoDB is installed on your machine you really don't have to do anything else because the tests will create the needed databases and
the collections as long as the user has prvileges to do so.

## Building and running the application

### Building the application

`gradle build`
or
`./gradlew build`

### Running only the integration tests

`./gradlew integrationTest`

If you want to use a different test database than the one running locally on your development machine you can create a configuration file
in which you override the database connection settings and then use JACS2_CONFIG_TEST environment variable to point to it, eg.,
`JACS2_CONFIG_TEST=/my/prefered/location/for/dev/my-config-test.properties ./gradlew integrationTest`

Keep in mind that since the integrationTests are configured to run as part of the build check stage you also need to have the environment variable
set you you run the build:
`JACS2_CONFIG_TEST=/my/prefered/location/for/dev/my-config-test.properties ./gradlew build`

For example my-config-test.properties could look as below if you want to use the dev mongo database. I recommend to prefix the database name with your
user name so that your tests will not clash with other users' tests in case the build runs simultaneously.
`
MongoDB.ConnectionURL=mongodb://dev-mongodb:27017
MongoDB.Database=myusername_jacs_test
`

and to build the application you simply run:

`JACS2_CONFIG_TEST=$PWD/my-config-test.properties ./gradlew clean build installDist`

### Package the application

`./gradlew installDist`

Note that 'installDist' target will not run any unit tests or integration tests.

### Run the application

To run with the default settings which assume a Mongo database instance running on the same machine where the web server is running:

`jacs2-web/build/install/jacs2-web/bin/jacs2-web`

If you want to debug the application you can start the application with the debug agent as below:

`JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" jacs2-web/build/install/jacs2-web/bin/jacs2-web`

The default settings could be overwritten with your own settings in a java properties file that contains only the updated properties
and then use JACS2_CONFIG environment variable to reference the settings file, e.g.

`JACS2_CONFIG=/usr/local/etc/myjacs2-config.properties jacs2-web/build/install/jacs2-web/bin/jacs2-web`


### Test the application

Here are a few service invocation examples:

* Generate sample MIPs and movies:
`
curl --request POST \
  --url http://goinac-ws1:8080/jacs/jacs-api/v2/async-services/getSampleMIPsAndMovies \
  --header 'acc: application/json' \
  --header 'content-type: application/json' \
  --data '{"processingLocation": "LOCAL", "args": ["-sampleId", "2230165384508473442" ,"-objective", "20x", "-sampleDataDir", "/home/goinac/Work/jacs-2/tt/missing"]}'
`

* Generate sample LSM metadata:

`
curl --request POST \
  --url http://goinac-ws1:8080/jacs/jacs-api/v2/async-services/getSampleLsmMetadata \
  --header 'acc: application/json' \
  --header 'content-type: application/json' \
  --data '{"processingLocation": "LOCAL", "args": ["-sampleId", "2230165384508473442" , "-objective", "20x", "-sampleDataDir", "/home/goinac/Work/jacs-2/tt/missing"]}'
`
