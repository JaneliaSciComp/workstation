package org.janelia.it.workstation.browser.gui.editor;

import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionSupport;
import org.janelia.model.domain.DomainObject;

/**
 * An interface for domain object node editors that have selection support.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeSelectionEditor<T extends DomainObject> extends DomainObjectNodeEditor<T>, DomainObjectSelectionSupport {
        
}
