package org.janelia.it.workstation.nb_action;

import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * Implement this to make a means of adding one or more domain objects to 
 * some sort of container of such objects.  Generally, this will involve
 * making proxies of some kind, to those objects.
 * 
 * @author fosterl
 */
public interface DomainObjectAppender extends Compatible<List<DomainObject>> {
    public static final String LOOKUP_PATH = "DomainPerspective/DomainObjectAppender";
    
    void useDomainObjects( List<DomainObject> l );
	
	/** 
	 * All objects in list must be compatible. 
	 * @param l selection for comparison.
	 * @return T=all compatible.
	 */
    @Override
    boolean isCompatible( List<DomainObject> l );
    String getActionLabel();
}
