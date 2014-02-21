package org.janelia.it.FlyWorkstation.gui.dialogs.search;

/**
 * Listener interface for search configurations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SearchConfigurationListener extends java.util.EventListener {

    void configurationChange(SearchConfigurationEvent evt); 
	
}
