/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.axes;

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
