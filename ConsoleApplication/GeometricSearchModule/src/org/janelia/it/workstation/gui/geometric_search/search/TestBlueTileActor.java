package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

/**
 * Created by murphys on 3/10/15.
 */
public class TestBlueTileActor implements GLActor
{

    private final Logger logger = LoggerFactory.getLogger(TestBlueTileActor.class);

    @Override
    public void display(GLAutoDrawable glDrawable) {

        logger.info("display()");

        GL2 gl2 = glDrawable.getGL().getGL2();

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
    public BoundingBox3d getBoundingBox3d() {
        // NOTE - Y coordinate is inverted w.r.t. glVertex3d(...)
        BoundingBox3d result = new BoundingBox3d();
        result.setMin(-100.0, -100.0, -100.0);
        result.setMax(100.0,  100.0, 100.0);
        return result;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
    }
}

