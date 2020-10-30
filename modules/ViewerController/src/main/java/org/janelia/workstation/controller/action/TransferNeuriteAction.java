package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransferNeuriteAction extends AbstractAction {

    public void actionPerformed(ActionEvent event) {
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currNeuron==null || currVertex==null)
            return;
        execute(currNeuron.getId(), currVertex.getId());
    }

    public void execute(Long neuronID, Long vertexID) {
        NeuronManager neuronManager = NeuronManager.getInstance();
        final TmGeoAnnotation annotation = neuronManager.getGeoAnnotationFromID(neuronID, vertexID);
        TmNeuronMetadata sourceNeuron = neuronManager.getNeuronFromNeuronID(neuronID);
        if (!TmModelManager.getInstance().checkOwnership(sourceNeuron))
            return;

        ArrayList<TmNeuronMetadata> neuronList = new ArrayList<>(neuronManager.getCurrentFilteredNeuronList());
        neuronList.remove(sourceNeuron);
        // not sure alphabetical is the best sort; neuron list is selectable (defaults to creation
        //  date), but I don't want to figure out how to grab that sort order and use it here;
        //  however, alphabetical seems reasonable enough (better than arbitrary order)
        Collections.sort(neuronList, new Comparator<TmNeuronMetadata>() {
            @Override
            public int compare(TmNeuronMetadata tmNeuronMetadata, TmNeuronMetadata tmNeuronMetadata2) {
                return tmNeuronMetadata.getName().compareToIgnoreCase(tmNeuronMetadata2.getName());
            }
        });

        // add "create new" at top of sorted list
        TmNeuronMetadata dummyCreateNewNeuron = new TmNeuronMetadata();
        dummyCreateNewNeuron.setId(-1L);
        dummyCreateNewNeuron.setName("(create new neuron)");
        neuronList.add(0, dummyCreateNewNeuron);

        List<TmDisplayNeuron> displayList = new ArrayList<>();
        for (TmNeuronMetadata tmNeuronMetadata : neuronList) {
            displayList.add(new TmDisplayNeuron(tmNeuronMetadata));
        }

        Object[] choices = displayList.toArray();
        Object choice = JOptionPane.showInputDialog(
                null,
                "Choose destination neuron:",
                "Choose neuron",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]
        );
        if (choice == null) {
            return;
        }

        final TmDisplayNeuron choiceNeuron = (TmDisplayNeuron) choice;
        final TmNeuronMetadata destinationNeuron = choiceNeuron.getTmNeuronMetadata();

        if (destinationNeuron.getId().equals(dummyCreateNewNeuron.getId())) {
            // create new neuron and move neurite to it
            final String neuronName = promptForNeuronName(null);
            if (neuronName == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "Neuron rename canceled; move neurite canceled",
                        "Move neurite canceled",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    TmNeuronMetadata newNeuron = neuronManager.createNeuron(neuronName);
                    neuronManager.moveNeurite(annotation, newNeuron);
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see here
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(new Exception( "Error while moving neurite!",
                            error));
                }
            };
            mover.execute();

        } else {
            // we're moving to an existing neuron; not allowed if destination is owned by someone else;
            //  otherwise, this is straightforward
            if (!TmModelManager.getInstance().checkOwnership(destinationNeuron))
                return;


            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    neuronManager.moveNeurite(annotation, destinationNeuron);
                }

                @Override
                protected void hadSuccess() {
                    // nothing to see here
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(new Exception( "Error while moving neurite!",
                            error));
                }
            };
            mover.execute();
        }
    }

    String promptForNeuronName(String suggestedName) {
        if (suggestedName == null) {
            suggestedName = "";
        }
        String neuronName = (String) JOptionPane.showInputDialog(
                null,
                "Neuron name:",
                "Name neuron",
                JOptionPane.PLAIN_MESSAGE,
                null, // icon
                null, // choice list; absent = freeform
                suggestedName);
        if (neuronName == null || neuronName.length() == 0) {
            return null;
        } else {
            // turns out ? or * will mess with Java's file dialogs
            //  (something about how file filters works)
            if (neuronName.contains("?") || neuronName.contains("*")) {
                JOptionPane.showMessageDialog(
                        null,
                        "Neuron names can't contain the ? or * characters!",
                        "Could not create neuron",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return neuronName;
        }
    }

    private class TmDisplayNeuron {
        private TmNeuronMetadata tmNeuronMetadata;
        TmDisplayNeuron(TmNeuronMetadata tmNeuronMetadata) {
            this.tmNeuronMetadata = tmNeuronMetadata;
        }
        @Override
        public String toString() {
            return tmNeuronMetadata.getName();
        }
        public TmNeuronMetadata getTmNeuronMetadata() {
            return tmNeuronMetadata;
        }
    }
}
