package org.janelia.it.workstation.gui.large_volume_viewer;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.VolumeLoadListener;
import org.janelia.workstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedVolumeImage implements VolumeImage3d {

    private static final Logger LOG = LoggerFactory.getLogger(SharedVolumeImage.class);

    private final BoundingBox3d boundingBox3d = new BoundingBox3d();
    private final Collection<VolumeLoadListener> volumeLoadListeners = new ArrayList<>();
    private BlockTiffOctreeLoadAdapter loadAdapter;
    private URL volumeBaseURL;

    public void addVolumeLoadListener(VolumeLoadListener l) {
        volumeLoadListeners.add(l);
    }

    public void removeVolumeLoadListener(VolumeLoadListener l) {
        volumeLoadListeners.remove(l);
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox3d;
    }

    @Override
    public int getMaximumIntensity() {
        if (getLoadAdapter() == null) {
            return 0;
        }
        return getLoadAdapter().getTileFormat().getIntensityMax();
    }

    @Override
    public Vec3 getVoxelCenter() {
        Vec3 result = new Vec3();
        for (int i = 0; i < 3; ++i) {
            final Double boundingMin = getBoundingBox3d().getMin().get(i);
            double range = getBoundingBox3d().getMax().get(i) - boundingMin;
            int voxelCount = (int) Math.round(range / getResolution(i));
            int midVoxel = voxelCount / 2;
            double center = 0.0;
            center = (midVoxel + 0.5) * getResolution(i) + boundingMin;
            result.set(i, center);
        }
        return result;
    }

    @Override
    public double getXResolution() {
        if (getLoadAdapter() == null) {
            return 0;
        }
        return getLoadAdapter().getTileFormat().getVoxelMicrometers()[0];
    }

    @Override
    public double getYResolution() {
        if (getLoadAdapter() == null) {
            return 0;
        }
        return getLoadAdapter().getTileFormat().getVoxelMicrometers()[1];
    }

    @Override
    public double getZResolution() {
        if (getLoadAdapter() == null) {
            return 0;
        }
        return getLoadAdapter().getTileFormat().getVoxelMicrometers()[2];
    }

    public int[] getOrigin() {
        if (getLoadAdapter() == null) {
            return new int[]{0, 0, 0};
        } else {
            return getLoadAdapter().getTileFormat().getOrigin();
        }
    }

    @Override
    public int getNumberOfChannels() {
        if (getLoadAdapter() == null) {
            return 0;
        }
        return getLoadAdapter().getTileFormat().getChannelCount();
    }

    public URL getVolumeBaseURL() {
        return volumeBaseURL;
    }

    public void setVolumeBaseURL(URL volumeBaseURL) {
        this.volumeBaseURL = volumeBaseURL;
    }

    @Override
    public boolean loadURL(URL volumeBaseURL) {
        // Sanity check before overwriting current view                                                                                                                                                                                                                                                                     
        if (volumeBaseURL == null) {
            LOG.warn("volumeBaseURL is null");
            return false;
        }

        loadAdapter = TileStackCacheController.createInstance(
                new TileStackOctreeLoadAdapter(new TileFormat(), URI.create(volumeBaseURL.toString())));
        loadAdapter.loadMetadata();

        // Update bounding box
        // Compute bounding box
        TileFormat tf = getLoadAdapter().getTileFormat();
        BoundingBox3d newBox = tf.calcBoundingBox();

        boundingBox3d.setMin(newBox.getMin());
        boundingBox3d.setMax(newBox.getMax());

        LOG.info("Volume loaded: {}", volumeBaseURL);
        fireVolumeLoaded(volumeBaseURL);

        return true;
    }

    @Override
    public double getResolution(int ix) {
        if (ix == 0) {
            return getXResolution();
        } else if (ix == 1) {
            return getYResolution();
        } else {
            return getZResolution();
        }
    }

    public AbstractTextureLoadAdapter getLoadAdapter() {
        return loadAdapter;
    }

    private void fireVolumeLoaded(URL volumeBaseURL) {
        for (VolumeLoadListener l : volumeLoadListeners) {
            l.volumeLoaded(volumeBaseURL);
        }
    }
}
