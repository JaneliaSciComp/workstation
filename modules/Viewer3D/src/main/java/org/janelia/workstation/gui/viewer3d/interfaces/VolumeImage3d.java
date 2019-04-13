package org.janelia.workstation.gui.viewer3d.interfaces;

import java.net.URL;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;

public interface VolumeImage3d {

    BoundingBox3d getBoundingBox3d();

    int getMaximumIntensity(); // e.g. "255" for 8-bit images

    double getXResolution(); // in scene units

    double getYResolution(); // in scene units

    double getZResolution(); // in scene units

    int getNumberOfChannels(); // e.g. "3" for RGB

    boolean loadURL(URL volumeURL);

    double getResolution(int ix);

    /**
     * Position at center of a voxel in the center of this volume
     *
     * @return
     */
    Vec3 getVoxelCenter();
}
