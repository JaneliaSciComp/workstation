/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d;

/**
 * Contains all of the old "volume model", plus more.
 *
 * @author fosterl
 */
public class MeshViewContext extends VolumeModel {    
    private float[] perspectiveMatrix;
    private float[] modelViewMatrix;

    public MeshViewContext() {
    }

    public float[] getPerspectiveMatrix() {
        return perspectiveMatrix;
    }

    public void setPerspectiveMatrix(float[] perspectiveMatrix) {
        this.perspectiveMatrix = perspectiveMatrix;
    }

    public float[] getModelViewMatrix() {
        return modelViewMatrix;
    }

    public void setModelViewMatrix(float[] modelViewMatrix) {
        this.modelViewMatrix = modelViewMatrix;
    }
}
