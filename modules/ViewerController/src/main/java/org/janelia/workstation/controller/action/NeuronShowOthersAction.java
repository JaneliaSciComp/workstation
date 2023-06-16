package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.NeuronHideEvent;
import org.janelia.workstation.controller.eventbus.NeuronUpdateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronShowOtherNeuronsAction"
)
@ActionRegistration(
        displayName = "Show others",
        lazy = true
)
public class NeuronShowOthersAction extends EditAction {

    public NeuronShowOthersAction() {
        super("Show others");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // show all others except for this neuron
        Collection<TmNeuronMetadata> neuronList = NeuronManager.getInstance().getNeuronList();
        TmNeuronMetadata currentNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        for (TmNeuronMetadata neuron: neuronList) {
            if (currentNeuron!=null && neuron.getId().equals(currentNeuron.getId())) {
                continue;
            }
            TmModelManager.getInstance().getCurrentView().removeAnnotationFromHidden(neuron.getId());
        }
        NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(
                this, neuronList);
        ViewerEventBus.postEvent(updateEvent);
    }
}
