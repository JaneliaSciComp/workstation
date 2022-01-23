package org.janelia.workstation.core.actions;

import org.janelia.workstation.core.events.selection.ChildSelectionModel;

import java.util.Collection;

/**
 * Current viewer context which can be used to construct context menus.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ViewerContext<T,S> {

    ChildSelectionModel<T,S> getSelectionModel();

    ChildSelectionModel<T,S> getEditSelectionModel();

    Object getViewerModel();

    default Object getContextObject() {
        return getSelectionModel().getParentObject();
    }

    default boolean isMultiple() {
        return getSelectionModel().getSelectedIds().size() > 1;
    }

    default T getLastSelectedObject() {
        return getSelectionModel().getLastSelectedObject();
    }

    default Collection<T> getSelectedObjects() {
        return getSelectionModel().getObjects();
    }
}
