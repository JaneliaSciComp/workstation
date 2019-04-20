package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Action to "View Details" about the selected domain object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=100)
public class ViewDetailsBuilder implements ContextualActionBuilder {

    private static ViewDetailsAction action = new ViewDetailsAction();

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
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class ViewDetailsAction extends DomainObjectNodeAction {

        @Override
        public String getName() {
            return "View Details";
        }

        @Override
        protected boolean isVisible() {
            return domainObjectList.size()==1;
        }

        @Override
        protected void executeAction() {
            DomainObject domainObject = domainObjectList.get(0);
            new DomainDetailsDialog().showForDomainObject(domainObject);
        }
    }
}
