package org.janelia.workstation.common.gui.support;

/**
 * An interface for dealing with the component that provides searching/sorting/pagination of large result sets. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SearchProvider {

	/**
	 * Get the sort field. The field name may be prefixed with a +/- to control the sort direction.
	 */
	String getSortField();

	/**
	 * Set the sort field. The field name may be prefixed with a +/- to control the sort direction.
	 * @param sortField
	 */
	void setSortField(String sortField);
	
	/**
	 * Re-run the current search with updated preferences. 
	 */
	void search();

	/**
	 * Export the current results.
	 */
    void export();
	
}
