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
import org.janelia.it.workstation.gui.util.WindowLocator;

/**
 * Facilitates launching the annotation skeleton viewer.
 *
 * @author fosterl
 */
public class AnnotationSkeletonViewLauncher {
    public List<JMenuItem> getMenuItems() {
        List<JMenuItem> menuItems = new ArrayList<>();
        Action launchAction = new AbstractAction() {
            {
                super.putValue(NAME, "Launch Annotation Skeleton Viewer");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                WindowLocator.makeVisibleAndGet(AnnotationSkeletalViewTopComponent.PREFERRED_ID);
            }
        };
        menuItems.add(new JMenuItem(launchAction));
        return menuItems;
    }
    
}
