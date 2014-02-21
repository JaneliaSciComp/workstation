package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;


import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;


/**
 * this widget displays a list of neurons in a workspace
 *
 * djo, 12/13
 */
public class WorkspaceNeuronList extends JPanel {

    private JList neuronListBox;
    private DefaultListModel neuronListModel;

    // to add new sort order: add to enum here, add menu in AnnotationPanel.java,
    //  and implement the sort in sortNeuronList below
    // default set in AnnotationPanel as well
    public enum NeuronSortOrder {ALPHABETICAL, CREATIONDATE};
    private NeuronSortOrder neuronSortOrder;

    // ----- slots
    public Slot1<TmWorkspace> workspaceLoadedSlot = new Slot1<TmWorkspace>() {
        @Override
        public void execute(TmWorkspace workspace) {
            loadWorkspace(workspace);
        }
    };
    public Slot1<TmNeuron> neuronSelectedSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            selectNeuron(neuron);
        }
    };

    // ----- signals
    public Signal1<TmNeuron> neuronClickedSignal = new Signal1<TmNeuron>();
    public Signal1<Vec3> cameraPanToSignal = new Signal1<Vec3>();

    public WorkspaceNeuronList() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new GridBagLayout());

        // list of neurons
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        add(new JLabel("Neurons", JLabel.LEADING), c);

        neuronListModel = new DefaultListModel();
        neuronListBox = new JList(neuronListModel);
        JScrollPane neuronScrollPane = new JScrollPane(neuronListBox);
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
                            } else {
                                selectedNeuron = null;
                            }
                            neuronClickedSignal.emit(selectedNeuron);
                        }
                    }
                }
        );
        // ...and you have to do it again if you want to get mouse clicks, ugh:
        neuronListBox.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent mouseEvent) {
                        JList list = (JList)mouseEvent.getSource();
                        // double-click:
                        if (mouseEvent.getClickCount() == 2) {
                            int index = list.locationToIndex(mouseEvent.getPoint());
                            TmNeuron neuron = (TmNeuron) list.getModel().getElementAt(index);
                            onNeuronDoubleClicked(neuron);
                        }
                    }
                }
        );

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.HORIZONTAL;
        add(neuronScrollPane, c2);

        loadWorkspace(null);

    }

    /**
     * called when current neuron changes
     */
    public void selectNeuron(TmNeuron neuron) {
        if (neuron == null) {
            return;
        }

        // find the neuron in the list model by ID:
        Enumeration<TmNeuron> neuronEnumeration = (Enumeration<TmNeuron>) neuronListModel.elements();
        TmNeuron foundNeuron = null;
        while (neuronEnumeration.hasMoreElements()) {
            TmNeuron testNeuron = neuronEnumeration.nextElement();
            if (testNeuron.getId().equals(neuron.getId())) {
                foundNeuron = testNeuron;
                break;
            }
        }

        // I should probably just count the index during the enum
        //  loop above...
        if (foundNeuron != null) {
            int index = neuronListModel.indexOf(foundNeuron);
            neuronListModel.setElementAt(neuron, index);
            neuronListBox.setSelectedValue(neuron, true);
        }
    }

    /**
     * called when the sort order is changed in the UI
     */
    public void sortOrderChanged(NeuronSortOrder sortOrder) {
        if (sortOrder == neuronSortOrder) {
            return;
        }
        this.neuronSortOrder = sortOrder;
        if (neuronListModel.size() > 0) {
            // this can't be the best way to do this...
            Vector<TmNeuron> neuronVector = new Vector<TmNeuron>(neuronListModel.size());
            for (int i=0; i<neuronListModel.size(); i++) {
                neuronVector.add((TmNeuron) neuronListModel.getElementAt(i));
            }
            sortNeuronList(neuronVector);
            neuronListModel.clear();
            for (TmNeuron tmNeuron: neuronVector) {
                neuronListModel.addElement(tmNeuron);
            }
        }
    }

    /**
     * populate the UI with info from the input workspace
     */
    public void loadWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            // repopulate neuron list
            Vector<TmNeuron> neuronVector = new Vector<TmNeuron>(workspace.getNeuronList());
            sortNeuronList(neuronVector);
            neuronListModel.clear();
            for (TmNeuron tmNeuron: neuronVector) {
                neuronListModel.addElement(tmNeuron);
            }
        }
    }

    private void sortNeuronList(Vector<TmNeuron> neuronVector) {
        switch(neuronSortOrder) {
            case ALPHABETICAL:
                Collections.sort(neuronVector, new Comparator<TmNeuron>() {
                    @Override
                    public int compare(TmNeuron tmNeuron, TmNeuron tmNeuron2) {
                        return tmNeuron.getName().compareToIgnoreCase(tmNeuron2.getName());
                    }
                });
                break;
            case CREATIONDATE:
                Collections.sort(neuronVector, new Comparator<TmNeuron>() {
                    @Override
                    public int compare(TmNeuron tmNeuron, TmNeuron tmNeuron2) {
                        return tmNeuron.getCreationDate().compareTo(tmNeuron2.getCreationDate());
                    }
                });
                break;
        }
    }

    private void onNeuronDoubleClicked(TmNeuron neuron) {
        // should pan to center of neuron; let's call that the center
        //  of the bounding cube for its annotations
        // I'd prefer this calculation be part of TmNeuron, but
        //  I can't use BoundingBox3d there
        if (neuron.getGeoAnnotationMap().size() != 0) {
            BoundingBox3d bounds = new BoundingBox3d();
            for (TmGeoAnnotation ann: neuron.getGeoAnnotationMap().values()) {
                bounds.include(new Vec3(ann.getX(), ann.getY(), ann.getZ()));
            }
            cameraPanToSignal.emit(bounds.getCenter());
        }
    }

}
