package org.janelia.horta.volume;

import Jama.Matrix;
import java.io.IOException;
import java.util.List;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.gltools.texture.Texture3d;

/**
 *
 * @author Christopher Bruns
 */
public interface BrickInfo {
    List<? extends ConstVector3> getCornerLocations(); // XYZ at extreme corners of raster volume
    List<? extends ConstVector3> getValidCornerLocations(); // XYZ at corners of non-zero subvolume, e.g. to ignore openGL padding to 4-byte boundary
    List<? extends ConstVector3> getTilingSubsetLocations(); // XYZ at corners of minimal tiling subvolume, e.g. to ignore adjacent tile overlap regions
    VoxelIndex getRasterDimensions(); // Length of principal edges, in voxels
    int getChannelCount(); // Number of color channels
    int getBytesPerIntensity(); // Number of bytes per intensity
    double getResolutionMicrometers(); // Finest resolution
    Box3 getBoundingBox();
    Texture3d loadBrick(double maxEdgePadWidth, String fileExtension) throws IOException;
    boolean isSameBrick(BrickInfo other);
    Matrix getStageCoordToTexCoord(); // Matrix that transforms stage coordinates to local texture coordinates
}
