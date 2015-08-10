package org.janelia.it.workstation.gui.browser.api;

import java.net.UnknownHostException;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Singleton for managing the Domain DAO. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgr {
    
    // Singleton
    private static final DomainMgr instance = new DomainMgr();
    public static DomainMgr getDomainMgr() {
        return instance;
    }
    
    protected static final String MONGO_SERVER_URL = "mongo-db";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "flyportal";
    protected static final String MONGO_PASSWORD = "flyportal";
    
    
    private DomainDAO dao;
    
    private DomainMgr() {
    
    }
    
    
    public DomainDAO getDao() {
        if (dao == null) {
            try {
                dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
            }
            catch (UnknownHostException e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        return dao;
    }
    

}
