package org.janelia.it.workstation.browser.api.http.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from https://github.com/zhonghao87/rspool
 */
public class RsClientPoolFactory extends BasePooledObjectFactory<RsClient> {

    private static final Logger log = LoggerFactory.getLogger(RsClientPoolFactory.class);
    
    /** Package protected constructor */
    RsClientPoolFactory() {
    }
    
    @Override
    public RsClient create() throws Exception {
        return new RsClient();
    }

    @Override
    public PooledObject<RsClient> wrap(RsClient client) {
        return new DefaultPooledObject<RsClient>(client);
    }

    @Override
    public void destroyObject(PooledObject<RsClient> p) throws Exception {
        log.info("Destroying pooled client {}", p.getObject());
        p.getObject().shutDown();
    }

}