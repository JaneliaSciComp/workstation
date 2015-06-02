package org.janelia.it.workstation.gui.geometric_search.gl;

import java.nio.ByteBuffer;
import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL4;
import java.nio.IntBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 5/15/15.
 */
public class OITMeshDrawShader extends GL4Shader {
    
    private Logger logger = LoggerFactory.getLogger( OITMeshDrawShader.class );

    static final int MAX_HEIGHT = 2048;
    static final int MAX_WIDTH = 2048;
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

    public void setProjection(GL4 gl, Matrix4 projection) {
        setUniformMatrix4fv(gl, "proj", false, projection.asArray());
        checkGlError(gl, "OITMeshDrawShader setProjection() error");
    }

    public void setView(GL4 gl, Matrix4 view) {
        setUniformMatrix4fv(gl, "view", false, view.asArray());
        checkGlError(gl, "OITMeshDrawShader setView() error");
    }

    public void setModel(GL4 gl, Matrix4 model) {
        setUniformMatrix4fv(gl, "model", false, model.asArray());
        checkGlError(gl, "OITMeshDrawShader setModel() error");
    }

    @Override
    public String getVertexShaderResourceName() {
        return "OITMeshDrawVertex.glsl";
    }

    @Override
    public String getFragmentShaderResourceName() {
        return "OITMeshDrawFragment.glsl";
    }

    @Override
    public void init(GL4 gl) throws ShaderCreationException {
        super.init(gl);
        checkGlError(gl, "OITMeshDrawShader super.init() error");
        
        logger.info("init() check 1");

        zeroValueBuffer.put(0,0);

        headPointerTotalPixels=MAX_HEIGHT * MAX_WIDTH;

        // Allocate empty texture of correct size
        gl.glGenTextures(1, headPointerId);
        checkGlError(gl, "OITMeshDrawShader glGenTextures() error");
        
                logger.info("init() check 1.1");
                
                        logger.info("headPointerId="+headPointerId.get(0));

        gl.glBindTexture(GL4.GL_TEXTURE_2D, headPointerId.get(0));
        checkGlError(gl, "OITMeshDrawShader glBindTexture() error");
        
        
                logger.info("init() check 1.2");


        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0,
                GL4.GL_R32UI,
                MAX_HEIGHT,
                MAX_WIDTH,
                0,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                null);
        checkGlError(gl, "OITMeshDrawShader glTexImage2D() error");
        
                logger.info("init() check 1.3");


        // Create PBO from which to clear the headPointerTexture
        IntBuffer hpiData = IntBuffer.allocate(headPointerTotalPixels);
        for (int i=0;i<hpiData.capacity();i++) {
            hpiData.put(i, Integer.MAX_VALUE);
        }
        
                logger.info("init() check 1.4");
                
       gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);
               checkGlError(gl, "i1 OITMeshDrawShader glBindBuffer() error");
               logger.info("init() check 1.4.1");


        gl.glGenBuffers(1, headPointerInitializerId);
        checkGlError(gl, "i1 OITMeshDrawShader glGenBuffers() error");
        
        logger.info("headPointerInitializerId="+headPointerInitializerId.get(0));
        
                logger.info("init() check 1.5");


        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerInitializerId.get(0));
        checkGlError(gl, "i2 OITMeshDrawShader glBindBuffer() error");
        
                logger.info("init() check 1.6");


        gl.glBufferData(GL4.GL_PIXEL_UNPACK_BUFFER,
                headPointerTotalPixels * 4,
                hpiData,
                GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i3 OITMeshDrawShader glBufferData() error");
        
                logger.info("init() check 1.7");


        // Create atomic counter for next head pointer position
        gl.glGenBuffers(1, atomicCounterId);
        checkGlError(gl, "i4 OITMeshDrawShader glGenBuffers() error");
        
                logger.info("init() check 1.8");



        gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, atomicCounterId.get(0));
        checkGlError(gl, "i5 OITMeshDrawShader glBindBuffer() error");
        
                logger.info("init() check 1.9");



        gl.glBufferData(GL4.GL_ATOMIC_COUNTER_BUFFER,
                4, null, GL4.GL_DYNAMIC_COPY);
        checkGlError(gl, "i6 OITMeshDrawShader glBufferData() error");
        
                logger.info("init() check 1.10");



        // Fragment storage buffer
        gl.glGenBuffers(1, fragmentStorageBuffer);
        checkGlError(gl, "i7 OITMeshDrawShader glGenBuffers() error");
        
                logger.info("init() check 1.11");


        gl.glBindBuffer(GL4.GL_TEXTURE_BUFFER, fragmentStorageBuffer.get(0));
        checkGlError(gl, "i8 OITMeshDrawShader glBindBuffer() error");
        
                logger.info("init() check 1.12");



        gl.glBufferData(GL4.GL_TEXTURE_BUFFER,
                2 * headPointerTotalPixels * 16, null, GL4.GL_DYNAMIC_COPY);
        checkGlError(gl, "i9 OITMeshDrawShader glBufferData() error");
        
        logger.info("check 2");

    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 OITMeshDrawShader super.display() error");


        // Clear the headPointerTexture
//        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerInitializerId.get(0));
//        checkGlError(gl, "d2 OITMeshDrawShader glBindBuffer() error");
        
                gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);

        gl.glBindTexture(GL4.GL_TEXTURE_2D, headPointerId.get(0));
        checkGlError(gl, "d3 OITMeshDrawShader glBindTexture() error");
        

        gl.glTexImage2D(GL4.GL_TEXTURE_2D,
                0,
                GL4.GL_R32UI,
                MAX_WIDTH,
                MAX_HEIGHT,
                0,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                null);
        checkGlError(gl, "d4 OITMeshDrawShader glTexImage2D() error");
       
        // Try moving the above command after glTexImage2D...
               gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, headPointerInitializerId.get(0));
        checkGlError(gl, "d2 OITMeshDrawShader glBindBuffer() error");


        // Bind the headPointerTexture for read-write
        gl.glBindImageTexture(0, headPointerId.get(0), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_R32UI);
        checkGlError(gl, "d5 OITMeshDrawShader glBindImageTexture() error");

        // Bind and reset the atomic counter
//        gl.glBindBufferBase(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, atomicCounterId.get(0));
//        checkGlError(gl, "d6 OITMeshDrawShader glBindBufferBase() error");
//
//        gl.glBufferSubData(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, zeroValueBuffer);
//        checkGlError(gl, "d7 OITMeshDrawShader glBufferSubData() error");
        
               gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, atomicCounterId.get(0));
        checkGlError(gl, "d6a OITMeshDrawShader glBindBuffer() error");


//        gl.glBufferData(GL4.GL_ATOMIC_COUNTER_BUFFER,
//                4, zeroValueBuffer, GL4.GL_DYNAMIC_COPY);
//        checkGlError(gl, "d7a OITMeshDrawShader glBufferData() error");
        
/////////////////// Lighthouse Example       
//            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, ac_buffer);
//    GLuint* ptr = (GLuint*)glMapBufferRange(GL_ATOMIC_COUNTER_BUFFER, 0, sizeof(GLuint),
//                                            GL_MAP_WRITE_BIT | 
//                                            GL_MAP_INVALIDATE_BUFFER_BIT | 
//                                            GL_MAP_UNSYNCHRONIZED_BIT);
//    ptr[0] = value;
//    glUnmapBuffer(GL_ATOMIC_COUNTER_BUFFER);
//    glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, 0);
        
  //      ByteBuffer bb = gl.glMapBufferRange(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, GL4.GL_MAP_WRITE_BIT | GL4.GL_MAP_INVALIDATE_BUFFER_BIT | GL4.GL_MAP_UNSYNCHRONIZED_BIT);
        ByteBuffer bb = gl.glMapBufferRange(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, GL4.GL_MAP_WRITE_BIT);
         checkGlError(gl, "d7a OITMeshDrawShader glMapBufferRange() error");
       
        IntBuffer ib = bb.asIntBuffer();
         checkGlError(gl, "d8a OITMeshDrawShader bb.asIntBuffer() error");
       
        ib.put(0, 0);
         checkGlError(gl, "d9a OITMeshDrawShader ib.put() error");
       
        gl.glUnmapBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER);
         checkGlError(gl, "d10a OITMeshDrawShader glUnmapBuffer() error");
       
    }

}
