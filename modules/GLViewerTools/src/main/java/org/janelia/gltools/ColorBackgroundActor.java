package org.janelia.gltools;

import java.awt.Color;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.ScreenQuadMesh;
import org.janelia.gltools.material.ScreenGradientColorMaterial;

/**
 *
 * @author Christopher Bruns
 */
public class ColorBackgroundActor extends MeshActor 
{
    private ScreenQuadMesh mesh;
    
    public ColorBackgroundActor(Color color) {
        super(new ScreenQuadMesh(color), 
              new ScreenGradientColorMaterial(),
              null);
        this.mesh = (ScreenQuadMesh)geometry;
    }
    
    public ColorBackgroundActor(Color topColor, Color bottomColor) {
        super(new ScreenQuadMesh(topColor, bottomColor), 
              new ScreenGradientColorMaterial(),
              null);
        this.mesh = (ScreenQuadMesh)geometry;
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        gl.glDisable(GL3.GL_DEPTH_TEST);
        gl.glDisable(GL3.GL_BLEND);
        super.display(gl, camera, parentModelViewMatrix);
    }
    
    public void setColor(Color color) {
        mesh.setColor(color);
        mesh.notifyObservers();
    }
    
    public void setColor(Color topColor, Color bottomColor) {
        mesh.setTopColor(topColor);
        mesh.setBottomColor(bottomColor);
        mesh.notifyObservers();
    }
    
    public void setBottomColor(Color color) {
        mesh.setBottomColor(color);
        mesh.notifyObservers();
    }
    
    public void setTopColor(Color color) {
        mesh.setTopColor(color);
        mesh.notifyObservers();
    }
}
