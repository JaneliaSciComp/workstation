/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.gl;

import javax.media.opengl.GL4;
import org.janelia.geometry3d.Matrix4;

/**
 *
 * @author murphys
 */
public class VolumeShader extends GL4Shader {

    @Override
    public String getVertexShaderResourceName() {
        return "VolumeVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "VolumeFragment.glsl";
    }
    
    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
        checkGlError(gl, "VolumeShader setProjection() error");
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
        checkGlError(gl, "VolumeShader setView() error");
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
        checkGlError(gl, "VolumeShader setModel() error");
    }
    
}
