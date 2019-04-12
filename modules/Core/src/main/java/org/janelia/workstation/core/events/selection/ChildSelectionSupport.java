package org.janelia.workstation.core.events.selection;

/**
 * Interface that indicates a component supports domain object selection. Typically
 * this is implemented by UI components which display multiple domain objects 
 * and allow the user to interact with them.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ChildSelectionSupport<T,S> {

	/**
	 * Returns the selection model associated with this component. 
	 * @return selection model
	 */
    public ChildSelectionModel<T,S> getSelectionModel();
    
}
