package org.janelia.it.FlyWorkstation.gui.viewer3d.learning;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.buffering.VtxCoordBufMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureMediator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.nio.*;

public class Rubics implements GLActor {

    // This helps to track whether the class has been re-loaded by Android or not.
    private static int __class_instance = 0;
    {
        __class_instance++;
    }

    private VtxCoordBufMgr bufferManager;
    private FloatBuffer matSpecBufF;
    private FloatBuffer matShinBufF;
    private FloatBuffer lightPosBufF;

    public Rubics() {
        bufferManager = new VtxCoordBufMgr( true );
        TextureMediator simplifiedMediator = new TextureMediator() {
            private Double[] volMicro = new Double[] {
                    360.0, 223.0, 110.0
            };
            private Double[] voxMicro = new Double[] {
                    1.0, 1.0, 1.0
            };
            public Double[] getVolumeMicrometers() {
                return volMicro;
            }

            public Double[] getVoxelMicrometers() {
                return voxMicro;
            }
            public float[] textureCoordFromVoxelCoord(float[] voxelCoord) {
                float[] tc = {voxelCoord[0], voxelCoord[1], voxelCoord[2]}; // micrometers, origin at center
                int[] voxels = new int[]{ volMicro[0].intValue(), volMicro[1].intValue(), volMicro[2].intValue() };
                for (int i =0; i < 3; ++i) {
                    // Move origin to upper left corner
                    tc[i] += volMicro[i] / 2.0; // micrometers, origin at corner
                    // Rescale from micrometers to voxels
                    tc[i] /= voxMicro[i]; // voxels, origin at corner
                    // Rescale from voxels to texture units (range 0-1)
                    tc[i] /= voxels[i]; // texture units
                }

                return tc;
            }

        };
        bufferManager.setTextureMediator( simplifiedMediator );

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
//        new RectSolid().draw( gl );
        errorCheck( gl, "Before rubics draw...");
        appearance( gl );
        bufferManager.drawNoTex( gl, CoordinateAxis.Z, 1.0 );
        errorCheck(gl, "After all rubics Drawing");
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
            bufferManager.buildBuffers();
            bufferManager.enableBuffers(gl);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    @Override
    public void dispose(GL2 gl) {
        bufferManager.dropBuffers();
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
