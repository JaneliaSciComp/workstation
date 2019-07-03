package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.NeuronFragment;
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
        category = "Actions",
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

    private NeuronFragment selectedObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(NeuronFragment.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(NeuronFragment.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }
    @Override
    public void performAction() {
        OpenInNeuronAnnotatorActionListener openInNeuronAnnotatorAction
                = new OpenInNeuronAnnotatorActionListener(selectedObject);
        openInNeuronAnnotatorAction.actionPerformed(null);
    }
}
