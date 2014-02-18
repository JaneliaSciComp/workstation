package org.janelia.it.FlyWorkstation.gui.opengl.demo;

import java.awt.Dimension;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.BoxLayout;
import javax.swing.JFrame;

// Demonstration program to show that GL_FRAMEBUFFER_SRGB is not
// respected by GLJPanel in JOGL 2.1. The two displayed boxes should
// be the same brightness. The one on the right is wrongly too dark.
// (GL_FRAMEBUFFER_SRGB behavior was correct in JOGL 2.0)
//
// Suggested solution of passing "-Djogl.gljpanel.noglsl" to the JVM does NOT
// resolve the problem.
public class TestSrgbFramebuffer extends JFrame implements GLEventListener 
{
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TestSrgbFramebuffer();
            }
        });
    }

    public TestSrgbFramebuffer() {
        // left GLCanvas demonstrates correct paler brightness
        GLCanvas glPanel1 = new GLCanvas(); 
        // right GLJPanel demonstrates wrong darker brightness
        GLJPanel glPanel2 = new GLJPanel();
        
        // setSkipGLOrientationVerticalFlip(true) does NOT resolve the problem
        glPanel2.setSkipGLOrientationVerticalFlip(true); 
        
        glPanel1.setPreferredSize(new Dimension(200, 200));
        glPanel2.setPreferredSize(new Dimension(200, 200));
        getContentPane().setLayout(new BoxLayout(
                getContentPane(), BoxLayout.LINE_AXIS));
        getContentPane().add(glPanel1);
        getContentPane().add(glPanel2);
        glPanel1.addGLEventListener(this);
        glPanel2.addGLEventListener(this);
        pack();
        setVisible(true);
    }

    @Override
    public void display(GLAutoDrawable gad) {
        GL gl = gad.getGL();
        
        // This line should raise the brightness of the image
        gl.glEnable(GL2GL3.GL_FRAMEBUFFER_SRGB);

        // Solid midtone clear color demonstrates the sRGB effect,
        // or lack thereof
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        
        // glDisable(GL2GL3.GL_FRAMEBUFFER_SRGB) does NOT resolve the problem
        gl.glDisable(GL2GL3.GL_FRAMEBUFFER_SRGB);
    }

    @Override
    public void dispose(GLAutoDrawable arg0) {}

    @Override
    public void init(GLAutoDrawable gad) {
        GL2GL3 gl2gl3 = gad.getGL().getGL2GL3();
        
        // This next line should raise the brightness of the image
        // (moved to display() method, to show futility of one proposed solution)
        // gl2gl3.glEnable(GL2GL3.GL_FRAMEBUFFER_SRGB);
        
        // midtone gray color clearly shows effect of sRGB correction
        // (when it works!)
        gl2gl3.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    }

    @Override
    public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
            int arg4) {}
}
