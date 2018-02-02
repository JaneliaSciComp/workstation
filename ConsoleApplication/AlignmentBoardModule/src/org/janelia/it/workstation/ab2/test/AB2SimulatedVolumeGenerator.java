package org.janelia.it.workstation.ab2.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.compute.AB2SkeletonWalker;
import org.janelia.it.workstation.ab2.compute.AB2SkeletonWalkerCallback;
import org.janelia.it.workstation.ab2.model.AB2Image3D_R32I;
import org.janelia.it.workstation.ab2.model.AB2Image3D_RGBA8UI;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SimulatedVolumeGenerator {

    Logger logger= LoggerFactory.getLogger(AB2SimulatedVolumeGenerator.class);

    AB2Image3D_RGBA8UI rawImage;
    AB2Image3D_R32I indexImage;
    Random random=new Random(new Date().getTime());
    //Random random=new Random(1);

    int xDim;
    int yDim;
    int zDim;

    public AB2SimulatedVolumeGenerator(int xDim, int yDim, int zDim) {
        this.xDim=xDim;
        this.yDim=yDim;
        this.zDim=zDim;
        rawImage=new AB2Image3D_RGBA8UI(xDim, yDim, zDim);
        indexImage=new AB2Image3D_R32I(xDim, yDim, zDim);
    }

    int labelCount=0;
    Map<Integer, Vector4> colorMap=new HashMap<>();

    /* Returns assigned index of skeleton

    The idea here is to trace the skeleton at a high resolution and add all participating voxels.

     */

    public int addSkeleton(AB2NeuronSkeleton skeleton) {
        Vector3 rc3=new Vector3(random.nextFloat(), random.nextFloat(), random.nextFloat());
        rc3.normalize();
        Vector4 rc4=new Vector4(rc3.getX(), rc3.getY(), rc3.getZ(), 1.0f);
        return addSkeleton(skeleton, rc4);
    }

    public Vector4 getColorByLabelIndex(int labelIndex) {
        return colorMap.get(labelIndex);
    }

    public int addSkeleton(AB2NeuronSkeleton skeleton, final Vector4 color) {
        colorMap.put(labelCount, color);
        int maxDim = getMaxDim();
        double dimUnit = 1.0 / (1.0 * maxDim);
        double sampleResolution = dimUnit / 5.0;
        AB2SkeletonWalker skeletonWalker = new AB2SkeletonWalker(skeleton, sampleResolution);
        final byte rgba[]=new byte[4];
        getRgbaFromVector4(color, rgba);

        int r=rgba[0];
        int g=rgba[1];
        int b=rgba[2];
        int a=rgba[3];

        logger.info("Skeleton color r="+r+" g="+g+" b="+b+" a="+a);

        skeletonWalker.walkSkeleton(new AB2SkeletonWalkerCallback() {

            int imageXYZ[] = new int[3];

            @Override
            public void processPosition(AB2NeuronSkeleton.Node parentNode, AB2NeuronSkeleton.Node childNode, double edgeFraction) {
                if (childNode==null) {
                    getImageXYZFromSkeletonXYZ(parentNode.x(), parentNode.y(), parentNode.z(), imageXYZ);
                    rawImage.setVoxel(imageXYZ[0], imageXYZ[1], imageXYZ[2], rgba);
                    indexImage.setVoxel(imageXYZ[0], imageXYZ[1], imageXYZ[2], labelCount);
                } else {
                    Vector3 parentPosition=new Vector3( (float)parentNode.x(), (float)parentNode.y(), (float)parentNode.z());
                    Vector3 childPosition=new Vector3((float)childNode.x(), (float)childNode.y(), (float)childNode.z());
                    Vector3 edge=childPosition.minus(parentPosition);
                    edge.multiplyScalar((float)edgeFraction);
                    Vector3 position=parentPosition.plus(edge);
                    getImageXYZFromSkeletonXYZ(position.getX(), position.getY(), position.getZ(), imageXYZ);
                    rawImage.setVoxel(imageXYZ[0], imageXYZ[1], imageXYZ[2], rgba);
                    indexImage.setVoxel(imageXYZ[0], imageXYZ[1], imageXYZ[2], labelCount);
                }
            }

        });
        labelCount++;
        return labelCount-1;
    }


    public AB2Image3D_RGBA8UI getRawImage() {
        return rawImage;
    }

    public AB2Image3D_R32I getIndexImage() {
        return indexImage;
    }

    public void addUniformNoise(double probability, double amplitude) {

    }

    public void performDilation(double sensitivity, double noise) {

        AB2Image3D_RGBA8UI newRawImage=new AB2Image3D_RGBA8UI(xDim, yDim, zDim);

        byte[] voxel=new byte[4];
        int[] rgba=new int[4];

        byte[] voxel2=new byte[4];
        int[] rgba2=new int[4];

        double[] avg=new double[3];
        double[] gap=new double[3];

        int noChangeCount=0;
        int changeCount=0;
        int updateCount=0;

        for (int iz=1;iz<zDim-1;iz++) {
            for (int iy=1;iy<yDim-1;iy++) {
                for (int ix=1;ix<xDim-1;ix++) {

                    rgba[0]=0;
                    rgba[1]=0;
                    rgba[2]=0;
                    rgba[3]=0;

                    accumVoxelIntFromBytes(ix, iy, iz, voxel, rgba);

                    double mag=Math.sqrt(rgba[0]*rgba[0]+rgba[1]*rgba[1]+rgba[2]*rgba[2]);

                    if (mag > 250) {
                        newRawImage.setVoxel(ix, iy, iz, voxel);
                        noChangeCount++;
                        continue; // this voxel is already maxed out
                    }

                    rgba2[0]=0;
                    rgba2[1]=0;
                    rgba2[2]=0;

                    for (int jz=iz-1;jz<iz+2;jz++) {
                        for (int jy=iy-1;jy<iy+2;jy++) {
                            for (int jx=ix-1;jx<ix+2;jx++) {
                                if (jz!=iz || jy!=iy || jx!=ix) {
                                    accumVoxelIntFromBytes(jx, jy, jz, voxel2, rgba2);
                                }
                            }
                        }
                    }

                    for (int a=0;a<3;a++) {
                        avg[a] = (1.0 * rgba2[a])/26.0;
                        gap[a] = avg[a] - (double)rgba[a];
                        if (gap[a]>0.0) {
                            int v=(int)(gap[a]*(sensitivity+random.nextDouble()*noise))+rgba[a];
                            if (v<256) {
                                changeCount++;
                                rgba[a]=v;
                            }
                        }
                    }

                    int maxRGB=rgba[0];
                    if (rgba[1]>maxRGB) {
                        maxRGB=rgba[1];
                    }
                    if (rgba[2]>maxRGB) {
                        maxRGB=rgba[2];
                    }

                    rgba[3]=maxRGB; // set alpha to max intensity

                    voxel[0]=(byte)rgba[0];
                    voxel[1]=(byte)rgba[1];
                    voxel[2]=(byte)rgba[2];
                    voxel[3]=(byte)rgba[3];

                    newRawImage.setVoxel(ix, iy, iz, voxel);

                    updateCount++;

                }
            }
        }
        rawImage=newRawImage;

        logger.info("No-Change count="+noChangeCount+" Change count="+changeCount+" Update count="+updateCount);
    }

    public void accumVoxelIntFromBytes(int x, int y, int z, byte[] voxel, int[] rgba) {
        rawImage.getVoxel(x,y,z,voxel);
        for (int c=0;c<4;c++) {
            int v=voxel[c];
            if (v > -1) {
                rgba[c] += v;
            }
            else {
                rgba[c] += (v + 256);
            }
        }
    }

    public int getMaxDim() {
        int maxDim=xDim;
        if (yDim>maxDim) {
            maxDim=yDim;
        }
        if (zDim>maxDim) {
            maxDim=zDim;
        }
        return maxDim;
    }

    public void getImageXYZFromSkeletonXYZ(double x, double y, double z, int[] imageXYZ) {
        int iX=(int)(x*xDim);
        int iY=(int)(y*yDim);
        int iZ=(int)(z*zDim);
        imageXYZ[0]=iX;
        imageXYZ[1]=iY;
        imageXYZ[2]=iZ;
    }

    public void getRgbaFromVector4(Vector4 color, byte[] rgba) {
        rgba[0]=(byte)(color.get(0)*255f);
        rgba[1]=(byte)(color.get(1)*255f);
        rgba[2]=(byte)(color.get(2)*255f);
        rgba[3]=(byte)(color.get(3)*255f);
    }

}
