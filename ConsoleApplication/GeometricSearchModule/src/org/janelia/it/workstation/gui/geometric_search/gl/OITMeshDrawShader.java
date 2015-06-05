package org.janelia.it.workstation.gui.geometric_search.gl;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.janelia.geometry3d.Matrix4;

import javax.media.opengl.GL4;
import java.nio.IntBuffer;
import org.janelia.it.workstation.gui.geometric_search.viewer.GL4TransparencyContext;
import static org.janelia.it.workstation.gui.geometric_search.viewer.GL4TransparencyContext.MAX_HEIGHT;
import static org.janelia.it.workstation.gui.geometric_search.viewer.GL4TransparencyContext.MAX_WIDTH;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by murphys on 5/15/15.
 */
public class OITMeshDrawShader extends GL4Shader {
    
    private Logger logger = LoggerFactory.getLogger( OITMeshDrawShader.class );
     
    private GL4TransparencyContext tc;

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
    
    public void setTransparencyContext(GL4TransparencyContext tc) {
        this.tc=tc;
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
    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
        checkGlError(gl, "d1 OITMeshDrawShader super.display() error");

        // Clear the headPointerTexture
        gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, tc.getHeadPointerInitializerId());
        checkGlError(gl, "d2 OITMeshDrawShader glBindBuffer() error");
              
        gl.glBindTexture(GL4.GL_TEXTURE_2D, tc.getHeadPointerTextureId());
        checkGlError(gl, "d3 OITMeshDrawShader glBindTexture() error");
        
        gl.glTexSubImage2D(GL4.GL_TEXTURE_2D,
                0, // level
                0, // xoffset
                0, // yoffset
                GL4TransparencyContext.MAX_WIDTH,
                GL4TransparencyContext.MAX_HEIGHT,
                GL4.GL_RED_INTEGER,
                GL4.GL_UNSIGNED_INT,
                0);
        checkGlError(gl, "d4 OITMeshDrawShader glTexImage2D() error");
       
       gl.glBindBuffer(GL4.GL_PIXEL_UNPACK_BUFFER, 0);
        
               // Bind the headPointerTexture for read-write
        
        //gl.glActiveTexture(GL4.GL_TEXTURE0);
                
        gl.glBindImageTexture(3, tc.getHeadPointerTextureId(), 0, false, 0, GL4.GL_READ_WRITE, GL4.GL_R32UI);
        checkGlError(gl, "d5 OITMeshDrawShader glBindImageTexture() error");
               
 //       gl.glBindBuffer(GL4.GL_TEXTURE_BUFFER, tc.getFragmentStorageBufferId());
 //       checkGlError(gl, "d5.3 OITMeshDrawShader glBindBuffer() error");
                
        // Bind and reset the atomic counter
//        gl.glBindBufferBase(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, atomicCounterId.get(0));
//        checkGlError(gl, "d6 OITMeshDrawShader glBindBufferBase() error");
//
//        gl.glBufferSubData(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, zeroValueBuffer);
//        checkGlError(gl, "d7 OITMeshDrawShader glBufferSubData() error");
        
//        gl.glBindBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER, tc.getAtomicCounterId());
//        checkGlError(gl, "d6a OITMeshDrawShader glBindBuffer() error");


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
//        ByteBuffer bb = gl.glMapBufferRange(GL4.GL_ATOMIC_COUNTER_BUFFER, 0, 4, GL4.GL_MAP_WRITE_BIT);
//         checkGlError(gl, "d7a OITMeshDrawShader glMapBufferRange() error");
//       
//        IntBuffer ib = bb.asIntBuffer();
//         checkGlError(gl, "d8a OITMeshDrawShader bb.asIntBuffer() error");
//       
//        ib.put(0, 0);
//         checkGlError(gl, "d9a OITMeshDrawShader ib.put() error");
//       
//        gl.glUnmapBuffer(GL4.GL_ATOMIC_COUNTER_BUFFER);
//         checkGlError(gl, "d10a OITMeshDrawShader glUnmapBuffer() error");
        
//        IntBuffer GL_IMAGE_BINDING_NAME_b = IntBuffer.allocate(1); GL_IMAGE_BINDING_NAME_b.put(0, -1);      
//        gl.glGetIntegeri_v(GL4.GL_IMAGE_BINDING_NAME, 0, GL_IMAGE_BINDING_NAME_b);       
//        logger.info("GL_IMAGE_BINDING_NAME = " + GL_IMAGE_BINDING_NAME_b.get(0));
//        
//        IntBuffer GL_IMAGE_BINDING_LEVEL_b = IntBuffer.allocate(1); GL_IMAGE_BINDING_LEVEL_b.put(0, -1);
//        gl.glGetIntegeri_v(GL4.GL_IMAGE_BINDING_LEVEL, 0, GL_IMAGE_BINDING_LEVEL_b);
//        logger.info("GL_IMAGE_BINDING_LEVEL = " + GL_IMAGE_BINDING_LEVEL_b.get(0));
//        
//        IntBuffer GL_IMAGE_BINDING_ACCESS_b = IntBuffer.allocate(1); GL_IMAGE_BINDING_ACCESS_b.put(0, -1);
//        gl.glGetIntegeri_v(GL4.GL_IMAGE_BINDING_ACCESS, 0, GL_IMAGE_BINDING_ACCESS_b);
//        logger.info("GL_IMAGE_BINDING_ACCESS = " + GL_IMAGE_BINDING_ACCESS_b.get(0));       
//        
//        logger.info("GL_READ_ONLY="+GL4.GL_READ_ONLY);
//        logger.info("GL_READ_WRITE="+GL4.GL_READ_WRITE);
//        logger.info("GL_WRITE_ONLY="+GL4.GL_WRITE_ONLY);
//        
//        IntBuffer GL_IMAGE_BINDING_FORMAT_b = IntBuffer.allocate(1); GL_IMAGE_BINDING_FORMAT_b.put(0, -1);
//        gl.glGetIntegeri_v(GL4.GL_IMAGE_BINDING_FORMAT, 0, GL_IMAGE_BINDING_FORMAT_b);
//        logger.info("GL_IMAGE_BINDING_FORMAT = " + GL_IMAGE_BINDING_FORMAT_b.get(0)); 
        
        ///////////////////////////////////////////
        
//        IntBuffer GL_CURRENT_PROGRAM_b = IntBuffer.allocate(1); GL_CURRENT_PROGRAM_b.put(0, -1);
//        gl.glGetIntegerv(GL4.GL_CURRENT_PROGRAM, GL_CURRENT_PROGRAM_b);
//        int currentProgram=GL_CURRENT_PROGRAM_b.get(0);
//        logger.info("GL_CURRENT_PROGRAM = " + currentProgram); 
//        
//        IntBuffer GL_LINK_STATUS_b = IntBuffer.allocate(1); GL_LINK_STATUS_b.put(0, -1);
//        gl.glGetProgramiv(currentProgram, GL4.GL_LINK_STATUS, GL_LINK_STATUS_b);
//        logger.info("GL_LINK_STATUS = " + GL_LINK_STATUS_b.get(0)); 
//        
//        IntBuffer GL_VALIDATE_STATUS_b = IntBuffer.allocate(1); GL_VALIDATE_STATUS_b.put(0, -1);
//        gl.glGetProgramiv(currentProgram, GL4.GL_VALIDATE_STATUS, GL_VALIDATE_STATUS_b);
//        logger.info("GL_VALIDATE_STATUS = " + GL_VALIDATE_STATUS_b.get(0));
//        
//        IntBuffer programLength_b = IntBuffer.allocate(1);
//        ByteBuffer lb = ByteBuffer.allocate(1000);
//        gl.glGetProgramInfoLog(currentProgram, 1000, programLength_b, lb);
//        try {
//            String programLog = new String(lb.array(), "UTF-8");
//            logger.info("Program log="+programLog);
//        } catch (UnsupportedEncodingException ex) {
//            Exceptions.printStackTrace(ex);
//        }
        
        
       
    }

}
