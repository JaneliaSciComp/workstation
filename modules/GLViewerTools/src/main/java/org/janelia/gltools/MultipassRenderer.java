package org.janelia.gltools;

import java.util.ArrayList;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;

/**
 *
 * @author Christopher Bruns
 */
public class MultipassRenderer extends ArrayList<RenderPass>
implements GL3Resource
{

    public void display(GL3 gl, AbstractCamera camera) {
        for (RenderPass renderPass : this)
            renderPass.display(gl, camera);
    }

    @Override
    public void dispose(GL3 gl)
    {
        for (RenderPass renderPass : this)
            renderPass.dispose(gl);
    }

    @Override
    public void init(GL3 gl)
    {
        for (RenderPass renderPass : this)
            renderPass.init(gl);
    }

}
