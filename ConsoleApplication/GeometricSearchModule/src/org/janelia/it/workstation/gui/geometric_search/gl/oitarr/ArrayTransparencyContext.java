package org.janelia.it.workstation.gui.geometric_search.gl.oitarr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created by murphys on 7/20/2015.
 */
public class ArrayTransparencyContext {

    private final Logger logger = LoggerFactory.getLogger(ArrayTransparencyContext.class);

    protected static GLU glu = new GLU();

    public static int width;
    public static int height;
    public static int depth;

    public static final int NODE_SIZE = 20; // vec4 color, float depth
    public static final int DEFAULT_WIDTH = 1600;
    public static final int DEFAULT_HEIGHT = 1200;
    public static final int DEFAULT_DEPTH = 100;

    IntBuffer headPointerId = IntBuffer.allocate(1);
    IntBuffer headPointerInitializerId = IntBuffer.allocate(1);
    IntBuffer fragmentSSBO = IntBuffer.allocate(1);
    IntBuffer zeroValueBuffer = IntBuffer.allocate(1);

    public ArrayTransparencyContext() {
        this.width = DEFAULT_WIDTH;
        this.height = DEFAULT_HEIGHT;
        this.depth = DEFAULT_DEPTH;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }
    
    public void setWidth(int width) { this.width = width; }
    
    public void setHeight(int height) { this.height = height; }

    public int getHeadPointerTextureId() {
        return headPointerId.get(0);
    }

    public int getFragmentSSBOId() {
        return fragmentSSBO.get(0);
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

        int headPointerTotalPixels = width * height;
        
        if (headPointerTotalPixels<1) {
            throw new Exception("width and height not initialized");
        }

        // Create PBO from which to clear the headPointerTexture
        ByteBuffer bb = ByteBuffer.allocate(headPointerTotalPixels * 4);
        byte ffb = 0; // 0xFF for java is -1, we want the shader to see unsigned int==0xFFFFFFFF
        for (int i=0;i<bb.capacity();i++) {
            bb.put(i, ffb);
        }

        gl.glGenBuffers(1, headPointerInitializerId);
        checkGlError(gl, "i1 ArrayTransparencyContext glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerInitializerId.get(0));
        checkGlError(gl, "i2 ArrayTransparencyContext glBindBuffer() error");

        gl.glBufferData(GL4.GL_PIXEL_UNPACK_BUFFER,
                headPointerTotalPixels * 4,
                bb,
                GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i3 ArrayTransparencyContext glBufferData() error");

        // Allocate empty texture of correct size
        gl.glGenTextures(1, headPointerId);
        checkGlError(gl, "ArrayTransparencyContext glGenTextures() error");

        gl.glBindTexture(GL4.GL_TEXTURE_2D, headPointerId.get(0));
        checkGlError(gl, "ArrayTransparencyContext glBindTexture() error");

        // MAJOR MYSTERY: glTexImage2D did not work with glBindImageTexture - silent non-functionality
        gl.glTexStorage2D(GL4.GL_TEXTURE_2D, 1, GL4.GL_R32UI, width, height);
        checkGlError(gl, "ArrayTransparencyContext glTexStorage2D() error");

        // WHY IS THIS TRIGGERING AN INVALID ERROR? - may be harmless since working (?) in display stage
        gl.glTexSubImage2D(GL4.GL_TEXTURE_2D,
                0,
                0,
                0,
                height,
                width,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                0);
        checkGlError(gl, "i0 ArrayTransparencyContext glTexSubImage2D() error");

        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);
        checkGlError(gl, "i1 ArrayTransparencyContext glBindBuffer() error");

        // SSBO
        //gl.glGenBuffers(1, fragmentSSBO);
        //gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, fragmentSSBO.get(0));
        //gl.glBufferData(GL4.GL_SHADER_STORAGE_BUFFER, MAX_NODES * NODE_SIZE, null, GL4.GL_DYNAMIC_DRAW);
        
        
        
        gl.glGenBuffers(1, fragmentSSBO);
        checkGlError(gl, "i5 ArrayTransparencyContext glGenBuffers() error");
        
        gl.glBindBufferBase(GL4.GL_SHADER_STORAGE_BUFFER, 0, fragmentSSBO.get(0));
        checkGlError(gl, "i7 ArrayTransparencyContext glBindBufferBase() error");

        //gl.glBindBuffer(GL4.GL_SHADER_STORAGE_BUFFER, fragmentSSBO.get(0));
        //checkGlError(gl, "i6 ArrayTransparencyContext glBindBuffer() error");

        logger.info("Calling glBufferData for SSBO with size="+headPointerTotalPixels);
        gl.glBufferData(GL4.GL_SHADER_STORAGE_BUFFER, headPointerTotalPixels*depth*24, null, GL4.GL_DYNAMIC_DRAW);
        checkGlError(gl, "i8 ArrayTransparencyContext glBufferData() error");

 

    }

}
