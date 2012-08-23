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
public class VolumeBrick implements GLActor 
{
	public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING};
	public enum TextureColorSpace {
		COLOR_SPACE_LINEAR, // R,G,B values are proportional to photons collected
		COLOR_SPACE_SRGB // R,G,B values are already gamma corrected for display on computer monitors
	};

	// Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod = 
		// RenderMethod.ALPHA_BLENDING;
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private int interpolationMethod =
    		GL2.GL_LINEAR; // blending across voxel edges
    		// GL2.GL_NEAREST; // discrete cube shaped voxels
    private boolean bUseShader = false; // whether to ray trace voxels
    // Color space is linear for most microscopy LSM, TIFF and V3DRAW files
    private TextureColorSpace textureColorSpace = TextureColorSpace.COLOR_SPACE_LINEAR;
	
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
    /**
     * Size of our opengl texture, which might be padded with extra voxels
     * to reach a multiple of 8
     */
    // OpenGL state
    private int[] textureVoxels = {8,8,8};
	IntBuffer data = Buffers.newDirectIntBuffer(textureVoxels[0]*textureVoxels[1]*textureVoxels[2]);
    private int textureId = 0;
    private boolean bTextureNeedsUpload = false;
    private VoxelRayShader shader = new VoxelRayShader();
    private MipRenderer renderer; // circular reference...
    private boolean bIsInitialized;
    private boolean bUseSyntheticData = true;

    VolumeBrick(MipRenderer mipRenderer) {
    		renderer = mipRenderer;
    }
    
	@Override
	public void init(GL2 gl) {
		gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);
		int[] textureIds = {0};
		gl.glGenTextures(1, textureIds, 0);
		textureId = textureIds[0];
		if (bUseSyntheticData) {
			// Clear texture data
			data.rewind();
			while (data.hasRemaining()) {
				data.put(0x00000000);
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
			setVoxelColor(0,1,2, 0xff0000ff); // opaque blue
			bTextureNeedsUpload = true;
		}
		if (bTextureNeedsUpload)
			uploadTexture(gl);
		if (bUseShader)
			shader.init(gl);
		// tidy up
		gl.glPopAttrib();
		bIsInitialized = true;
	}

	@Override
	public void display(GL2 gl) {
		if (! bIsInitialized)
			init(gl);
		if (bTextureNeedsUpload)
			uploadTexture(gl);
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
        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, interpolationMethod);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, interpolationMethod);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        // set blending to enable transparent voxels
        if (renderMethod == RenderMethod.ALPHA_BLENDING) {
        		gl.glEnable(GL2.GL_BLEND);
        		gl.glBlendEquation(GL2.GL_FUNC_ADD);
        		gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }
        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
    			gl.glEnable(GL2.GL_BLEND);
    			gl.glBlendEquation(GL2.GL_MAX);
    			gl.glBlendFunc(GL2.GL_ONE, GL2.GL_DST_ALPHA);
        }
        if (bUseShader) {
        		shader.setUniforms(textureVoxels, voxelMicrometers);
        		shader.load(gl);
        }
		displayVolumeSlices(gl);
		if (bUseShader)
			shader.unload(gl);
		gl.glPopAttrib();

		// System.out.println("Focus = " + renderer.focusInGround);
		// System.out.println("Camera distance = " + renderer.cameraFocusDistance);
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
		Vec3 viewVectorInGround = renderer.getRotation().times(new Vec3(0,0,-1));
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
		int[] textureIds = {textureId};
		gl.glDeleteTextures(1, textureIds, 0);
	}

	public BoundingBox getBoundingBox() {
		BoundingBox result = new BoundingBox();
		Vec3 half = new Vec3(0,0,0);
		for (int i = 0; i < 3; ++i)
			half.set(i, 0.5 * volumeMicrometers[i]);
		result.include(half.minus());
		result.include(half);
		return result;
	}
	
	private void printPoints(double[] p1, double[] p2, double[] p3, double[] p4) {
		printPoint(p1);
		printPoint(p2);
		printPoint(p3);
		printPoint(p4);
	}
	
	private void printPoint(double[] p) {
		System.out.println(
				new Double(p[0]).toString() + ", " +
				new Double(p[1]).toString() + ", " +
				new Double(p[2]).toString());
	}
	
	public void setTextureColorSpace(TextureColorSpace colorSpace) {
		if (colorSpace != textureColorSpace) {
			textureColorSpace = colorSpace;
			bTextureNeedsUpload = true;
		}
	}
	
	/**
	 * Colors are already premultiplied by alpha.
	 * Colors are assumed to be completely saturated.
	 * So alpha component will be computed from largest RGB component.
	 * @param w width/x dimension in voxels
	 * @param h height/y dimension in voxels
	 * @param d depth/z dimension in voxels
	 * @param rgbArray RGBA pixel data as integers
	 */
	public void setVolumeDataComputeAlpha(int sx, int sy, int sz, int[] rgbArray)
	{
		textureVoxels = new int[]{sx, sy, sz};
		final int numVoxels = sx * sy * sz;
		data = Buffers.newDirectIntBuffer(numVoxels);
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
					data.put(i_out, rgb);
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
		bTextureNeedsUpload = true;
		bUseSyntheticData = false;
	}
	
	public void setVolumeData(int sx, int sy, int sz, IntBuffer rgbaBuffer) 
	{
		textureVoxels = new int[]{sx, sy, sz};
		data = rgbaBuffer;
		bTextureNeedsUpload = true;
		bUseSyntheticData = false;
	}
	
	public void setVolumeData(int sx, int sy, int sz, int[] intArray) 
	{
		setVolumeData(sx, sy, sz, Buffers.newDirectIntBuffer(intArray));
	}

	public void setVolumeMicrometers(double x, double y, double z) {
		volumeMicrometers[0] = x;
		volumeMicrometers[1] = y;
		volumeMicrometers[2] = z;
	}
	
	public void setVoxelColor(int x, int y, int z, int color) {
		int sx = textureVoxels[0];
		int sy = textureVoxels[1];
		data.put(z*sx*sy + y*sx + x, color);
	}
	
	public void setVoxelMicrometers(double x, double y, double z) {
		voxelMicrometers[0] = x;
		voxelMicrometers[1] = y;
		voxelMicrometers[2] = z;
	}
	
	private double[] textureCoordinateFromXyz(double[] xyz) {
		double[] tc = {xyz[0], xyz[1], xyz[2]}; // micrometers, origin at center
		for (int i =0; i < 3; ++i) {
			// Move origin to upper left corner
			tc[i] += volumeMicrometers[i] / 2.0; // micrometers, origin at corner
			// Rescale from micrometers to voxels
			tc[i] /= voxelMicrometers[i]; // voxels, origin at corner
			// Rescale from voxels to texture units (range 0-1)
			tc[i] /= textureVoxels[i]; // texture units
		}
		return tc;
	}
	
	private void uploadTexture(GL2 gl) {
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
		int sx = textureVoxels[0];
		int sy = textureVoxels[1];
		int sz = textureVoxels[2];
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
				data.rewind());
	}
}
