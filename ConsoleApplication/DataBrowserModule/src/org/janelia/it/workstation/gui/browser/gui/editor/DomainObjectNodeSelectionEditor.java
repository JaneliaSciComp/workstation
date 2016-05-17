package org.janelia.it.workstation.gui.browser.gui.editor;

import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionSupport;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;

/**
 * An interface for domain object node editors that have selection support.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectNodeSelectionEditor<T extends DomainObjectNode> extends DomainObjectNodeEditor<T>, DomainObjectSelectionSupport {
        
}
