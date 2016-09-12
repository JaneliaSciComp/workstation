package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import java.util.Arrays;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.alignment_board_viewer.shader.MultiTexVolumeBrickShader;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.buffering.VtxCoordBufMgr;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import javax.swing.*;

/**
 * Class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public class MultiTexVolumeBrick implements VolumeBrickI
{
    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    private TextureMediator signalTextureMediator;
    private TextureMediator maskTextureMediator;
    private TextureMediator colorMapTextureMediator;
    private final List<TextureMediator> textureMediators = new ArrayList<>();

    // Vary these parameters to taste
	// Rendering variables
	private final RenderMethod renderMethod = 
		// RenderMethod.ALPHA_BLENDING;
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).

    private int[] textureIds;

    /**
     * Size of our opengl texture, which might be padded with extra voxels
     * to reach a multiple of 8
     */
    // OpenGL state
    private final int[] signalTextureVoxels = {8,8,8};
	private final IntBuffer signalData = Buffers.newDirectIntBuffer(signalTextureVoxels[0]* signalTextureVoxels[1]* signalTextureVoxels[2]);
    private boolean bSignalTextureNeedsUpload = false;
    private boolean bMaskTextureNeedsUpload = false;
    private boolean bColorMapTextureNeedsUpload = false;
    private boolean bBuffersNeedUpload = true;

    private final MultiTexVolumeBrickShader volumeBrickShader = new MultiTexVolumeBrickShader();

    private boolean bIsInitialized;
    @SuppressWarnings("ALL")
    private final boolean bUseSyntheticData = false;

    private final VtxCoordBufMgr bufferManager;
    private VolumeModel volumeModel;

    private static final Logger logger = LoggerFactory.getLogger( MultiTexVolumeBrick.class );

    static {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
// 3rd
//                    GLProfile profile = GLProfile.get(GLProfile.GL3);
//                    GLCapabilities glCaps = new GLCapabilities(profile);
//                    glCaps.setPBuffer(true);
//                    GLPBuffer pbuffer =
//                    GLDrawableFactory.getFactory(profile).createGLPbuffer(glCaps,
//                        new DefaultGLCapabilitiesChooser(),
//                        1, 1, null);
//                    GLCanvas canvas = new GLCanvas(glCaps, new DefaultGLCapabilitiesChooser(),
//                            PanelGL.pbuffer.getContext(), null);

// 2nd
//                    GLProfile profile = GLProfile.get(GLProfile.GL2);
//                    GLCapabilities capabilities = new GLCapabilities(profile);
//                    capabilities.setGLProfile( profile );
//                    capabilities.setPBuffer(true);
//                    GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
//                    GLContext ctx = factory.createExternalGLContext();  // Dies here.
//                    GL2 gl = ctx.getGL().getGL2();

// 1st
//                    new JOCLSimpleGL2(capabilities);
                }
            });
        } catch ( Throwable th ) {
            logger.error( "No GL2 profile available" );
        }

    }

    public MultiTexVolumeBrick(VolumeModel volumeModel) {
        bufferManager = new VtxCoordBufMgr();
        setVolumeModel( volumeModel );
    }

    //---------------------------------------IMPLEMEMNTS GLActor
    @Override
    @SuppressWarnings("CallToThreadDumpStack")
	public void init(GLAutoDrawable glDrawable) {
        // Avoid carrying out any operations if there is no real data.
        if ( signalTextureMediator == null  &&  maskTextureMediator == null ) {
            logger.warn("No textures for volume brick.");
            return;
        }

        GL2 gl = glDrawable.getGL().getGL2();
        if (reportError( gl, "mult-tex init upon entry" )) {
            return;
        }

        // Initialization of mediators has failed.  Await next cycle.
        if (! initMediators( gl )) {
            return;
        }
        if (bUseSyntheticData) {
            createSyntheticData();
        }

        gl.glEnable(GL2.GL_TEXTURE_3D);
        if (bSignalTextureNeedsUpload) {
            uploadSignalTexture(gl);
            if (reportError( gl, "init mux brick - upload signal" )) {
                return;
            }
        }
		if (bUseShader) {
            if ( maskTextureMediator != null  &&  bMaskTextureNeedsUpload ) {
                uploadMaskingTexture(gl);
                if (reportError( gl, "init mux brick - upload mask" )) {
                    return;
                }
            }

            if ( colorMapTextureMediator != null  &&  bColorMapTextureNeedsUpload ) {
                uploadColorMapTexture(gl);
                if (reportError( gl, "init mux brick - upload color" )) {
                    return;
                }
            }

            try {
                volumeBrickShader.setTextureMediators(
                        signalTextureMediator, maskTextureMediator, colorMapTextureMediator
                );
                volumeBrickShader.init(gl);
                if (reportError( gl, "init mux brick - shader" )) {
                    return;
                }
            } catch ( ShaderCreationException ex ) {
                ex.printStackTrace();
                bUseShader = false;
            }
        }
        if (bBuffersNeedUpload) {
            try {
                // This vertex-build must be done here, now that all information is set.
                bufferManager.buildBuffers();
                reportError( gl, "building buffers" );

                if (! bufferManager.enableBuffers( gl )) {
                    logger.error("Failed to enable buffers");
                }
                else {
                    bufferManager.dropBuffers();

                    bBuffersNeedUpload = false;
                    reportError(gl, "init mux brick - buffers");
                }
            } catch ( Exception ex ) {
                FrameworkImplProvider.handleException( ex );
            }
        }
		// tidy up
        gl.glDisable(GL2.GL_TEXTURE_3D);
        reportError( gl, "mult-tex init exit" );
		bIsInitialized = true;
	}

    @Override
	public void display(GLAutoDrawable glDrawable) {
        // Avoid carrying out operations if there is no data.
        if ( maskTextureMediator == null  &&  signalTextureMediator == null ) {
            logger.warn( "No textures for volume brick." );
            return;
        }

        GL2 gl = glDrawable.getGL().getGL2();
        reportError( gl, "Upon entry to mux-tex display." );
		if (! bIsInitialized)
			init(glDrawable);
		if (bSignalTextureNeedsUpload)
			uploadSignalTexture(gl);
        if (maskTextureMediator != null  &&  bMaskTextureNeedsUpload)
            uploadMaskingTexture(gl);
        if (colorMapTextureMediator != null  &&  bColorMapTextureNeedsUpload)
            uploadColorMapTexture(gl);

        reportError( gl, "display mux brick - flagged resource init" );

		// debugging objects showing useful boundaries of what we want to render
		//gl.glColor3d(1,1,1);
		// displayVoxelCenterBox(gl);
		//gl.glColor3d(1,1,0.3);
		// displayVoxelCornerBox(gl);
		// a stack of transparent slices looks like a volume
		gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL2.GL_LIGHTING);

        reportError( gl, "display mux brick - attribs" );

        // set blending to enable transparent voxels
        gl.glEnable(GL2.GL_BLEND);
        if (renderMethod == RenderMethod.ALPHA_BLENDING) {
            gl.glBlendEquation(GL2.GL_FUNC_ADD);
            // Weight source by GL_ONE because we are using premultiplied alpha.
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
            reportError( gl, "display mux brick - alpha" );
        }
        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
            // gl.glBlendFunc(GL2.GL_ONE_MINUS_DST_COLOR, GL2.GL_ZERO); // inverted?  http://stackoverflow.com/questions/2656905/opengl-invert-framebuffer-pixels
            if ( Arrays.equals( volumeModel.getBackgroundColorFArr(), VolumeModel.DEFAULT_BACKGROUND_COLOR ) ) {
                gl.glBlendEquation(GL2.GL_MAX);
                gl.glBlendFunc( GL2.GL_ONE, GL2.GL_DST_ALPHA );
            }
            else {
//                gl.glBlendEquation(GL2.GL_FUNC_SUBTRACT);
                gl.glBlendEquation(GL2.GL_MIN);
                gl.glBlendFunc( GL2.GL_ONE, GL2.GL_DST_ALPHA );
            }
            reportError( gl, "display mux brick - max intensity" );
        }
        if (bUseShader) {
            if ( maskTextureMediator != null ) {
                volumeBrickShader.setVolumeMaskApplied();
            }
            volumeBrickShader.setGammaAdjustment( volumeModel.getGammaAdjustment() );
            volumeBrickShader.setCropOutLevel( volumeModel.getCropOutLevel() );
            volumeBrickShader.setCropCoords( volumeModel.getCropCoords() );
            volumeBrickShader.setWhiteBackground(volumeModel.isWhiteBackground());            
            volumeBrickShader.load(gl);
            int vertexAttribLoc = volumeBrickShader.getVertexAttribLoc();
            int texCoordAttribLoc = volumeBrickShader.getTexCoordAttribLoc();
            bufferManager.setCoordAttributeLocations( vertexAttribLoc, texCoordAttribLoc );

            reportError( gl, "display mux brick - flagged shader init" );
        }

        displayVolumeSlices(gl);
		if (bUseShader) {
            volumeBrickShader.unload(gl);
            reportError( gl, "display mux brick - unload shader" );
        }
        gl.glDisable(GL2.GL_BLEND);
        reportError(gl, "Volume Brick, end of display.");

    }

    @Override
	public void dispose(GLAutoDrawable glDrawable) {
        // Were the volume model listener removed at this point, it would leave NO listener available to it,
        // and it would never subsequently be restored.
        GL2 gl = glDrawable.getGL().getGL2();
        if ( textureIds != null )
		    gl.glDeleteTextures(textureIds.length, textureIds, 0);
		// Retarded JOGL GLJPanel frequently reallocates the GL context
		// during resize. So we need to be ready to reinitialize everything.
        textureIds = null;
		bSignalTextureNeedsUpload = true;
        bMaskTextureNeedsUpload = true;
        bColorMapTextureNeedsUpload = true;
        bIsInitialized = false;

        bufferManager.releaseBuffers(gl);
        bBuffersNeedUpload = true;
	}

    @Override
	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d result = new BoundingBox3d();
		Vec3 half = new Vec3(0,0,0);
		for (int i = 0; i < 3; ++i)
			half.set(i, 0.5 * signalTextureMediator.getVolumeMicrometers()[i]);
		result.include(half.minus());
		result.include(half);
		return result;
	}
    //---------------------------------------END IMPLEMENTATION GLActor

    //---------------------------------------IMPLEMENT VolumeDataAcceptor
    @Override
    public void setPrimaryTextureData(TextureDataI textureData) {
        if ( signalTextureMediator == null ) {
            signalTextureMediator = new TextureMediator();
            textureMediators.add( signalTextureMediator );
        }
        signalTextureMediator.setTextureData( textureData );
        bSignalTextureNeedsUpload = true;
        bufferManager.setTextureMediator( signalTextureMediator );
    }
    
    /**
     * This one merely sets the primary one.  Do not call this for
     * mask or color map.
     * 
     * @See #setPrimaryTextureData
     * @param textureData 
     */
    @Override
    public void addTextureData(TextureDataI textureData) {
        setPrimaryTextureData(textureData);
    }

    //----------------------------------------END IMPLEMENTATION VolumeDataAcceptor

    public void setVoxelColor(int x, int y, int z, int color) {
        int sx = signalTextureVoxels[0];
        int sy = signalTextureVoxels[1];
        signalData.put(z * sx * sy + y * sx + x, color);
    }

    public void setMaskTextureData( TextureDataI textureData ) {
        if ( maskTextureMediator == null ) {
            maskTextureMediator = new TextureMediator();
            textureMediators.add( maskTextureMediator );
        }
        maskTextureMediator.setTextureData(textureData);
        bMaskTextureNeedsUpload = true;
        bColorMapTextureNeedsUpload = true;  // New mask implies new map.
    }

    /** Use this to feed color mapping between neuron number in mask, and color desired. */
    public void setColorMapTextureData( TextureDataI textureData ) {
        if ( colorMapTextureMediator == null ) {
            colorMapTextureMediator = new TextureMediator();
            textureMediators.add( colorMapTextureMediator );
        }
        colorMapTextureMediator.setTextureData( textureData );
        bMaskTextureNeedsUpload = true;      // Given color map is new, mask must also need re-push.
        bColorMapTextureNeedsUpload = true;
    }

    /** Call this when the brick is to be re-shown after an absense. */
    public void refresh() {
        bSignalTextureNeedsUpload = true;
    }

    /** Calling this causes the special mapping texture to be pushed again at display or init time. */
    public void refreshColorMapping() {
        bColorMapTextureNeedsUpload = true;
    }

    /**
     * Volume rendering by painting a series of transparent,
     * one-voxel-thick slices, in back-to-front painter's algorithm
     * order.
     * @param gl wrapper object for OpenGL context.
     */
    private void displayVolumeSlices(GL2 gl) {

        // Get the view vector, so we can choose the slice direction,
        // along one of the three principal axes(X,Y,Z), and either forward
        // or backward.
        // "InGround" means in the WORLD object reference frame.
        // (the view vector in the EYE reference frame is always [0,0,-1])
        Vec3 vv = volumeModel.getCamera3d().getRotation().times(new Vec3(0,0,1));

        // Compute the principal axis of the view direction; that's the direction we will slice along.
        CoordinateAxis a1 = CoordinateAxis.X; // First guess principal axis is X.  Who knows?
        if ( Math.abs(vv.y()) > Math.abs(vv.get(a1.index())) )
            a1 = CoordinateAxis.Y; // OK, maybe Y axis is principal
        if ( Math.abs(vv.z()) > Math.abs(vv.get(a1.index())) )
            a1 = CoordinateAxis.Z; // Alright, it's definitely Z principal.

        if (setupSignalTexture(gl) &&
            setupMaskingTexture(gl) &&
            setupColorMapTexture(gl)) {
            
            // If principal axis points away from viewer, draw slices front to back,
            // instead of back to front.
            double direction = 1.0; // points away from viewer, render back to front, n to 0
            if (vv.get(a1.index()) < 0.0) {
                direction = -1.0; // points toward, front to back, 0 to n
            }
            bufferManager.draw(gl, a1, direction);
            reportError(gl, "Volume Brick, after draw.");            
        }
        else {
            logger.error("Volume Brick, display vol slices - failed setup tex's" );
            return;
        }


    }

    /** This is a constructor-helper.  It has the listener setup required to properly use the volume model. */
    private void setVolumeModel( VolumeModel volumeModel ) {
        this.volumeModel = volumeModel;
        VolumeModel.UpdateListener updateVolumeListener = new VolumeModel.UpdateListener() {
            @Override
            public void updateVolume() {
                refresh();
            }

            @Override
            public void updateRendering() {
                refreshColorMapping();
            }
        };
        volumeModel.addUpdateListener(updateVolumeListener);
    }

    private boolean initMediators( GL2 gl ) {
        textureIds = TextureMediator.genTextureIds( gl, textureMediators.size() );
        if (reportError(gl, "Generating tex ids")) {
            return false;
        }
        if ( signalTextureMediator != null ) {
            signalTextureMediator.init( textureIds[ 0 ], TextureMediator.SIGNAL_TEXTURE_OFFSET );
        }
        if ( maskTextureMediator != null  &&  textureIds.length >= 2 ) {
            maskTextureMediator.init( textureIds[ 1 ], TextureMediator.MASK_TEXTURE_OFFSET );
        }
        if ( colorMapTextureMediator != null  &&  textureIds.length >= 3 ) {
            colorMapTextureMediator.init( textureIds[ 2 ], TextureMediator.COLOR_MAP_TEXTURE_OFFSET );
        }
        return true;
    }

    private void createSyntheticData() {
        // Clear texture data
        signalData.rewind();
        while (signalData.hasRemaining()) {
            signalData.put(0x00000000);
        }
        // Create simple synthetic image for testing.
        // 0xAARRGGBB
			/*
			setVoxelColor(0,0,0, 0x11ff0000); // ghostly red
			setVoxelColor(0,0,1, 0x4400ff00);
			setVoxelColor(0,0,2, 0x770000ff);
			setVoxelColor(0,1,0, 0xaaff0000);
			setVoxelColor(0,1,1, 0xdd00ff00);
			setVoxelColor(0,1,2, 0xff0000ff); // opaque blue
			 */
        // TESTING PREMULTIPLIED ALPHA
        setVoxelColor(0,0,0, 0x11110000); // ghostly red
        setVoxelColor(0,0,1, 0x44004400);
        setVoxelColor(0,0,2, 0x77000077);
        setVoxelColor(0,1,0, 0xaaaa0000);
        setVoxelColor(0,1,1, 0xdd00dd00);
        setVoxelColor(0, 1, 2, 0xff0000ff); // opaque blue
        bSignalTextureNeedsUpload = true;
    }

    /** Uploading the signal texture. */
    private void uploadSignalTexture(GL2 gl) {
        if ( signalTextureMediator != null ) {
            signalTextureMediator.deleteTexture( gl );
            if (! signalTextureMediator.uploadTexture( gl )) {
                return;
            }        
        }
        bSignalTextureNeedsUpload = false;
    }

    /** Upload the masking texture to open GL "state". */
    private void uploadMaskingTexture(GL2 gl) {
        if ( maskTextureMediator != null ) {
            maskTextureMediator.deleteTexture( gl );
            if (! maskTextureMediator.uploadTexture( gl )) {
                return;
            }
        }
        bMaskTextureNeedsUpload = false;
    }

    private void uploadColorMapTexture(GL2 gl) {
        if ( colorMapTextureMediator != null ) {
            colorMapTextureMediator.deleteTexture( gl );
            if (! colorMapTextureMediator.uploadTexture( gl )) {
                return;
            }
        }
        bColorMapTextureNeedsUpload = false;
    }

    private boolean setupMaskingTexture(GL2 gl) {
        boolean rtnVal = true;
        if ( maskTextureMediator != null ) {
            rtnVal = maskTextureMediator.setupTexture( gl );
        }
        return rtnVal;
    }

    private boolean setupSignalTexture(GL2 gl) {
        boolean rtnVal = true;
        if ( signalTextureMediator != null ) {
            rtnVal = signalTextureMediator.setupTexture( gl );
        }
        return rtnVal;
    }

    private boolean setupColorMapTexture(GL2 gl) {
        boolean rtnVal = true;
        if ( colorMapTextureMediator != null ) {
            rtnVal = colorMapTextureMediator.setupTexture( gl );
        }
        return rtnVal;
    }

    /** DEBUG code to help understand what is happening with vtx or tex points. */
    @SuppressWarnings("unused")
    private void printPoints(String type, double[] p1, double[] p2, double[] p3, double[] p4) {
        printPoint(type, p1);
        printPoint(type, p2);
        printPoint(type, p3);
        printPoint(type, p4);
    }

    private void printPoint(String type, double[] p) {
        logger.info(String.format(
                "%s [%s, %s, %s]", type, Double.toString(p[0]), Double.toString(p[1]), Double.toString(p[2]))
        );
    }

}
