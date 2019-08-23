
package org.janelia.horta;

import Jama.Matrix;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.VoxelIndex;
import org.janelia.rendering.RawImage;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents Mouse Brain tile information entry from tilebase.cache.yml file.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BrainTileInfo implements BrickInfo {
    private static final Logger LOG = LoggerFactory.getLogger(BrainTileInfo.class);

    private final RenderedVolumeLocation volumeLocation;
    private final String basePath;
    private final int[] bbOriginNanometers;
    private final int[] bbShapeNanometers;
    private final String tileRelativePath;
    private final int[] pixelDims; // includes color channel count
    private final int bytesPerIntensity;
    private Matrix transform; // converts voxels to stage coordinates in nanometers
    private Matrix stageCoordToTexCoord; // cached transform inverse; after conversion to micrometers
    
    // TODO  colorChannelIndex is a temporary hack that should be removed when we can show more than one channel at once
    private int colorChannelIndex = 0;
    private boolean leverageCompressedFiles;

    BrainTileInfo(RenderedVolumeLocation volumeLocation,
                  String basePath,
                  String tileRelativePath,
                  boolean leverageCompressedFiles,
                  int[] bbOriginNanometers,
                  int[] bbShapeNanometers,
                  int[] pixelDims,
                  int bytesPerIntensity,
                  Matrix transform) {
        this.volumeLocation = volumeLocation;
        this.basePath = basePath;
        this.tileRelativePath = tileRelativePath;
        this.leverageCompressedFiles = leverageCompressedFiles;
        this.bbOriginNanometers = bbOriginNanometers;
        this.bbShapeNanometers = bbShapeNanometers;
        this.pixelDims = pixelDims;
        this.bytesPerIntensity = bytesPerIntensity;
        this.transform = transform;
    }
    
    public String getTileRelativePath() {
        return tileRelativePath;
    }

    /**
     * Construct a matrix to help convert spatial positions to texture
     * coordinates; for use in efficient ray casting.
     * 
     * @return Matrix that maps microscope stage coordinates, in micrometers,
     * to normalized 3D texture coordinates in tile.
     */
    @Override
    public Matrix getStageCoordToTexCoord() {
        // Compute matrix just-in-time
        if (stageCoordToTexCoord == null) {
            // Standard transform converts voxel positions into microscope stage coordinates (in nanometers)
            // Remove weird channel dimension, lowering size from 5x5 to 4x4
            Matrix voxelToStageNanos = new Matrix(
                    new double[][] {
                {transform.get(0, 0), transform.get(0, 1), transform.get(0, 2), transform.get(0, 4)},
                {transform.get(1, 0), transform.get(1, 1), transform.get(1, 2), transform.get(1, 4)},
                {transform.get(2, 0), transform.get(2, 1), transform.get(2, 2), transform.get(2, 4)},
                {transform.get(4, 0), transform.get(4, 1), transform.get(4, 2), transform.get(4, 4)}});
            // Invert, to turn microscope stage coordinates into voxel positions:
            Matrix stageNanosToVoxel = voxelToStageNanos.inverse();
            // Convert nanometers to micrometers:
            Matrix nanosToMicros = new Matrix(new double[][] {
                {1000, 0, 0, 0},
                {0, 1000, 0, 0},
                {0, 0, 1000, 0},
                {0, 0, 0, 1}});
            // For ray casting, convert from stageUm to texture coordinates (i.e. normalized voxels)
            Matrix nanosToPixel = new Matrix(new double[][] {
                {1.0/pixelDims[0], 0, 0, 0},
                {0, 1.0/pixelDims[1], 0, 0},
                {0, 0, 1.0/pixelDims[2], 0},
                {0, 0, 0, 1}});
            stageCoordToTexCoord = nanosToPixel.times(stageNanosToVoxel).times(nanosToMicros);
        }
        return stageCoordToTexCoord;
    }

    /**
     *
     * @return resolution in nanometers
     */
    private float getMinResolutionNanometers() {
        float resolution = Float.MAX_VALUE;
        for (int xyz = 0; xyz < 3; ++xyz) {
            float res = bbShapeNanometers[xyz] / (float)pixelDims[xyz];
            if (res < resolution)
                resolution = res;
        }
        return resolution;
    }

    @Override
    public List<? extends ConstVector3> getCornerLocations() {
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

    // TODO - remove this hack after we can show more than one channel at a time
    int getColorChannelIndex() {
        return this.colorChannelIndex;
    }
    
    private void setColorChannelIndex(int index) {
        this.colorChannelIndex = index;
    }

    /**
     * Loads the texture and returns it.
     *
     * Returns null if the load is interrupted. It would be better to use an InterruptedIOException for that,
     * but it's too slow.
     */
    public Texture3d loadBrick(double maxEdgePadWidth, int colorChannel) throws IOException {
        setColorChannelIndex(colorChannel);
        return loadBrick(maxEdgePadWidth);
    }

    @Override
    public Texture3d loadBrick(double maxEdgePadWidth) throws IOException {
        Texture3d texture = new Texture3d();

        RawImage rawImage = new RawImage();
        rawImage.setRenderedVolumePath(volumeLocation.getVolumeLocation().toString());
        rawImage.setAcquisitionPath(basePath);
        rawImage.setRelativePath(tileRelativePath);
        rawImage.setOriginInNanos(Arrays.stream(bbOriginNanometers).boxed().toArray(Integer[]::new));
        rawImage.setDimsInNanos(Arrays.stream(bbShapeNanometers).boxed().toArray(Integer[]::new));
        rawImage.setBytesPerIntensity(bytesPerIntensity);
        rawImage.setTileDims(Arrays.stream(pixelDims).boxed().toArray(Integer[]::new));
        rawImage.setTransform(Arrays.stream(transform.getRowPackedCopy()).boxed().toArray(Double[]::new));

        return volumeLocation.getRawTileContent(rawImage, colorChannelIndex)
               .consume(rawImageStream -> {
                   try {
                       if (!texture.loadTiffStack(rawImage.toString() + "-ch-" + colorChannelIndex, rawImageStream)) {
                           return null;
                       } else {
                           return texture;
                       }
                   } catch (IOException e) {
                       throw new UncheckedIOException(e);
                   } finally {
                       try {
                           rawImageStream.close();
                       } catch (IOException ignore) {
                           LOG.info("Exception closing the stream for image {}, channel {}", rawImage, colorChannelIndex, ignore);
                       }
                   }
               }, (t, s) -> s)
               .getContent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BrainTileInfo that = (BrainTileInfo) o;

        return new EqualsBuilder()
                .append(basePath, that.basePath)
                .append(tileRelativePath, that.tileRelativePath)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(basePath)
                .append(tileRelativePath)
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
        
        return rhs.basePath.equals(basePath) && rhs.tileRelativePath.equals(tileRelativePath);

    }
}
