package org.janelia.workstation.site.jrc.nb_action;

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
        category = "Actions",
        id = "StageForPublishingAction"
)
@ActionRegistration(
        displayName = "#CTL_StageForPublishingAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/Actions/Samples", position = 540)
})
@Messages("CTL_StageForPublishingAction=Stage for Publishing")
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
    public void performAction() {
        new StageForPublishingDialog().showForSamples(samples);
    }

}
