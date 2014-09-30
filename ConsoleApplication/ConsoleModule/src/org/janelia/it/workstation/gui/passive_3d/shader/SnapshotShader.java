/**
 * This class will allow dynamic selection of colors to present in the renderer, by forwarding parameters changed
 * programmatically, onward into the shader-language implementations.
 */
package org.janelia.it.workstation.gui.passive_3d.shader;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;

import javax.media.opengl.GL2;
import java.nio.IntBuffer;
import org.janelia.it.workstation.gui.viewer3d.shader.SignalShader;

public class SnapshotShader extends SignalShader {
    // Shader GLSL source is expected to be in the same package as this class.  Otherwise,
    // a prefix of the relative path could be given, as in "shader_sub_pkg/AShader.glsl"
    public static final String VERTEX_SHADER = "SnapshotVtx.glsl";
    public static final String FRAGMENT_SHADER = "SnapshotFrg.glsl";

    private static final float[] SHOW_ALL  = new float[] {
        1.0f, 1.0f, 1.0f
    };

    private int previousShader = 0;
    private float[] rgb;

    private int vertexAttribLoc = -1;
    private int texCoordAttribLoc = -1;

    private int channelGammaLoc;
    private int channelMinLoc;
    private int channelColorLoc;
    private int channelScaleLoc;
    private int channelCountLoc;
    private int interleaveFlagLoc;
    
    private TextureMediator signalTextureMediator;
    private TextureMediator interleavedTextureMediator; // Optional: can be null.

    @Override
    public String getVertexShader() {
        return VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    @Override
    public void load(GL2 gl) {
        IntBuffer buffer = IntBuffer.allocate( 1 );
        gl.glGetIntegerv( GL2.GL_CURRENT_PROGRAM, buffer );
        previousShader = buffer.get();
        int shaderProgram = getShaderProgram();

        gl.glUseProgram( shaderProgram );
        setTextureUniforms( gl );

        vertexAttribLoc   = gl.glGetAttribLocation( shaderProgram, "vertexAttribute" );
        texCoordAttribLoc = gl.glGetAttribLocation( shaderProgram, "texCoordAttribute" );

        channelGammaLoc   = gl.glGetUniformLocation( shaderProgram, "channel_gamma" );
        channelMinLoc     = gl.glGetUniformLocation( shaderProgram, "channel_min" );
        channelColorLoc   = gl.glGetUniformLocation( shaderProgram, "channel_color" );
        channelScaleLoc   = gl.glGetUniformLocation( shaderProgram, "channel_scale" );
        channelCountLoc   = gl.glGetUniformLocation( shaderProgram, "channel_count" );
        interleaveFlagLoc = gl.glGetUniformLocation( shaderProgram, "interleave_flag" );
    }

    public int getVertexAttribLoc() {
        if ( vertexAttribLoc == -1 ) {
            throw new IllegalStateException("Unset value.");
        }
        return vertexAttribLoc;
    }

    public int getTexCoordAttribLoc() {
        if ( texCoordAttribLoc == -1 ) {
            throw new IllegalStateException("Unset value.");
        }
        return texCoordAttribLoc;
    }

    /**
     * Note, this must be called with at least the signal mediator, before attempting to set texture uniforms.
     *
     * @param signalTextureMediator intermediator for signal.
     */
    @Override
    public void setSignalTextureMediator(TextureMediator signalTextureMediator) {
        this.signalTextureMediator = signalTextureMediator;
    }

    @Override
    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }
    
    public void setInterleavedTextureMediator(TextureMediator interleavedTextureMediator) {
        this.interleavedTextureMediator = interleavedTextureMediator;
    }
    
    //------------------------------Unique-to-shader setters
    public void setChannelGamma( GL2 gl, float[] channelGamma ) {
        gl.glUniform4fv( channelGammaLoc, 1, channelGamma, 0 );
    }
    
    public void setChannelMin( GL2 gl, float[] channelMin ) {
        gl.glUniform4fv( channelMinLoc, 1, channelMin, 0 );
    }
    
    public void setChannelColor( GL2 gl, float[] channelColor ) {
        gl.glUniform3fv( channelColorLoc, 4, channelColor, 0 );
    }
    
    public void setChannelScale( GL2 gl, float[] channelScale ) {
        gl.glUniform4fv( channelScaleLoc, 1, channelScale, 0 );
    }
    
    public void setChannelCount( GL2 gl, int count ) {
        gl.glUniform1i( channelCountLoc, count );
    }
    
    /** This flag should be set, if the texture is broken into two interleaved parts. */
    public void setExplicitInterleave( GL2 gl, boolean interleaveFlag ) {
        gl.glUniform1i( interleaveFlagLoc, interleaveFlag ? 1 : 0 );
    }

    private void setTextureUniforms(GL2 gl) {
        setTextureUniform(gl, "signalTexture", signalTextureMediator);
        if ( interleavedTextureMediator != null ) {
            setTextureUniform(gl, "interleavedTexture", interleavedTextureMediator);
        }
    }

    private void setTextureUniform( GL2 gl, String shaderUniformName, TextureMediator textureMediator ) {
        int signalTextureLoc = gl.glGetUniformLocation( getShaderProgram(), shaderUniformName );
        if ( signalTextureLoc == -1 ) {
            throw new RuntimeException( "Failed to find " + shaderUniformName + " texture location." );
        }
        gl.glUniform1i(signalTextureLoc, textureMediator.getTextureOffset());
        // This did not work.  GL.GL_TEXTURE1 ); //textureIds[ 1 ] );
    }

}
