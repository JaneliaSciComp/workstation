package org.janelia.it.workstation.browser.api.services;

import java.util.Map;

import org.janelia.it.jacs.integration.framework.system.InspectionHandler;
import org.janelia.it.workstation.browser.components.DomainInspectorTopComponent;
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
