package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.CreateNeuronReviewEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GenerateTaskReviewAction extends AbstractAction {
    private TmWorkspace origWorkspace;
    private static final Logger log = LoggerFactory.getLogger(GenerateTaskReviewAction.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        generateTaskReview();
    }

    // use the currently selected neuron for context
    public void generateTaskReview() {
        TmNeuronMetadata currNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        if (currNeuron!=null) {
            CreateNeuronReviewEvent event = new CreateNeuronReviewEvent(this,
                    NeuronManager.getInstance().getNeuronFromNeuronID(currNeuron.getId()));
            ViewerEventBus.postEvent(event);
        }
    }
}
