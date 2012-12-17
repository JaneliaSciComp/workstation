/**
 * This class will allow dynamic selection of colors to present in the renderer, by forwarding parameters changed
 * programmatically, onward into the shader-language implementations.
 */
package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.omg.CORBA.PUBLIC_MEMBER;

import javax.media.opengl.GL2;
import java.nio.IntBuffer;

public class ColorFilterShader extends AbstractShader {
    public static final String VERTEX_SHADER = "shaders/ColorFilterVtx.glsl";
    public static final String FRAGMENT_SHADER = "shaders/ColorFilterFrg.glsl";

    private static final float[] SHOW_ALL  = new float[] {
        1.0f, 1.0f, 1.0f
    };

    private int previousShader = 0;
    private float[] rgb;

    @Override
    public String getVertexShader() {
        return VERTEX_SHADER;
    }

    @Override
    public String getFragmentShader() {
        return FRAGMENT_SHADER;
    }

    public void load(GL2 gl) {
        IntBuffer buffer = IntBuffer.allocate(1);
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, buffer);
        previousShader = buffer.get();
        gl.glUseProgram( getShaderProgram() );
        gl.glUniform1i(gl.glGetUniformLocation(getShaderProgram(), "volumeTexture"), 0);
        pushFilterUniform(gl);


    }

    public void setColorMask( float red, float green, float blue ) {
        setColorMask( new float[] {red, green, blue} );
    }

    public void setColorMask( float[] rgb ) {
        this.rgb = rgb;
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
    }

    private void pushFilterUniform(GL2 gl) {
        // Need to push uniform for the filtering parameter.
        int colorMaskLocation = gl.glGetUniformLocation(getShaderProgram(), "colorMask");
        if ( colorMaskLocation == -1 ) {
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
                colorMaskLocation,
                localrgb[0],
                localrgb[1],
                localrgb[2],
                1.0f
        );
    }

}
