package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by murphys on 7/30/2015.
 */
public class VoxelViewerRejectedExecutionHandler implements RejectedExecutionHandler {

    private final Logger logger = LoggerFactory.getLogger(VoxelViewerRejectedExecutionHandler.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        logger.error("rejectedExecution() for Runnable r="+r.toString()+" and executor="+executor.toString());
    }
}
