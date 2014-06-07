/**
 * This class will allow dynamic selection of colors to present in the renderer, by forwarding parameters changed
 * programmatically, onward into the shader-language implementations.
 */
package org.janelia.it.workstation.gui.static_view.shader;

import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;

import javax.media.opengl.GL2;
import java.nio.IntBuffer;

public class RGBExcludableShader extends AbstractShader {
    // Shader GLSL source is expected to be in the same package as this class.  Otherwise,
    // a prefix of the relative path could be given, as in "shader_sub_pkg/AShader.glsl"
    public static final String VERTEX_SHADER = "RGBExcludableVtx.glsl";
    public static final String FRAGMENT_SHADER = "RGBExcludableFrg.glsl";

    private static final float[] SHOW_ALL  = new float[] {
        1.0f, 1.0f, 1.0f
    };

    private int previousShader = 0;
    private float[] rgb;

    private int vertexAttribLoc = -1;
    private int texCoordAttribLoc = -1;

    private TextureMediator signalTextureMediator;

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
        pushFilterUniform( gl, shaderProgram );
        setTextureUniforms( gl );

        vertexAttribLoc = gl.glGetAttribLocation( shaderProgram, "vertexAttribute" );
        texCoordAttribLoc = gl.glGetAttribLocation( shaderProgram, "texCoordAttribute" );
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
    public void setSignalTextureMediator(TextureMediator signalTextureMediator) {
        this.signalTextureMediator = signalTextureMediator;
    }

    public void setColorMask( float[] rgb ) {
        this.rgb = rgb;
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

    private void setTextureUniforms(GL2 gl) {
        setTextureUniform(gl, "signalTexture", signalTextureMediator);
    }

    private void setTextureUniform( GL2 gl, String shaderUniformName, TextureMediator textureMediator ) {
        int signalTextureLoc = gl.glGetUniformLocation( getShaderProgram(), shaderUniformName );
        if ( signalTextureLoc == -1 ) {
            throw new RuntimeException( "Failed to find " + shaderUniformName + " texture location." );
        }
        gl.glUniform1i(signalTextureLoc, textureMediator.getTextureOffset());
        // This did not work.  GL.GL_TEXTURE1 ); //textureIds[ 1 ] );
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
