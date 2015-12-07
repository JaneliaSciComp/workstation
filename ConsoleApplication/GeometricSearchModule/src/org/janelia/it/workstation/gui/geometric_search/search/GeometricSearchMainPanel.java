package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerController;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerMainPanel;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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
    VoxelViewerSearchData searchData = new VoxelViewerSearchData();
    VoxelViewerController viewerController;

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
            createVoxelViewer();
        }
        viewerMain.refresh();
    }

    public void createVoxelViewer() {
        viewerMain = new VoxelViewerMainPanel();
        try {
            SessionMgr.getSessionMgr().logGenericToolEvent(
                    new ToolString("GeometricSearch"), 
                    new CategoryString("OpenPanel"), 
                    new ActionString(""));
            viewerController = viewerMain.getController(searchData);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        viewerMain.setTransferHandler(new GeometricSearchTransferHandler(this, viewerController));

        add(viewerMain, BorderLayout.CENTER);
    }

}
