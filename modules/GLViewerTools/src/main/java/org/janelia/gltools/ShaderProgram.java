
package org.janelia.gltools;

import javax.media.opengl.GL3;

/**
 *
 * @author brunsc
 */
public interface ShaderProgram extends GL3Resource 
{
    public int load(GL3 gl);
    
    public void unload(GL3 gl);
    
    public int getProgramHandle();
}
