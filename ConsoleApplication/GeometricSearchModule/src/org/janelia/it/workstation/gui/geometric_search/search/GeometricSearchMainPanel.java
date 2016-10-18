package org.janelia.it.workstation.gui.geometric_search.search;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerController;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerMainPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchMainPanel extends JPanel {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchMainPanel.class);

    BorderLayout borderLayout = new BorderLayout();

    GeometricSearchMenuPanel menuPanel = new GeometricSearchMenuPanel();
    VoxelViewerMainPanel viewerMain;
    VoxelViewerSearchData searchData = new VoxelViewerSearchData();
    VoxelViewerController viewerController;

    public GeometricSearchMainPanel() {
        super();
        setLayout(borderLayout);
        add(menuPanel, BorderLayout.NORTH);
    }

    public void refresh() {
        if (viewerMain != null) {
            viewerMain.refresh();
        }
    }

    public void totalRefresh() {
        refresh();
    }

    public void displayReady() {
        if (viewerMain==null) {
            createVoxelViewer();
        }
        viewerMain.refresh();
    }

    public void createVoxelViewer() {
        viewerMain = new VoxelViewerMainPanel();
        try {
            viewerController = viewerMain.getController(searchData);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        viewerMain.setTransferHandler(new GeometricSearchTransferHandler(this, viewerController));

        add(viewerMain, BorderLayout.CENTER);
    }

}
