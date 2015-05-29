/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fosterl
 */
public class OpenGLUtils {
    private static final Logger logger = LoggerFactory.getLogger( OpenGLUtils.class );

    public static final void reportError( String operation, GL2 gl, int textureName ) {
        int errorNum = gl.glGetError();
        String hexErrorNum = Integer.toHexString( errorNum );
        if ( errorNum > 0 ) {
            logger.error( "Error " + errorNum + "/x0" + hexErrorNum + " during " + operation +
                          " on texture (by 'name' id) " + textureName );
            new Exception("reportError").printStackTrace();
        }
    }
    
    public static void reportError(GL2 gl, String source) {
        int errNum = gl.glGetError();
        if ( errNum > 0 ) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
        }
    }

    public static void reportError(GL2GL3 gl, String source) {
        int errNum = gl.glGetError();
        if (errNum > 0) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
        }
    }

    public static void dumpFloatBuffer(FloatBuffer attribBuffer) {
        attribBuffer.rewind();
        StringBuilder bldr = new StringBuilder();
        for (int i = 0; i < attribBuffer.capacity(); i++) {
            if (i % 3 == 0) {
                bldr.append("\n");
            }
            float nextF = attribBuffer.get();
            bldr.append(nextF + "f, ");
        }
        System.out.println("[------------- Buffer Contents -------------]");
        logger.info(bldr.toString());
        attribBuffer.rewind();
    }

    public static void dumpIntBuffer(IntBuffer inxBuf) {
        inxBuf.rewind();
        StringBuilder bldr = new StringBuilder();
        for (int i = 0; i < inxBuf.capacity(); i++) {
            if (i % 3 == 0) {
                bldr.append("\n");
            }
            int nextI = inxBuf.get();
            bldr.append(nextI + ", ");
        }
        System.out.println("[------------- Index Buffer Contents -------------]");
        logger.info(bldr.toString());
        inxBuf.rewind();
    }

}
