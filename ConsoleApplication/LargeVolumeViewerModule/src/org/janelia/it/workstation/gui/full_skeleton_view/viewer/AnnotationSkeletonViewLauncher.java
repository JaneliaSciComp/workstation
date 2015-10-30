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
    
    /** Always re-open the view on initial creation. */
    public AnnotationSkeletonViewLauncher() {
        this(true);
    }
    
    public AnnotationSkeletonViewLauncher(boolean refresh) {
        if (refresh) {
            refreshView();
        }
    }
    
    public List<JMenuItem> getMenuItems() {
        List<JMenuItem> menuItems = new ArrayList<>();
        Action launchAction = new AbstractAction() {
            {
                super.putValue(NAME, AnnotationSkeletalViewTopComponent.LABEL_TEXT.trim());
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                reopenView();
                refreshTopComponent();
            }
        };
        menuItems.add(new JMenuItem(launchAction));
        return menuItems;
    }

    public void refreshTopComponent() {
        AnnotationSkeletalViewTopComponent comp = getTopComponent();
        // Coerce a redraw.
        comp.invalidate();
        comp.validate();
        comp.repaint();
    }
    
    public void reopenView() {
        AnnotationSkeletalViewTopComponent topComponent = showTopComponent();
        // This exchange will refresh contents of viewer.
        topComponent.componentOpened();
    }
    
    private void refreshView() {
        AnnotationSkeletalViewTopComponent comp = getTopComponent();
        if (comp == null) 
            return;
        comp.componentOpened();
    }

    protected AnnotationSkeletalViewTopComponent getTopComponent() {
        AnnotationSkeletalViewTopComponent topComponent =
                (AnnotationSkeletalViewTopComponent) WindowLocator
                        .getByName(AnnotationSkeletalViewTopComponent.PREFERRED_ID);
        return topComponent;
    }
    
    protected AnnotationSkeletalViewTopComponent showTopComponent() {
        AnnotationSkeletalViewTopComponent topComponent = (AnnotationSkeletalViewTopComponent) WindowLocator.makeVisibleAndGet(AnnotationSkeletalViewTopComponent.PREFERRED_ID);
        return topComponent;
    }

}
