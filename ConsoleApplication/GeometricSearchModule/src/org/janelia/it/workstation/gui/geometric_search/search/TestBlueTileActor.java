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
        GL2 gl = glDrawable.getGL().getGL2();
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        // NOTE - Y coordinate is inverted w.r.t. glVertex3d(...)
        BoundingBox3d result = new BoundingBox3d();
        result.setMin(0, -1.0, 0);
        result.setMax(1.0,  0, 0);
        return result;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
    }
}

