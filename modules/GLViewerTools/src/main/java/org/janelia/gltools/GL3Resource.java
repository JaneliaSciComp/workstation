
package org.janelia.gltools;

import javax.media.opengl.GL3;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public interface GL3Resource 
{
    void dispose(GL3 gl);

    void init(GL3 gl);
}
