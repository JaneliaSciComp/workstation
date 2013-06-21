package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;

import java.awt.*;


// workstation imports

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected workspace
 *
 * djo, 6/13
 */
public class WorkspaceInfoPanel extends JPanel 
{

    JLabel workspaceNameLabel;


    public WorkspaceInfoPanel() {
        setupUI();
    }

    public void clear() {
        workspaceNameLabel.setText("(untitled)");
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
        // workspace information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Workspace information panel"));

        workspaceNameLabel = new JLabel("(untitled)");
        add(workspaceNameLabel);


        clear();
    }


    public void update(TmWorkspace workspace) {
        workspaceNameLabel.setText(workspace.getName());
    }

}