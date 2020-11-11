package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.eventbus.NeuronUpdateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronShowAction"
)
@ActionRegistration(
        displayName = "Show neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "S")
})
public class NeuronShowAction extends EditAction {
    
    public NeuronShowAction() {
        super("Show neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TmNeuronMetadata neuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        if (neuron!=null) {
            TmModelManager.getInstance().getCurrentView().removeAnnotationFromHidden(neuron.getId());
            NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(
                    Arrays.asList(new TmNeuronMetadata[]{neuron}));
            ViewerEventBus.postEvent(updateEvent);
        }
    }
}
