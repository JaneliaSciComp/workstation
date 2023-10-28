package org.janelia.horta.blocks;

import Jama.Matrix;
import org.aind.omezarr.OmeZarrDataset;
import org.aind.omezarr.image.AutoContrastParameters;
import org.aind.omezarr.image.TCZYXRasterZStack;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OmeZarrBlockTileKey implements BlockTileKey {
    private static final Logger log = LoggerFactory.getLogger(OmeZarrBlockTileKey.class);

    private final OmeZarrDataset dataset;

    private final int[] readShape;

    private final int[] readOffset;

    private final double[] voxelSize;

    private final double[] shapeMicrometers;

    private final int[] pixelDims;

    private final Vector3 blockOrigin;

    private final Vector3 blockExtents;

    private final Vector3 blockCentroid;

    private final int keyDepth;

    private final Matrix transform;

    private Matrix stageCoordToTexCoord;

    private List<ConstVector3> cornerLocations;

    private String relativePath = null;

    public OmeZarrBlockTileKey(OmeZarrDataset dataset, int keyDepth, int[] shape, int[] offset, double[] voxelSize, int channelCount) {
        this.dataset = dataset;

        this.keyDepth = keyDepth;

        this.voxelSize = voxelSize;

        if (shape.length == 3) {
            // switch to [z, y, x] for jomezarr
            this.readShape = new int[]{1, 1, shape[2], shape[1], shape[0]};
        } else {
            this.readShape = shape;
        }

        if (shape.length == 3) {
            // switch to [z, y, x] for jomezarr
            // Assumes tczyx dataset.  Default to time 0, channel 0.
            this.readOffset = new int[]{0, 0, offset[2], offset[1], offset[0]};
        } else {
            this.readOffset = offset;
        }

        double[] originMicrometers = new double[3];

        shapeMicrometers = new double[3];

        pixelDims = new int[4];

        pixelDims[0] = this.readShape[4];
        pixelDims[1] = this.readShape[3];
        pixelDims[2] = this.readShape[2];
        pixelDims[3] = channelCount;

        originMicrometers[0] = this.voxelSize[0] * this.readOffset[4];
        originMicrometers[1] = this.voxelSize[1] * this.readOffset[3];
        originMicrometers[2] = this.voxelSize[2] * this.readOffset[2];

        shapeMicrometers[0] = this.voxelSize[0] * this.readShape[4];
        shapeMicrometers[1] = this.voxelSize[1] * this.readShape[3];
        shapeMicrometers[2] = this.voxelSize[2] * this.readShape[2];

        transform = new Matrix(
                new double[][]{
                        {shapeMicrometers[0] / pixelDims[0], 0, 0, originMicrometers[0]},
                        {0, shapeMicrometers[1] / pixelDims[1], 0, originMicrometers[1]},
                        {0, 0, shapeMicrometers[2] / pixelDims[2], originMicrometers[2]},
                        {0, 0, 0, 1}
                }
        );

        blockOrigin = new Vector3(originMicrometers[0], originMicrometers[1], originMicrometers[2]);

        blockExtents = new Vector3(shapeMicrometers[0], shapeMicrometers[1], shapeMicrometers[2]);

        blockCentroid = new Vector3(blockExtents.get(0) * 0.5f + blockOrigin.get(0), blockExtents.get(1) * 0.5f + blockOrigin.get(1), blockExtents.get(2) * 0.5f + blockOrigin.get(2));
    }

    @Override
    public ConstVector3 getCentroid() { // Micrometers?
        return blockCentroid;
    }

    public List<? extends ConstVector3> getCornerLocations() {
        if (cornerLocations == null) {
            cornerLocations = new ArrayList<>();
            for (double pz : new double[]{0, pixelDims[2]}) {
                for (double py : new double[]{0, pixelDims[1]}) {
                    for (double px : new double[]{0, pixelDims[0]}) {
                        Matrix corner = new Matrix(new double[]{(px + readOffset[4]) * voxelSize[0], (py + readOffset[3]) * voxelSize[1], (pz + readOffset[2]) * voxelSize[2], 1}, 4);
                        ConstVector3 v = new Vector3(
                                (float) corner.get(0, 0),
                                (float) corner.get(1, 0),
                                (float) corner.get(2, 0));
                        cornerLocations.add(v);
                    }
                }
            }
        }

        return cornerLocations;
    }

    public Matrix getStageCoordToTexCoord() {
        // Compute matrix just-in-time
        if (stageCoordToTexCoord == null) {

            Matrix stageToVoxel = transform.inverse();

            Matrix nanosToPixel = new Matrix(new double[][]{
                    {1.0 / pixelDims[0], 0, 0, 0},
                    {0, 1.0 / pixelDims[1], 0, 0},
                    {0, 0, 1.0 / pixelDims[2], 0},
                    {0, 0, 0, 1}});

            stageCoordToTexCoord = nanosToPixel.times(stageToVoxel);
        }

        return stageCoordToTexCoord;
    }

    public double getResolutionMicrometers() {
        double resolution = Float.MAX_VALUE;

        for (int xyz = 0; xyz < 3; ++xyz) {
            double res = shapeMicrometers[xyz] / (double) pixelDims[xyz];
            if (res < resolution)
                resolution = res;
        }

        return resolution;
    }

    private static final ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, true, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

    public Texture3d loadBrick() {
        Texture3d texture = new Texture3d();

        try {
            WritableRaster[] slices = TCZYXRasterZStack.fromDataset(dataset, readShape, readOffset, 1, false, null, null);

            texture.loadRasterSlices(slices, colorModel);

            return texture;
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public Vector3 getOrigin() {
        return blockOrigin;
    }

    public Vector3 getExtents() {
        return blockExtents;
    }

    public int getKeyDepth() {
        return keyDepth;
    }

    public int[] getShape() {
        return pixelDims;
    }

    public OmeZarrDataset getDataset() {
        return dataset;
    }

    public double[] getVoxelSize() {
        return voxelSize;
    }

    public int[] getReadOffset() {
        return readOffset;
    }

    public String getRelativePath() {
        if (relativePath == null) {
            relativePath = String.format("[%s] [%.0f, %.0f, %.0f] [%.0f, %.0f, %.0f]", dataset.getPath(), blockOrigin.getX(), blockOrigin.getY(), blockOrigin.getY(), shapeMicrometers[0], shapeMicrometers[1], shapeMicrometers[2]);
        }
        return relativePath;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Arrays.hashCode(this.readShape);
        hash = 53 * hash + Arrays.hashCode(this.readOffset);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final OmeZarrBlockTileKey other = (OmeZarrBlockTileKey) obj;

        if (dataset != other.dataset) {
            return false;
        }

        if (!Arrays.equals(this.readShape, other.readShape)) {
            return false;
        }

        return Arrays.equals(this.readOffset, other.readOffset);
    }
}
