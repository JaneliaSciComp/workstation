package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


// std lib imports

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;


// workstation imports

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected workspace
 *
 * djo, 6/13
 */
public class WorkspaceInfoPanel extends JPanel 
{

    private JLabel workspaceNameLabel;
    private JLabel sampleNameLabel;

    private JList neuronListBox;
    private DefaultListModel neuronListModel;
    private JScrollPane neuronScrollPane;

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


    public Signal1<TmNeuron> neuronClickedSignal = new Signal1<TmNeuron>();

    public Signal1<Vec3> cameraPanToSignal = new Signal1<Vec3>();

    public WorkspaceInfoPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    
        // workspace information; show name, whatever attributes
        add(new JLabel("Workspace", JLabel.CENTER));
        add(Box.createRigidArea(new Dimension(0, 10)));

        workspaceNameLabel = new JLabel("", JLabel.LEADING);
        add(workspaceNameLabel);

        sampleNameLabel = new JLabel("", JLabel.LEADING);
        add(sampleNameLabel);



        // list of neurons

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Neurons", JLabel.CENTER));
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

        add(neuronScrollPane);

        loadWorkspace(null);
    }

    public void selectNeuron(TmNeuron neuron) {
        if (neuron == null) {
            return;
        }

        // object identities vary; find the TmNeuron in the model that has
        //  the desired ID:
        Enumeration<TmNeuron> neuronEnumeration = (Enumeration<TmNeuron>) neuronListModel.elements();
        TmNeuron foundNeuron = null;
        while (neuronEnumeration.hasMoreElements()) {
            TmNeuron testNeuron = neuronEnumeration.nextElement();
            if (testNeuron.getId().equals(neuron.getId())) {
                foundNeuron = testNeuron;
                break;
            }
        }

        // select neuron in neuron list
        if (foundNeuron != null) {
            neuronListBox.setSelectedValue(foundNeuron, true);
        }
    }

    public void loadWorkspace(TmWorkspace workspace) {
        updateMetaData(workspace);

        if (workspace != null) {
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

    private void updateMetaData(final TmWorkspace workspace) {
        if (workspace == null) {
            workspaceNameLabel.setText("Name: (no workspace)");
            sampleNameLabel.setText("Sample:");
        } else {
            workspaceNameLabel.setText("Name: " + workspace.getName());

            SimpleWorker labelFiller = new SimpleWorker() {
                String sampleName;

                @Override
                protected void doStuff() throws Exception {
                    sampleName = ModelMgr.getModelMgr().getEntityById(workspace.getSampleID()).getName();
                }

                @Override
                protected void hadSuccess() {
                    sampleNameLabel.setText("Sample: " + sampleName);
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            labelFiller.execute();

        }
    }
}

