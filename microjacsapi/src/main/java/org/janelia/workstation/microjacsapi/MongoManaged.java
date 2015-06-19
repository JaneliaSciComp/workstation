package org.janelia.workstation.microjacsapi;

import io.dropwizard.lifecycle.Managed;

import java.util.Arrays;

import org.jongo.Jongo;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MapperFeature;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class MongoManaged implements Managed {
    
    private static final Logger log = LoggerFactory.getLogger(MongoManaged.class);
    
    private MicroJACSConfiguration configuration;
    private MongoClient m;
    private DB db;
    private Jongo jongo;
    
    public MongoManaged(MicroJACSConfiguration configuration) {
        this.configuration = configuration;
    }

    public void start() throws Exception {

        String serverUrl = configuration.mongoHost+":"+configuration.mongoPort;
        String username = configuration.mongoUsername;
        String password = configuration.mongoPassword;
        String databaseName = configuration.mongoDatabase;
        
        if (username != null && password != null) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
            m = new MongoClient(new ServerAddress(serverUrl), Arrays.asList(credential));
            log.info("Connected to MongoDB (" + databaseName + "@" + serverUrl + ") as user " + username);
        } 
        else {
            m = new MongoClient(serverUrl);
            log.info("Connected to MongoDB (" + databaseName + "@" + serverUrl + ")");
        }

        this.db = m.getDB(configuration.mongoDatabase);;
        
        this.jongo = new Jongo(m.getDB(databaseName), 
                new JacksonMapper.Builder()
                    .enable(MapperFeature.AUTO_DETECT_GETTERS)
                    .enable(MapperFeature.AUTO_DETECT_SETTERS)
                    .build());
    }

    public void stop() throws Exception {
        m.close();
    }
    
    public MongoClient getClient() {
        return m;
    }
    
    public DB getDatabase() {
        return db;
    }
    
    public Jongo getJongo() {
        return jongo;
    }
}
