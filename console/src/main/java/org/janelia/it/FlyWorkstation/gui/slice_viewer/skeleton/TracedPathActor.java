package org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.MicrometerXyz;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.VoxelXyz;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.tracing.AnchoredVoxelPath;
import org.janelia.it.FlyWorkstation.tracing.PathTraceRequest.SegmentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracedPathActor 
implements GLActor
{
    private static Logger logger = LoggerFactory.getLogger( TracedPathActor.class );
    protected static GLU glu = new GLU();

    private final int floatsPerVertex = 3;
    private final int bytesPerFloat = Float.SIZE/8;
    private int vertexLocation = 0; // shader program uniform index
    private BoundingBox3d boundingBox = new BoundingBox3d();
    private int vertexVbo = 0;
    // private int vertexArrayObject = 0;
    private ByteBuffer vertexByteBuffer;
    private int pointCount = 0;
    private boolean bIsInitialized = false;
	private SegmentIndex segmentIndex;
	// For determining if traced path is still appropriate
	AnchoredVoxelPath segment;

    public TracedPathActor(AnchoredVoxelPath segment, TileFormat tileFormat) 
    {
    	this.segment = segment;
    	// TODO guid
    	//
        pointCount = segment.getPath().size();
        // Store vertices
        long totalVertexByteCount = floatsPerVertex * bytesPerFloat * pointCount;
        vertexByteBuffer = ByteBuffer.allocateDirect(
                (int)totalVertexByteCount);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertices = vertexByteBuffer.asFloatBuffer();
        vertices.rewind();
        for (ZoomedVoxelIndex zv : segment.getPath()) {
            VoxelXyz vx = tileFormat.voxelXyzForZoomedVoxelIndex(zv, CoordinateAxis.Z);
            MicrometerXyz umXyz = tileFormat.micrometerXyzForVoxelXyz(vx, CoordinateAxis.Z);
            Vec3 v = new Vec3(
                    // Translate from upper left front corner of voxel to center of voxel
                    umXyz.getX() + 0.5 * tileFormat.getVoxelMicrometers()[0], 
                    umXyz.getY() + 0.5 * tileFormat.getVoxelMicrometers()[1], 
                    umXyz.getZ() - 0.5 * tileFormat.getVoxelMicrometers()[2]); // Minus? Really? TODO
            boundingBox.include(v);
            vertices.put((float)v.getX());
            vertices.put((float)v.getY());
            vertices.put((float)v.getZ());
            if (floatsPerVertex >= 4)
                vertices.put(1.0f);
        }
        vertices.rewind();
        this.segmentIndex = segment.getSegmentIndex();
    }

    private void checkGlError(GL gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );  
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        checkGlError(gl, "render traced path 87");
        if (! bIsInitialized)
            init(glDrawable);
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        checkGlError(gl, "render traced path 91");
        // gl2gl3.glBindVertexArray(vertexArrayObject);
        checkGlError(gl, "render traced path 93");
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVbo);
        gl2gl3.glEnableVertexAttribArray(vertexLocation);
        gl2gl3.glVertexAttribPointer(vertexLocation, floatsPerVertex, GL.GL_FLOAT, false, 0, 0);
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, pointCount);
        gl2gl3.glDisableVertexAttribArray(vertexLocation);
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }

    public AnchoredVoxelPath getSegment() {
		return segment;
	}

	@Override
    public void init(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        checkGlError(gl, "init traced path 116");
        // One-time initialization
        int[] vbos = {0};
        gl.glGenBuffers(1, vbos, 0);
        vertexVbo = vbos[0];
        checkGlError(gl, "create buffer handles");
        //
        // int ix[] = {0};
        // GL2GL3 gl2gl3 = gl.getGL2GL3();
        checkGlError(gl, "init traced path 122");
        // gl2gl3.glGenVertexArrays(1, ix, 0);
        checkGlError(gl, "init traced path 124");
        // vertexArrayObject = ix[0];
        // gl2gl3.glBindVertexArray(vertexArrayObject);
        // vertices
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVbo);
        FloatBuffer vertices = vertexByteBuffer.asFloatBuffer();
        vertices.rewind();
        checkGlError(gl, "init traced path 131");
        gl.glBufferData(GL.GL_ARRAY_BUFFER, 
                vertexByteBuffer.capacity(), 
                vertices, GL.GL_STATIC_DRAW);
        checkGlError(gl, "init traced path 135");
        // gl2gl3.glBindVertexArray(0);
        checkGlError(gl, "init traced path 137");
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        bIsInitialized = true;
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        if (vertexVbo > 0) {
            int[] ix = {vertexVbo};
            GL gl = glDrawable.getGL();
            gl.glDeleteBuffers(1, ix, 0);
            vertexVbo = 0;
            //
            // ix[0] = vertexArrayObject;
            // GL2GL3 gl2gl3 = gl.getGL2GL3();
            // gl2gl3.glDeleteVertexArrays(1, ix, 0);
            // vertexArrayObject = 0;
        }
        bIsInitialized = false;
    }

	public SegmentIndex getSegmentIndex() {
		return segmentIndex;
	}

}
