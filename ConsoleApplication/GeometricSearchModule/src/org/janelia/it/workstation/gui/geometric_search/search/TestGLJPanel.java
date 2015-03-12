package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.viewer3d.BaseGLViewer;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;

import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Created by murphys on 3/11/15.
 */
public class TestGLJPanel extends BaseGLViewer {

    public TestGLJPanel() {

        setPreferredSize( new Dimension( 800, 800 ) );

        addGLEventListener( new GLEventListener() {

            @Override
            public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
            }

            @Override
            public void init( GLAutoDrawable glautodrawable ) {
            }

            @Override
            public void dispose( GLAutoDrawable glautodrawable ) {
            }

            @Override
            public void display( GLAutoDrawable glautodrawable ) {
                render(glautodrawable.getGL().getGL2(), glautodrawable.getWidth(), glautodrawable.getHeight());
            }
        });

    }

    protected void render( GL2 gl2, int width, int height ) {
        gl2.glClear( gl2.GL_COLOR_BUFFER_BIT );

        gl2.glLoadIdentity();
        gl2.glPointSize(10.0f);
        gl2.glColor3f(0.10f, 0.10f, 1.0f);
        gl2.glBegin(gl2.GL_POINTS);
        for (int i=-100;i<100;i++) {
            for (int j=-100;j<100;j++) {
                for (int k=-100;k<100;k++) {
                    float x=k*1.0f;
                    float y=j*1.0f;
                    float z=i*1.0f;
                    gl2.glVertex3f(x,y,z);
                    if (i==0 && k==0 && j==0) {
                        gl2.glEnd();
                        gl2.glColor3f(1.0f, 0.1f, 0.1f);
                        gl2.glBegin(gl2.GL_POINTS);
                        gl2.glVertex3f(x,y,z);
                        gl2.glEnd();
                        gl2.glColor3f(0.1f, 0.1f, 1.0f);
                        gl2.glBegin(gl2.GL_POINTS);
                    }
                }
            }
        }
        gl2.glEnd();


    }


    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }
}
