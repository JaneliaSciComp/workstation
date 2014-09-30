package org.janelia.it.workstation.gui.static_view;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.buffering.VtxCoordBufMgr;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.workstation.gui.viewer3d.shader.SignalShader;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public abstract class AbstractVolumeBrick implements VolumeBrickI
{

    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    private TextureMediator signalTextureMediator;
    protected List<TextureMediator> textureMediators = new ArrayList<>();

    private SignalShader shader;
    
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

    //private RGBExcludableShader shader = new RGBExcludableShader();

    private boolean bIsInitialized;

    private VtxCoordBufMgr bufferManager;
    private VolumeModel volumeModel;

    private static Logger logger = LoggerFactory.getLogger( AbstractVolumeBrick.class );

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

    public AbstractVolumeBrick(VolumeModel volumeModel) {
        bufferManager = new VtxCoordBufMgr();
        setVolumeModel( volumeModel );
    }

    /**
     * @return the shader
     */
    public SignalShader getShader() {
        return shader;
    }

    /**
     * @param shader the shader to set
     */
    public void setShader(SignalShader shader) {
        this.shader = shader;
    }

    @Override
	public void init(GLAutoDrawable glDrawable) {

        // Avoid carrying out any operations if there is no real data.
        if ( getSignalTextureMediator() == null ) {
            logger.warn("No textures for volume brick.");
            return;
        }

        GL2 gl = glDrawable.getGL().getGL2();
        initMediators( gl );

		//gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);

        if (bSignalTextureNeedsUpload) {
            uploadSignalTexture(gl);
        }
		if (bUseShader) {
            try {
                getShader().setSignalTextureMediator(getSignalTextureMediator());
                getShader().init(gl);
            } catch ( Exception ex ) {
                ex.printStackTrace();
                bUseShader = false;
            }
        }
        if (bBuffersNeedUpload) {
            try {
                // This vertex-build must be done here, now that all information is set.
                getBufferManager().buildBuffers();
                reportError( gl, "building buffers" );

                getBufferManager().enableBuffers( gl );
                reportError( gl, "uploading buffers" );
                getBufferManager().dropBuffers();

                bBuffersNeedUpload = false;
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }
		// tidy up
		//gl.glPopAttrib();
		bIsInitialized = true;
	}

	/**
	 * Volume rendering by painting a series of transparent,
	 * one-voxel-thick slices, in back-to-front painter's algorithm
	 * order.
	 * @param gl wrapper object for OpenGL context.
	 */
	public void displayVolumeSlices(GL2 gl) {

        reportError(gl, "Display Volume Slices, on entry.");
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
        getBufferManager().draw( gl, a1, direction );
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

        getBufferManager().releaseBuffers(gl);
        bBuffersNeedUpload = true;
	}

    @Override
	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d result = new BoundingBox3d();
		Vec3 half = new Vec3(0,0,0);
		for (int i = 0; i < 3; ++i)
			half.set(i, 0.5 * getSignalTextureMediator().getVolumeMicrometers()[i]);
		result.include(half.minus());
		result.include(half);
		return result;
	}

    /**
     * @return the signalTextureMediator
     */
    public TextureMediator getSignalTextureMediator() {
        return signalTextureMediator;
    }

    /**
     * @param signalTextureMediator the signalTextureMediator to set
     */
    public void setSignalTextureMediator(TextureMediator signalTextureMediator) {
        this.signalTextureMediator = signalTextureMediator;
    }

    /**
     * @return the bufferManager
     */
    public VtxCoordBufMgr getBufferManager() {
        return bufferManager;
    }
    
    /**
     * @return the volume model.
     */
    public VolumeModel getVolumeModel() {
        return volumeModel;
    }

    //---------------------------------IMPLEMENT VolumeDataAcceptor
    @Override
    public void setTextureData(TextureDataI textureData) {
        if ( getSignalTextureMediator() == null ) {
            setSignalTextureMediator(new TextureMediator());
            textureMediators.add( getSignalTextureMediator());
        }
        getSignalTextureMediator().setTextureData( textureData );
        bSignalTextureNeedsUpload = true;
        getBufferManager().setTextureMediator( getSignalTextureMediator());
    }

    //---------------------------------END: IMPLEMENT VolumeDataAcceptor

    /** Call this when the brick is to be re-shown after an absence. */
    public void refresh() {
        bSignalTextureNeedsUpload = true;
    }

    /** This is a constructor-helper.  It has the listener setup required to properly use the volume model. */
    protected void setVolumeModel( VolumeModel volumeModel ) {
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

    protected void initMediators( GL2 gl ) {
        textureIds = TextureMediator.genTextureIds( gl, textureMediators.size() );
        if ( getSignalTextureMediator() != null ) {
            getSignalTextureMediator().init( textureIds[ 0 ], TextureMediator.SIGNAL_TEXTURE_OFFSET );
        }
    }

    /** Uploading the signal texture. */
    protected void uploadSignalTexture(GL2 gl) {
        if ( getSignalTextureMediator() != null ) {
            getSignalTextureMediator().deleteTexture( gl );
            getSignalTextureMediator().uploadTexture( gl );
        }
        bSignalTextureNeedsUpload = false;
    }

    protected void setupSignalTexture(GL2 gl) {
        if ( getSignalTextureMediator() != null ) {
            getSignalTextureMediator().setupTexture( gl );
        }
    }

    protected void reportError(GL2 gl, String source) {
        int errNum = gl.glGetError();
        if ( errNum > 0 ) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
        }
    }

}
