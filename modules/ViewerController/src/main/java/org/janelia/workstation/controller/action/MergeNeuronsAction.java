package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
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
        category = "Horta",
        id = "MergeNeuronsAction"
)
@ActionRegistration(
        displayName = "#CTL_MergeNeuronsAction",
        lazy = false
)
@NbBundle.Messages("CTL_MergeNeuronsAction=Merge Neurons")
public class MergeNeuronsAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(MergeNeuronsAction.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        mergeNeurons();
    }

    public void execute() {
        mergeNeurons();
    }

    public void mergeNeurons() {
        // get primary and secondary selections
        TmSelectionState selectionState =  TmSelectionState.getInstance();
        TmNeuronMetadata sourceNeuron = selectionState.getCurrentNeuron();
        TmGeoAnnotation sourceAnnotation = selectionState.getCurrentVertex();
        TmNeuronMetadata targetNeuron = selectionState.getSecondaryNeuron();
        TmGeoAnnotation targetAnnotation = selectionState.getSecondaryVertex();
        if (!TmModelManager.getInstance().checkOwnership(sourceNeuron)||!TmModelManager.getInstance().checkOwnership(targetNeuron))
            return;

        // same neurite = cycle = NO!
        // this should already be filtered out, but it's important enough to check twice
        if (NeuronManager.getInstance().sameNeurite(sourceAnnotation, targetAnnotation)) {
            NeuronManager.handleException(
                    "You can't merge a neurite with itself!",
                    "Can't merge!!");
            NeuronManager.getInstance().fireAnnotationNotMoved(sourceAnnotation);
            return;
        }

        SimpleWorker merger = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                NeuronManager.getInstance().mergeNeurite(sourceNeuron.getId(), sourceAnnotation.getId(),
                        targetNeuron.getId(), targetAnnotation.getId());
            }

            @Override
            protected void hadSuccess() {
                // sends its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        merger.execute();
    }
}
