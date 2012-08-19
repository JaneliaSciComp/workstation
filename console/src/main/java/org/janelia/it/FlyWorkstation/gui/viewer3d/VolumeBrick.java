package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
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
    int vertexShader = 0;
    int fragmentShader = 0;
    int shaderProgram = 0;
    boolean haveShaders = false;
    // 
    private MipRenderer renderer; // circular reference...
    private boolean bIsInitialized;

    VolumeBrick(MipRenderer mipRenderer) {
    		renderer = mipRenderer;
    }
    
	@Override
	public void init(GL2 gl) {
		gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);
		int[] textureIds = {0};
		gl.glGenTextures(1, textureIds, 0);
		textureId = textureIds[0];
		int sx = textureVoxels[0];
		int sy = textureVoxels[1];
		int sz = textureVoxels[2];
		// Clear texture data
		data.rewind();
		while (data.hasRemaining()) {
			data.put(0x00000000);
		}
		// Create simple synthetic image for testing.
		// 0xAARRGGBB
		setVoxelColor(0,0,0, 0x66ff0000); // ghostly red
		setVoxelColor(0,0,1, 0x7700ff00);
		setVoxelColor(0,0,2, 0x990000ff);
		setVoxelColor(0,1,0, 0xbbff0000);
		setVoxelColor(0,1,1, 0xdd00ff00);
		setVoxelColor(0,1,2, 0xff0000ff); // opaque blue
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
		gl.glTexImage3D(GL2.GL_TEXTURE_3D, 
				0, // mipmap level
				GL2.GL_RGBA8, // number of bytes per voxel
				sx, // width 
				sy, // height
				sz, // depth
				0, // border 
				GL2.GL_BGRA, // voxel component order
				GL2.GL_UNSIGNED_INT_8_8_8_8_REV, // voxel component type
				data.rewind());
		// Create shader program
		vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		if (loadShader(vertexShader, "shaders/PassThroughVtx.glsl", gl)) {
			System.out.println("loaded vertex shader");
			fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
			if (loadShader(fragmentShader, "shaders/PassThroughFrg.glsl", gl)) {
				System.out.println("loaded fragment shader");
				shaderProgram = gl.glCreateProgram();
				gl.glAttachShader(shaderProgram, vertexShader);
				gl.glAttachShader(shaderProgram, fragmentShader);
				gl.glLinkProgram(shaderProgram);
				gl.glValidateProgram(shaderProgram);
				IntBuffer intBuffer = IntBuffer.allocate(1);
				gl.glGetProgramiv(shaderProgram, GL2.GL_LINK_STATUS, intBuffer);
				if (intBuffer.get(0) != 1) {
					gl.glGetProgramiv(shaderProgram, GL2.GL_INFO_LOG_LENGTH, intBuffer);
					int size = intBuffer.get(0);
					System.err.println("Program link error: ");
					if (size > 0) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(size);
						gl.glGetProgramInfoLog(shaderProgram, size, intBuffer, byteBuffer);
						for (byte b : byteBuffer.array()) {
							System.err.print((char)b);
						}
					} else {
						System.out.println("Unknown");
					}
				}
				else
					haveShaders = true;
			}
		}
		// tidy up
		gl.glPopAttrib();
		bIsInitialized = true;
	}

	@Override
	public void display(GL2 gl) {
		if (! bIsInitialized)
			init(gl);
		// debugging objects showing useful boundaries of what we want to render
		gl.glColor3d(1,1,1);
		displayVoxelCenterBox(gl);
		gl.glColor3d(1,1,0.3);
		displayVoxelCornerBox(gl);
		// a stack of transparent slices looks like a volume
		gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_TEXTURE_BIT | GL2.GL_ENABLE_BIT);
		gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        // set blending to enable transparent voxels
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        //
        IntBuffer buffer = IntBuffer.allocate(1);
    	gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, buffer);
        int previousShader = buffer.get();
        if (haveShaders) {
    		gl.glUseProgram(shaderProgram);
    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "volumeTexture"), 0);
        }
		displayVolumeSlices(gl);
        if (haveShaders)
    		gl.glUseProgram(previousShader);
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

	private boolean loadShader(int shaderId, String resourceName, GL2 gl) {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(
							getClass().getResourceAsStream(
									resourceName) ));
			StringBuffer stringBuffer = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuffer.append(line);
				stringBuffer.append("\n");
			}
			String progString = stringBuffer.toString();
			// System.out.println(progString);
			gl.glShaderSource(shaderId, 1, new String[]{progString}, (int[])null, 0);
			gl.glCompileShader(shaderId);
			
			// query compile status and possibly read log
			int[] status = new int[1];
			gl.glGetShaderiv(shaderId, GL2.GL_COMPILE_STATUS, status, 0);
			if (status[0] == GL2.GL_TRUE){
				System.out.println(resourceName + ": successful");
				// everything compiled successfully, no log
			} 
			else {
				// compile failed, read the log and return it
				gl.glGetShaderiv(shaderId, GL2.GL_INFO_LOG_LENGTH, status, 0);
				int maxLogLength = status[0];
				if (maxLogLength > 0) {
					byte[] log = new byte[maxLogLength];
					gl.glGetShaderInfoLog(shaderId, maxLogLength, status, 0, log, 0);
					System.out.println(resourceName + ": " + new String(log, 0, status[0]));
				} else
					System.out.println(resourceName + ": "+ "unknown compilation error");
				return false;
			}
			return true;
		} catch (Exception exc) {
			exc.printStackTrace();
		}		
		return false;
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
	
	public void setVoxelColor(int x, int y, int z, int color) {
		int sx = textureVoxels[0];
		int sy = textureVoxels[1];
		data.put(z*sx*sy + y*sx + x, color);
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
}
