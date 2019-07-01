package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=150)
public class AddRelatedItemsBuilder implements ContextualActionBuilder {

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        return SystemAction.get(AddRelatedItemsAction.class);
    }

}
