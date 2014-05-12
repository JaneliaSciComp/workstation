package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.util.EventObject;

/**
 * Event fired when the search configuration has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SearchConfigurationEvent extends EventObject {

	private final SearchConfiguration searchConfig;
	
	public SearchConfigurationEvent(SearchConfiguration searchConfig) {
		super(searchConfig);
		this.searchConfig = searchConfig;
	}

	public SearchConfiguration getSearchConfig() {
		return searchConfig;
	}
}
