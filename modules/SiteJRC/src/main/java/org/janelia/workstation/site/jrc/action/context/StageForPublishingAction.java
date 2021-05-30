package org.janelia.workstation.site.jrc.action.context;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.site.jrc.gui.dialogs.StageForPublishingDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "actions",
        id = "StageForPublishingAction"
)
@ActionRegistration(
        displayName = "#CTL_StageForPublishingAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Actions/Sample", position = 540)
})
@Messages("CTL_StageForPublishingAction=Stage for Publishing...")
public final class StageForPublishingAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(StageForPublishingAction.class);

    private Collection<Sample> samples = new ArrayList<>();

    @Override
    protected void processContext() {
        samples.clear();
        if (getNodeContext().isOnlyObjectsOfType(Sample.class)) {
            samples.addAll(getNodeContext().getOnlyObjectsOfType(Sample.class));
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        if (samples!=null && samples.size()>1) {
            return "Stage "+samples.size()+" Samples for Publishing";
        }
        return super.getName();
    }

    @Override
    public void performAction() {
        Collection<Sample> samples = new ArrayList<>(this.samples);
        new StageForPublishingDialog().showForSamples(samples);
    }

}
