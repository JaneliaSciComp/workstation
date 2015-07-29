package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerMainPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchMainPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchMainPanel.class);

    BorderLayout borderLayout = new BorderLayout();

    GeometricSearchMenuPanel menuPanel = new GeometricSearchMenuPanel();
    VoxelViewerMainPanel viewerMain;

    public GeometricSearchMainPanel() {
        super();
        setLayout(borderLayout);
        add(menuPanel, BorderLayout.NORTH);
    }

    @Override
    public void refresh() {
        if (viewerMain != null) {
            viewerMain.refresh();
        }
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    public void displayReady() {
        if (viewerMain==null) {
            viewerMain = new VoxelViewerMainPanel();
            add(viewerMain, BorderLayout.CENTER);
        }
        viewerMain.refresh();
    }

}
