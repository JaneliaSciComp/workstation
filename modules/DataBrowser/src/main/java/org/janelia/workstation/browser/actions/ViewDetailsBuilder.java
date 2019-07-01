package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.nb_action.ViewDetailsAction;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;

/**
 * Action to "View Details" about the selected domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=100)
public class ViewDetailsBuilder implements ContextualActionBuilder {

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return SystemAction.get(ViewDetailsAction.class);
    }

    @Override
    public Action getNodeAction(Object obj) {
        return SystemAction.get(ViewDetailsAction.class);
    }
}
