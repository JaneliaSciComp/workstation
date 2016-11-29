package org.janelia.it.workstation.browser.nb_action;

import java.util.List;

import org.janelia.it.jacs.model.domain.sample.Sample;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

/**
 * Allows the user to bind the "set publishing name" action to a key or toolbar button.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "org.janelia.it.workstation.browser.nb_action.SetPublishingNameAction"
)
@ActionRegistration(
        displayName = "#CTL_SetPublishingNameAction"
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "D-P")
})
@Messages("CTL_SetPublishingNameAction=Set Line Publishing Name")
public final class SetPublishingNameAction extends CallableSystemAction {

    private List<Sample> samples;
    
    public SetPublishingNameAction(List<Sample> sample) {
        this.samples = sample;
    }

    @Override
    public String getName() {
        return "Set Line Publishing Name";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public void performAction() {
        SetPublishingNameActionListener actionListener = new SetPublishingNameActionListener(samples);
        actionListener.actionPerformed(null);
    }
}
