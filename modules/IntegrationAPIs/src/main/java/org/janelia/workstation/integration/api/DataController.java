package org.janelia.workstation.integration.api;

import java.util.List;

/**
 * Service for dealing with data in the client side.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DataController {

    /**
     * Generate any number of globally unique identifiers.
     * @param count number of identifiers to generate
     * @return list of new GUIDs
     */
    List<Number> generateGUIDs(long count);

}
