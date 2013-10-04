package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GLError {
    private static Logger logger = LoggerFactory.getLogger( GLError.class );
    private static GLU glu = new GLU();

    public static void checkGlError(GL gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );  
    }
}
