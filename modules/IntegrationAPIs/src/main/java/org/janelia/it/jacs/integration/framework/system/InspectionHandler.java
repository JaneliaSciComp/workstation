package org.janelia.it.jacs.integration.framework.system;

import java.util.Map;

import org.janelia.model.domain.DomainObject;

/**
 * Service which handles requests for object inspection. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface InspectionHandler {

    public static final String LOOKUP_PATH = "Handlers/InspectionHandler";
    
    void inspect(Object object);
    
    void inspect(DomainObject domainObject);
    
    void inspect(Map<String,Object> properties);
}
