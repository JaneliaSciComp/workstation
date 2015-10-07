package org.janelia.it.workstation.gui.geometric_search.viewer.renderable;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.DenseVolumeActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 8/7/2015.
 */
public class DenseVolumeRenderable extends Renderable {

    private static final Logger logger = LoggerFactory.getLogger(DenseVolumeRenderable.class);

    List<Vector4> voxels=new ArrayList<>();
    List<Vector4> sampledVoxels=new ArrayList<>();

    public static final String INTENSITY_THRESHOLD_FLOAT="Intensity Threshold";
    public static final float INTENSITY_THRESHOLD_FLOAT_DEFAULT=0.2f;

    public static final String MAX_VOXELS_INT="Max Voxels";
    public static final int MAX_VOXELS_INT_DEFAULT=15000000;

    private long totalVoxelCount;
    private int thresholdVoxelCount;
    private int sampledVoxelCount;

    int xSize=0;
    int ySize=0;
    int zSize=0;
    float voxelSize=0.0f;

    int downsampleLevel=1;

    public DenseVolumeRenderable() {
        parameterMap.put(INTENSITY_THRESHOLD_FLOAT, new Float(INTENSITY_THRESHOLD_FLOAT_DEFAULT));
        parameterMap.put(MAX_VOXELS_INT, new Integer(MAX_VOXELS_INT_DEFAULT));
    }

    public float getIntensityThreshold() {
        return (Float)parameterMap.get(INTENSITY_THRESHOLD_FLOAT);
    }

    public void setIntensityThreshold(float intensityThreshold) {
        parameterMap.put(INTENSITY_THRESHOLD_FLOAT, new Float(intensityThreshold));
    }

    public int getMaxVoxels() { return (Integer)parameterMap.get(MAX_VOXELS_INT); }

    public void setMaxVoxels(int maxVoxels) {
        parameterMap.put(MAX_VOXELS_INT, new Integer(maxVoxels));
    }

    public void init(int xsize, int ysize, int zsize, float voxelSize, byte[] data8) {
        xSize=xsize;
        ySize=ysize;
        zSize=zsize;
        this.voxelSize=voxelSize;
        totalVoxelCount=0;
        thresholdVoxelCount=0;
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
                    totalVoxelCount++;
                    if (fw > intensityThreshold) {
                        Vector4 v = new Vector4(fx, fy, fz, fw);
                        voxels.add(v);
                        thresholdVoxelCount++;
                    }
                }
            }
        }
        logger.info("totalVoxels="+totalVoxelCount);
        logger.info("selectedVoxels="+thresholdVoxelCount);
        consolidateVoxels();
    }

    public void init(int xsize, int ysize, int zsize, float voxelSize, short[] data16) {
        xSize=xsize;
        ySize=ysize;
        zSize=zsize;
        this.voxelSize=voxelSize;
        totalVoxelCount=0;
        thresholdVoxelCount=0;
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
                    totalVoxelCount++;
                    if (fw > intensityThreshold) {
                        Vector4 v = new Vector4(fx, fy, fz, fw);
                        voxels.add(v);
                        thresholdVoxelCount++;
                    }
                }
            }
        }
        logger.info("totalVoxels="+totalVoxelCount);
        logger.info("selectedVoxels="+thresholdVoxelCount);
        consolidateVoxels();
    }

    public long getTotalVoxelCount() {
        return totalVoxelCount;
    }

    public int getThresholdVoxelCount() {
        return thresholdVoxelCount;
    }

    public int getSampledVoxelCount() {
        return sampledVoxelCount;
    }

    protected void consolidateVoxels() {
        int maxVoxels=(Integer)parameterMap.get(MAX_VOXELS_INT);
        logger.info("Starting viList size="+voxels.size()+" with maxVoxels="+maxVoxels);
        while(sampledVoxels.size()==0 || sampledVoxels.size()>maxVoxels) {
            if (sampledVoxels.size()==0) {
                sampledVoxels.addAll(voxels);
            } else {
                // If here, we need to downsample
                int failedSampledVoxelsSize=sampledVoxels.size();
                sampledVoxels.clear();
                downsampleLevel++;
                int dZ=zSize/downsampleLevel;
                int dY=ySize/downsampleLevel;
                int dX=xSize/downsampleLevel;
                logger.info("Downsampling to level="+downsampleLevel+" due to failed size="+failedSampledVoxelsSize);
                ArrayList voxelMatrix[][][] = new ArrayList[dZ][dY][dX];
                for (int vi=0;vi<voxels.size();vi++) {
                    Vector4 vg=voxels.get(vi);
                    float[] data=vg.toArray();
                    float sd=voxelSize*downsampleLevel;
                    int xIndex=(int) (data[0] / sd);
                    int yIndex=(int) (data[1] / sd);
                    int zIndex=(int) (data[2] / sd);
                    if (voxelMatrix[zIndex][yIndex][xIndex]==null) {
                        voxelMatrix[zIndex][yIndex][xIndex]=new ArrayList<Vector4>();
                    }
                    voxelMatrix[zIndex][yIndex][xIndex].add(vg);
                }
                for (int z=0;z<dZ;z++) {
                    for (int y=0;y<dY;y++) {
                        for (int x=0;x<dX;x++) {
                            List<Vector4> vList=voxelMatrix[z][y][x];
                            if (vList!=null) {
                                float ax=1000000f;
                                float ay=1000000f;
                                float az=1000000f;
                                float aw=0.0f;
                                for (int v=0;v<vList.size();v++) {
                                    Vector4 vg=vList.get(v);
                                    float[] data=vg.toArray();
                                    if (data[0]<ax) {
                                        ax=data[0];
                                    }
                                    if (data[1]<ay) {
                                        ay=data[1];
                                    }
                                    if (data[2]<az) {
                                        az=data[2];
                                    }
                                    if (data[3]>aw) { // MIP
                                        aw=data[3];
                                    }
                                }
                                //float ls=new Float(vList.size());
                                sampledVoxels.add(new Vector4(ax, ay, az, aw));
                            }
                        }
                    }
                }
            }
        }
        sampledVoxelCount=sampledVoxels.size();
        logger.info("Final downsample level="+downsampleLevel+" with voxelCount="+sampledVoxels.size());
    }

    public float getVoxelSize() {
        return voxelSize * downsampleLevel;
    }

    public float getXSize() { return (float)((xSize*1.0) / (downsampleLevel*1.0)); }

    public float getYSize() { return (float)((ySize*1.0) / (downsampleLevel*1.0)); }

    public float getZSize() { return (float)((zSize*1.0) / (downsampleLevel*1.0)); }

    @Override
    public Actor createAndSetActor() {
        if (actor!=null) {
            disposeActor();
        }
        actor = new DenseVolumeActor(this.name, sampledVoxels, getXSize(), getYSize(), getZSize(), getVoxelSize());
        actor.setColor(preferredColor);
        return actor;
    }

    @Override
    public void disposeActor() {
        if (actor!=null) {
            actor.dispose();
        }
    }

}
