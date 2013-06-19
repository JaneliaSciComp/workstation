package org.janelia.it.FlyWorkstation.gui.viewer3d.learning;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.nio.*;

public class RectSolid implements GLActor {

    private static final float Z_BACK_PLANE = -75.0f;
    private static final float Z_FRONT_PLANE = 75.0f;

    private static final float Y_TOP_PLANE = 125f;
    private static final float Y_BOTTOM_PLANE = -125f;

    private static final float X_LEFT_PLANE = -150f;
    private static final float X_RIGHT_PLANE = 150f;

    // This helps to track whether the class has been re-loaded by Android or not.
    private static int __class_instance = 0;
    {
        __class_instance++;
    }

    private FloatBuffer mFVertexBuffer;
    private FloatBuffer mFTexVtxBuffer;

    //	private ByteBuffer mColorBuffer;
    private ShortBuffer mTriangles;

    private FloatBuffer matSpecBufF;
    private FloatBuffer matShinBufF;
    private FloatBuffer lightPosBufF;

    private int vertexBufHandle = 0;

    public RectSolid() {
        float[] vertices = {
                // Front plane. 0-3
                X_LEFT_PLANE, Y_TOP_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_TOP_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_BOTTOM_PLANE, Z_FRONT_PLANE,
                X_LEFT_PLANE, Y_BOTTOM_PLANE, Z_FRONT_PLANE,

                // Top plane.  4-7
                X_LEFT_PLANE, Y_TOP_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_TOP_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_TOP_PLANE, Z_BACK_PLANE,
                X_LEFT_PLANE, Y_TOP_PLANE, Z_BACK_PLANE,

                // Left Plane.  8-11
                X_LEFT_PLANE, Y_TOP_PLANE, Z_FRONT_PLANE,
                X_LEFT_PLANE, Y_BOTTOM_PLANE, Z_FRONT_PLANE,
                X_LEFT_PLANE, Y_BOTTOM_PLANE, Z_BACK_PLANE,
                X_LEFT_PLANE, Y_TOP_PLANE, Z_BACK_PLANE,

                // Right plane  12-15
                X_RIGHT_PLANE, Y_TOP_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_BOTTOM_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_BOTTOM_PLANE, Z_BACK_PLANE,
                X_RIGHT_PLANE, Y_TOP_PLANE, Z_BACK_PLANE,

                // Back plane.  16-19
                X_LEFT_PLANE, Y_TOP_PLANE, Z_BACK_PLANE,
                X_RIGHT_PLANE, Y_TOP_PLANE, Z_BACK_PLANE,
                X_RIGHT_PLANE, Y_BOTTOM_PLANE, Z_BACK_PLANE,
                X_LEFT_PLANE, Y_BOTTOM_PLANE, Z_BACK_PLANE,

                // Bottom plane.  20-23
                X_LEFT_PLANE, Y_BOTTOM_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_BOTTOM_PLANE, Z_FRONT_PLANE,
                X_RIGHT_PLANE, Y_BOTTOM_PLANE, Z_BACK_PLANE,
                X_LEFT_PLANE, Y_BOTTOM_PLANE, Z_BACK_PLANE,
        };

        float[] texVertices = {
                0.3f, 0.3f,
                0.5f, 1.0f,
                0.7f, 0.3f,
        };

        short triangles[] = {
                2, 1, 0,
                0, 3, 2,

                6, 5, 4,
                4, 7, 6,

                10, 9, 8,
                8, 11, 10,

                12, 13, 14,
                14, 15, 12,

                16, 17, 18,
                18, 19, 16,

                20, 21, 22,
                22, 23, 20,
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();
        mFVertexBuffer.put(vertices);
        mFVertexBuffer.position(0);

//        ByteBuffer tvbb = ByteBuffer.allocateDirect(texVertices.length * 4);
//        tvbb.order(ByteOrder.nativeOrder());
//        mFTexVtxBuffer = tvbb.asFloatBuffer();
//        mFTexVtxBuffer.put(texVertices);
//        mFTexVtxBuffer.position(0);

        ByteBuffer tempBuffer = ByteBuffer.allocateDirect(triangles.length * Short.SIZE/8);
        tempBuffer.order( ByteOrder.nativeOrder() );
        mTriangles = tempBuffer.asShortBuffer();
        mTriangles.put(triangles);
        mTriangles.position(0);

        float mat_specular[] = { 1.0f, 1.0f, 1.0f, 1.0f };
        ByteBuffer matSpecBuf = ByteBuffer.allocateDirect( 4 * 4 );
        matSpecBuf.order(ByteOrder.nativeOrder());
        matSpecBufF = matSpecBuf.asFloatBuffer();
        matSpecBufF.put( mat_specular );
        matSpecBufF.position( 0 );

        float mat_shininess[] = { 50.0f };
        ByteBuffer matShinBuf = ByteBuffer.allocateDirect( 1* 4 );
        matShinBuf.order(ByteOrder.nativeOrder());
        matShinBufF = matShinBuf.asFloatBuffer();
        matShinBufF.put( mat_shininess );
        matShinBufF.position( 0 );

        float light_position[] = { 1.0f, 1.0f, -1.0f, 0.0f };
        ByteBuffer lightPosBuf = ByteBuffer.allocateDirect( 4 * 4 );
        lightPosBuf.order( ByteOrder.nativeOrder() );
        lightPosBufF = lightPosBuf.asFloatBuffer();
        lightPosBufF.put( light_position );
        lightPosBufF.position( 0 );

    }

    public void draw( GL2 gl ) throws Exception {
        errorCheck( gl, "Before rect-solid draw...");
        //Rotate around the axis based on the rotation matrix (rotation, x, y, z)
        long timeS = new java.util.Date().getTime() / 20;
        float yDegrees = timeS - ((timeS / 360) * 360);
        //Log.v("DUMP", "degrees=" + yDegrees);

        gl.glShadeModel (GL2.GL_SMOOTH);

        //gl.glTranslatef(0.0f, 0.0f, 800.0f); // Push back into screen to make it visible to user.
        //gl.glScalef(2.0f, 2.0f, 2.0f); 			//Scale the glyph to make a good fit to screen
        gl.glTranslatef(0.0f, 0.7f, 0.0f); // Move up.
        //gl.glRotatef(yDegrees, 0.0f, 0.0f, 1.0f);	//Z
        errorCheck( gl, "Positioning");
        appearance( gl );

        //Enable the vertex and texture state
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glFrontFace(GL2.GL_CCW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBufHandle);
        errorCheck(gl, "Bind Vertex Buffer");
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

        errorCheck( gl, "Vertex Pointer");
        gl.glColor4f(1.0f, 1.0f, 0.5f, 1.0f);
        gl.glDrawElements( GL2.GL_TRIANGLES, 12 * 3, GL2.GL_UNSIGNED_SHORT, mTriangles);
        errorCheck(gl, "Draw Elements");
        gl.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);

        errorCheck( gl, "After all RectSolid Drawing");
    }

    //---------------------------------------IMPLEMENTS GLActor
    @Override
    public void display(GL2 gl) {
        try {
            draw( gl );
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        BoundingBox3d result = new BoundingBox3d();
        Vec3 half = new Vec3(0,0,0);
        for (int i = 0; i < 3; ++i)
            half.set( i, 0.5 * 1024 );
        result.include(half.minus());
        result.include(half);
        return result;
    }

    @Override
    public void init(GL2 gl) {
        // Here, buffer-uploads are carried out.  This static data will reside in the shader until app completion.
        try {
            vertexBufHandle = enableBuffer(gl, mFVertexBuffer);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    @Override
    public void dispose(GL2 gl) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /** Convenience method to cut down on repeated code. */
    private int enableBuffer( GL2 gl, Buffer buffer ) throws Exception {
        // Make handles for subsequent use.
        int[] handles = new int[ 1 ];
        gl.glGenBuffers( 1, handles, 0 );

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, handles[ 0 ]);

        // NOTE: this operates on the most recent "bind".  Therefore unless
        // synchronization is in use, this makes the method un-thread-safe.
        // Imagine multiple "bind" and "buffer-data" calls in parallel threads...disaster!
        gl.glBufferData(
                GL2.GL_ARRAY_BUFFER,
                (long)(buffer.capacity() * (Float.SIZE/8)),
                buffer,
                GL2.GL_STATIC_DRAW
        );

        return handles[ 0 ];
    }

    private void appearance( GL2 gl ) {

        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, matSpecBufF);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SHININESS, matSpecBufF);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosBufF);
        errorCheck(gl, "Appearance");

    }

    private void errorCheck( GL2 gl, String tag ) {
        int err = gl.glGetError();
        if ( err != 0 ) {
            throw new RuntimeException( tag + " returned " + err );
        }

    }

}
