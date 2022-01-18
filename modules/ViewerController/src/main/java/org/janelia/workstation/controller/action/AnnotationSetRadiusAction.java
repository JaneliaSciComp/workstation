package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Horta",
        id = "NeuronSetRadiusAction"
)
@ActionRegistration(
        displayName = "#CTL_AnnotationSetRadiusAction",
        lazy = false
)
@NbBundle.Messages("CTL_AnnotationSetRadiusAction=Set Vertex Radius")

public class AnnotationSetRadiusAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(AnnotationSetRadiusAction.class);

    public AnnotationSetRadiusAction() {
        super("Set Vertex Radius");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        if (currNeuron==null)
            return;

        TmGeoAnnotation currVertex = TmModelManager.getInstance().getCurrentSelections().getCurrentVertex();
        if (currVertex==null)
            return;

        setAnnotationRadius(currNeuron.getId(), currVertex.getId());
    }

    public void execute(Long neuronID, Long vertexID) {
        setAnnotationRadius(neuronID, vertexID);
    }

    public void setAnnotationRadius(Long neuronID, Long vertexID) {
        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;
        double defaultRadius = 1.0f;
        TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(neuronID);
        TmGeoAnnotation vertex = NeuronManager.getInstance().getGeoAnnotationFromID(neuronID, vertexID);
        defaultRadius = vertex.getRadius();

        String ans = (String) JOptionPane.showInputDialog(
                null,
                "Set radius for vertex (µm): ",
                "Set radius",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultRadius);

        if (ans == null || ans.length() == 0) {
            // canceled or no input
            return;
        }

        Float radius = -1.0f;
        try {
            radius = Float.parseFloat(ans);
        } catch(NumberFormatException e) {
            FrameworkAccess.handleException(new Throwable(ans + " cannot be parsed into a radius"));
            return;
        }

        if (radius <= 0.0f) {
            FrameworkAccess.handleException(new Throwable("Radius must be positive, not " + radius));
            return;
        }

        final Float finalRadius = radius;
        SimpleWorker setter = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                NeuronManager manager = NeuronManager.getInstance();
                manager.updateAnnotationRadius(neuronID, vertexID, finalRadius);
            }

            @Override
            protected void hadSuccess() {
                // nothing; listeners will update
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        setter.execute();
    }

}
