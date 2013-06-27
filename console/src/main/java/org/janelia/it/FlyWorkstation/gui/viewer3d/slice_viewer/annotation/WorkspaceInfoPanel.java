package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;

import java.awt.*;
import java.util.Vector;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected workspace
 *
 * djo, 6/13
 */
public class WorkspaceInfoPanel extends JPanel 
{

    private JLabel workspaceNameLabel;

    private JList neuronList;
    private JScrollPane neuronScrollPane;


    public Slot1<TmWorkspace> updateWorkspaceSlot = new Slot1<TmWorkspace>() {
        @Override
        public void execute(TmWorkspace workspace) {
            updateWorkspace(workspace);
        }
    };



    public WorkspaceInfoPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
        // workspace information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Workspace information panel"));

        workspaceNameLabel = new JLabel("");
        add(workspaceNameLabel);

        // list of neurons

        neuronList = new JList();
        neuronScrollPane = new JScrollPane(neuronList);
        add(neuronScrollPane);

        updateWorkspace(null);
    }


    public void updateWorkspace(TmWorkspace workspace) {
        if (workspace == null) {
            // clear
            workspaceNameLabel.setText("(no workspace)");
        } else {
            // normal update
            workspaceNameLabel.setText(workspace.getName());

            // repopulate neuron list
            neuronList.setListData(new Vector<TmNeuron>(workspace.getNeuronList()));

            }
        }


    }

