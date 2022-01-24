package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.workstation.browser.actions.OpenInNeuronAnnotatorActionListener;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OpenInNeuronAnnotatorAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInNeuronAnnotatorAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 220)
})
@NbBundle.Messages("CTL_OpenInNeuronAnnotatorAction=Open With Neuron Annotator")
public class OpenInNeuronAnnotatorAction extends BaseContextualNodeAction {

    protected NeuronSeparation neuronSeparation;
    protected NeuronFragment neuronFragment;
    protected PipelineResult pipelineResult;

    @Override
    protected void processContext() {
        neuronSeparation = null;
        neuronFragment = null;
        pipelineResult = null;
        setEnabledAndVisible(false);

        if (getNodeContext().isSingleObjectOfType(NeuronFragment.class)) {
            this.neuronFragment = getNodeContext().getSingleObjectOfType(NeuronFragment.class);
            setEnabledAndVisible(true);
        }
        else if (getNodeContext().isSingleObjectOfType(PipelineResult.class)) {
            PipelineResult pipelineResult = getNodeContext().getSingleObjectOfType(PipelineResult.class);
            this.neuronSeparation = pipelineResult.getLatestSeparationResult();
            if (neuronSeparation != null) {
                setEnabledAndVisible(true);
            }
            else {
                this.pipelineResult = pipelineResult;
                if (DomainUtils.getDefault3dImageFilePath(pipelineResult) != null) {
                    setEnabledAndVisible(true);
                }
            }
        }
    }

    @Override
    public void performAction() {
        if (neuronSeparation != null) {
            OpenInNeuronAnnotatorActionListener openInNeuronAnnotatorAction
                    = new OpenInNeuronAnnotatorActionListener(neuronSeparation);
            openInNeuronAnnotatorAction.actionPerformed(null);
        }
        else if (neuronFragment != null) {
            OpenInNeuronAnnotatorActionListener openInNeuronAnnotatorAction
                    = new OpenInNeuronAnnotatorActionListener(neuronFragment);
            openInNeuronAnnotatorAction.actionPerformed(null);
        }
        else if (pipelineResult != null) {
            OpenInNeuronAnnotatorActionListener openInNeuronAnnotatorAction
                    = new OpenInNeuronAnnotatorActionListener(pipelineResult);
            openInNeuronAnnotatorAction.actionPerformed(null);
        }
    }
}
