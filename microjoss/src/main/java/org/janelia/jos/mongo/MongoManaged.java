package org.janelia.jos.mongo;

import io.dropwizard.lifecycle.Managed;

import java.util.Arrays;

import org.jongo.Jongo;
import org.jongo.MongoCollection;
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
    
    private MongoConfiguration config;
    private MongoClient m;
    private DB db;
    private Jongo jongo;
    
    public MongoManaged(MongoConfiguration config) {
        this.config = config;
    }

    public void start() throws Exception {
        String[] members = config.host.split(",");
        ServerAddress[] replicaUrls = new ServerAddress[members.length];
        for (int i=0; i<members.length; i++) {
            replicaUrls[i] = new ServerAddress(members[i] + ":"+config.port);
        }
        String username = config.username;
        String password = config.password;
        String databaseName = config.database;
        
        if (username != null && password != null) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(username, databaseName, password.toCharArray());
            m = new MongoClient(Arrays.asList(replicaUrls), Arrays.asList(credential));
            log.info("Connected to MongoDB (" + databaseName + "@" + replicaUrls + ") as user " + username);
        } 
        else {
            m = new MongoClient(Arrays.asList(replicaUrls));
            log.info("Connected to MongoDB (" + databaseName + "@" + replicaUrls + ")");
        }

        this.db = m.getDB(databaseName);
        
        this.jongo = new Jongo(db, 
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

    public MongoCollection getObjectCollection() {
        String collectionName = (config.getCollection()==null)?"object":config.getCollection();
        return jongo.getCollection(collectionName);
    }
}
