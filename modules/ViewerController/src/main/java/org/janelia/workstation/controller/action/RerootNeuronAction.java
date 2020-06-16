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
        id = "RerootNeuronAction"
)
@ActionRegistration(
        displayName = "#CTL_RerootNeuronAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/Large Volume", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_RerootNeuronAction=Split Neuron Between Vertices")

public class RerootNeuronAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(RerootNeuronAction.class);

    public RerootNeuronAction() {
        super("Split neuron between vertices");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron and selected annotation
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currNeuron==null || currVertex==null)
            return;

        rerootNeuron(currNeuron, currVertex);
    }

    public void execute(Long neuronID, Long vertexID) {
        NeuronManager manager = NeuronManager.getInstance();
        TmGeoAnnotation vertex = manager.getGeoAnnotationFromID(neuronID, vertexID);
        TmNeuronMetadata neuron = manager.getNeuronFromNeuronID(neuronID);
        rerootNeuron(neuron, vertex);
    }

    public void rerootNeuron(TmNeuronMetadata neuron, TmGeoAnnotation newRootVertex) {
        Long neuronID = neuron.getId();
        Long newRootAnnotationID = newRootVertex.getId();
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;


        SimpleWorker rerooter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                NeuronManager.getInstance().rerootNeurite(neuronID, newRootAnnotationID);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
               FrameworkAccess.handleException(new Throwable("Could not reroot neurite!"));
            }
        };
        rerooter.execute();
    }

}
