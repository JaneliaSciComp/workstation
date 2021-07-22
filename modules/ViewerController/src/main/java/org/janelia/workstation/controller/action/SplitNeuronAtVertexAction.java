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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

;

@ActionID(
        category = "actions",
        id = "SplitNeuronAtVertexAction"
)
@ActionRegistration(
        displayName = "#CTL_SplitNeuronAtVertexAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Large Volume", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_SplitNeuronAtVertexAction=Split Neuron At Vertex")

public class SplitNeuronAtVertexAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(SplitNeuronAtVertexAction.class);

    public SplitNeuronAtVertexAction() {
        super("Split neuron at vertex");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron and selected annotation
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currNeuron==null || currVertex==null)
            return;

        splitNeurite(currNeuron, currVertex);
    }

    public void execute(Long neuronID, Long vertexID) {
        NeuronManager manager = NeuronManager.getInstance();
        TmGeoAnnotation vertex = manager.getGeoAnnotationFromID(neuronID, vertexID);
        TmNeuronMetadata neuron = manager.getNeuronFromNeuronID(neuronID);
        splitNeurite(neuron, vertex);
    }

    public void splitNeurite(TmNeuronMetadata neuron, TmGeoAnnotation vertex) {
        NeuronManager manager = NeuronManager.getInstance();
        Long neuronID = neuron.getId();
        Long newRootAnnotationID = vertex.getId();
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;


        // if it's already the root, can't split
        final TmGeoAnnotation annotation = manager.getGeoAnnotationFromID(neuronID, newRootAnnotationID);
        if (annotation.isRoot()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Cannot split neurite at its root annotation!",
                    "Can't split!",
                    JOptionPane.ERROR_MESSAGE
                    );
            return;
        }

        SimpleWorker splitter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                manager.splitAndMoveNeurite(neuronID, annotation.getId());
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(
                        null,
                        "Split error: " + error.getMessage(),
                        "Error during split!",
                        JOptionPane.ERROR_MESSAGE
                );
                FrameworkAccess.handleException(new Throwable(
                        "Split error: " + error.getMessage()));
            }
        };
        splitter.execute();

    }

}
