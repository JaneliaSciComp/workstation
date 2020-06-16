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
        id = "SplitNeuronBetweenVerticesAction"
)
@ActionRegistration(
        displayName = "#CTL_SplitNeuronBetweenVerticesAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/Large Volume", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_SplitNeuronBetweenVerticesAction=Split Neuron Between Vertices")
/**
 * place a new annotation near the annotation with the input ID; place it
 * "nearby" in the direction of its parent if it has one; if it's a root
 * annotation with one child, place it in the direction of the child
 * instead; if it's a root with many children, it's an error, since there is
 * no unambiguous location to place the new anchor
 */
public class SplitNeuronBetweenVerticesAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(SplitNeuronBetweenVerticesAction.class);

    public SplitNeuronBetweenVerticesAction() {
        super("Split neuron between vertices");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron and selected annotation
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currNeuron==null || currVertex==null)
            return;

        splitVertex(currNeuron, currVertex);
    }

    public void execute(Long neuronID, Long vertexID) {
        NeuronManager manager = NeuronManager.getInstance();
        TmGeoAnnotation vertex = manager.getGeoAnnotationFromID(neuronID, vertexID);
        TmNeuronMetadata neuron = manager.getNeuronFromNeuronID(neuronID);
        splitVertex(neuron, vertex);
    }

    public void splitVertex(TmNeuronMetadata neuron, TmGeoAnnotation vertex) {
        Long neuronID = neuron.getId();
        Long vertexID = vertex.getId();
        NeuronManager neuronManager = NeuronManager.getInstance();
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;

        // can't split a root if it has multiple children (ambiguous):
        final TmGeoAnnotation annotation = neuronManager.getGeoAnnotationFromID(neuronID, vertexID);
        if (annotation.isRoot() && annotation.getChildIds().size() != 1) {
            FrameworkAccess.handleException(new Throwable(
                    "Cannot split root annotation with multiple children (ambiguous)!"));
            return;
        }

        SimpleWorker splitter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                neuronManager.splitAnnotation(annotation);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, model emits signals
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(new Throwable(
                        "Could not split anchor!"));
            }
        };
        splitter.execute();
    }
}
