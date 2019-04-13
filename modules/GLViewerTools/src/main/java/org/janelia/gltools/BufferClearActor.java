
package org.janelia.gltools;

import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;

/**
 * Clears an offscreen render buffer
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class BufferClearActor extends BasicGL3Actor {
    private final float[] clearColor4 = new float[] {0,0,0,0};
    private final float[] clearColor2 = new float[] {0,0};
    private final float[] clearColor1 = new float[] {0};
    private final float[] depthOne = new float[] {1};
    private Framebuffer framebuffer;
    
    public BufferClearActor(Framebuffer framebuffer) {
        super(null);
        this.framebuffer = framebuffer;
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        // TODO - how to choose which buffers?
        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor4, 0);
        gl.glClearBufferfv(GL3.GL_COLOR, 1, clearColor4, 0); // pick buffer...
        gl.glClearBufferfv(GL3.GL_DEPTH, 0, depthOne, 0);
    }
    
}
