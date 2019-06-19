
package org.janelia.horta;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import Jama.Matrix;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.VoxelIndex;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.integration.spi.compression.CompressedFileResolverI;
import org.janelia.it.jacs.shared.img_3d_loader.FileStreamSource;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents Mouse Brain tile information entry from tilebase.cache.yml file.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BrainTileInfo implements BrickInfo {
    private static final Logger LOG = LoggerFactory.getLogger(BrainTileInfo.class);

    private final String parentPath;
    private final int[] bbOriginNanometers;
    private final int[] bbShapeNanometers;
    private final String localPath;
    private final int[] pixelDims; // includes color channel count
    private final int bytesPerIntensity;
    private Matrix transform; // converts voxels to stage coordinates in nanometers
    private Matrix texCoord_X_stageUm; // cached transform inverse; after conversion to micrometers
    
    // TODO  colorChannelIndex is a temporary hack that should be removed when we can show more than one channel at once
    private int colorChannelIndex = 0;
    private boolean leverageCompressedFiles;

    public BrainTileInfo(String parentPath,
                         String localPath,
                         boolean leverageCompressedFiles,
                         int[] bbOriginNanometers,
                         int[] bbShapeNanometers,
                         int[] pixelDims,
                         int bytesPerIntensity,
                         Matrix transform) {
        this.parentPath = parentPath;
        this.localPath = localPath;
        this.leverageCompressedFiles = leverageCompressedFiles;
        this.bbOriginNanometers = bbOriginNanometers;
        this.bbShapeNanometers = bbShapeNanometers;
        this.pixelDims = pixelDims;
        this.bytesPerIntensity = bytesPerIntensity;
        this.transform = transform;
    }
    
    public String getLocalPath() {
        return localPath;
    }

    public String getParentPath() {
        return parentPath;
    }

    /**
     * Construct a matrix to help convert spatial positions to texture
     * coordinates; for use in efficient ray casting.
     * 
     * @return Matrix that maps microscope stage coordinates, in micrometers,
     * to normalized 3D texture coordinates in tile.
     */
    public Matrix getTexCoord_X_stageUm() {
        // Compute matrix just-in-time
        if (texCoord_X_stageUm == null) {
            // Standard transform converts voxel positions into microscope stage coordinates (in nanometers)
            // Remove weird channel dimension, lowering size from 5x5 to 4x4
            Matrix m5 = transform;
            // m5.print(5, 5);
            Matrix stageNm_X_voxel = new Matrix(new double[][] {
                {m5.get(0, 0), m5.get(0, 1), m5.get(0, 2), m5.get(0, 4)},
                {m5.get(1, 0), m5.get(1, 1), m5.get(1, 2), m5.get(1, 4)},
                {m5.get(2, 0), m5.get(2, 1), m5.get(2, 2), m5.get(2, 4)},
                {m5.get(4, 0), m5.get(4, 1), m5.get(4, 2), m5.get(4, 4)}});
            // stageNm_X_voxel.print(16, 12);
            // Invert, to turn microscope stage coordinates into voxel positions:
            Matrix voxel_X_stageNm = stageNm_X_voxel.inverse();
            // voxel_X_stageNm.print(16, 12);
            // Convert nanometers to micrometers:
            Matrix nm_X_um = new Matrix(new double[][] {
                {1000, 0, 0, 0},
                {0, 1000, 0, 0},
                {0, 0, 1000, 0},
                {0, 0, 0, 1}});
            Matrix voxel_X_stageUm = voxel_X_stageNm.times(nm_X_um);
            // For ray casting, convert from stageUm to texture coordinates (i.e. normalized voxels)
            // voxel_X_stageUm.print(16, 12);
            Matrix tc_X_vx = new Matrix(new double[][] {
                {1.0/pixelDims[0], 0, 0, 0},
                {0, 1.0/pixelDims[1], 0, 0},
                {0, 0, 1.0/pixelDims[2], 0},
                {0, 0, 0, 1}});
            texCoord_X_stageUm = tc_X_vx.times(voxel_X_stageUm);
        }
        return texCoord_X_stageUm;
    }

    /**
     * 
     * @return resolution in nanometers
     */
    float getMinResolutionNanometers() {
        float resolution = Float.MAX_VALUE;
        for (int xyz = 0; xyz < 3; ++xyz) {
            float res = bbShapeNanometers[xyz] / (float)pixelDims[xyz];
            if (res < resolution)
                resolution = res;
        }
        return resolution;
    }

    @Override
    public List<? extends ConstVector3> getCornerLocations() 
    {
        List<ConstVector3> result = new ArrayList<>();
        for (int pz : new int[]{0, pixelDims[2]}) {
            for (int py : new int[]{0, pixelDims[1]}) {
                for (int px : new int[]{0, pixelDims[0]}) {
                    Matrix corner = new Matrix(new double[]{px, py, pz, 0, 1}, 5);
                    Matrix um = transform.times(corner).times(1.0 / 1000.0);
                    ConstVector3 v = new Vector3(
                            (float) um.get(0, 0),
                            (float) um.get(1, 0),
                            (float) um.get(2, 0));
                    result.add(v);
                }
            }
        }
        return result;
    }

    @Override
    public List<Vector3> getValidCornerLocations() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Vector3> getTilingSubsetLocations() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoxelIndex getRasterDimensions() {
        return new VoxelIndex(pixelDims[0], pixelDims[1], pixelDims[2]);
    }

    @Override
    public int getChannelCount() {
        return pixelDims[3];
    }

    @Override
    public int getBytesPerIntensity() {
        return bytesPerIntensity;
    }

    @Override
    public double getResolutionMicrometers() {
        return getMinResolutionNanometers() / 1000.0;
    }

    @Override
    public Box3 getBoundingBox() {
        Box3 result = new Box3();
        Vector3 bbOrigin = new Vector3(
                bbOriginNanometers[0],
                bbOriginNanometers[1],
                bbOriginNanometers[2]);
        Vector3 bbSize = new Vector3(
                bbShapeNanometers[0],
                bbShapeNanometers[1],
                bbShapeNanometers[2]);
        bbOrigin = bbOrigin.multiplyScalar(1e-3f); // Convert nm to um
        bbSize = bbSize.multiplyScalar(1e-3f); // Convert nm to um
        result.include(bbOrigin);
        result.include(bbOrigin.add(bbSize));
        return result;
    }

    public boolean folderExists() {
        // OS specific path should have already been translated in LocalVolumeBrickSource
        File folderPath = new File(parentPath, localPath);
        return folderPath.exists();
    }

    // TODO - remove this hack after we can show more than one channel at a time
    public int getColorChannelIndex() {
        return this.colorChannelIndex;
    }
    
    public void setColorChannelIndex(int index) {
        this.colorChannelIndex = index;
    }

    /**
     * Loads the texture and returns it.
     *
     * Returns null if the load is interrupted. It would be better to use an InterruptedIOException for that,
     * but it's too slow.
     */
    public Texture3d loadBrick(double maxEdgePadWidth, int colorChannel) throws IOException
    {
        setColorChannelIndex(colorChannel);
        return loadBrick(maxEdgePadWidth);
    }

    @Override
    public Texture3d loadBrick(double maxEdgePadWidth) throws IOException {
        // OS specific path should have already been translated in LocalVolumeBrickSource

        File folderPath = new File(parentPath, localPath);
        File tileFile = null;

        Texture3d texture = new Texture3d();

        if (!HttpDataSource.useHttp()) {

            if (!folderExists())
                throw new IOException("no such tile folder " + folderPath.getAbsolutePath());

            CompressedFileResolverI resolver = FrameworkAccess.getCompressedFileResolver();

            if (leverageCompressedFiles && resolver == null) {
                throw new IOException("Failed to find compression resolver.");
            }

            // That path is just a folder. Now find the actual files.
            // TODO - this just loads the first channel.
            String imageSuffix = "." + Integer.toString(colorChannelIndex); // + ".tif";
            File compressedTileFile = null;
            for (File file : folderPath.listFiles()) {
                if (leverageCompressedFiles && file.getName().contains(imageSuffix) && resolver.canDecompress(file)) {
                    File decompressedName = resolver.getDecompressedNameForFile(file);
                    if (decompressedName != null && decompressedName.getName().endsWith(imageSuffix)) {
                        LOG.info("Starting with compressed version of file {}.", file);
                        compressedTileFile = file;
                        break;
                    }
                }
                // Use the first channel file
                if (file.getName().endsWith(imageSuffix + ".tif")) {
                    LOG.info("Using never-compressed version of file {}.", file);
                    tileFile = file;
                    break;
                }
            }
            if (compressedTileFile != null) {
                try {
                    LOG.info("Decompressing...");
                    tileFile = resolver.decompressToFile(compressedTileFile);
                    LOG.info("Decompressed as {}.", tileFile);
                }
                catch (Exception ex) {
                    throw new IOException("Decompression step failed for " + compressedTileFile, ex);
                }
            }
            if (tileFile == null){
                throw new IOException("No channel tiff file found");
            } else {
                LOG.debug("loadBrick {}", tileFile.getAbsolutePath());
            }

        } else {

            tileFile=new File(folderPath, "default."+colorChannelIndex);
            LOG.debug("loadBrick {}", tileFile.getAbsolutePath());

            texture.setOptionalFileStreamSource(new FileStreamSource() {
                @Override
                public GetMethod getStreamForFile(String filepath) throws Exception {
                    return HttpDataSource.getMouseLightTiffStream(filepath);
                }
            });

        }

        if (!texture.loadTiffStack(tileFile)) {
            return null;
        }

        return texture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BrainTileInfo that = (BrainTileInfo) o;

        return new EqualsBuilder()
                .append(parentPath, that.parentPath)
                .append(localPath, that.localPath)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(parentPath)
                .append(localPath)
                .toHashCode();
    }

    @Override
    public boolean isSameBrick(BrickInfo other) {
        if (!(other instanceof BrainTileInfo)) {
            return false;
        }
        BrainTileInfo rhs = (BrainTileInfo) other;
        if (rhs.getColorChannelIndex() != this.getColorChannelIndex())
            return false;
        
        return rhs.parentPath.equals(parentPath) && rhs.localPath.equals(localPath);

    }
}
