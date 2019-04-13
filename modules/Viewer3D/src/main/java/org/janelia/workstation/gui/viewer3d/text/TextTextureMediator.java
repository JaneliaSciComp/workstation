/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.gui.viewer3d.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL2;
import org.apache.commons.io.IOUtils;
import org.janelia.workstation.gui.viewer3d.OpenGLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acts as the intermediary between the application and OpenGL, wrt 2D textures
 suitable for presenting text.  PNG only.
 * 
 * @author fosterl
 */
public class TextTextureMediator {

    public static final int STORAGE_FORMAT_MULTIPLIER = 4;
    public static final int VOXEL_COMPONENT_TYPE = GL2.GL_UNSIGNED_BYTE;
    public static final int VOXEL_COMPONENT_ORDER = GL2.GL_RGBA;
    public static final int INTERNAL_FORMAT = GL2.GL_RGBA;

    private byte[] textureData;
    private final static Logger logger = LoggerFactory.getLogger( TextTextureMediator.class );
    private final int textureSymbolicId;
    private final int textureName;
    private final int textureOffset;
    private final FontInfo fontInfo;
    
    private boolean hasBeenUploaded = false;
    
    public TextTextureMediator( FontInfo fontInfo, int textureName, int offset, GL2 gl ) {
        textureData = getBytes(fontInfo);
        this.textureName = textureName;
        this.textureOffset = offset;
        this.textureSymbolicId = GL2.GL_TEXTURE0 + offset;
        this.fontInfo = fontInfo;
    }

    public void setupTexture( GL2 gl ) {
        gl.glActiveTexture( textureSymbolicId );
        OpenGLUtils.reportError( "setupTexture glActiveTexture", gl, textureName );
        gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
        OpenGLUtils.reportError( "setupTexture glBindTexture", gl, textureName );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST );
        OpenGLUtils.reportError( "setupTexture glTexParam MIN FILTER", gl, textureName );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST );
        OpenGLUtils.reportError( "setupTexture glTexParam MAG_FILTER", gl, textureName );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        OpenGLUtils.reportError( "setupTexture glTexParam TEX-WRAP-R", gl, textureName );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        OpenGLUtils.reportError( "setupTexture glTexParam TEX-WRAP-S", gl, textureName );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        OpenGLUtils.reportError( "setupTexture glTexParam TEX-WRAP-T", gl, textureName );

    }

    /**
     * Pushes the texture bytes to GPU.
     */
    public void uploadTexture(GL2 gl) {

        gl.glActiveTexture(textureSymbolicId);
        OpenGLUtils.reportError("glActiveTexture", gl, textureName);

        gl.glEnable(GL2.GL_TEXTURE_3D);
        OpenGLUtils.reportError("glEnable", gl, textureName);

        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureName);
        OpenGLUtils.reportError("glBindTexture", gl, textureName);

        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        OpenGLUtils.reportError("glTexEnv MODE-REPLACE", gl, textureName);

        try {

            ByteBuffer data = ByteBuffer.wrap(textureData);
            data.rewind();

            gl.glTexImage2D(
                    GL2.GL_TEXTURE_3D,
                    0, // mipmap level
                    INTERNAL_FORMAT, // as stored INTO graphics hardware, w/ srgb info (GLint internal format)
                    fontInfo.getTotalWidth(), // width
                    fontInfo.getFontHeight(), // height
                    0, // border
                    VOXEL_COMPONENT_ORDER, // voxel component order (GLenum format)
                    VOXEL_COMPONENT_TYPE, // voxel component type=packed RGBA values(GLenum type)
                    data
            );

        } catch (Exception exGlTexImage) {
            logger.error(
                    "Exception reported during texture upload of NAME:OFFS={}, FORMAT:COMP-ORDER:MULTIPLIER={}",
                    this.textureName + ":" + textureOffset,
                    INTERNAL_FORMAT + ":" + VOXEL_COMPONENT_ORDER + ":"
                    + STORAGE_FORMAT_MULTIPLIER, exGlTexImage
            );
        }
        OpenGLUtils.reportError("glTexImage", gl, textureName);

        gl.glBindTexture(GL2.GL_TEXTURE_3D, 0);
        gl.glDisable(GL2.GL_TEXTURE_3D);
        OpenGLUtils.reportError("disable-tex", gl, textureName);

        textureData = null; // GC-dismmiss.
        hasBeenUploaded = true;
    }

    /** Release the texture data memory from the GPU. */
    public void deleteTexture( GL2 gl ) {
        if ( hasBeenUploaded ) {
            OpenGLUtils.reportError( "tex-mediator: upon entry to delete tex", gl, textureName );
            IntBuffer textureNameBuffer = IntBuffer.allocate( 1 );
            textureNameBuffer.put( textureName );
            textureNameBuffer.rewind();
            textureNameBuffer.rewind();
            gl.glDeleteTextures( 1, textureNameBuffer );
            OpenGLUtils.reportError( "tex-mediator: delete texture", gl, textureName );
            hasBeenUploaded = false;
        }
    }
    
    private byte[] getBytes(FontInfo fontInfo) {
        byte[] rtnVal;
        try (InputStream fontImageStream =
                fontInfo.getFontComponentStream(FontInfo.FontComponentExtension.png)) {
            
            rtnVal = IOUtils.toByteArray( fontImageStream );

        } catch ( IOException ioe ) {
            logger.error( "Failed to load texture data. " + fontInfo );
            throw new RuntimeException( "Failed to load texture " + fontInfo.getBaseFontName(), ioe );
        }
        return rtnVal;
    }
}
