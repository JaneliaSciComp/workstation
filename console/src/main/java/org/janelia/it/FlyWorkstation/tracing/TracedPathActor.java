package org.janelia.it.FlyWorkstation.tracing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.MicrometerXyz;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.VoxelXyz;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
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
    private int vertexArrayObject = 0;
    private ByteBuffer vertexByteBuffer;
    private int pointCount = 0;
    private boolean bIsInitialized = false;

    public TracedPathActor(TracedPathSegment path, TileFormat tileFormat) 
    {
        pointCount = path.getPath().size();
        // Store vertices
        long totalVertexByteCount = floatsPerVertex * bytesPerFloat * pointCount;
        vertexByteBuffer = ByteBuffer.allocateDirect(
                (int)totalVertexByteCount);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertices = vertexByteBuffer.asFloatBuffer();
        vertices.rewind();
        for (ZoomedVoxelIndex zv : path.getPath()) {
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
        if (! bIsInitialized)
            init(glDrawable);
        GL gl = glDrawable.getGL();
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        gl2gl3.glBindVertexArray(vertexArrayObject);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVbo);
        gl2gl3.glEnableVertexAttribArray(vertexLocation);
        gl2gl3.glVertexAttribPointer(vertexLocation, floatsPerVertex, GL.GL_FLOAT, false, 0, 0);
        GL2 gl2 = gl.getGL2();
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, pointCount);
        gl2gl3.glDisableVertexAttribArray(vertexLocation);
        checkGlError(gl, "render traced path");
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        // One-time initialization
        int[] vbos = {0};
        gl.glGenBuffers(1, vbos, 0);
        vertexVbo = vbos[0];
        checkGlError(gl, "create buffer handles");
        //
        int ix[] = {0};
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        gl2gl3.glGenVertexArrays(1, ix, 0);
        vertexArrayObject = ix[0];
        // vertices
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVbo);
        FloatBuffer vertices = vertexByteBuffer.asFloatBuffer();
        vertices.rewind();
        gl.glBufferData(GL.GL_ARRAY_BUFFER, 
                vertexByteBuffer.capacity(), 
                vertices, GL.GL_STATIC_DRAW);
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
            ix[0] = vertexArrayObject;
            GL2GL3 gl2gl3 = gl.getGL2GL3();
            gl2gl3.glDeleteVertexArrays(1, ix, 0);
            vertexArrayObject = 0;
        }
        bIsInitialized = false;
    }

}
