package org.janelia.it.workstation.browser.gui.editor;

import org.janelia.it.workstation.browser.events.selection.ChildPickingSupport;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionSupport;
import org.janelia.model.domain.DomainObject;

/**
 * Convenience interface for domain object node editors which also implement child selection and child picking. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ParentNodeSelectionEditor<P extends DomainObject, T, S> 
    extends DomainObjectNodeEditor<P,T,S>, 
            ChildSelectionSupport<T,S>,
            ChildPickingSupport<T,S> {
}
