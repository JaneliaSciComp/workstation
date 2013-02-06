/**
 * This class will allow dynamic selection of colors to present in the renderer, by forwarding parameters changed
 * programmatically, onward into the shader-language implementations.
 */
package org.janelia.it.FlyWorkstation.gui.viewer3d.shader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureMediator;

import javax.media.opengl.GL2;
import java.nio.IntBuffer;

public class VolumeBrickShader extends AbstractShader {
    // Shader GLSL source is expected to be in the same package as this class.  Otherwise,
    // a prefix of the relative path could be given, as in "shader_sub_pkg/AShader.glsl"
    public static final String VERTEX_SHADER = "VolumeBrickVtx.glsl";
    public static final String FRAGMENT_SHADER = "VolumeBrickFrg.glsl";

    private static final float[] SHOW_ALL  = new float[] {
        1.0f, 1.0f, 1.0f
    };

    private int previousShader = 0;
    private float[] rgb;

    private TextureMediator signalTextureMediator;
    private TextureMediator maskTextureMediator;
    private TextureMediator colorMapTextureMediator;

    private boolean volumeMaskApplied = false;

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

        pushMaskUniform( gl, shaderProgram );
        pushFilterUniform( gl, shaderProgram );
        setTextureUniforms( gl );
    }

    /**
     * Note, this must be called with at least the signal mediator, before attempting to set texture uniforms.
     *
     * @param signalTextureMediator intermediator for signal.
     * @param maskTextureMediator intermediator for mask.
     */
    public void setTextureMediators(
            TextureMediator signalTextureMediator,
            TextureMediator maskTextureMediator,
            TextureMediator colorMapTextureMediator ) {
        this.signalTextureMediator = signalTextureMediator;
        this.maskTextureMediator = maskTextureMediator;
        this.colorMapTextureMediator = colorMapTextureMediator;
    }

    public void setColorMask( float[] rgb ) {
        this.rgb = rgb;
    }

    /** Calling this implies that all steps for setting up this special texture have been carried out. */
    public void setVolumeMaskApplied() {
        volumeMaskApplied = true;
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

    private void setTextureUniforms(GL2 gl) {
        setTextureUniform(gl, "signalTexture", signalTextureMediator);
        //  This did not work.  GL.GL_TEXTURE0 ); //textureIds[ 0 ] );

        if ( volumeMaskApplied ) {
            setTextureUniform(gl, "maskingTexture", maskTextureMediator);
            setTextureUniform(gl, "colorMapTexture", colorMapTextureMediator);
        }
    }

    private void setTextureUniform( GL2 gl, String shaderUniformName, TextureMediator textureMediator ) {
        int signalTextureLoc = gl.glGetUniformLocation( getShaderProgram(), shaderUniformName );
        if ( signalTextureLoc == -1 ) {
            throw new RuntimeException( "Failed to find " + shaderUniformName + " texture location." );
        }
        gl.glUniform1i( signalTextureLoc, textureMediator.getTextureOffset() );
        // This did not work.  GL.GL_TEXTURE1 ); //textureIds[ 1 ] );
    }

    private void pushMaskUniform( GL2 gl, int shaderProgram ) {
        // Need to push uniform for masking parameter.
        int hasMaskLoc = gl.glGetUniformLocation(shaderProgram, "hasMaskingTexture");
        if ( hasMaskLoc == -1 ) {
            throw new RuntimeException( "Failed to find masking texture flag location." );
        }
        gl.glUniform1i( hasMaskLoc, volumeMaskApplied ? 1 : 0 );

    }

    private void pushFilterUniform(GL2 gl, int shaderProgram) {
        // Need to push uniform for the filtering parameter.
        int colorMaskLoc = gl.glGetUniformLocation(shaderProgram, "colorMask");
        if ( colorMaskLoc == -1 ) {
            throw new RuntimeException( "Failed to find color mask uniform location." );
        }

        float[] localrgb = null;
        if ( rgb == null ) {
            localrgb = SHOW_ALL;
        }
        else {
            localrgb = rgb;
        }

        gl.glUniform4f(
                colorMaskLoc,
                localrgb[0],
                localrgb[1],
                localrgb[2],
                1.0f
        );

    }

}
