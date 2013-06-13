package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.VolumeBrickShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public class VolumeBrick implements GLActor, VolumeDataAcceptor
{
    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    private TextureMediator signalTextureMediator;
    private TextureMediator maskTextureMediator;
    private TextureMediator colorMapTextureMediator;
    private List<TextureMediator> textureMediators = new ArrayList<TextureMediator>();

    // Buffer objects for setting geometry on the GPU side.  Trying GL_TRIANGLE_STRIP at first.
    //   I need one per starting direction (x,y,z) times one for positive, one for negative.
    //
    private DoubleBuffer texCoordBuf[] = new DoubleBuffer[ 6 ];
    private DoubleBuffer geometryCoordBuf[] = new DoubleBuffer[ 6 ];

    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod = 
		// RenderMethod.ALPHA_BLENDING;
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).

    private int[] textureIds;

    /**
     * Size of our opengl texture, which might be padded with extra voxels
     * to reach a multiple of 8
     */
    // OpenGL state
    private int[] signalTextureVoxels = {8,8,8};
	private IntBuffer signalData = Buffers.newDirectIntBuffer(signalTextureVoxels[0]* signalTextureVoxels[1]* signalTextureVoxels[2]);
    private boolean bSignalTextureNeedsUpload = false;
    private boolean bMaskTextureNeedsUpload = false;
    private boolean bColorMapTextureNeedsUpload = false;

    private VolumeBrickShader volumeBrickShader = new VolumeBrickShader();

    private VolumeModel.UpdateListener updateVolumeListener;
    private boolean bIsInitialized;
    private boolean bUseSyntheticData = false;

    private VolumeModel volumeModel;

    private Logger logger = LoggerFactory.getLogger( VolumeBrick.class );

//    static {
//        GLProfile profile = GLProfile.get(GLProfile.GL3);
//        final GLCapabilities capabilities = new GLCapabilities(profile);
//        capabilities.setGLProfile( profile );

//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                new JOCLSimpleGL3(capabilities);
//            }
//        });
//

//    }

    VolumeBrick(VolumeModel volumeModel) {
        setVolumeModel( volumeModel );
    }

    /**
     * This should be called to filter in/out the three channels.  Set any of these to 0.0f to turn off that
     * channel.  Conversely, set any to 1.0 to turn on that channel.  It is also possible (but currently not
     * advised) to use intermediate values.  Please do not use negative values, as such will be rejected.
     */
    public void setColorMask( float red, float green, float blue ) {
        if ( red < 0.0f  ||  green < 0.0f  ||  blue < 0.0f ) {
            throw new RuntimeException( "Invalid, negative value(s) provided." );
        }
        float[] colorMask = new float[ 3 ];
        colorMask[ 0 ] = red;
        colorMask[ 1 ] = green;
        colorMask[ 2 ] = blue;

        volumeModel.setColorMask( colorMask );
    }

    public float[] getColorMask() {
        return volumeModel.getColorMask();
    }

    @Override
	public void init(GL2 gl) {
        //buildVertexBuffers();

        // Avoid carrying out any operations if there is no real data.
        if ( signalTextureMediator == null  &&  maskTextureMediator == null ) {
            logger.warn("No textures for volume brick.");
            return;
        }

        initMediators( gl );
        if (bUseSyntheticData) {
            createSyntheticData();
        }

		gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);

        if (bSignalTextureNeedsUpload) {
            uploadSignalTexture(gl);
        }
		if (bUseShader) {
            if ( maskTextureMediator != null  &&  bMaskTextureNeedsUpload ) {
                uploadMaskingTexture(gl);
            }

            if ( colorMapTextureMediator != null  &&  bColorMapTextureNeedsUpload ) {
                uploadColorMapTexture(gl);
            }

            try {
                volumeBrickShader.setTextureMediators(
                        signalTextureMediator, maskTextureMediator, colorMapTextureMediator
                );
                volumeBrickShader.init(gl);
            } catch ( Exception ex ) {
                ex.printStackTrace();
                bUseShader = false;
            }
        }
		// tidy up
		gl.glPopAttrib();
		bIsInitialized = true;
	}

    @Override
	public void display(GL2 gl) {
        // Avoid carrying out operations if there is no data.
        if ( maskTextureMediator == null  &&  signalTextureMediator == null ) {
            logger.warn( "No textures for volume brick." );
            return;
        }

		if (! bIsInitialized)
			init(gl);
		if (bSignalTextureNeedsUpload)
			uploadSignalTexture(gl);
        if (maskTextureMediator != null  &&  bMaskTextureNeedsUpload)
            uploadMaskingTexture(gl);
        if (colorMapTextureMediator != null  &&  bColorMapTextureNeedsUpload)
            uploadColorMapTexture(gl);

		// debugging objects showing useful boundaries of what we want to render
		gl.glColor3d(1,1,1);
		// displayVoxelCenterBox(gl);
		gl.glColor3d(1,1,0.3);
		// displayVoxelCornerBox(gl);
		// a stack of transparent slices looks like a volume
		gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);
		gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_3D);

        setupSignalTexture(gl);
        setupMaskingTexture(gl);
        setupColorMapTexture(gl);

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
            // gl.glBlendFunc(GL2.GL_ONE_MINUS_DST_COLOR, GL2.GL_ZERO); // inverted?  http://stackoverflow.com/questions/2656905/opengl-invert-framebuffer-pixels
        }
        if (bUseShader) {
            volumeBrickShader.setColorMask(volumeModel.getColorMask());
            if ( maskTextureMediator != null ) {
                volumeBrickShader.setVolumeMaskApplied();
            }
            volumeBrickShader.setGammaAdjustment( volumeModel.getGammaAdjustment() );
            volumeBrickShader.setCropOutLevel( volumeModel.getCropOutLevel() );
            volumeBrickShader.setCropCoords( volumeModel.getCropCoords() );
            volumeBrickShader.load(gl);
        }

        displayVolumeSlices(gl);
		if (bUseShader) {
            volumeBrickShader.unload(gl);
        }
		gl.glPopAttrib();
	}

    // Debugging object with corners at the center of
	// the corner voxels of this volume.
//	public void displayVoxelCenterBox(GL2 gl) {
//		gl.glPushMatrix();
//		double[] v = volumeMicrometers;
//		double[] pad = voxelMicrometers;
//		gl.glScaled(v[0]-pad[0], v[1]-pad[1], v[2]-pad[2]);
//		glut.glutWireCube(1);
//		gl.glPopMatrix();
//	}

	// Debugging object with corners at the outer corners of
	// the corner voxels of this volume.
//	public void displayVoxelCornerBox(GL2 gl) {
//		gl.glPushMatrix();
//		double[] v = volumeMicrometers;
//		gl.glScaled(v[0], v[1], v[2]);
//		glut.glutWireCube(1);
//		gl.glPopMatrix();
//	}

	/**
	 * Volume rendering by painting a series of transparent,
	 * one-voxel-thick slices, in back-to-front painter's algorithm
	 * order.
	 * @param gl
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
		// Choose the other two axes in right-handed convention
		CoordinateAxis a2 = a1.next();
		CoordinateAxis a3 = a2.next();
		// If principal axis points away from viewer, draw slices front to back,
		// instead of back to front.
		double direction = 1.0; // points away from viewer, render back to front, n to 0
		if (vv.get(a1.index()) < 0.0) 
			direction = -1.0; // points toward, front to back, 0 to n
		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
		// These axes might be permuted from the real world XYZ
		// Each slice cuts through an exact voxel center,
		// but slice edges extend all the way to voxel edges.
		// Compute dimensions of one x slice: y0-y1 and z0-z1
		// Pad edges of rectangles by one voxel to support oblique ray tracing past edges
		double y0 = direction * (signalTextureMediator.getVolumeMicrometers()[a2.index()] / 2.0 + signalTextureMediator.getVoxelMicrometers()[a2.index()]);
		double y1 = -y0;
		double z0 = direction * (signalTextureMediator.getVolumeMicrometers()[a3.index()] / 2.0 + signalTextureMediator.getVoxelMicrometers()[a3.index()]);
		double z1 = -z0;
		// Four points for four slice corners
		double[] p00 = {0,0,0};
		double[] p10 = {0,0,0};
		double[] p11 = {0,0,0};
		double[] p01 = {0,0,0};
		// reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
		p00[a2.index()] = p01[a2.index()] = y0;
		p10[a2.index()] = p11[a2.index()] = y1;
		p00[a3.index()] = p10[a3.index()] = z0;
		p01[a3.index()] = p11[a3.index()] = z1;
		// compute number of slices
		int sx = (int)(0.5 + signalTextureMediator.getVolumeMicrometers()[a1.index()] / signalTextureMediator.getVoxelMicrometers()[a1.index()]);
		// compute position of first slice
		double x0 = -direction * (signalTextureMediator.getVoxelMicrometers()[a1.index()] - signalTextureMediator.getVolumeMicrometers()[a1.index()]) / 2.0;
		// compute distance between slices
		double dx = -direction * signalTextureMediator.getVoxelMicrometers()[a1.index()];

        reportError(gl, "Volume Brick, before setting coords.");

        // deal out the slices, like cards from a deck
        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
		for (int xi = 0; xi < sx; ++xi) {
			// insert final coordinate into corner vectors
			double x = x0 + xi * dx;
			int a = a1.index();
			p00[a] = p01[a] = p10[a] = p11[a] = x;
			// Compute texture coordinates
			double[] t00 = signalTextureMediator.textureCoordFromVoxelCoord( p00 );
			double[] t01 = signalTextureMediator.textureCoordFromVoxelCoord( p01 );
			double[] t10 = signalTextureMediator.textureCoordFromVoxelCoord( p10 );
			double[] t11 = signalTextureMediator.textureCoordFromVoxelCoord( p11 );
			// color from black(back) to white(front) for debugging.
			// double c = xi / (double)sx;
			// gl.glColor3d(c, c, c);
			// draw the quadrilateral as a triangle strip with 4 points
            // (somehow GL_QUADS never works correctly for me)
            setTextureCoordinates(gl, t00[0], t00[1], t00[2]);
            gl.glVertex3d(p00[0], p00[1], p00[2]);
            setTextureCoordinates(gl, t10[0], t10[1], t10[2]);
            gl.glVertex3d(p10[0], p10[1], p10[2]);
            setTextureCoordinates(gl, t01[0], t01[1], t01[2]);
            gl.glVertex3d(p01[0], p01[1], p01[2]);
            setTextureCoordinates(gl, t11[0], t11[1], t11[2]);
            gl.glVertex3d(p11[0], p11[1], p11[2]);

			boolean bDebug = false;
			if (bDebug)
				printPoints(t00, t10, t01, t11);

		}
        gl.glEnd();
        reportError(gl, "Volume Brick, after setting coords.");

    }

    @Override
	public void dispose(GL2 gl) {
        // Were the volume model listener removed at this point, it would leave NO listener available to it,
        // and it would never subsequently be restored.
		gl.glDeleteTextures(1, textureIds, 0);
		// Retarded JOGL GLJPanel frequently reallocates the GL context
		// during resize. So we need to be ready to reinitialize everything.
        textureIds = null;
		bSignalTextureNeedsUpload = true;
        bMaskTextureNeedsUpload = true;
        bColorMapTextureNeedsUpload = true;
        bIsInitialized = false;
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

    /**
	 * Colors are already premultiplied by alpha.
	 * Colors are assumed to be completely saturated.
	 * So alpha component will be computed from largest RGB component.
	 * @param sx width/x dimension in voxels
	 * @param sy height/y dimension in voxels
	 * @param sz depth/z dimension in voxels
	 * @param rgbArray RGBA pixel data as integers
	 */
	public void setVolumeDataComputeAlpha(int sx, int sy, int sz, int[] rgbArray)
	{
		signalTextureVoxels = new int[]{sx, sy, sz};
		final int numVoxels = sx * sy * sz;
        signalData = Buffers.newDirectIntBuffer(numVoxels);
		for (int z_in = 0; z_in < sz; ++z_in) {
			int i_z_in = z_in * sx * sy;
			int z_out = sz - z_in - 1; // flip z
			int i_z_out = z_out * sx * sy;
			for (int y_in = 0; y_in < sy; ++y_in) {
				int i_y_in = i_z_in + y_in * sx;
				int y_out = sy - y_in - 1; // flip y
				int i_y_out = i_z_out + y_out * sx;
				for (int x_in = 0; x_in < sx; ++x_in) {
					int i_in = i_y_in + x_in;
					int x_out = x_in; // no change in x
					int i_out = i_y_out + x_out;
					// Compute alpha opacity as max of red, green, blue
					int rgb = rgbArray[i_in];
					int r = (rgb & 0x00ff0000) >>> 16;
					int g = (rgb & 0x0000ff00) >>> 8;
					int b =  rgb & 0x000000ff;
					int a = Math.max(r, Math.max(g, b));
					rgb = (rgb & 0x00ffffff) | (a << 24);
                    signalData.put(i_out, rgb);
				}
			}
		}
		/*
		for (int i = 0; i < numVoxels; ++i) {
			// Compute alpha opacity as max of red, green, blue
			int rgb = rgbArray[i];
			int r = (rgb & 0x00ff0000) >>> 16;
			int g = (rgb & 0x0000ff00) >>> 8;
			int b =  rgb & 0x000000ff;
			int a = Math.max(r, Math.max(g, b));
			rgb = (rgb & 0x00ffffff) | (a << 24);
			if (a > 0) {
				a = a*1;
			}
			data.put(i, rgb);
			// data.put(i, 0xffffffff);
		}
		*/
		bSignalTextureNeedsUpload = true;
        bMaskTextureNeedsUpload = false; //Dummy data has no masking at this time.
        bColorMapTextureNeedsUpload = false;
		bUseSyntheticData = false;
	}

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

    //---------------------------------IMPLEMENT VolumeDataAcceptor
    @Override
    public void setTextureData(TextureDataI textureData) {
        if ( signalTextureMediator == null ) {
            signalTextureMediator = new TextureMediator();
            textureMediators.add( signalTextureMediator );
        }
        signalTextureMediator.setTextureData( textureData );
        bSignalTextureNeedsUpload = true;
    }

    //---------------------------------END: IMPLEMENT VolumeDataAcceptor

    /** Call this when the brick is to be re-shown after an absense. */
    public void refresh() {
        bSignalTextureNeedsUpload = true;
    }

    /** Calling this causes the special mapping texture to be pushed again at display or init time. */
    public void refreshColorMapping() {
        bColorMapTextureNeedsUpload = true;
    }

    /** This is a constructor-helper.  It has the listener setup required to properly use the volume model. */
    private void setVolumeModel( VolumeModel volumeModel ) {
        this.volumeModel = volumeModel;
        updateVolumeListener = new VolumeModel.UpdateListener() {
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

    private void initMediators( GL2 gl ) {
        textureIds = TextureMediator.genTextureIds( gl, textureMediators.size() );
        if ( signalTextureMediator != null ) {
            signalTextureMediator.init( textureIds[ 0 ], TextureMediator.SIGNAL_TEXTURE_OFFSET );
        }
        if ( maskTextureMediator != null  &&  textureIds.length >= 2 ) {
            maskTextureMediator.init( textureIds[ 1 ], TextureMediator.MASK_TEXTURE_OFFSET );
        }
        if ( colorMapTextureMediator != null  &&  textureIds.length >= 3 ) {
            colorMapTextureMediator.init( textureIds[ 2 ], TextureMediator.COLOR_MAP_TEXTURE_OFFSET );
        }
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
        if ( signalTextureMediator != null )
            signalTextureMediator.uploadTexture( gl );
        bSignalTextureNeedsUpload = false;
    }

    /** Upload the masking texture to open GL "state". */
    private void uploadMaskingTexture(GL2 gl) {
        if ( maskTextureMediator != null )
            maskTextureMediator.uploadTexture( gl );
        bMaskTextureNeedsUpload = false;
    }

    private void uploadColorMapTexture(GL2 gl) {
        if ( colorMapTextureMediator != null )
            colorMapTextureMediator.uploadTexture( gl );
        bColorMapTextureNeedsUpload = false;
    }

    private void setTextureCoordinates( GL2 gl, double tX, double tY, double tZ ) {
        for ( TextureMediator mediator: textureMediators ) {
            mediator.setTextureCoordinates( gl, tX, tY, tZ );
        }
    }

    private void setupMaskingTexture(GL2 gl) {
        if ( maskTextureMediator != null ) {
            maskTextureMediator.setupTexture( gl );
        }
    }

    private void setupSignalTexture(GL2 gl) {
        if ( signalTextureMediator != null ) {
            signalTextureMediator.setupTexture( gl );
        }
    }

    private void setupColorMapTexture(GL2 gl) {
        if ( colorMapTextureMediator != null ) {
            colorMapTextureMediator.setupTexture( gl );
        }
    }

    /**
     * This method builds the buffers of vertices for both geometry and texture.  These are calculated similarly,
     * but with different ranges.  There are multiple such buffers of both types, and they are kept in arrays.
     * The arrays are indexed as follows:
     * 1. Offsets [ 0..2 ] are for positive direction.
     * 2. Offsets 0,3 are X; 1,4 are Y and 2,5 are Z.
     *
     * NOTE: where I am, I need to work when to upload the buffers, and how to differentiate texture vertex and
     *       geometric vertex buffer values.  The calls are likely  glVertexAttribPointer and glEnableVertexAttribArray,
     *       followed by glDrawArrays with argument of GL_TRIANGLE_STRIP.  Example seems to upload prior.
     */
    private void buildVertexBuffers() {
        /*
        // compute number of slices
		int sx = (int)(0.5 + signalTextureMediator.getVolumeMicrometers()[a1.index()] / signalTextureMediator.getVoxelMicrometers()[a1.index()]);
		// compute position of first slice
		double x0 = -direction * (signalTextureMediator.getVoxelMicrometers()[a1.index()] - signalTextureMediator.getVolumeMicrometers()[a1.index()]) / 2.0;
		// compute distance between slices
		double dx = -direction * signalTextureMediator.getVoxelMicrometers()[a1.index()];

         */
        if ( texCoordBuf[ 0 ] == null  &&  volumeModel != null  &&  signalTextureMediator != null ) {
            // Compute sizes, and allocate buffers.
            int numVertices = (int)
                    (
                            signalTextureMediator.getVolumeMicrometers()[ 0 ] *
                            signalTextureMediator.getVolumeMicrometers()[ 1 ] *
                            signalTextureMediator.getVolumeMicrometers()[ 2 ]
                    );
            int numCoords = 3 * numVertices;

            for ( int i = 0; i < 6; i++ ) {
                ByteBuffer texByteBuffer = ByteBuffer.allocate(numCoords * 8);
                texByteBuffer.order( ByteOrder.nativeOrder() );
                texCoordBuf[ i ] = texByteBuffer.asDoubleBuffer();
                ByteBuffer geoByteBuffer = ByteBuffer.allocate(numCoords * 8);
                geoByteBuffer.order(ByteOrder.nativeOrder());
                geometryCoordBuf[ i ] = geoByteBuffer.asDoubleBuffer();
            }

            // Now produce the vertexes to stuff into all of the buffers.
            //  Making sets of four.
            // FORWARD axes.
            for ( int firstInx = 0; firstInx < 3; firstInx++ ) {
                double firstAxisLen = signalTextureMediator.getVolumeMicrometers()[ firstInx ];
                int secondInx = (firstInx + 1) % 3;
                double secondAxisLen = signalTextureMediator.getVolumeMicrometers()[ secondInx ];
                int thirdInx = (firstInx + 2) % 3;
                double thirdAxisLen = signalTextureMediator.getVolumeMicrometers()[ thirdInx ];
                // compute number of slices
                int sliceCount = (int)(0.5 + firstAxisLen / signalTextureMediator.getVoxelMicrometers()[ firstInx ]);
                double slice0 = (signalTextureMediator.getVoxelMicrometers()[ firstInx ] - firstAxisLen) / 2.0;
                double sliceSep = signalTextureMediator.getVoxelMicrometers()[ firstInx ];

                /*
                		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
		// These axes might be permuted from the real world XYZ
		// Each slice cuts through an exact voxel center,
		// but slice edges extend all the way to voxel edges.
		// Compute dimensions of one x slice: y0-y1 and z0-z1
		// Pad edges of rectangles by one voxel to support oblique ray tracing past edges
		double y0 = direction * (signalTextureMediator.getVolumeMicrometers()[a2.index()] / 2.0 + signalTextureMediator.getVoxelMicrometers()[a2.index()]);
		double y1 = -y0;
		double z0 = direction * (signalTextureMediator.getVolumeMicrometers()[a3.index()] / 2.0 + signalTextureMediator.getVoxelMicrometers()[a3.index()]);
		double z1 = -z0;

                 */
                double second0 = secondAxisLen / 2.0 + signalTextureMediator.getVoxelMicrometers()[secondInx];
                double second1 = -second0;

                double third0 = thirdAxisLen / 2.0 + signalTextureMediator.getVoxelMicrometers()[thirdInx];
                double third1 = -third0;

                // Four points for four slice corners
                double[] p00p = {0,0,0};
                double[] p10p = {0,0,0};
                double[] p11p = {0,0,0};
                double[] p01p = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00p[ secondInx ] = p01p[ secondInx ] = second0;
                p10p[ secondInx ] = p11p[ secondInx ] = second1;
                p00p[ thirdInx ] = p10p[ thirdInx ] = third0;
                p01p[ thirdInx ] = p11p[ thirdInx ] = third1;

                // Four negated points for four slice corners
                double[] p00n = {0,0,0};
                double[] p10n = {0,0,0};
                double[] p11n = {0,0,0};
                double[] p01n = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00n[ secondInx ] = p01n[ secondInx ] = -second0;
                p10n[ secondInx ] = p11n[ secondInx ] = -second1;
                p00n[ thirdInx ] = p10n[ thirdInx ] = -third0;
                p01n[ thirdInx ] = p11n[ thirdInx ] = -third1;

                texCoordBuf[ firstInx ].rewind();
                for (int sliceInx = 0; sliceInx < sliceCount; ++sliceInx) {
                    // insert final coordinate into buffers
                    double sliceLoc = slice0 + sliceInx * sliceSep;
                    p00p[ firstInx ] = p01p[firstInx] = p10p[firstInx] = p11p[firstInx] = sliceLoc;

                    double[] t00 = signalTextureMediator.textureCoordFromVoxelCoord( p00p );
                    double[] t01 = signalTextureMediator.textureCoordFromVoxelCoord( p01p );
                    double[] t10 = signalTextureMediator.textureCoordFromVoxelCoord( p10p );
                    double[] t11 = signalTextureMediator.textureCoordFromVoxelCoord( p11p );


                    /*
                    			// Compute texture coordinates
			// color from black(back) to white(front) for debugging.
			// double c = xi / (double)sx;
			// gl.glColor3d(c, c, c);
			// draw the quadrilateral as a triangle strip with 4 points
            // (somehow GL_QUADS never works correctly for me)
            setTextureCoordinates(gl, t00[0], t00[1], t00[2]);
            gl.glVertex3d(p00[0], p00[1], p00[2]);
            setTextureCoordinates(gl, t10[0], t10[1], t10[2]);
            gl.glVertex3d(p10[0], p10[1], p10[2]);
            setTextureCoordinates(gl, t01[0], t01[1], t01[2]);
            gl.glVertex3d(p01[0], p01[1], p01[2]);
            setTextureCoordinates(gl, t11[0], t11[1], t11[2]);
            gl.glVertex3d(p11[0], p11[1], p11[2]);

                     */
                    texCoordBuf[ firstInx ].put( p00p );
                    texCoordBuf[ firstInx ].put( p10p );
                    texCoordBuf[ firstInx ].put( p01p );
                    texCoordBuf[ firstInx ].put( p11p );

                    geometryCoordBuf[ firstInx ].put( t00 );
                    geometryCoordBuf[ firstInx ].put( t10 );
                    geometryCoordBuf[ firstInx ].put( t01 );
                    geometryCoordBuf[ firstInx ].put( t11 );

                    // Now, take care of the negative-direction alternate to this buffer pair.
                    t00 = signalTextureMediator.textureCoordFromVoxelCoord( p00n );
                    t01 = signalTextureMediator.textureCoordFromVoxelCoord( p01n );
                    t10 = signalTextureMediator.textureCoordFromVoxelCoord( p10n );
                    t11 = signalTextureMediator.textureCoordFromVoxelCoord( p11n );

                    texCoordBuf[ firstInx + 3 ].put( p00n );
                    texCoordBuf[ firstInx + 3 ].put( p10n );
                    texCoordBuf[ firstInx + 3 ].put( p01n );
                    texCoordBuf[ firstInx + 3 ].put( p11n );

                    geometryCoordBuf[ firstInx + 3 ].put( t00 );
                    geometryCoordBuf[ firstInx + 3 ].put( t10 );
                    geometryCoordBuf[ firstInx + 3 ].put( t01 );
                    geometryCoordBuf[ firstInx + 3 ].put( t11 );

                }

                /*
                		for (int xi = 0; xi < sx; ++xi) {
			// insert final coordinate into corner vectors
			double x = x0 + xi * dx;
			int a = a1.index();
			p00[a] = p01[a] = p10[a] = p11[a] = x;

                 */
            }

        }
    }

    private void printPoints(double[] p1, double[] p2, double[] p3, double[] p4) {
        printPoint(p1);
        printPoint(p2);
        printPoint(p3);
        printPoint(p4);
    }

    private void printPoint(double[] p) {
        System.out.printf("%s, %s, %s%n", Double.toString(p[0]), Double.toString(p[1]), Double.toString(p[2]));
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
