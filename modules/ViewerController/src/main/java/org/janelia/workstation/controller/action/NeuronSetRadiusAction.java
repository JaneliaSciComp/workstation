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
import java.util.Map;

@ActionID(
        category = "actions",
        id = "NeuronSetRadiusAction"
)
@ActionRegistration(
        displayName = "#CTL_NeuronSetRadiusAction",
        lazy = false
)
@NbBundle.Messages("CTL_NeuronSetRadiusAction=Set Neuron Radius")

public class NeuronSetRadiusAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(NeuronSetRadiusAction.class);

    public NeuronSetRadiusAction() {
        super("Set Neuron Radius");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // get current neuron
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        if (currNeuron==null)
            return;

        setNeuronRadius(currNeuron.getId());
    }

    public void execute(Long neuronID) {
        setNeuronRadius(neuronID);
    }

    public void setNeuronRadius(Long neuronID) {
        if (!TmModelManager.getInstance().checkOwnership(neuronID))
            return;
        double defaultRadius = 1.0f;
        TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(neuronID);
        Map<Long, TmGeoAnnotation> map = neuron.getGeoAnnotationMap();
        if (!map.isEmpty()) {
             TmGeoAnnotation ann = map.values().iterator().next();
             if (ann != null && ann.getRadius() != null) {
                 defaultRadius = ann.getRadius();
             }
        }

        String ans = (String) JOptionPane.showInputDialog(
                null,
                "Set radius for neuron (µm): ",
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
                manager.updateNeuronRadius(neuronID, finalRadius);
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
