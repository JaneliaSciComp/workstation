package org.janelia.workstation.lm.actions.context;

import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.workstation.browser.actions.context.OpenInVvdNAPluginActionListener;
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
        category = "Actions",
        id = "OpenInVvdNAPluginAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInVvdNAPluginAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 241)
})
@NbBundle.Messages("CTL_OpenInVvdNAPluginAction=View In VVD Viewer (Neuron Annotator Plugin)")
public class OpenInVvdNAPluginAction extends BaseContextualNodeAction {

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
            if (neuronSeparation!= null) {
                setEnabledAndVisible(true);
            }
        }
    }

    @Override
    public void performAction() {

        if (neuronSeparation != null) {
            OpenInVvdNAPluginActionListener openInVvdNAPluginAction
                    = new OpenInVvdNAPluginActionListener(neuronSeparation);
            openInVvdNAPluginAction.actionPerformed(null);
        }
        else if (neuronFragment != null) {
            OpenInVvdNAPluginActionListener openInVvdNAPluginAction
                    = new OpenInVvdNAPluginActionListener(neuronFragment);
            openInVvdNAPluginAction.actionPerformed(null);
        }
    }
}
