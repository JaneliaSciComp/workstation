/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.geometric_search.viewer;

import java.nio.ByteBuffer;
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
    public static final int MAX_NODES = MAX_HEIGHT * MAX_WIDTH * 2;
    public static final int NODE_SIZE = 24; // 4 float color vector, 1 depth float, 1 next pointer
    
    int headPointerTotalPixels=0;

    IntBuffer headPointerId = IntBuffer.allocate(1);
    IntBuffer headPointerInitializerId = IntBuffer.allocate(1);
    IntBuffer atomicCounterId = IntBuffer.allocate(1);
    //IntBuffer fragmentStorageTexture = IntBuffer.allocate(1);
    //IntBuffer fragmentStorageBuffer = IntBuffer.allocate(1);
    IntBuffer fragmentSSBO = IntBuffer.allocate(1);
    IntBuffer zeroValueBuffer = IntBuffer.allocate(1);
    
    public int getHeadPointerTextureId() {
        return headPointerId.get(0);
    }

//    public int getFragmentStorageBufferId() {
//        return fragmentStorageBuffer.get(0);
//    }
    
//    public int getFragmentStorageTextureId() {
//        return fragmentStorageTexture.get(0);
//    }
    
    public int getFragmentSSBOId() {
        return fragmentSSBO.get(0);
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

        // Create PBO from which to clear the headPointerTexture
        ByteBuffer bb = ByteBuffer.allocate(headPointerTotalPixels * 4);
        byte ffb = -1; // 0xFF for java is -1, we want the shader to see unsigned int==0xFFFFFFFF
        for (int i=0;i<bb.capacity();i++) {
            bb.put(i, ffb);
        }
        
        gl.glGenBuffers(1, headPointerInitializerId);
        checkGlError(gl, "i1 GL4TransparencyContext glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerInitializerId.get(0));
        checkGlError(gl, "i2 GL4TransparencyContext glBindBuffer() error");

        gl.glBufferData(GL4.GL_PIXEL_UNPACK_BUFFER,
                headPointerTotalPixels * 4,
                bb,
                GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i3 GL4TransparencyContext glBufferData() error");                   
                
        // Allocate empty texture of correct size
        gl.glGenTextures(1, headPointerId);
        checkGlError(gl, "GL4TransparencyContext glGenTextures() error");

        gl.glBindTexture(GL4.GL_TEXTURE_2D, headPointerId.get(0));
        checkGlError(gl, "GL4TransparencyContext glBindTexture() error");

        // MAJOR MYSTERY: glTexImage2D did not work with glBindImageTexture - silent non-functionality
        gl.glTexStorage2D(GL4.GL_TEXTURE_2D, 1, GL4.GL_R32UI, MAX_WIDTH, MAX_HEIGHT);
        checkGlError(gl, "OITMeshDrawShader glTexStorage2D() error");
        
        // WHY IS THIS TRIGGERING AN INVALID ERROR? - may be harmless since working (?) in display stage
        gl.glTexSubImage2D(GL4.GL_TEXTURE_2D, 
                0,
                0,
                0,
                MAX_HEIGHT,
                MAX_WIDTH,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                0);
        checkGlError(gl, "i0 GL4TransparencyContext glTexSubImage2D() error");
                
       gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);
       checkGlError(gl, "i1 GL4TransparencyContext glBindBuffer() error");
        
        // Create atomic counter for next head pointer position
        gl.glGenBuffers(1, atomicCounterId);
        checkGlError(gl, "i4 GL4TransparencyContext glGenBuffers() error");
       
        gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, atomicCounterId.get(0));
        checkGlError(gl, "i5 GL4TransparencyContext glBindBuffer() error");

        gl.glBufferData(GL4.GL_ATOMIC_COUNTER_BUFFER,
                4, null, GL4.GL_DYNAMIC_COPY);
        checkGlError(gl, "i6 GL4TransparencyContext glBufferData() error");

        // Fragment storage buffer
        
        // TEXTURE_BUFFER
        
//        gl.glGenBuffers(1, fragmentStorageBuffer);
//        checkGlError(gl, "i7 GL4TransparencyContext glGenBuffers() error");
//
//        gl.glBindBuffer(GL4.GL_TEXTURE_BUFFER, fragmentStorageBuffer.get(0));
//        checkGlError(gl, "i8.1 GL4TransparencyContext glBindBuffer() error");
//
//        gl.glBufferData(GL4.GL_TEXTURE_BUFFER,
//                2 * headPointerTotalPixels * 16, null, GL4.GL_DYNAMIC_COPY);
//        checkGlError(gl, "i9 GL4TransparencyContext glBufferData() error");
        
        // TEXTURE
        
//        gl.glGenTextures(1, fragmentStorageTexture);
//        checkGlError(gl, "i10 GL4TransparencyContext glGenTextures() error");
//        
//        gl.glBindTexture(GL4.GL_TEXTURE_1D, fragmentStorageTexture.get(0));
//        checkGlError(gl, "i11 GL4TransparencyContext glBindTexture() error");
//        
//        gl.glTexStorage1D(GL4.GL_TEXTURE_1D, 1, GL4.GL_RGBA32UI, 16000);
//        checkGlError(gl, "OITMeshDrawShader i12 glTexStorage1D() error");
        
        // SSBO
        
        gl.glGenBuffers(1, fragmentSSBO);
        gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, fragmentSSBO.get(0));
        gl.glBufferData(GL4.GL_SHADER_STORAGE_BUFFER, MAX_NODES * NODE_SIZE, null, GL4.GL_DYNAMIC_DRAW);
               
    }

}
