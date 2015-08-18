package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

import org.janelia.geometry3d.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by murphys on 8/7/2015.
 */
public class DenseVolumeRenderable extends Renderable {

    private static final Logger logger = LoggerFactory.getLogger(DenseVolumeRenderable.class);

    Set<Vector4> voxels=new HashSet<>();

    public static final String INTENSITY_THRESHOLD_FLOAT="Intensity Threshold";
    public static final float INTENSITY_THRESHOLD_FLOAT_DEFAULT=0.2f;

    public DenseVolumeRenderable() {
        parameterMap.put(INTENSITY_THRESHOLD_FLOAT, new Float(INTENSITY_THRESHOLD_FLOAT_DEFAULT));
    }

    public float getIntensityThreshold() {
        return (Float)parameterMap.get(INTENSITY_THRESHOLD_FLOAT);
    }

    public void setIntensityThreshold(float intensityThreshold) {
        parameterMap.put(INTENSITY_THRESHOLD_FLOAT, new Float(intensityThreshold));
    }

    public void init(int xsize, int ysize, int zsize, float voxelSize, byte[] data8) {
        long totalVoxels=0;
        int selectedVoxels=0;
        float intensityThreshold=(Float)parameterMap.get(INTENSITY_THRESHOLD_FLOAT);
        for (int z=0;z<zsize;z++) {
            float fz=z*voxelSize;
            for (int y=0;y<ysize;y++) {
                float fy=y*voxelSize;
                for (int x=0;x<xsize;x++) {
                    float fx=x*voxelSize;
                    int p=z*ysize*xsize+y*xsize+x;
                    int cv=data8[p] & 0x000000ff;
                    float fw=(float) (cv*1.0/255.0);
                    totalVoxels++;
                    if (fw > intensityThreshold) {
                        Vector4 v = new Vector4(fx, fy, fz, fw);
                        voxels.add(v);
                        selectedVoxels++;
                    }
                }
            }
        }
        logger.info("totalVoxels="+totalVoxels);
        logger.info("selectedVoxels="+selectedVoxels);
    }

    public void init(int xsize, int ysize, int zsize, float voxelSize, short[] data16) {
        long totalVoxels=0;
        int selectedVoxels=0;
        float intensityThreshold=(Float)parameterMap.get(INTENSITY_THRESHOLD_FLOAT);
        for (int z=0;z<zsize;z++) {
            float fz=z*voxelSize;
            for (int y=0;y<ysize;y++) {
                float fy=y*voxelSize;
                for (int x=0;x<xsize;x++) {
                    float fx=x*voxelSize;
                    int p=z*ysize*xsize+y*xsize+x;
                    int cv=data16[p];
                    float fw=(float) (cv*1.0/255.0);
                    totalVoxels++;
                    if (fw > intensityThreshold) {
                        Vector4 v = new Vector4(fx, fy, fz, fw);
                        voxels.add(v);
                        selectedVoxels++;
                    }
                }
            }
        }
        logger.info("totalVoxels="+totalVoxels);
        logger.info("selectedVoxels="+selectedVoxels);
    }

}
