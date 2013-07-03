package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected workspace
 *
 * djo, 6/13
 */
public class WorkspaceInfoPanel extends JPanel 
{

    private JLabel workspaceNameLabel;

    private JList neuronListBox;
    private DefaultListModel neuronListModel;
    private JScrollPane neuronScrollPane;

    public Slot1<TmWorkspace> updateWorkspaceSlot = new Slot1<TmWorkspace>() {
        @Override
        public void execute(TmWorkspace workspace) {
            updateWorkspace(workspace);
        }
    };
    public Signal1<TmNeuron> neuronClickedSignal = new Signal1<TmNeuron>();


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

        neuronListModel = new DefaultListModel();
        neuronListBox = new JList(neuronListModel);
        neuronScrollPane = new JScrollPane(neuronListBox);
        neuronListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        neuronListBox.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent listSelectionEvent) {
                        if (!listSelectionEvent.getValueIsAdjusting()) {
                            int index = neuronListBox.getSelectedIndex();
                            TmNeuron selectedNeuron;
                            if (index >= 0) {
                                selectedNeuron = (TmNeuron) neuronListModel.getElementAt(index);
                                System.out.println("neuron selected: " + selectedNeuron.getName());
                            } else {
                                System.out.println("no neuron selected");
                                selectedNeuron = null;
                            }
                            neuronClickedSignal.emit(selectedNeuron);
                        }
                    }
                }
        );
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
            Vector<TmNeuron> neuronVector = new Vector<TmNeuron>(workspace.getNeuronList());
            Collections.sort(neuronVector, new Comparator<TmNeuron>() {
                @Override
                public int compare(TmNeuron tmNeuron, TmNeuron tmNeuron2) {
                    return tmNeuron.getName().compareTo(tmNeuron2.getName());
                }
            });
            neuronListModel.clear();
            for (TmNeuron tmNeuron: neuronVector) {
                neuronListModel.addElement(tmNeuron);
            }
            }
        }


    }

