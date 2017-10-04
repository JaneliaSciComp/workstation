package org.janelia.it.workstation.ab2.test;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.model.AB2Image3D_R32I;
import org.janelia.it.workstation.ab2.model.AB2Image3D_RGBA8UI;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;

public class AB2SimulatedVolumeGenerator {

    AB2Image3D_RGBA8UI rawImage;
    AB2Image3D_R32I indexImage;

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

    // Returns assigned index of skeleton
    public int addSkeleton(AB2NeuronSkeleton skeleton) {
        return 0;
    }

    public int addSkeleton(AB2NeuronSkeleton skeleton, Vector4 color) {
        return 0;
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

}
