package org.janelia.it.workstation.browser.gui.editor;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

/**
 * An interface for domain object node editors that have selection support.
 * 
 * P - the type of parent object
 * T - the type of child objects
 * S - the id type of the child objects
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeSelectionEditor<P extends DomainObject> extends ParentNodeSelectionEditor<P, DomainObject, Reference> {
        
}
