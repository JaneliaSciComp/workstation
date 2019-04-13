package org.janelia.workstation.browser.api.services;

import java.util.Map;

import org.janelia.workstation.integration.framework.system.InspectionHandler;
import org.janelia.workstation.browser.gui.components.DomainInspectorTopComponent;
import org.janelia.model.domain.DomainObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = InspectionHandler.class, path=InspectionHandler.LOOKUP_PATH)
public class ConsoleInspectorHandler implements InspectionHandler {

    @Override
    public void inspect(Object object) {
        DomainInspectorTopComponent.getInstance().inspect(object);
    }

    @Override
    public void inspect(DomainObject domainObject) {
        DomainInspectorTopComponent.getInstance().inspect(domainObject);
    }

    @Override
    public void inspect(Map<String, Object> properties) {
        DomainInspectorTopComponent.getInstance().inspect(properties);
    }
    
}
