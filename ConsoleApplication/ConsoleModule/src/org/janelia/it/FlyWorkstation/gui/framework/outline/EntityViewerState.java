package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.Set;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;

/**
 * Snapshot of the state of an entity viewer for navigation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityViewerState {
    
    private final Class<?> viewerClass;
    private final RootedEntity contextRootedEntity;
    private final Set<String> selectedIds;

    public EntityViewerState(Class<?> viewerClass, RootedEntity contextRootedEntity, Set<String> selectedIds) {
        this.viewerClass = viewerClass;
        this.contextRootedEntity = contextRootedEntity;
        this.selectedIds = selectedIds;
    }

    public Class<?> getViewerClass() {
        return viewerClass;
    }

    public RootedEntity getContextRootedEntity() {
        return contextRootedEntity;
    }

    public Set<String> getSelectedIds() {
        return selectedIds;
    }
}
