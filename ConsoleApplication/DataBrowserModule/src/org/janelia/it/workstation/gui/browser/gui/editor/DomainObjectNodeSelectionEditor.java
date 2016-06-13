package org.janelia.it.workstation.gui.browser.gui.editor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionSupport;

/**
 * An interface for domain object node editors that have selection support.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeSelectionEditor<T extends DomainObject> extends DomainObjectNodeEditor<T>, DomainObjectSelectionSupport {
        
}
