
package org.janelia.gltools;

import java.awt.Color;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

/**
 * Renderer that paints the entire viewport a single color.
 * 
 * @author Christopher Bruns
 */
public class SolidColorGLRenderer 
implements GLEventListener
{
    private final Color color;

    public SolidColorGLRenderer(Color color) {
        this.color = color;
    }
    
    @Override
    public void init(GLAutoDrawable glad) {}

    @Override
    public void dispose(GLAutoDrawable glad) {}

    @Override
    public void display(GLAutoDrawable glad) {
        GL gl = glad.getGL();
        gl.glClearColor(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {}
    
}
