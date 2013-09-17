package org.janelia.it.FlyWorkstation.gui.static_view;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.buffering.VtxCoordBufMgr;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.static_view.shader.RGBExcludableShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import java.util.ArrayList;
import java.util.List;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public class RGBExcludableVolumeBrick implements VolumeBrickI
{
    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    private TextureMediator signalTextureMediator;
    private List<TextureMediator> textureMediators = new ArrayList<TextureMediator>();

    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod =
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).

    private int[] textureIds;

    /**
     * Size of our opengl texture, which might be padded with extra voxels
     * to reach a multiple of 8
     */
    // OpenGL state
    private boolean bSignalTextureNeedsUpload = false;
    private boolean bBuffersNeedUpload = true;

    private RGBExcludableShader shader = new RGBExcludableShader();

    private boolean bIsInitialized;

    private VtxCoordBufMgr bufferManager;
    private VolumeModel volumeModel;

    private static Logger logger = LoggerFactory.getLogger( RGBExcludableVolumeBrick.class );

    static {
        try {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            final GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setGLProfile( profile );
            // KEEPING this for use of GL3 under MAC.  So far, unneeded, and not debugged.
            //        SwingUtilities.invokeLater(new Runnable() {
            //            public void run() {
            //                new JOCLSimpleGL3(capabilities);
            //            }
            //        });
        } catch ( Throwable th ) {
            logger.error( "No GL3 profile available" );
        }

    }

    public RGBExcludableVolumeBrick(VolumeModel volumeModel) {
        bufferManager = new VtxCoordBufMgr( true );
        setVolumeModel( volumeModel );
    }

    @Override
	public void init(GLAutoDrawable glDrawable) {

        // Avoid carrying out any operations if there is no real data.
        if ( signalTextureMediator == null ) {
            logger.warn("No textures for volume brick.");
            return;
        }

        GL2 gl = glDrawable.getGL().getGL2();
        initMediators( gl );

		gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);

        if (bSignalTextureNeedsUpload) {
            uploadSignalTexture(gl);
        }
		if (bUseShader) {
            try {
                shader.setSignalTextureMediator(signalTextureMediator);
                shader.init(gl);
            } catch ( Exception ex ) {
                ex.printStackTrace();
                bUseShader = false;
            }
        }
        if (bBuffersNeedUpload) {
            try {
                // This vertex-build must be done here, now that all information is set.
                bufferManager.buildBuffers();
                reportError( gl, "building buffers" );

                bufferManager.enableBuffers( gl );
                reportError( gl, "uploading buffers" );
                bufferManager.dropBuffers();

                bBuffersNeedUpload = false;
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }
		// tidy up
		gl.glPopAttrib();
		bIsInitialized = true;
	}

    @Override
	public void display(GLAutoDrawable glDrawable) {
        // Avoid carrying out operations if there is no data.
        if ( signalTextureMediator == null ) {
            logger.warn( "No texture for volume brick." );
            return;
        }

		if (! bIsInitialized)
			init(glDrawable);
        GL2 gl = glDrawable.getGL().getGL2();
		if (bSignalTextureNeedsUpload)
			uploadSignalTexture(gl);

		// debugging objects showing useful boundaries of what we want to render
		//gl.glColor3d(1,1,1);
		// displayVoxelCenterBox(gl);
		//gl.glColor3d(1,1,0.3);
		// displayVoxelCornerBox(gl);
		// a stack of transparent slices looks like a volume
		gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);
		gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_3D);

        // set blending to enable transparent voxels
        if (renderMethod == RenderMethod.ALPHA_BLENDING) {
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendEquation(GL2.GL_FUNC_ADD);
            // Weight source by GL_ONE because we are using premultiplied alpha.
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }
        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
    	    gl.glEnable(GL2.GL_BLEND);
            gl.glBlendEquation(GL2.GL_MAX);
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_DST_ALPHA);
        }
        if (bUseShader) {
            shader.setColorMask(volumeModel.getColorMask());
            shader.load(gl);
        }

        displayVolumeSlices(gl);
		if (bUseShader) {
            shader.unload(gl);
        }
		gl.glPopAttrib();
	}

	/**
	 * Volume rendering by painting a series of transparent,
	 * one-voxel-thick slices, in back-to-front painter's algorithm
	 * order.
	 * @param gl wrapper object for OpenGL context.
	 */
	public void displayVolumeSlices(GL2 gl) {

		// Get the view vector, so we can choose the slice direction,
		// along one of the three principal axes(X,Y,Z), and either forward
		// or backward.
		// "InGround" means in the WORLD object reference frame.
		// (the view vector in the EYE reference frame is always [0,0,-1])
		Vec3 viewVectorInGround = volumeModel.getCamera3d().getRotation().times(new Vec3(0,0,1));

		// Compute the principal axis of the view direction; that's the direction we will slice along.
		CoordinateAxis a1 = CoordinateAxis.X; // First guess principal axis is X.  Who knows?
		Vec3 vv = viewVectorInGround;
		if ( Math.abs(vv.y()) > Math.abs(vv.get(a1.index())) )
			a1 = CoordinateAxis.Y; // OK, maybe Y axis is principal
		if ( Math.abs(vv.z()) > Math.abs(vv.get(a1.index())) )
			a1 = CoordinateAxis.Z; // Alright, it's definitely Z principal.

        setupSignalTexture(gl);

		// If principal axis points away from viewer, draw slices front to back,
		// instead of back to front.
		double direction = 1.0; // points away from viewer, render back to front, n to 0
		if (vv.get(a1.index()) < 0.0) 
			direction = -1.0; // points toward, front to back, 0 to n
        bufferManager.draw( gl, a1, direction );
        reportError(gl, "Volume Brick, after draw.");

    }

    @Override
	public void dispose(GLAutoDrawable glDrawable) {
        // Were the volume model listener removed at this point, it would leave NO listener available to it,
        // and it would never subsequently be restored.
        GL2 gl = glDrawable.getGL().getGL2();
		gl.glDeleteTextures(textureIds.length, textureIds, 0);
		// Retarded JOGL GLJPanel frequently reallocates the GL context
		// during resize. So we need to be ready to reinitialize everything.
        textureIds = null;
		bSignalTextureNeedsUpload = true;
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

    //---------------------------------IMPLEMENT VolumeDataAcceptor
    @Override
    public void setTextureData(TextureDataI textureData) {
        if ( signalTextureMediator == null ) {
            signalTextureMediator = new TextureMediator();
            textureMediators.add( signalTextureMediator );
        }
        signalTextureMediator.setTextureData( textureData );
        bSignalTextureNeedsUpload = true;
        bufferManager.setTextureMediator( signalTextureMediator );
    }

    //---------------------------------END: IMPLEMENT VolumeDataAcceptor

    /** Call this when the brick is to be re-shown after an absense. */
    public void refresh() {
        bSignalTextureNeedsUpload = true;
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
            }
        };
        volumeModel.addUpdateListener(updateVolumeListener);
    }

    private void initMediators( GL2 gl ) {
        textureIds = TextureMediator.genTextureIds( gl, textureMediators.size() );
        if ( signalTextureMediator != null ) {
            signalTextureMediator.init( textureIds[ 0 ], TextureMediator.SIGNAL_TEXTURE_OFFSET );
        }
    }

    /** Uploading the signal texture. */
    private void uploadSignalTexture(GL2 gl) {
        if ( signalTextureMediator != null ) {
            signalTextureMediator.deleteTexture( gl );
            signalTextureMediator.uploadTexture( gl );
        }
        bSignalTextureNeedsUpload = false;
    }

    private void setupSignalTexture(GL2 gl) {
        if ( signalTextureMediator != null ) {
            signalTextureMediator.setupTexture( gl );
        }
    }

    private void reportError(GL2 gl, String source) {
        int errNum = gl.glGetError();
        if ( errNum > 0 ) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
        }
    }

}
