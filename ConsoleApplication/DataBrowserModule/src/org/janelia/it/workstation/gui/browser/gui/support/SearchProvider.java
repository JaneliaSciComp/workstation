package org.janelia.it.workstation.gui.browser.gui.support;

/**
 * An interface for dealing with the component that provides searching of large result sets with some 
 * sorting. Kind of kludgey, we might want to refactor this later. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SearchProvider {

	/**
	 * Set the sort field. The field name may be prefixed with a +/- to control the sort direction.
	 * @param sortField
	 */
	public void setSortField(String sortField);
	
	/**
	 * Re-run the current search with updated preferences. 
	 */
	public void search();
	
}
