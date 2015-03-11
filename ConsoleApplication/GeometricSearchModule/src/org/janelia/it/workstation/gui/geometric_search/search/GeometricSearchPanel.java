package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.swing.*;

import org.janelia.it.workstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchPanel.class);
    Mip3d mip3d;


    @Override
    public void refresh() {
        logger.info("*** refresh()");

        if ( mip3d == null ) {
            logger.warn("Have to create a new mip3d on refresh.");
            createMip3d();
        }

        mip3d.refresh();
    }

    @Override
    public void totalRefresh() {
        logger.info("*** totalRefresh()");
        refresh();
    }

    private void createMip3d() {
        logger.info("*** createMip3d()");
        if ( mip3d != null ) {
            mip3d.releaseMenuActions();
        }
        mip3d = new TestMip3d();
        mip3d.setPreferredSize( new Dimension( 800, 800 ) );
        mip3d.setVisible(true);
        double cameraFocusDistance = mip3d.getVolumeModel().getCameraFocusDistance();
        double pixelsPerSceneUnit = Math.abs(BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS / cameraFocusDistance);
        logger.info("createMip3d() cameraFocusDistance=" + cameraFocusDistance + " pixelsPerSceneUnit=" + pixelsPerSceneUnit);
        mip3d.getVolumeModel().getCamera3d().setPixelsPerSceneUnit(pixelsPerSceneUnit);
        mip3d.addActor(new TestBlueTileActor());
        mip3d.resetView();
        mip3d.setResetFirstRedraw(true);
        add(mip3d, BorderLayout.CENTER);
    }

    public void displayReady() {
        logger.info("*** displayReady()");
        createMip3d();
        logger.info("*** displayReady() done");
    }

}
