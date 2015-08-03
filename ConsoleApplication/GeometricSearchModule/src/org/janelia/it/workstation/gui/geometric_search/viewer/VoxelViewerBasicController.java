package org.janelia.it.workstation.gui.geometric_search.viewer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by murphys on 7/28/2015.
 */
public class VoxelViewerBasicController implements VoxelViewerController {

    VoxelViewerModel model;
    VoxelViewerGLPanel viewer;

    public VoxelViewerBasicController() {
    }

    protected void setModel(VoxelViewerModel model) {
        this.model=model;
    }

    protected void setViewer(VoxelViewerGLPanel viewer) {
        this.viewer=viewer;
    }

    public int addAlignedStackDataset(File alignedStackFile) {

        return 0;
    }

}


