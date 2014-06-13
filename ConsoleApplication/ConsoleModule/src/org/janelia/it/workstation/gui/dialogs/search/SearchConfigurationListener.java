package org.janelia.it.workstation.gui.dialogs.search;

import java.util.EventListener;

/**
 * Listener interface for search configurations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SearchConfigurationListener extends EventListener {

    void configurationChange(SearchConfigurationEvent evt); 
	
}
