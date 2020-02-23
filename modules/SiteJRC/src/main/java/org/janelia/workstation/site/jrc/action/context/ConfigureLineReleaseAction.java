package org.janelia.workstation.site.jrc.action.context;

import java.util.Collection;

import org.janelia.model.domain.sample.LineRelease;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.site.jrc.gui.dialogs.LineReleaseDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "actions",
        id = "ConfigureLineReleaseAction"
)
@ActionRegistration(
        displayName = "#CTL_ConfigureLineReleaseAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/actions/Sample", position = 545)
})
@Messages("CTL_ConfigureLineReleaseAction=Configure Line Release...")
public final class ConfigureLineReleaseAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(ConfigureLineReleaseAction.class);

    private LineRelease release;

    @Override
    protected void processContext() {
        this.release = null;
        setEnabledAndVisible(false);
        if (getNodeContext().isOnlyObjectsOfType(LineRelease.class)) {
            Collection<LineRelease> releases = getNodeContext().getOnlyObjectsOfType(LineRelease.class);
            if (releases.size()==1) {
                this.release = releases.iterator().next();
                setEnabledAndVisible(true);
            }
        }
    }

    @Override
    public void performAction() {
        if (release != null) {
            new LineReleaseDialog(null).showForRelease(release);
        }
    }

}
