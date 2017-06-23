package org.janelia.it.workstation.browser.api.http;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.janelia.it.workstation.browser.api.http.pool.RsClient;
import org.janelia.it.workstation.browser.api.http.pool.RsClientPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestJsonClientManager {

    private static final Logger log = LoggerFactory.getLogger(RestJsonClientManager.class);

    // Singleton
    private static RestJsonClientManager instance;
    public static RestJsonClientManager getInstance() {
        if (instance==null) {
            instance = new RestJsonClientManager();
        }
        return instance;
    }
    
    private ObjectPool<RsClient> pool;

    private RestJsonClientManager() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setBlockWhenExhausted(true);
        config.setMaxIdle(10);
        config.setMaxTotal(20);
        config.setMinIdle(5);
        this.pool = new RsClientPool(config);
    }

    public RsClient borrowClient() throws Exception {
        return pool.borrowObject();
    }
    
    public void returnClient(RsClient client) throws Exception {
        pool.returnObject(client);
    }
}
