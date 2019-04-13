/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.gui.task_workflow;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;

import org.janelia.workstation.common.gui.support.WindowLocator;


public class TaskWorkflowViewLauncher {

    /** Always re-open the view on initial creation. */
    public TaskWorkflowViewLauncher() {
        this(true);
    }

    public TaskWorkflowViewLauncher(boolean refresh) {
        if (refresh) {
            refreshView();
        }
    }
    
    public List<JMenuItem> getMenuItems() {
        List<JMenuItem> menuItems = new ArrayList<>();
        Action launchAction = new AbstractAction() {
            {
                super.putValue(NAME, TaskWorkflowViewTopComponent.LABEL_TEXT.trim());
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
        TaskWorkflowViewTopComponent comp = getTopComponent();
        // Coerce a redraw.
        comp.invalidate();
        comp.validate();
        comp.repaint();
    }
    
    public void reopenView() {
        TaskWorkflowViewTopComponent topComponent = showTopComponent();
        // This exchange will refresh contents of viewer.
        topComponent.componentOpened();
    }
    
    private void refreshView() {
        TaskWorkflowViewTopComponent comp = getTopComponent();
        if (comp == null) 
            return;
        comp.componentOpened();
    }

    protected TaskWorkflowViewTopComponent getTopComponent() {
        TaskWorkflowViewTopComponent topComponent =
                (TaskWorkflowViewTopComponent) WindowLocator
                        .getByName(TaskWorkflowViewTopComponent.PREFERRED_ID);
        return topComponent;
    }
    
    protected TaskWorkflowViewTopComponent showTopComponent() {
        TaskWorkflowViewTopComponent topComponent = (TaskWorkflowViewTopComponent) WindowLocator.makeVisibleAndGet(TaskWorkflowViewTopComponent.PREFERRED_ID);
        return topComponent;
    }

}
