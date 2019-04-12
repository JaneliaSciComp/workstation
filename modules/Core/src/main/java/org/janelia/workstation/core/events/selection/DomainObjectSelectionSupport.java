package org.janelia.workstation.core.events.selection;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

/**
 * Interface that indicates a component supports domain object selection. Typically
 * this is implemented by UI components which display multiple domain objects 
 * and allow the user to interact with them.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectSelectionSupport extends ChildSelectionSupport<DomainObject,Reference> {

	/**
	 * Returns the selection model associated with this component. 
	 * @return selection model
	 */
    @Override
    public DomainObjectSelectionModel getSelectionModel();
    
}
