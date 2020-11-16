package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.eventbus.NeuronHideEvent;
import org.janelia.workstation.controller.eventbus.NeuronUpdateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronHideAction"
)
@ActionRegistration(
        displayName = "Hide neuron",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-H")
})
public class NeuronHideAction extends EditAction {

    public NeuronHideAction() {
        super("Hide neuron");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TmNeuronMetadata neuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        if (neuron!=null) {
            TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(neuron.getId());
            NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(
                    this, Arrays.asList(new TmNeuronMetadata[]{neuron}));
            ViewerEventBus.postEvent(updateEvent);
        }
    }
}
