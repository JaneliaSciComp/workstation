package org.janelia.horta.cache;

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.geometry3d.Box3;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.StaticVolumeBrickSource;

import java.util.Collection;

/**
 * Created by murphys on 1/19/2016.
 */
public class VolumeCacheBrickSource implements StaticVolumeBrickSource {


    @Override
    public Collection<Double> getAvailableResolutions() {
        return null;
    }

    @Override
    public BrickInfoSet getAllBrickInfoForResolution(Double resolution) {
        return null;
    }

    @Override
    public Box3 getBoundingBox() {
        return null;
    }

    public static VolumeCacheBrickSource createFromSampleLocation(SampleLocation sampleLocation) {
        return null;
    }
}
