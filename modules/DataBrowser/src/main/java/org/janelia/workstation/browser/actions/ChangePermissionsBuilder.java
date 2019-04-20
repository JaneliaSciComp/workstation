package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=110)
public class ChangePermissionsBuilder extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "Change Permissions";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ClientDomainUtils.isOwner((DomainObject)obj);
    }

    @Override
    protected void performAction(Object obj) {
        DomainObject domainObject = (DomainObject)obj;
        new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_PERMISSIONS);
    }
}
