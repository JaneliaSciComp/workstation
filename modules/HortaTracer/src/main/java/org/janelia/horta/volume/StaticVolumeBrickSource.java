package org.janelia.horta.volume;

import java.util.Collection;
import org.janelia.geometry3d.Box3;

/**
 * StaticVolumeBrickSource interface is intended to expose a uniform interface
 * to loading:
 *   - single raster volume files (like any volume file...)
 *   - octree raster volume files (like in Mouse Light)
 *   - ad hoc multiresolution raster volume files (like in Fly Light fast load)
 * 
 * In each case, the expectation is that it's possible to load volume metadata
 * before loading the actual large raster image data.
 * 
 * @author Christopher Bruns
 */
public interface StaticVolumeBrickSource 
{
    public enum FileType {
        MJ2 ("mj2"), TIFF ("tif"), ZARR("zarr");

        FileType(String ext) {
            extension = ext;
        }
        String extension;

        public String getExtension() {
            return extension;
        }
    };
    Collection<Double> getAvailableResolutions();
    BrickInfoSet getAllBrickInfoForResolution(Double resolution);
    FileType getFileType();
}
