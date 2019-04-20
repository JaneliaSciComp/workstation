package org.janelia.workstation.browser.actions;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=110)
public class ChangePermissionsBuilder implements ContextualActionBuilder {

    private static ChangePermissionsAction action = new ChangePermissionsAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ClientDomainUtils.isOwner((DomainObject)obj);
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class ChangePermissionsAction extends DomainObjectNodeAction {

        @Override
        public String getName() {
            return "Change Permissions";
        }

        @Override
        protected boolean isVisible() {
            return domainObjectList.size()==1;
        }

        @Override
        protected void executeAction() {
            DomainObject domainObject = domainObjectList.get(0);
            new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_PERMISSIONS);
        }
    }
}
