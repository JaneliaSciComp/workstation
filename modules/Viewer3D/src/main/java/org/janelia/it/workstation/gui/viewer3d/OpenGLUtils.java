/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.TreeSet;
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

    /**
     * Checks for, and logs any errors encountered from most recent opengl
     * operation.  Specific to texture operations.
     *
     * @param textureName a texture whose failure might be reported. 
     * @param gl required for proper checking state.
     * @param operation message to decode output.
     * @return True if error found; false otherwise.
     */
    public static final boolean reportError( String operation, GL2 gl, int textureName ) {
        int errorNum = gl.glGetError();
        boolean rtnVal = false;
        String hexErrorNum = Integer.toHexString( errorNum );
        if ( errorNum > 0 ) {
            logger.error( "Error " + errorNum + "/x0" + hexErrorNum + " during " + operation +
                          " on texture (by 'name' id) " + textureName );
            new Exception("reportError").printStackTrace();
            rtnVal = true;
        }
        return rtnVal;
    }
    
    /**
     * Checks for, and logs any errors encountered from most recent opengl
     * operation.
     *
     * @param gl required for proper checking state.
     * @param source message to decode output.
     * @return True if error found; false otherwise.
     */
    public static final boolean reportError(GL2 gl, String source) {
        boolean rtnVal = false;
        int errNum = gl.glGetError();
        if ( errNum > 0 ) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
            rtnVal = true;
        }
        return rtnVal;
    }

    /**
     * Checks for, and logs any errors encountered from most recent
     * opengl operation.
     * 
     * @param gl required for proper checking state.
     * @param source message to decode output.
     * @return True if error found; false otherwise.
     */
    public static final boolean reportError(GL2GL3 gl, String source) {
        boolean rtnVal = false;
        int errNum = gl.glGetError();
        if (errNum > 0) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
            rtnVal = true;
        }
        return rtnVal;
    }

    public static final void dumpFloatBuffer(FloatBuffer attribBuffer) {
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

    public static final void dumpIntBuffer(IntBuffer inxBuf) {
        inxBuf.rewind();
        Set<Integer> indicesInUse = new TreeSet<>();
        StringBuilder allIndicesBuilder = new StringBuilder();
        for (int i = 0; i < inxBuf.capacity(); i++) {
            if (i % 3 == 0) {
                allIndicesBuilder.append("\n");
            }
            int nextI = inxBuf.get();
            allIndicesBuilder.append(nextI).append(", ");
            indicesInUse.add( nextI );
        }
        Integer previousIndex = -1;
        StringBuilder outOfOrderBuilder = new StringBuilder();
        for (Integer nextI: indicesInUse) {
            if (nextI - previousIndex != 1) {
                if (outOfOrderBuilder.length() > 0) {
                    outOfOrderBuilder.append(",");
                }
                outOfOrderBuilder.append(nextI);
                if (nextI % 10 == 1) {
                    outOfOrderBuilder.append("\n");
                }
            }
            previousIndex = nextI;
        }
        if (inxBuf.capacity() % 3 != 0) {
            logger.warn("Count of integers in Index Buffer is not a multiple of 3.");
        }
        System.out.println("[--------------Out of order/Missing Indices-------]");
        logger.info(outOfOrderBuilder.toString());
        System.out.println("[------------- Index Buffer Contents -------------]");
        logger.info(allIndicesBuilder.toString());
        inxBuf.rewind();
    }

}
