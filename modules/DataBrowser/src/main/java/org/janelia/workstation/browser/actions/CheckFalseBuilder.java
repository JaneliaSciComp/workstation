package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builds action to "uncheck" all the selected items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=21)
public class CheckFalseBuilder implements ContextualActionBuilder {

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return CheckSelectedAction.UNCHECK_ACTION;
    }

    @Override
    public Action getNodeAction(Object obj) {
        // This action is not available for Nodes
        return null;
    }
}
