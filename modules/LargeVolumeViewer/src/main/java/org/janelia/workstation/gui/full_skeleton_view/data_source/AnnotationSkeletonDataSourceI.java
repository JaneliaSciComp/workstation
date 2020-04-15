package org.janelia.workstation.gui.full_skeleton_view.data_source;

import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.workstation.gui.large_volume_viewer.style.NeuronStyleModel;

/**
 * Implement this to provide data to the skeleton view.
 * 
 * @author fosterl
 */
public interface AnnotationSkeletonDataSourceI {
    Skeleton getSkeleton();
    NeuronStyleModel getNeuronStyleModel();
    TileFormat getTileFormat();
    NeuronManager getAnnotationModel();
}
