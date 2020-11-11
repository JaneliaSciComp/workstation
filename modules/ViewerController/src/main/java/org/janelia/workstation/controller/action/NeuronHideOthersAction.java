package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.eventbus.NeuronHideEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronHideOtherNeuronsAction"
)
@ActionRegistration(
        displayName = "Hide others (make only the selected neuron visible)",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "S-H")
})
public class NeuronHideOthersAction extends EditAction {

    public NeuronHideOthersAction() {
        super("Hide others");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // hide all others except for this neuron
        Collection<TmNeuronMetadata> neuronList = NeuronManager.getInstance().getNeuronList();
        for (TmNeuronMetadata neuron: neuronList) {
            TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(neuron.getId());
        }
        NeuronHideEvent neuronHideEvent = new NeuronHideEvent(neuronList);
        ViewerEventBus.postEvent(neuronHideEvent);
    }
}
