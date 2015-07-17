/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.viewer3d.picking;

//import javax.media.opengl.GL2GL3;
import java.nio.ByteBuffer;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;

import java.nio.IntBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import org.slf4j.LoggerFactory;

/**
 * Setup a buffer for IDs-as-renders, object selection.  Support
 * carrying out the prep, teardown, and actual selection.  Meant
 * to be called from a GLActor.
 * 
 * @see http://www.opengl-tutorial.org/intermediate-tutorials/tutorial-14-render-to-texture/
 *
 * @author fosterl
 */
public class RenderedIdPicker {
    private int frameBufId;
    private int textureId;
    private final int viewportWidth;
    private final int viewportHeight;

    // Unknown utility.
    static {
        try {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            final GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setGLProfile(profile);
            // KEEPING this for use of GL3 under MAC.  So far, unneeded, and not debugged.
            //        SwingUtilities.invokeLater(new Runnable() {
            //            public void run() {
            //                new JOCLSimpleGL3(capabilities);
            //            }
            //        });
        } catch (Throwable th) {
            LoggerFactory.getLogger(RenderedIdPicker.class).error("No GL3 profile available");
        }

    }

    public RenderedIdPicker(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }
    
    /**
     * Call this for the one-time-only steps.
     * @param glDrawable 
     */
    public void init(GLAutoDrawable glDrawable) {        
        GL3 gl = glDrawable.getGL().getGL2().getGL3();
        
        // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        IntBuffer exchange = IntBuffer.allocate(1);
        exchange.rewind();
        gl.glGenFramebuffers(1, exchange);
        exchange.rewind();
        frameBufId = exchange.get();
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, frameBufId);
        
        // The texture we're going to render to
        exchange.rewind();
        gl.glGenTextures(1, exchange);
        exchange.rewind();
        textureId = exchange.get();

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL3.GL_TEXTURE_2D, textureId);

        // Give an empty image to OpenGL ( the last "0" )
        gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGB, viewportWidth, viewportHeight, 0, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, 0);
        
        // Set "renderedTexture" as our color attachement #0
        gl.glFramebufferTexture(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, textureId, 0);

        // Set the list of draw buffers.
        //GLenum DrawBuffers[
        //1] = {GL_COLOR_ATTACHMENT0
        //};
        //gl.glDrawBuffers(1, DrawBuffers); // "1" is the size of DrawBuffers        
        
    }
    
    public void postPick(GLAutoDrawable glDrawable) {
        GL3 gl = glDrawable.getGL().getGL2().getGL3();
        GL2 gl2 = glDrawable.getGL().getGL2();
        //gl.glActiveTexture(textureSymbolicId);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, textureId);
        reportError("testTextureContents glBindTexture", gl2, textureId);

        int pixelSize = 3 * (Float.SIZE / Byte.SIZE);
        int bufferSize = viewportWidth * viewportHeight * pixelSize;
        byte[] rawBuffer = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);
        gl.glGetTexImage(GL2.GL_TEXTURE_2D, 0, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, buffer);
        
    }
}
