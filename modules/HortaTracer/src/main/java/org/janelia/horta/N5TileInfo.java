package org.janelia.horta;

import Jama.Matrix;
import org.janelia.geometry3d.Box3;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.VoxelIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class N5TileInfo implements BrickInfo {
    int colorChannelIndex = 0;
    int[] originNanometers;
    int[] pixelDims;
    int[] shapeNanometers = new int[]{351352, 456077, 205050};
    int bytesPerIntensity;
    final Matrix transform = new Matrix(new double[][]{{-376.4592779641631, -1.6963194031904338, 15.376948828572566, 0.0, 7.403554247454457E7},
        {2.158024053075783, -296.33358820739977, -26.469435093495793, 0.0, 1.035566585158884E7},
            {0.6130136514285094, 0.9691912300647493, 958.6481131788605, 0.0, 3.53861700924787E7},
            {0.0, 0.0, 0.0, 1.0, 0.0},
            {0.0, 0.0, 0.0, 0.0, 1.0}});
    Matrix stageCoordToTexCoord;

    public N5TileInfo(int[] origin, int[] pixelDims, int bitDepth) {
        this.pixelDims = pixelDims;
        originNanometers = new int[3];
        originNanometers[0] = origin[0]*1000;
        originNanometers[1] = origin[1]*1000;
        originNanometers[2] = origin[2]*1000;
        bytesPerIntensity = bitDepth;
    }

    @Override
    public int getColorChannelIndex() {
        return this.colorChannelIndex;
    }

    @Override
    public void setColorChannelIndex(int index) {
        this.colorChannelIndex = index;
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
        return 1;
    }

    @Override
    public int getBytesPerIntensity() {
        return 2;
    }

    @Override
    public double getResolutionMicrometers() {
        return 0;
    }

    @Override
    public Box3 getBoundingBox() {
        Box3 result = new Box3();
        Vector3 bbOrigin = new Vector3(
                originNanometers[0],
                originNanometers[1],
                originNanometers[2]);
        Vector3 bbSize = new Vector3(
                shapeNanometers[0],
                shapeNanometers[1],
                shapeNanometers[2]);
        bbOrigin = bbOrigin.multiplyScalar(1e-3f); // Convert nm to um
        bbSize = bbSize.multiplyScalar(1e-3f); // Convert nm to um
        result.include(bbOrigin);
        result.include(bbOrigin.add(bbSize));
        return result;
    }

    @Override
    public Texture3d loadBrick(double maxEdgePadWidth, String fileExtension) throws IOException {
        return null;
    }

    @Override
    public boolean isSameBrick(BrickInfo other) {
        return false;
    }

    public Matrix getStageCoordToTexCoord() {
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
}
