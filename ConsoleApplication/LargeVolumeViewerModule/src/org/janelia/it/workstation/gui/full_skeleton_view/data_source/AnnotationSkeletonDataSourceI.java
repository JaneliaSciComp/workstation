package org.janelia.it.workstation.gui.full_skeleton_view.data_source;

import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;

/**
 * Implement this to provide data to the skeleton view.
 * 
 * @author fosterl
 */
public interface AnnotationSkeletonDataSourceI {
    Skeleton getSkeleton();
    NeuronStyleModel getNeuronStyleModel();
    TileFormat getTileFormat();
    AnnotationManager getAnnotationManager();
}
