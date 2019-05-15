package org.janelia.workstation.common.gui.editor;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.model.domain.DomainObject;
import org.openide.windows.TopComponent;

/**
 * Snapshot of the state of a list viewer for navigation purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public interface DomainObjectEditorState<P extends DomainObject, T, S> {

    P getDomainObject();

    void setDomainObject(DomainObject domainObject);

    Integer getPage();

    ListViewerState getListViewerState();

    Collection<S> getSelectedIds();

    DomainObjectNode<P> getDomainObjectNode();

    <C extends TopComponent> C getTopComponent();

    <C extends TopComponent> void setTopComponent(C topComponent);

}
