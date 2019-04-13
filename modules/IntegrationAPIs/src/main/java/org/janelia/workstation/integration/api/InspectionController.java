package org.janelia.workstation.integration.api;

import java.util.Map;

import org.janelia.model.domain.DomainObject;

/**
 * Service which handles requests for object inspection. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface InspectionController {

    public static final String LOOKUP_PATH = "Handlers/InspectionController";
    
    void inspect(Object object);
    
    void inspect(DomainObject domainObject);
    
    void inspect(Map<String,Object> properties);
}
