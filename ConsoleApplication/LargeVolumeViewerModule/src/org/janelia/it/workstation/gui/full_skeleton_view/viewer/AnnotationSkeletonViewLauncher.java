/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.viewer;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.util.WindowLocator;

/**
 * Facilitates launching the annotation skeleton viewer.
 *
 * @author fosterl
 */
public class AnnotationSkeletonViewLauncher {
    private TileFormat tileFormat;
    
    public List<JMenuItem> getMenuItems() {
        List<JMenuItem> menuItems = new ArrayList<>();
        Action launchAction = new AbstractAction() {
            {
                super.putValue(NAME, "Launch Annotation Skeleton Viewer");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationSkeletalViewTopComponent topComponent = (AnnotationSkeletalViewTopComponent)
                        WindowLocator.makeVisibleAndGet(AnnotationSkeletalViewTopComponent.PREFERRED_ID);
                // This exchange will refresh contents of viewer.
                topComponent.componentClosed();
                topComponent.componentOpened();
                topComponent.setTileFormat(tileFormat);
            }
        };
        menuItems.add(new JMenuItem(launchAction));
        return menuItems;
    }

    /**
     * @return the tileFormat
     */
    public TileFormat getTileFormat() {
        return tileFormat;
    }

    /**
     * @param tileFormat the tileFormat to set
     */
    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat = tileFormat;
    }
    
}
