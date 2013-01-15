package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.gl2.GLUT;

import javax.media.opengl.GL2;

/**
 * VolumeTexture class draws a transparent rectangular volume with a 3D opengl texture
 * @author brunsc
 *
 */
public class VolumeBrick implements GLActor, VolumeDataAcceptor
{
    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING};
	// Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod = 
		// RenderMethod.ALPHA_BLENDING;
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private int interpolationMethod =
    		GL2.GL_LINEAR; // blending across voxel edges
    		// GL2.GL_NEAREST; // discrete cube shaped voxels
    private boolean bUseShader = true; // Controls whether to load and use shader program(s).
    // Color space is linear for most microscopy LSM, TIFF and V3DRAW files
    private TextureColorSpace signalTextureColorSpace = TextureColorSpace.COLOR_SPACE_LINEAR;
    private TextureColorSpace maskTextureColorSpace = TextureColorSpace.COLOR_SPACE_LINEAR;

    private int[] textureIds;


    private GLUT glut = new GLUT();    
    /**
     * Dimensions of rectangular volume, from centers of corner voxels, in world units.
     */
    // Geometry parameters
    private double[] volumeMicrometers = {1.0, 2.0, 3.0};
    /**
     * Size of a single voxel, in world units.
     */
    private double[] voxelMicrometers = {1.0, 1.0, 1.0};

    private float[] colorMask = { 1.0f, 1.0f, 1.0f };
    /**
     * Size of our opengl texture, which might be padded with extra voxels
     * to reach a multiple of 8
     */
    // OpenGL state
    private int[] signalTextureVoxels = {8,8,8};
	private IntBuffer signalData = Buffers.newDirectIntBuffer(signalTextureVoxels[0]* signalTextureVoxels[1]* signalTextureVoxels[2]);
    private int signalTextureId = 0;
    private boolean bSignalTextureNeedsUpload = false;

    private int[] maskingVoxels = {8,8,8};
    private IntBuffer maskData;
    private int maskTextureId;
    private boolean bMaskTextureNeedsUpload = false;

    private VolumeBrickShader volumeBrickShader = new VolumeBrickShader();

    private MipRenderer renderer; // circular reference...
    private boolean bIsInitialized;
    private boolean bUseSyntheticData = false;

    VolumeBrick(MipRenderer mipRenderer) {
    		renderer = mipRenderer;
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
        colorMask[ 0 ] = red;
        colorMask[ 1 ] = green;
        colorMask[ 2 ] = blue;
    }

    public float[] getColorMask() {
        return colorMask;
    }

	@Override
	public void init(GL2 gl) {
        if (bUseSyntheticData) {
            createSyntheticData();
        }

		gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);
        genTextureIds(gl);

		if (bSignalTextureNeedsUpload) {
			uploadSignalTexture(gl);
        }
		if (bUseShader) {
            if ( bMaskTextureNeedsUpload ) {
                uploadMaskingTexture(gl);
            }

            try {
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

    private void genTextureIds(GL2 gl) {
        textureIds = new int[ maskData == null ? 1 : 2 ];
        gl.glGenTextures(textureIds.length, textureIds, 0);
        signalTextureId = textureIds[0];
        if ( textureIds.length > 1 )
            maskTextureId = textureIds[1];
    }

    @Override
	public void display(GL2 gl) {
		if (! bIsInitialized)
			init(gl);
		if (bSignalTextureNeedsUpload)
			uploadSignalTexture(gl);
        if (bMaskTextureNeedsUpload)
            uploadMaskingTexture(gl);

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
        if ( maskData != null )
            setupMaskingTexture(gl);

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
            volumeBrickShader.setColorMask(colorMask);
            if ( maskData != null ) {
                volumeBrickShader.setVolumeMaskApplied();
            }
            volumeBrickShader.load(gl);
            //volumeBrickShader.setTextureUniforms( gl, textureIds );
        }

        displayVolumeSlices(gl);
		if (bUseShader) {
            volumeBrickShader.unload(gl);
        }
		gl.glPopAttrib();
	}

    // Debugging object with corners at the center of
	// the corner voxels of this volume.
	public void displayVoxelCenterBox(GL2 gl) {
		gl.glPushMatrix();
		double[] v = volumeMicrometers;
		double[] pad = voxelMicrometers;
		gl.glScaled(v[0]-pad[0], v[1]-pad[1], v[2]-pad[2]);
		glut.glutWireCube(1);
		gl.glPopMatrix();		
	}

	// Debugging object with corners at the outer corners of
	// the corner voxels of this volume.
	public void displayVoxelCornerBox(GL2 gl) {
		gl.glPushMatrix();
		double[] v = volumeMicrometers;
		gl.glScaled(v[0], v[1], v[2]);
		glut.glutWireCube(1);
		gl.glPopMatrix();		
	}

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
		Vec3 viewVectorInGround = renderer.getRotation().times(new Vec3(0,0,1));
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
		double y0 = direction * (volumeMicrometers[a2.index()] / 2.0 + voxelMicrometers[a2.index()]);
		double y1 = -y0;
		double z0 = direction * (volumeMicrometers[a3.index()] / 2.0 + voxelMicrometers[a3.index()]);
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
		int sx = (int)(0.5 + volumeMicrometers[a1.index()] / voxelMicrometers[a1.index()]);
		// compute position of first slice
		double x0 = -direction * (voxelMicrometers[a1.index()] - volumeMicrometers[a1.index()]) / 2.0;
		// compute distance between slices
		double dx = -direction * voxelMicrometers[a1.index()];
		// deal out the slices, like cards from a deck
		for (int xi = 0; xi < sx; ++xi) {
			// insert final coordinate into corner vectors
			double x = x0 + xi * dx;
			int a = a1.index();
			p00[a] = p01[a] = p10[a] = p11[a] = x;
			// Compute texture coordinates
			double[] t00 = textureCoordinateFromXyz(p00);
			double[] t01 = textureCoordinateFromXyz(p01);
			double[] t10 = textureCoordinateFromXyz(p10);
			double[] t11 = textureCoordinateFromXyz(p11);
			// color from black(back) to white(front) for debugging.
			// double c = xi / (double)sx;
			// gl.glColor3d(c, c, c);
			// draw the quadrilateral as a triangle strip with 4 points
            // (somehow GL_QUADS never works correctly for me)
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
            gl.glTexCoord3d(t00[0], t00[1], t00[2]);
            gl.glVertex3d(p00[0], p00[1], p00[2]);

            gl.glTexCoord3d(t10[0], t10[1], t10[2]);
            gl.glVertex3d(p10[0], p10[1], p10[2]);

            gl.glTexCoord3d(t01[0], t01[1], t01[2]);
            gl.glVertex3d(p01[0], p01[1], p01[2]);

            gl.glTexCoord3d(t11[0], t11[1], t11[2]);
            gl.glVertex3d(p11[0], p11[1], p11[2]);
            gl.glEnd();
			boolean bDebug = false;
			if (bDebug)
				printPoints(t00, t10, t01, t11);
		}

    }

	@Override
	public void dispose(GL2 gl) {
		gl.glDeleteTextures(1, textureIds, 0);
		// Retarded JOGL GLJPanel frequently reallocates the GL context
		// during resize. So we need to be ready to reinitialize everything.
		signalTextureId = 0;
        maskTextureId = 0;
		bSignalTextureNeedsUpload = true;
        bMaskTextureNeedsUpload = true;
        resetMaskingTextures();
		bIsInitialized = false;
	}

    @Override
	public BoundingBox getBoundingBox() {
		BoundingBox result = new BoundingBox();
		Vec3 half = new Vec3(0,0,0);
		for (int i = 0; i < 3; ++i)
			half.set(i, 0.5 * volumeMicrometers[i]);
		result.include(half.minus());
		result.include(half);
		return result;
	}

	public void setTextureColorSpace(TextureColorSpace colorSpace) {
		if (colorSpace != signalTextureColorSpace) {
			signalTextureColorSpace = colorSpace;
			bSignalTextureNeedsUpload = true;
            bMaskTextureNeedsUpload = true;
		}
	}

    public void setMaskTextureColorSpace(TextureColorSpace colorSpace) {
        if (colorSpace != maskTextureColorSpace) {
            maskTextureColorSpace = colorSpace;
            bSignalTextureNeedsUpload = true;
            bMaskTextureNeedsUpload = true;
        }
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
		bUseSyntheticData = false;
	}
	
	public void setVolumeData(int sx, int sy, int sz, IntBuffer rgbaBuffer) 
	{
		signalTextureVoxels = new int[]{sx, sy, sz};
        signalData = rgbaBuffer;
		bSignalTextureNeedsUpload = true;
		bUseSyntheticData = false;
	}
	
	public void setVolumeData(int sx, int sy, int sz, int[] intArray) 
	{
		setVolumeData(sx, sy, sz, Buffers.newDirectIntBuffer(intArray));
	}

    public void setMaskingData(int sx, int sy, int sz, IntBuffer rgbaBuffer)
    {
//setVolumeData(sx, sy, sz, rgbaBuffer); // *** TEMP ***
        maskingVoxels = new int[]{sx, sy, sz};
        maskData = rgbaBuffer;
        bMaskTextureNeedsUpload = true;
    }

    public void setVolumeMicrometers(double x, double y, double z) {
		volumeMicrometers[0] = x;
		volumeMicrometers[1] = y;
		volumeMicrometers[2] = z;
	}
	
	public void setVoxelColor(int x, int y, int z, int color) {
		int sx = signalTextureVoxels[0];
		int sy = signalTextureVoxels[1];
        signalData.put(z * sx * sy + y * sx + x, color);
	}
	
	public void setVoxelMicrometers(double x, double y, double z) {
		voxelMicrometers[0] = x;
		voxelMicrometers[1] = y;
		voxelMicrometers[2] = z;
	}

    /** Call this when the brick is to be re-shown after an absense. */
    public void refresh() {
        bSignalTextureNeedsUpload = true;
    }
	
	private double[] textureCoordinateFromXyz(double[] xyz) {
		double[] tc = {xyz[0], xyz[1], xyz[2]}; // micrometers, origin at center
		for (int i =0; i < 3; ++i) {
			// Move origin to upper left corner
			tc[i] += volumeMicrometers[i] / 2.0; // micrometers, origin at corner
			// Rescale from micrometers to voxels
			tc[i] /= voxelMicrometers[i]; // voxels, origin at corner
			// Rescale from voxels to texture units (range 0-1)
			tc[i] /= signalTextureVoxels[i]; // texture units
		}

		return tc;
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
        uploadTexture(
                gl,
                signalTextureId,
                signalTextureVoxels[0],
                signalTextureVoxels[1],
                signalTextureVoxels[2],
                signalTextureColorSpace,
                signalData
        );
        bSignalTextureNeedsUpload = false;
    }

    /** Upload the masking texture to open GL "state". */
    private void uploadMaskingTexture(GL2 gl) {
        uploadTexture(
                gl,
                maskTextureId,
                maskingVoxels[0],
                maskingVoxels[1],
                maskingVoxels[2],
                maskTextureColorSpace,
                maskData
        );
        bMaskTextureNeedsUpload = false;
    }

    /** Uploading any texture. */
    private void uploadTexture(
            GL2 gl, int textureId, int sx, int sy, int sz, TextureColorSpace textureColorSpace, IntBuffer data
    ) {
        if (data != null) {
            gl.glEnable(GL2.GL_TEXTURE_3D);
            gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
            int internalFormat = GL2.GL_RGBA8;
            if (textureColorSpace == TextureColorSpace.COLOR_SPACE_SRGB)
                internalFormat = GL2.GL_SRGB8_ALPHA8;
            gl.glTexImage3D(GL2.GL_TEXTURE_3D,
                    0, // mipmap level
                    internalFormat, // bytes per pixel, plus somehow srgb info
                    sx, // width
                    sy, // height
                    sz, // depth
                    0, // border
                    GL2.GL_BGRA, // voxel component order
                    GL2.GL_UNSIGNED_INT_8_8_8_8_REV, // voxel component type
                    data.rewind()
            );
        }

	}

    private void resetMaskingTextures() {
        maskData = null;
        maskingVoxels = null;
    }

    private void setupMaskingTexture(GL2 gl) {
        setupTexture(gl, maskTextureId);
    }

    private void setupSignalTexture(GL2 gl) {
        setupTexture(gl, signalTextureId);
    }

    private void setupTexture(GL2 gl, int textureId) {
        gl.glActiveTexture(textureId);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, interpolationMethod);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, interpolationMethod);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
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

}
