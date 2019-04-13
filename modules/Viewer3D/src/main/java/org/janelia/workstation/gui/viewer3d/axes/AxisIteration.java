package org.janelia.workstation.gui.viewer3d.axes;

public class AxisIteration {

    private int axisNum;
    private int iterationDirectionMultiplier;

    public AxisIteration(int axisNum, int iterationDirectionMultiplier) {
        this.axisNum = axisNum;
        this.iterationDirectionMultiplier = iterationDirectionMultiplier;
    }

    public int getAxisNum() {
        return axisNum;
    }

    public int getIterationDirectionMultiplier() {
        return iterationDirectionMultiplier;
    }

}
