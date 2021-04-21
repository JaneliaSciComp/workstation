package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.sample.NeuronFragment;
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
@NbBundle.Messages("CTL_OpenInVvdNAPluginAction=Open With VVD Viewer (Neurons)")
public class OpenInVvdNAPluginAction extends BaseOpenExternallyAction {

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
        OpenInVvdNAPluginActionListener openInVvdNAPluginAction
                = new OpenInVvdNAPluginActionListener(selectedObject);
        openInVvdNAPluginAction.actionPerformed(null);
    }
}
