package org.janelia.jos.mongo;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MongoConfiguration {

    @NotEmpty
    public String host = "localhost";

    @Min(1)
    @Max(65535)
    public int port = 27017;

    @NotEmpty
    public String database;

    public String username;
    public String password;

    public String getHost() {
        return host;
    }

    @JsonProperty 
    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    @JsonProperty 
    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    @JsonProperty 
    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    @JsonProperty 
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @JsonProperty 
    public void setPassword(String password) {
        this.password = password;
    }
}
