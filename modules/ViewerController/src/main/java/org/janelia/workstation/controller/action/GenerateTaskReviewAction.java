package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.eventbus.CreateNeuronReviewEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.task_workflow.NeuronTree;
import org.janelia.workstation.controller.task_workflow.TaskWorkflowViewTopComponent;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
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
import java.util.List;

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
