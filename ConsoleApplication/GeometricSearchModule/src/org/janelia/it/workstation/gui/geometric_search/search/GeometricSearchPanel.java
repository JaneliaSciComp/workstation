package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.gl.experiment.ArrayCubeExperiment;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerMainPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchPanel.class);
    VoxelViewerMainPanel viewer;

    @Override
    public void refresh() {
        if (viewer != null) {
            viewer.refresh();
        }
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    public void displayReady() {
        if (viewer==null) {
            viewer = new VoxelViewerMainPanel();
            add(viewer, BorderLayout.CENTER);
        }
        viewer.refresh();
    }

}
