/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.viewer;

import java.nio.IntBuffer;
import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author murphys
 */
public class GL4TransparencyContext {
    
    private final Logger logger = LoggerFactory.getLogger( GL4TransparencyContext.class );
    
    protected static GLU glu = new GLU();
 
    public static final int MAX_HEIGHT = 2048;
    public static final int MAX_WIDTH = 2048;
    int headPointerTotalPixels=0;

    IntBuffer headPointerId = IntBuffer.allocate(1);
    IntBuffer headPointerInitializerId = IntBuffer.allocate(1);
    IntBuffer atomicCounterId = IntBuffer.allocate(1);
    IntBuffer fragmentStorageBuffer = IntBuffer.allocate(1);
    IntBuffer zeroValueBuffer = IntBuffer.allocate(1);
    
    public int getHeadPointerTextureId() {
        return headPointerId.get(0);
    }

    public int getFragmentStorageBufferId() {
        return fragmentStorageBuffer.get(0);
    }
    
    public int getAtomicCounterId() {
        return atomicCounterId.get(0);
    }
    
    public int getHeadPointerInitializerId() {
        return headPointerInitializerId.get(0);
    }
        
    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }
    
    public void init(GL4 gl) throws Exception {
        zeroValueBuffer.put(0,0);
        headPointerTotalPixels=MAX_HEIGHT * MAX_WIDTH;

        // Allocate empty texture of correct size
        gl.glGenTextures(1, headPointerId);
        checkGlError(gl, "GL4TransparencyContext glGenTextures() error");
        logger.info("init() check 1.1");
        logger.info("headPointerId="+headPointerId.get(0));

        gl.glBindTexture(GL4.GL_TEXTURE_2D, headPointerId.get(0));
        checkGlError(gl, "GL4TransparencyContext glBindTexture() error");
        logger.info("init() check 1.2");

        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0,
                GL4.GL_R32UI,
                MAX_HEIGHT,
                MAX_WIDTH,
                0,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                null);
        checkGlError(gl, "GL4TransparencyContext glTexImage2D() error");
        logger.info("init() check 1.3");

        // Create PBO from which to clear the headPointerTexture
        IntBuffer hpiData = IntBuffer.allocate(headPointerTotalPixels);
        for (int i=0;i<hpiData.capacity();i++) {
            hpiData.put(i, Integer.MAX_VALUE);
        }
        logger.info("init() check 1.4");
                
       gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);
       checkGlError(gl, "i1 GL4TransparencyContext glBindBuffer() error");
       logger.info("init() check 1.4.1");

        gl.glGenBuffers(1, headPointerInitializerId);
        checkGlError(gl, "i1 GL4TransparencyContext glGenBuffers() error");
        logger.info("headPointerInitializerId="+headPointerInitializerId.get(0));
        logger.info("init() check 1.5");

        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerInitializerId.get(0));
        checkGlError(gl, "i2 GL4TransparencyContext glBindBuffer() error");
        logger.info("init() check 1.6");

        gl.glBufferData(GL4.GL_PIXEL_UNPACK_BUFFER,
                headPointerTotalPixels * 4,
                hpiData,
                GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i3 GL4TransparencyContext glBufferData() error");
        logger.info("init() check 1.7");

        // Create atomic counter for next head pointer position
        gl.glGenBuffers(1, atomicCounterId);
        checkGlError(gl, "i4 GL4TransparencyContext glGenBuffers() error");
        logger.info("init() check 1.8");
       
        gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, atomicCounterId.get(0));
        checkGlError(gl, "i5 GL4TransparencyContext glBindBuffer() error");
        logger.info("init() check 1.9");

        gl.glBufferData(GL4.GL_ATOMIC_COUNTER_BUFFER,
                4, null, GL4.GL_DYNAMIC_COPY);
        checkGlError(gl, "i6 GL4TransparencyContext glBufferData() error");
        logger.info("init() check 1.10");

        // Fragment storage buffer
        gl.glGenBuffers(1, fragmentStorageBuffer);
        checkGlError(gl, "i7 GL4TransparencyContext glGenBuffers() error");
        logger.info("init() check 1.11");

        gl.glBindBuffer(GL4.GL_TEXTURE_BUFFER, fragmentStorageBuffer.get(0));
        checkGlError(gl, "i8 GL4TransparencyContext glBindBuffer() error");
        logger.info("init() check 1.12");

        gl.glBufferData(GL4.GL_TEXTURE_BUFFER,
                2 * headPointerTotalPixels * 16, null, GL4.GL_DYNAMIC_COPY);
        checkGlError(gl, "i9 GL4TransparencyContext glBufferData() error");
        logger.info("check 2");
        
    }

}
