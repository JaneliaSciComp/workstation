package org.janelia.workstation.common.gui.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.ImageModel;

/**
 * Utility methods for dealing with Domain Objects in context of the UI.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainUIUtils {

    public static DomainObjectImageModel getDomainObjectImageModel(ViewerContext viewerContext) {
        if (viewerContext==null) return null;
        ImageModel imageModel = viewerContext.getImageModel();
        if (imageModel instanceof DomainObjectImageModel) {
            return (DomainObjectImageModel) imageModel;
        }
        return null;
    }

    public static DomainObject getLastSelectedDomainObject(ViewerContext viewerContext) {
        if (viewerContext==null) return null;
        if (viewerContext.getLastSelectedObject() instanceof DomainObject) {
            return ((DomainObject) viewerContext.getLastSelectedObject());
        }
        return null;
    }

    public static Collection<DomainObject> getSelectedDomainObjects(ViewerContext viewerContext) {
        if (viewerContext==null) return null;
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Object obj : viewerContext.getSelectedObjects()) {
            if (obj instanceof DomainObject) {
                DomainObject domainObject = (DomainObject) obj;
                domainObjects.add(domainObject);
            }
        }
        return domainObjects;
    }

    /**
     * Returns the subset of the given objects which are of a certain class.
     * @param objects list of objects to search
     * @param clazz class to filter by
     * @param <T> type of object to return
     * @return subset of the list
     */
    public static <T> Collection<T> getObjectsOfType(Collection<?> objects, Class<T> clazz) {
        return objects
                .stream()
                .filter(d -> clazz.isAssignableFrom(d.getClass()))
                .map(clazz::cast)
                .collect(Collectors.toList());
    }
}
