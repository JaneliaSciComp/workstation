package org.janelia.it.workstation.browser.gui.editor;

import org.janelia.it.workstation.browser.events.selection.ChildSelectionSupport;
import org.janelia.model.domain.DomainObject;

public interface ParentNodeSelectionEditor<P extends DomainObject, T, S> extends DomainObjectNodeEditor<P>, ChildSelectionSupport<T,S> {
}
