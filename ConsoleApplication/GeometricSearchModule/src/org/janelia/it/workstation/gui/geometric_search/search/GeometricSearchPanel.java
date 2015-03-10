package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.swing.*;

import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.janelia.it.workstation.model.viewer.AlignmentBoardContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchPanel.class);
    Mip3d mip3d;


    /** Call this for refresh time. */
    public void refresh() {
        logger.info("refresh()");

        if ( mip3d == null ) {
            logger.warn("Have to create a new mip3d on refresh.");
            createMip3d();
        }

        displayReady();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void createMip3d() {
        logger.info("createMip3d()");
        if ( mip3d != null ) {
            mip3d.releaseMenuActions();
        }
        mip3d = new Mip3d();
        double cameraFocusDistance = mip3d.getVolumeModel().getCameraFocusDistance();
        mip3d.getVolumeModel().getCamera3d().setPixelsPerSceneUnit(Math.abs(BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS / cameraFocusDistance));
    }

    public void displayReady() {
        mip3d.refresh();
        removeAll();
        add(mip3d, BorderLayout.CENTER);
        mip3d.resetView();
        mip3d.setResetFirstRedraw( true );
    }


}
