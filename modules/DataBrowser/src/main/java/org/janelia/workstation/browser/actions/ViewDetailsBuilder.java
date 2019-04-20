package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Action to "View Details" about the selected domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=100)
public class ViewDetailsBuilder extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "View Details";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    protected void performAction(Object obj) {
        DomainObject domainObject = (DomainObject)obj;
        new DomainDetailsDialog().showForDomainObject(domainObject);
    }}
