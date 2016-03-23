package org.janelia.it.workstation.gui.browser.api.facade.impl.mongo;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.domain.support.DomainDAO;

/**
 * Manages the DomainDAO, and therefore the Mongo client. The MongoClient is thread-safe and is meant to 
 * be shared among threads in a single JVM. It does it's own connection pooling, so a singleton is fine here.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainDAOManager {

    private static final Logger logger = Logger.getLogger(DomainDAOManager.class);
    
//    private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
//    private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
//    private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
//    private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");
    protected static final String MONGO_SERVER_URL = "dev-mongodb";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "";
    protected static final String MONGO_PASSWORD = "";
    
    private static DomainDAOManager instance;

    protected DomainDAO dao;
    
    private DomainDAOManager() {
        try {
            this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
        }
        catch (UnknownHostException e) {
            logger.error("Error connecting to Mongo",e);
        }
    }
    
    public static DomainDAOManager getInstance() {
        if (instance==null) {
            instance = new DomainDAOManager();
        }
        return instance;
    }

    public DomainDAO getDao() {
        return dao;
    }
}
