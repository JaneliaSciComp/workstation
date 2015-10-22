package org.janelia.it.workstation.cache.large_volume;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.OctreeMetadataSniffer;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Created by murphys on 10/20/2015.
 */
public class CubicNeighborhoodBuilder extends GeometricNeighborhoodBuilder {

    private static Logger log = LoggerFactory.getLogger(CubicNeighborhoodBuilder.class);

    @Override
    public CubicNeighborhood buildNeighborhood(double[] focus, Double zoom, double pixelsPerSceneUnit) {
        return null;
    }

}
