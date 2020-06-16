package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

;

    @ActionID(
            category = "actions",
            id = "DeleteNeuronSubtreeAction"
    )
    @ActionRegistration(
            displayName = "#CTL_DeleteNeuronSubtreeAction",
            lazy = false
    )
    @ActionReferences({
            @ActionReference(path = "Menu/actions/Large Volume", position = 1510, separatorBefore = 1499)
    })
    @NbBundle.Messages("CTL_DeleteNeuronSubtreeAction=Delete Neuron Subtree")
    public class DeleteNeuronSubtreeAction extends AbstractAction {
        private static final Logger log = LoggerFactory.getLogger(org.janelia.workstation.controller.action.MergeNeuronsAction.class);

        public DeleteNeuronSubtreeAction() {
            super("Delete subtree rooted at this vertex");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            // get current neuron and selected annotation
            TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
            TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
            if (currNeuron==null || currVertex==null)
                return;

            deleteSubTree(currNeuron, currVertex);
        }

        public void execute(Long neuronID, Long vertexID) {
            NeuronManager manager = NeuronManager.getInstance();
            TmGeoAnnotation vertex = manager.getGeoAnnotationFromID(neuronID, vertexID);
            TmNeuronMetadata neuron = manager.getNeuronFromNeuronID(neuronID);
            deleteSubTree(neuron, vertex);
        }

        public void deleteSubTree(TmNeuronMetadata neuron, TmGeoAnnotation vertex) {
            if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
                return;
            }

            if (!TmModelManager.getInstance().checkOwnership(neuron.getId()))
                return;

            NeuronManager neuronManager = NeuronManager.getInstance();
            // if more than one point, ask the user if they are sure (we have
            //  no undo right now!)
            final TmGeoAnnotation annotation = neuronManager.getGeoAnnotationFromID(neuron.getId(),
                    vertex.getId());
            int nAnnotations = neuronManager.getNeuronFromNeuronID(vertex.getId()).getSubTreeList(annotation).size();
            if (nAnnotations > 1) {
                int ans = JOptionPane.showConfirmDialog(
                        null,
                        String.format("Selected subtree has %d points; delete?", nAnnotations),
                        "Delete subtree?",
                        JOptionPane.OK_CANCEL_OPTION);
                if (ans != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    neuronManager.deleteSubTree(annotation);
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; annotationModel emits signals
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            deleter.execute();
        }
    }
