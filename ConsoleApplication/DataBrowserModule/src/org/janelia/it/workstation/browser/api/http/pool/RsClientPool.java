package org.janelia.it.workstation.browser.api.http.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from https://github.com/zhonghao87/rspool
 */
public class RsClientPool extends GenericObjectPool<RsClient> {

    private static final Logger log = LoggerFactory.getLogger(RsClientPool.class);
    
    public RsClientPool(GenericObjectPoolConfig config) {
        super(new RsClientPoolFactory(), config);

        // use FIFO
//        setLifo(false);
    }

    @Override
    public RsClient borrowObject() throws Exception {

        RsClient client = super.borrowObject();

        /*
         * since it is FIFO, may try multiple times until a ready RsClient is
         * borrowed.
         */
//        while (!client.isReady()) {
//            returnObject(client);
//            client = super.borrowObject();
//        }

        return client;
    }

}
