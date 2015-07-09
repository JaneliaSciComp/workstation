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
import java.util.Collection;
import java.util.Iterator;
import org.janelia.it.workstation.gui.viewer3d.DirectionalAxis;
import org.janelia.it.workstation.gui.viewer3d.shader.TexturedShader;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public abstract class AbstractVolumeBrick implements VolumeBrickI
{

    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    private TextureMediator primaryTextureMediator;
    protected Collection<TextureMediator> textureMediators = new ArrayList<>();

    private TexturedShader shader;
    
    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod =
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).

    private int[] textureIds;

    // OpenGL state
    protected boolean bTexturesNeedUploaded = false;
    private boolean bBuffersNeedUpload = true;

    //private RGBExcludableShader shader = new RGBExcludableShader();

    protected boolean bIsInitialized;

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
    public TexturedShader getShader() {
        return shader;
    }

    /**
     * @param shader the shader to set
     */
    public void setShader(TexturedShader shader) {
        this.shader = shader;
    }

    @Override
	public void init(GLAutoDrawable glDrawable) {

        // Avoid carrying out any operations if there is no real data.
        if ( ! hasTextures() ) {
            logger.warn("No textures for volume brick.");
            return;
        }

        GL2 gl = glDrawable.getGL().getGL2();
        initMediators( gl );

		//gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);

        if (bTexturesNeedUploaded) {            
            uploadAllTextures(gl);
            if (reportError(gl, "AVB, after uploading all textures.")) {
                return;
            }
        }
		if (bUseShader) {
            try {
                getShader().addTextureMediator(getPrimaryTextureMediator(), TexturedShader.SIGNAL_TEXTURE_NAME);
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
                if (reportError( gl, "building buffers" )) {
					bTexturesNeedUploaded = true;
                    return;
                }

                if (! getBufferManager().enableBuffers( gl )) {
					bTexturesNeedUploaded = true;
                    return;
                }
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

        if (reportError(gl, "Display Volume Slices, on entry.")) {
            return;
        }
		// Get the view vector, so we can choose the slice direction,
		// along one of the three principal axes(X,Y,Z), and either forward
		// or backward.
        DirectionalAxis directionalAxis = DirectionalAxis.findAxis( volumeModel.getCamera3d().getRotation() );

        if (! setupTextures(gl)) {
            return;
        }

		// If principal axis points away from viewer, draw slices front to back,
		// instead of back to front.
        getBufferManager().draw( gl, directionalAxis.getCoordinateAxis(), directionalAxis.getDirection() );
        if (reportError(gl, "Volume Brick, after draw.")) {
            return;
        }

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
		bTexturesNeedUploaded = true;
        bIsInitialized = false;

        getBufferManager().releaseBuffers(gl);
        bBuffersNeedUpload = true;
	}

    @Override
	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d result = new BoundingBox3d();
		Vec3 half = new Vec3(0,0,0);
		for (int i = 0; i < 3; ++i)
			half.set(i, 0.5 * getPrimaryTextureMediator().getVolumeMicrometers()[i]);
		result.include(half.minus());
		result.include(half);
		return result;
	}

    public boolean hasTextures() {
        return textureMediators != null  &&  textureMediators.size() > 0;
    }
    
    /**
     * @return the primaryTextureMediator
     */
    public TextureMediator getPrimaryTextureMediator() {
        return primaryTextureMediator;
    }
    
    public Collection<TextureMediator> getTextureMediators() {
        return textureMediators;
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
    /**
     * Set the primary texture data.  Note that if a primary texture mediator
     * already exists, this texture data will be given to it.
     * 
     * @param textureData 
     */
    @Override
    public void setPrimaryTextureData(TextureDataI textureData) {
        if ( getPrimaryTextureMediator() == null ) {
            this.primaryTextureMediator = new TextureMediator();
            textureMediators.add( primaryTextureMediator );
        }
        getPrimaryTextureMediator().setTextureData( textureData );
        bTexturesNeedUploaded = true;
        getBufferManager().setTextureMediator( getPrimaryTextureMediator());
    }
    
    /**
     * Add a texture data to those available to this volume. Will not set this
     * texture data, on any existing texture mediator, but will always make
     * a new one.
     * 
     * @param textureData 
     */
    @Override
    public void addTextureData(TextureDataI textureData) {
        TextureMediator textureMediator = new TextureMediator();
        textureMediator.setTextureData( textureData );
        textureMediators.add( textureMediator );
        if ( getPrimaryTextureMediator() == null ) {
            this.primaryTextureMediator = textureMediator;
            getBufferManager().setTextureMediator( getPrimaryTextureMediator() );
        }
        bTexturesNeedUploaded = true;
    }
    
    //---------------------------------END: IMPLEMENT VolumeDataAcceptor

    /** Call this when the brick is to be re-shown after an absence. */
    public void refresh() {
        bTexturesNeedUploaded = true;
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
        Iterator<TextureMediator> iter = textureMediators.iterator();
        for ( int i = 0; i < textureMediators.size() ; i++ ) {
            TextureMediator tm = iter.next();
            tm.init( textureIds[ i ], TextureMediator.SIGNAL_TEXTURE_OFFSET+i );
        }
    }

    /** Push all textures to the GPU, which have been added to the list. */
    protected void uploadAllTextures(GL2 gl) {
        for ( TextureMediator textureMediator: textureMediators ) {
            textureMediator.deleteTexture( gl );
            textureMediator.uploadTexture( gl );
        }
        bTexturesNeedUploaded = false;
    }

    protected boolean setupTextures(GL2 gl) {
        boolean rtnVal = true;
        for ( TextureMediator textureMediator: textureMediators ) {
            if (! textureMediator.setupTexture(gl) ) {
                rtnVal = false;
                break;
            }
        }
        return rtnVal;
    }

    protected int[] getTextureIds() {
        return textureIds;
    }
}
