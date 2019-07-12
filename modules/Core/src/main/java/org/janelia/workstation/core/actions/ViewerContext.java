package org.janelia.workstation.core.actions;

import java.util.Collection;

import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.ImageModel;

/**
 * Current viewer context which can be used to construct context menus.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ViewerContext<T,S> {

    ChildSelectionModel<T,S> getSelectionModel();

    ChildSelectionModel<T,S> getEditSelectionModel();

    ImageModel<T,S> getImageModel();

    default Object getContextObject() {
        return getSelectionModel().getParentObject();
    }

    default boolean isMultiple() {
        return getSelectionModel().getSelectedIds().size() > 1;
    }

    default T getLastSelectedObject() {
        return getImageModel().getImageByUniqueId(getSelectionModel().getLastSelectedId());
    }

    default Collection<T> getSelectedObjects() {
        return getSelectionModel().getObjects();
    }
}
