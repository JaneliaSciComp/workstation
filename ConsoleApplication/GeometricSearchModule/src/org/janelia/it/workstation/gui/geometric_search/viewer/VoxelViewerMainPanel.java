package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.geometric_search.gl.experiment.ArrayCubeExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 7/29/2015.
 */
public class VoxelViewerMainPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(VoxelViewerMainPanel.class);
    VoxelViewerGLPanel viewer;

    @Override
    public void refresh() {

        if ( viewer == null ) {
            createGL4Viewer();
        }

        viewer.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void createGL4Viewer() {

        if ( viewer != null ) {
            viewer.releaseMenuActions();
        }
        viewer = new VoxelViewerGLPanel();
        viewer.setPreferredSize(new Dimension(1600, 1200));
        viewer.setVisible(true);
        viewer.setResetFirstRedraw(true);

        // Experiment setup goes here ==============

        ArrayCubeExperiment arrayCubeExperiment=new ArrayCubeExperiment();
        arrayCubeExperiment.setup(viewer);

        //===========================================

        add(viewer, BorderLayout.CENTER);

    }

    public void displayReady() {
        if (viewer==null) {
            createGL4Viewer();
        }
        viewer.resetView();
        viewer.refresh();
    }

}
