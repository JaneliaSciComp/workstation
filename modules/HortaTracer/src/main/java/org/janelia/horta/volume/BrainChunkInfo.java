package org.janelia.horta.volume;

import Jama.Matrix;
import org.aind.omezarr.OmeZarrDataset;
import org.aind.omezarr.image.AutoContrastParameters;
import org.aind.omezarr.image.TCZYXRasterZStack;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.BrainTileInfo;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * In principle intended to implement BrickInfo.  There are substantial enough differences from BrainTileInfo that it
 * probably does not make sense to subclass.  Or at least refactor what bit is shared to a parent.  However, there
 * appear to be a lot of places that require BrainTileInfo rather than BrickInfo so this is the shortest path to get
 * Ome Zarr support for now.
 */
public class BrainChunkInfo extends BrainTileInfo {
    private final OmeZarrDataset dataset;

    private final int[] readShape;

    private final int[] readOffset;

    private final int[] shapeMicrometers;

    private final int[] originMicrometers;

    private final int[] pixelDims;

    private final int bytesPerIntensity;

    private final double[] voxelSize;

    private final Matrix transform;

    private Matrix stageCoordToTexCoord;

    private final String tileRelativePath;

    private final AutoContrastParameters autoContrastParameters;

    private int colorChannelIndex = 0;

    public BrainChunkInfo(OmeZarrDataset dataset, int[] shape, int[] offset, double[] voxelSize, int channelCount, AutoContrastParameters autoContrastParameters) throws IOException {
        super();

        System.out.println(String.format("creating chunk for path %s: shape [%d, %d, %d]; offset  [%d, %d, %d]", dataset.getPath(),
                shape[0], shape[1], shape[2], offset[0], offset[1], offset[1]));

        this.dataset = dataset;

        this.autoContrastParameters = autoContrastParameters;

        this.voxelSize = voxelSize;

        // TODO include any translate from multiscale/dataset coordinate transforms.
        originMicrometers = new int[3];
        shapeMicrometers = new int[3];
        pixelDims = new int[4];

        pixelDims[0] = shape[0];
        pixelDims[1] = shape[1];
        pixelDims[2] = shape[2];

        originMicrometers[0] = (int) (voxelSize[0] * offset[0]);
        originMicrometers[1] = (int) (voxelSize[1] * offset[1]);
        originMicrometers[2] = (int) (voxelSize[2] * offset[2]);

        shapeMicrometers[0] = (int) (voxelSize[0] * pixelDims[0]);
        shapeMicrometers[1] = (int) (voxelSize[1] * pixelDims[1]);
        shapeMicrometers[2] = (int) (voxelSize[2] * pixelDims[2]);

        System.out.println(String.format("created chunk with origin [%d, %d, %d]; size  [%d, %d, %d]",
                originMicrometers[0], originMicrometers[1], originMicrometers[2], shapeMicrometers[0], shapeMicrometers[1], shapeMicrometers[2]));

        pixelDims[3] = channelCount;

        // Matrix rotation = new Matrix(new double[][]{{0, 0, -1, 0}, {0, 1, 0, 0}, {1, 0, 0, 0}, {0, 0, 0, 1}});

        // Matrix translation = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {offset[0], offset[1], offset[2], 1}});

        Matrix scaling = new Matrix(new double[][]{{voxelSize[0], 0, 0, 0}, {0, voxelSize[1], 0, 0}, {0, 0, voxelSize[2], 0}, {0, 0, 0, 1}});

        transform = scaling; // translation.times(scaling);

        // TODO assumes 2 bytes per intensity.
        this.bytesPerIntensity = 2;

        // switch to [z, y, x] for jomezarr
        this.readShape = new int[]{1, 1, shape[2], shape[1], shape[0]};

        // switch to [z, y, x] for jomezarr
        // Assumes tczyx dataset.  Default to time 0, channel 0.
        this.readOffset = new int[]{0, 0, offset[2], offset[1], offset[0]};

        tileRelativePath = String.format("[%s] [%d, %d, %d] [%d, %d, %d]", dataset.getPath(), originMicrometers[0], originMicrometers[1], originMicrometers[2], shapeMicrometers[0], shapeMicrometers[1], shapeMicrometers[2]);
    }
    @Override
    public String getTileRelativePath() {
        return tileRelativePath;
    }
/*
    @Override
    public List<? extends ConstVector3> getCornerLocations() {
        List<ConstVector3> result = new ArrayList<>();
        for (int pz : new int[]{readOffset[2], readOffset[2] + pixelDims[2]}) {
            for (int py : new int[]{readOffset[3], readOffset[3] + pixelDims[1]}) {
                for (int px : new int[]{readOffset[4], readOffset[4] + pixelDims[0]}) {
                    Matrix corner = new Matrix(new double[]{px, py, pz, 1}, 4);
                    Matrix um = transform.times(corner);
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
    */
    @Override
    public List<? extends ConstVector3> getCornerLocations() {
        List<ConstVector3> result = new ArrayList<>();
        for (int pz : new int[]{0, pixelDims[2]}) {
            for (int py : new int[]{0, pixelDims[1]}) {
                for (int px : new int[]{0, pixelDims[0]}) {
                    // Matrix corner = new Matrix(new double[]{px * voxelSize[0], py * voxelSize[1], pz * voxelSize[2], 1}, 4);
                    Matrix corner = new Matrix(new double[]{px, py, pz, 1}, 4);
                    Matrix um = corner; //transform.times(corner);
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
    public VoxelIndex getRasterDimensions() {
        return new VoxelIndex(pixelDims[0], pixelDims[1], pixelDims[2]);
    }

    @Override
    public int getChannelCount() {
        return pixelDims[4];
    }

    @Override
    public int getBytesPerIntensity() {
        return bytesPerIntensity;
    }

    @Override
    public double getResolutionMicrometers() {
        float resolution = Float.MAX_VALUE;

        for (int xyz = 0; xyz < 3; ++xyz) {
            float res = shapeMicrometers[xyz] / (float) pixelDims[xyz];
            if (res < resolution)
                resolution = res;
        }
        return resolution;
    }

    @Override
    public Box3 getBoundingBox() {
        Box3 result = new Box3();

        Vector3 bbOrigin = new Vector3(originMicrometers[0], originMicrometers[1], originMicrometers[2]);
        Vector3 bbSize = new Vector3(shapeMicrometers[0], shapeMicrometers[1], shapeMicrometers[2]);

        result.include(bbOrigin);
        result.include(bbOrigin.add(bbSize));

        return result;
    }

    private static final ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, true, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

    @Override
    public Texture3d loadBrick(double maxEdgePadWidth, String fileExtension) {
        Texture3d texture = new Texture3d();

        try {
            WritableRaster[] slices = TCZYXRasterZStack.fromDataset(dataset, readShape, readOffset, true, autoContrastParameters, false);

            texture.loadRasterSlices(slices, colorModel);

            return texture;
        } catch (Exception e) {
        }

        return null;
    }

    @Override
    public boolean isSameBrick(BrickInfo other) {
        if (!(other instanceof BrainChunkInfo)) {
            return false;
        }

        return ((BrainChunkInfo) other).getTileRelativePath() == tileRelativePath;
    }

    @Override
    public Matrix getStageCoordToTexCoord() {
        // Compute matrix just-in-time
        if (stageCoordToTexCoord == null) {
            // For ray casting, convert from stageUm to texture coordinates (i.e. normalized voxels)
            stageCoordToTexCoord = new Matrix(new double[][]{
                    {1.0 / pixelDims[0], 0, 0, 0},
                    {0, 1.0 / pixelDims[1], 0, 0},
                    {0, 0, 1.0 / pixelDims[2], 0},
                    {0, 0, 0, 1}});
        }
        return stageCoordToTexCoord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BrainChunkInfo that = (BrainChunkInfo) o;

        return new EqualsBuilder()
                .append(tileRelativePath, that.tileRelativePath)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(tileRelativePath)
                .toHashCode();
    }
}
