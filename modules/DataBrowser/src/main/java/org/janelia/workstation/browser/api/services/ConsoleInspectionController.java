package org.janelia.workstation.browser.api.services;

import java.util.Map;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.components.DomainInspectorTopComponent;
import org.janelia.workstation.integration.api.InspectionController;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = InspectionController.class, path= InspectionController.LOOKUP_PATH)
public class ConsoleInspectionController implements InspectionController {

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
