package org.janelia.workstation.admin.nb_actions;

import javax.swing.Action;

import org.janelia.workstation.common.nb_action.RunAsMenuAction;
import org.janelia.workstation.integration.spi.actions.AdminActionBuilder;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builds the admin menu item for launching the Admin GUI.
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
@ServiceProvider(service = AdminActionBuilder.class, position=1)
public final class OpenAdminPanelActionBuilder implements AdminActionBuilder {
    @Override
    public Action getAction() {
        return SystemAction.get(OpenAdminPanelAction.class);
    }
}

