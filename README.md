# Setting up the development environment

To build the application you can either install gradle locally on your machine using the appropriate package manager for your OS
(brew or macports for OSX, yum for Centos based Linux distros, apt-get for Debian based Linux distros) or use gradle scripts packaged
with the project.

Because the build is configure to run the integration tests as part of the full build you also need access to a MySQL database and
to a Mongo database.

## Setup MySQL test database.

1. Start mysql as root or as a user that has admin privileges
`mysql -u root -p`
2. Create the test database and the test user.
```
create database jacs2_test;
create user jacs2 identified by 'jacs2';
grant all on jacs2.* to 'jacs2' identified by 'jacs2';
```

## Setup MongoDB

For mongo you really don't have to do anything else because the tests will create the needed databases and the collections as long as the user
has prvileges to do so.

## Building and running the application

### Building the application

`gradle build`
or
`./gradlew build`

### Running only the integration tests

`gradle integrationTest`

### Package the application

`gradle installDist`

Note that 'installDist' target will not run any tests or unit tests.

### Run the application

`jacs2-web/build/install/jacs2-web/bin/jacs2-web`

If you want to debug the application you can start the application with the debug agent as below:

`JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" jacs2-web/build/install/jacs2-web/bin/jacs2-web`

The application uses a default configuration that is packaged in the applications' jar but this can be overwritten with your own settings
by simply creating a java properties file in which you overwrite the settings that you want and setup an environment variable JACS2_CONFIG to
reference that file, e.g.

`JACS2_CONFIG=/usr/local/etc/myjacs2-config.properties jacs2-web/build/install/jacs2-web/bin/jacs2-web`
