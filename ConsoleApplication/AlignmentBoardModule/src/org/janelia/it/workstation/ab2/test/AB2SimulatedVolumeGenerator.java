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

public class AB2SimulatedVolumeGenerator {

    AB2Image3D_RGBA8UI rawImage;
    AB2Image3D_R32I indexImage;
    Random random=new Random(new Date().getTime());

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

    public int addSkeleton(AB2NeuronSkeleton skeleton, final Vector4 color) {
        labelCount++;
        colorMap.put(labelCount, color);
        int maxDim = getMaxDim();
        double dimUnit = 1.0 / (1.0 * maxDim);
        double sampleResolution = dimUnit / 5.0;
        AB2SkeletonWalker skeletonWalker = new AB2SkeletonWalker(skeleton, sampleResolution);
        final byte rgba[]=new byte[4];
        getRgbaFromVector4(color, rgba);

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
        return labelCount;
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
