package org.janelia.it.workstation.gui.browser.gui.editor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionSupport;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectSelectionEditor<T extends DomainObject> extends DomainObjectEditor<T>, DomainObjectSelectionSupport {
        
}
