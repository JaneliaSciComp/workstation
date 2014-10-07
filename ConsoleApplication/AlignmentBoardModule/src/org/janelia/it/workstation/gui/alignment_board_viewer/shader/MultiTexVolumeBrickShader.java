/**
 * This class will allow dynamic selection of colors to present in the renderer, by forwarding parameters changed
 * programmatically, onward into the shader-language implementations.
 */
package org.janelia.it.workstation.gui.alignment_board_viewer.shader;

import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.janelia.it.workstation.gui.viewer3d.CropCoordSet;

import javax.media.opengl.GL2;
import java.nio.IntBuffer;

public class MultiTexVolumeBrickShader extends AbstractShader {
    public static final String SIGNAL_TEXTURE_NAME = "signalTexture";
    // Shader GLSL source is expected to be in the same package as this class.  Otherwise,
    // a prefix of the relative path could be given, as in "shader_sub_pkg/AShader.glsl"
    public static final String VERTEX_SHADER = "VolumeBrickVtx.glsl";
    public static final String FRAGMENT_SHADER = "VolumeBrickFrg.glsl";

    private int previousShader = 0;

    private TextureMediator signalTextureMediator;
    private TextureMediator maskTextureMediator;
    private TextureMediator colorMapTextureMediator;

    private int vertexAttribLoc = -1;
    private int texCoordAttribLoc = -1;

    private boolean volumeMaskApplied = false;
    private boolean whiteBackground = false;
    
    private float gammaAdjustment = 1.0f;
    private float cropOutLevel = VolumeModel.DEFAULT_CROPOUT;
    private CropCoordSet cropCoordSet = CropCoordSet.getDefaultCropCoordSet();

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
        pushBackgroundColorUniform( gl, shaderProgram );
        pushCropUniforms( gl, shaderProgram );

        setTextureUniforms( gl );
        vertexAttribLoc = gl.glGetAttribLocation( shaderProgram, "vertexAttribute" );
        texCoordAttribLoc = gl.glGetAttribLocation( shaderProgram, "texCoordAttribute" );
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
    
    /** Signals to the shader, that it must act appropriately for white surrounding, rather than black. */
    public void setWhiteBackground(boolean whiteBackground) {
        this.whiteBackground = whiteBackground;
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
    public void setCropCoords( CropCoordSet cropCoordSet ) {
        if ( cropCoordSet.getAcceptedCoordinates().size() > 0 ) {
            for ( float[] cropCoords: cropCoordSet.getAcceptedCoordinates() ) {
                if ( cropCoords.length < 6 ) {
                    throw new IllegalArgumentException("Crop coords need a start and end in three dimensions.");
                }
            }
        }
        this.cropCoordSet = cropCoordSet;
    }

    public void unload(GL2 gl) {
        gl.glUseProgram(previousShader);
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

    private void setTextureUniforms(GL2 gl) {
        setTextureUniform(gl, MultiTexVolumeBrickShader.SIGNAL_TEXTURE_NAME, signalTextureMediator);
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
        gl.glUniform1f(gammaAdjustmentLoc, gammaAdjustment * VolumeModel.STANDARDIZED_GAMMA_MULTIPLIER);
    }
    
    private void pushBackgroundColorUniform( GL2 gl, int shaderProgram ) {
        int whiteBackgroundLoc = gl.glGetUniformLocation(shaderProgram, "whiteBackground");
        if ( whiteBackgroundLoc == -1 ) {
            throw new RuntimeException( "Failed to find white background flag location." );
        }
        gl.glUniform1i( whiteBackgroundLoc, whiteBackground ? 1 : 0 );
    }

    /** Upload all cropping starts/ends to GPU. */
    private void pushCropUniforms( GL2 gl, int shaderProgram ) {
        if ( cropCoordSet != null  &&  cropCoordSet.getCurrentCoordinates() != null ) {
            float[] cropCoords = cropCoordSet.getCurrentCoordinates();
            if ( cropCoords[ 0 ] > -1 ) {
                for ( int i = 0; i < 6; i++ ) {
                    // Example: startCropX is for item i == 0.
                    // Example: endCropZ is for item i == 5.
                    String cropUniformName = decodeCropUniformName(i);
                    int adjustmentLoc = testUniformLoc(gl, shaderProgram, cropUniformName);
                    //System.out.println(
                    //        "Have adjustment location of " + adjustmentLoc + " for " + cropUniformName +
                    //                ", and setting it to " + cropCoordsCollection[ i ]
                    //);
                    gl.glUniform1f(
                            adjustmentLoc,
                            cropCoords[i]
                    );
                    int setUniformError = gl.glGetError();
                    if ( setUniformError != 0 ) {
                        throw new RuntimeException( "Failed to set uniform for " + cropUniformName + ".  Error is " + setUniformError );
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
        else {
            // Send "unused" values for all crop uniforms.
            for ( int i = 0; i < 6; i++ ) {
                String cropUniformName = decodeCropUniformName(i);
                int adjustmentLoc = testUniformLoc(gl, shaderProgram, cropUniformName);
                gl.glUniform1f(
                        adjustmentLoc,
                        -1.0f
                );

                int setUniformError = gl.glGetError();
                if ( setUniformError != 0 ) {
                    throw new RuntimeException( "Failed to clear uniform for " + cropUniformName + ".  Error is " + setUniformError );
                }
            }
        }
    }

    private int testUniformLoc(GL2 gl, int shaderProgram, String cropUniformName) {
        int adjustmentLoc = gl.glGetUniformLocation(shaderProgram, cropUniformName);
        if ( adjustmentLoc == -1 ) {
            throw new RuntimeException( "Failed to find uniform location for " + cropUniformName );
        }
        return adjustmentLoc;
    }

    private String decodeCropUniformName(int i) {
        String startEnd = i % 2 == 0 ? "start" : "end";
        String xyz = "XYZ".substring( i/2, i/2 + 1 );
        return String.format("%sCrop%s", startEnd, xyz);
    }

}
