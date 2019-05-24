package org.janelia.workstation.common.nb_action;

import javax.swing.Action;

import org.janelia.workstation.integration.spi.actions.AdminActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builds the admin menu item for running as another user.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = AdminActionBuilder.class, position=1)
public final class RunAsMenuActionBuilder implements AdminActionBuilder {

    private static final Action ACTION = new RunAsMenuAction();

    @Override
    public Action getAction() {
        return ACTION;
    }
}
