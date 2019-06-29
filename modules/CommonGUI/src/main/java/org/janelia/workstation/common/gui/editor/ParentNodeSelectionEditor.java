package org.janelia.workstation.common.gui.editor;

import org.janelia.workstation.core.events.selection.ChildPickingSupport;
import org.janelia.workstation.core.events.selection.ChildSelectionSupport;
import org.janelia.model.domain.DomainObject;

/**
 * Convenience interface for domain object node editors which also implement child selection and child picking. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ParentNodeSelectionEditor<P extends DomainObject, T, S> 
    extends DomainObjectNodeEditor<P,T,S>,
        ChildSelectionSupport<T,S>, // TODO: obsolete, replace with calls to ViewerContextProvider
        ChildPickingSupport<T,S>, // TODO: obsolete, replace with calls to ViewerContextProvider
        ViewerContextProvider<T,S> {
}
