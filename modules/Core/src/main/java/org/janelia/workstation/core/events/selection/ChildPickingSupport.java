package org.janelia.workstation.core.events.selection;

/**
 * Interface that indicates a component supports domain object picking. Typically
 * this is implemented by UI components which display multiple domain objects 
 * and allow the user to use checkboxes to pick a subset as targets for future operations.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ChildPickingSupport<T,S> {

	/**
	 * Returns the selection model associated with this component. 
	 * @return selection model
	 */
    public ChildSelectionModel<T,S> getEditSelectionModel();
    
}
