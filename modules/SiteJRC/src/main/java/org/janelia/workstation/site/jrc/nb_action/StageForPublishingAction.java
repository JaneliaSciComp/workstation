package org.janelia.workstation.site.jrc.nb_action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.api.actions.ContextualNodeActionTracker;
import org.janelia.workstation.browser.api.actions.NodeContext;
import org.janelia.workstation.browser.api.actions.ContextualNodeAction;
import org.janelia.workstation.site.jrc.gui.dialogs.StageForPublishingDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
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
    @ActionReference(path = "Menu/Actions", position = 2000, separatorBefore = 1999)
})
@Messages("CTL_StageForPublishingAction=Stage for Publishing")
public final class StageForPublishingAction extends AbstractAction implements ContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(StageForPublishingAction.class);
    private static final String NAME = NbBundle.getBundle(StageForPublishingAction.class).getString("CTL_StageForPublishingAction");

    private Collection<Sample> samples = new ArrayList<>();

    public StageForPublishingAction() {
        super(NAME); // Setting name explicitly is necessary for eager actions
        setEnabled(false);
        ContextualNodeActionTracker.getInstance().register(this);
    }

    @Override
    public boolean enable(NodeContext nodeSelection) {
        samples.clear();
        if (nodeSelection.isOnlyObjectsOfType(Sample.class)) {
            samples.addAll(nodeSelection.getOnlyObjectsOfType(Sample.class));
            log.debug("enabled for new viewer context ({} samples)", samples.size());
            setEnabled(true);
        }
        else {
            log.debug("disabled for new viewer context");
            setEnabled(false);
        }
        return isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new StageForPublishingDialog().showForSamples(samples);
    }
}
