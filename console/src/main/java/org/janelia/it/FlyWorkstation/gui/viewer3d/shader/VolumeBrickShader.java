/**
 * This class will allow dynamic selection of colors to present in the renderer, by forwarding parameters changed
 * programmatically, onward into the shader-language implementations.
 */
package org.janelia.it.FlyWorkstation.gui.viewer3d.shader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
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
    private float gammaAdjustment = 1.0f;
    private float cropOutLevel = Mip3d.DEFAULT_CROPOUT;
    private float[] cropCoords = Mip3d.DEFAULT_CROP_COORDS;

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
        pushGammaUniform( gl, shaderProgram );
        pushFilterUniform( gl, shaderProgram );
        pushCropUniforms( gl, shaderProgram );

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

    /**
     * How bright should "out-of-crop-volume" voxels shine?
     *
     * @param cropOutLevel 0 means blacked out; 1.0 means bright as anything else.
     */
    public void setCropOutLevel(float cropOutLevel) {
        this.cropOutLevel = cropOutLevel;
    }

    /** Calling this implies that all steps for setting up this special texture have been carried out. */
    public void setVolumeMaskApplied() {
        volumeMaskApplied = true;
    }

    /**
     * A power trip: all colors from final texture calculation, raised to this power.  Color values within
     * shader run the range 0.0..1.0.  Therefore, higher powers (values for gammaAdjustment) yield lower results,
     * approaching 0.  A gammaAdjustment of 1.0 yields the original number, so that is the default.  Very low
     * fractional values like 0.001, 0.00001, etc., approach a result of 1.0 (white).  Negative values should
     * not be used as they would push the resulting value out of the normalized range.
     *
     * @param gammaAdjustment a value between 1.0 and 0.0.
     */
    public void setGammaAdjustment(float gammaAdjustment) {
        this.gammaAdjustment = gammaAdjustment;
    }

    /**
     *  Allow caller to tell which crop coords to upload when the time comes.  These will be specified
     *  as values between 0.0 and 1.0.
     */
    public void setCropCoords( float[] cropCoords ) {
        // Null crop coords will be mis-interpretted.
        if ( cropCoords == null ) {
            return;
        }
        if ( cropCoords.length < 6 ) {
            throw new IllegalArgumentException("Crop coords need a start and end in three dimensions.");
        }
        this.cropCoords = cropCoords;
    }

    /**
     *  Allow caller to tell which crop coords to upload when the time comes.  These will be specified
     *  as values between 0.0 and 1.0.
     */
    public void setCropCoords( float[] xStartEnd, float[] yStartEnd, float[] zStartEnd ) {
        setCropX( xStartEnd );
        setCropY( yStartEnd );
        setCropZ( zStartEnd );
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
        gl.glUniform1i(signalTextureLoc, textureMediator.getTextureOffset());
        // This did not work.  GL.GL_TEXTURE1 ); //textureIds[ 1 ] );
    }

    private void setCropX( float[] startXendX ) {
        setCropAny( startXendX, 0, 1 );
    }

    private void setCropY( float[] startYendY ) {
        setCropAny( startYendY, 2, 3 );
    }

    private void setCropZ( float[] startZendZ ) {
        setCropAny( startZendZ, 4, 5 );
    }

    private void setCropAny( float[] startEnd, int startCoord, int endCoord ) {
        if ( startEnd[ 0 ] > startEnd[ 1 ] ) {
            throw new IllegalArgumentException("Invalid range " + startEnd[ 0 ] + ".." + startEnd[ 1 ]);
        }
        cropCoords[ startCoord ] = startEnd[ 0 ];
        cropCoords[ endCoord ] = startEnd[ 1 ];
    }

    private void pushMaskUniform( GL2 gl, int shaderProgram ) {
        // Need to push uniform for masking parameter.
        int hasMaskLoc = gl.glGetUniformLocation(shaderProgram, "hasMaskingTexture");
        if ( hasMaskLoc == -1 ) {
            throw new RuntimeException( "Failed to find masking texture flag location." );
        }
        gl.glUniform1i( hasMaskLoc, volumeMaskApplied ? 1 : 0 );

    }

    private void pushGammaUniform( GL2 gl, int shaderProgram ) {
        int gammaAdjustmentLoc = gl.glGetUniformLocation(shaderProgram, "gammaAdjustment");
        if ( gammaAdjustmentLoc == -1 ) {
            throw new RuntimeException( "Failed to find gamma adjustment setting location." );
        }
        gl.glUniform1f(gammaAdjustmentLoc, gammaAdjustment);
    }

    /** Upload all cropping starts/ends to GPU. */
    private void pushCropUniforms( GL2 gl, int shaderProgram ) {
        if ( cropCoords[ 0 ] > -1 ) {
            for ( int i = 0; i < 6; i++ ) {
                // Example: startCropX is for item i == 0.
                // Example: endCropZ is for item i == 5.
                String startEnd = i % 2 == 0 ? "start" : "end";
                String xyz = "XYZ".substring( i/2, i/2 + 1 );
                String cropUniformName = String.format("%sCrop%s", startEnd, xyz);
                int adjustmentLoc = gl.glGetUniformLocation(shaderProgram, cropUniformName);
                if ( adjustmentLoc == -1 ) {
                    throw new RuntimeException( "Failed to find uniform location for " + cropUniformName );
                }
                //System.out.println(
                //        "Have adjustment location of " + adjustmentLoc + " for " + cropUniformName +
                //                ", and setting it to " + cropCoords[ i ]
                //);
                gl.glUniform1f(
                        adjustmentLoc,
                        cropCoords[i]
                );
                int setUniformError = gl.glGetError();
                if ( setUniformError != 0 ) {
                    throw new RuntimeException( "Failed to set uniform for " + cropUniformName + " error is " + setUniformError );
                }

            }

            int cropLevelLoc = gl.glGetUniformLocation(shaderProgram, "cropOutLevel");
            if ( cropLevelLoc == -1 ) {
                throw new RuntimeException( "Failed to find unifomrm location for crop-out level" );
            }
            gl.glUniform1f(
                    cropLevelLoc,
                    cropOutLevel
            );
            int setUniformError = gl.glGetError();
            if ( setUniformError != 0 ) {
                throw new RuntimeException( "Failed to set uniform for crop-out level" );
            }
        }
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
