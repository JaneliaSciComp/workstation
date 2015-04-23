/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.axes.Geometry;

/**
 * Actor which shows a trio of vertex-joined rays, indicating what direction the
 * user has left the main view, up to the current time.
 *
 * @author fosterl
 */
public class DirectionalReferenceAxesActor implements GLActor {

    private BoundingBox3d boundingBox;
    private boolean bBuffersNeedUpload = true;
    private VolumeModel volumeModel;
    private int lineBufferVertexCount = 0;
    private int previousShader;

    private int inxBufferHandle;
    private int lineBufferHandle;
    
    private int vertexAttribLoc;
    private int shaderLoc = -20;

    private int[] handleArr = new int[1];

    public enum Placement {

        TOP_LEFT, BOTTOM_LEFT, TOP_RIGHT, BOTTOM_RIGHT
    }

    public DirectionalReferenceAxesActor(
            float[] screenSize,
            BoundingBox3d parentBoundingBox,
            VolumeModel volumeModel,
            Placement placement) {

        // Closest to viewer, on the volume.
        this.volumeModel = volumeModel;
        
        createBoundingBox(placement, parentBoundingBox, screenSize);
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {

        GL2 gl = glDrawable.getGL().getGL2();
        reportError(gl, "Display of axes-actor upon entry");
        IntBuffer ib = IntBuffer.allocate(1);
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, ib);
        previousShader = ib.get();
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CW);
        reportError(gl, "Display of axes-actor cull-face");

        reportError(gl, "Display of axes-actor lighting 1");
        gl.glShadeModel(GL2.GL_FLAT);
        reportError(gl, "Display of axes-actor lighting 2");
        gl.glDisable(GL2.GL_LIGHTING);
        reportError(gl, "Display of axes-actor lighting 3");
        setRenderMode(gl, true);

        gl.glEnable(GL2.GL_LINE_SMOOTH);                     // May not be in v2
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);   // May not be in v2

        // Draw the little lines.
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, lineBufferHandle);
        reportError(gl, "Display of axes-actor 1");

        float alpha = 1.0f;
        float[] color = new float[3];
        if (volumeModel.isWhiteBackground()) {
            color[ 0] = 1.0f;
            color[ 1] = 0.85f;
            color[ 2] = 0.85f;
        } else {
            color[ 0] = 0.30f;
            color[ 1] = 0.15f;
            color[ 2] = 0.15f;
        }

        gl.glLineWidth(1.0f);
        gl.glColor4f(color[ 0 ], color[ 1 ], color[ 2 ], alpha);
        reportError(gl, "Display of axes-actor 2");

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);  // Prob: not in v2.
        reportError(gl, "Display of axes-actor 3");

        // 3 floats per coord. Stride is 0, offset to first is 0.
        gl.glEnableVertexAttribArray( lineBufferHandle );
        gl.glVertexAttribPointer(vertexAttribLoc, 3, GL2.GL_FLOAT, false, 0, 0);
        reportError(gl, "Display of axes-actor 4");

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        reportError(gl, "Display of axes-actor 4a.");

        gl.glDrawElements(GL2.GL_LINES, lineBufferVertexCount, GL2.GL_UNSIGNED_INT, 0);
        reportError(gl, "Display of axes-actor 5");

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);   // Prob: not in v2
        gl.glDisable(GL2.GL_LINE_SMOOTH);               // May not be in v2
        reportError(gl, "Display of axes-actor 6");

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

        setRenderMode(gl, false);
        reportError(gl, "Axes-actor, end of display.");

        gl.glUseProgram(previousShader);

    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();

        if (bBuffersNeedUpload) {
            try {
                // Uploading buffers sufficient to draw the axes, ticks, etc.
                buildBuffers(gl);

                bBuffersNeedUpload = false;
                vertexAttribLoc = gl.glGetAttribLocation(shaderLoc, "vertexAttribute");
            } catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }

    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        bBuffersNeedUpload = true;
    }

    private void buildBuffers(GL2 gl) {
        Geometry axisGeometries = new Geometry(createAxisGeometry(), createAxisIndices(0));
        // Get these buffers.
        IntBuffer indexBuf = bufferAxisIndices(axisGeometries);
        FloatBuffer vtxBuf = bufferAxisGeometry(axisGeometries);
        lineBufferVertexCount = vtxBuf.capacity();

        // Now push them over to GPU.
        lineBufferHandle = createBufferHandle(gl);
        inxBufferHandle = createBufferHandle(gl);

        // Bind data to the handle, and upload it to the GPU.
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, lineBufferHandle);
        reportError(gl, "Bind buffer");
        gl.glBufferData(
                GL2.GL_ARRAY_BUFFER,
                (long) (vtxBuf.capacity() * (Float.SIZE / 8)),
                vtxBuf,
                GL2.GL_STATIC_DRAW
        );
        reportError(gl, "Buffer Data");

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        reportError(gl, "Bind Inx Buf");

        gl.glBufferData(
                GL2.GL_ELEMENT_ARRAY_BUFFER,
                (long) (indexBuf.capacity() * (Integer.SIZE / 8)),
                indexBuf,
                GL2.GL_STATIC_DRAW
        );
    }

    private void createBoundingBox(Placement placement, BoundingBox3d parentBoundingBox, float[] screenSize) {
        double minZ = parentBoundingBox.getMinZ();
        boundingBox = new BoundingBox3d();
        switch (placement) {
            case TOP_LEFT:
                boundingBox.include(new Vec3(parentBoundingBox.getMinX(), parentBoundingBox.getMaxY(), minZ));
                boundingBox.include(new Vec3(parentBoundingBox.getMinX() + screenSize[0], parentBoundingBox.getMaxY() - screenSize[1], minZ));
                break;
            case TOP_RIGHT:
                boundingBox.include(new Vec3(parentBoundingBox.getMaxX(), parentBoundingBox.getMaxY(), minZ));
                boundingBox.include(new Vec3(parentBoundingBox.getMaxX() - screenSize[0], parentBoundingBox.getMaxY() - screenSize[1], minZ));
                break;
            case BOTTOM_LEFT:
                boundingBox.include(new Vec3(parentBoundingBox.getMinX(), parentBoundingBox.getMinY(), minZ));
                boundingBox.include(new Vec3(parentBoundingBox.getMinX() + screenSize[0], parentBoundingBox.getMinY() + screenSize[1], minZ));
                break;
            case BOTTOM_RIGHT:
                boundingBox.include(new Vec3(parentBoundingBox.getMaxX(), parentBoundingBox.getMinY(), minZ));
                boundingBox.include(new Vec3(parentBoundingBox.getMaxX() - screenSize[0], parentBoundingBox.getMinY() + screenSize[1], minZ));
                break;
            default:
                break;
        }
    }

    private int createBufferHandle(GL2 gl) {
        gl.glGenBuffers(1, handleArr, 0);
        return handleArr[ 0];
    }

    private float[] createAxisGeometry() {
        // 3 coords per point; 2 points per axis; 3 axes.
        float[] rtnVal = new float[3 * 2 * 3];
        // Handle X axis.
        rtnVal[0] = (float) boundingBox.getMinX();
        rtnVal[1] = (float) boundingBox.getMinY();
        rtnVal[2] = (float) boundingBox.getMinZ();

        rtnVal[3] = (float) boundingBox.getMaxX();
        rtnVal[4] = (float) boundingBox.getMinY();
        rtnVal[5] = (float) boundingBox.getMinZ();

        // Handle Y axis.
        rtnVal[6] = (float) boundingBox.getMinX();
        rtnVal[7] = (float) boundingBox.getMinY();
        rtnVal[8] = (float) boundingBox.getMinZ();

        rtnVal[9] = (float) boundingBox.getMinX();
        rtnVal[10] = (float) boundingBox.getMaxY();
        rtnVal[10] = (float) boundingBox.getMinZ();

        // Handle Z axis.
        rtnVal[11] = (float) boundingBox.getMinX();
        rtnVal[12] = (float) boundingBox.getMinY();
        rtnVal[13] = (float) boundingBox.getMinZ();

        rtnVal[14] = (float) boundingBox.getMinX();
        rtnVal[15] = (float) boundingBox.getMinY();
        rtnVal[16] = (float) boundingBox.getMaxZ();
        return rtnVal;
    }

    private int[] createAxisIndices(int indexOffset) {
        // 2 coord sets per line; 3 axis lines
        int coordsPerAxis = 3 * 2;
        int[] indices = new int[coordsPerAxis];
        for (int i = 0; i < 3; i++) {
            indices[ 2 * i] = 2 * i + indexOffset;
            indices[ 2 * i + 1] = 2 * i + 1 + indexOffset;
        }

        return indices;
    }

    private FloatBuffer bufferAxisGeometry(Geometry axisGeometries) {
        float[] axisGeometry = axisGeometries.getCoords();
        ByteBuffer bb = ByteBuffer.allocateDirect(axisGeometry.length * Float.SIZE / 8);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(axisGeometry);
        fb.rewind();

        return fb;
    }

    private IntBuffer bufferAxisIndices(Geometry axisGeometries) {
        int[] indices = axisGeometries.getIndices();
        ByteBuffer bb = ByteBuffer.allocateDirect(indices.length * Integer.SIZE / 8);
        bb.order(ByteOrder.nativeOrder());
        IntBuffer ib = bb.asIntBuffer();
        ib.put(indices);
        ib.rewind();

        return ib;
    }

    private void setRenderMode(GL2 gl, boolean enable) {
        if (enable) {
            // set blending to enable transparent voxels
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LESS);
            reportError(gl, "Display of axes-actor depth");
        } else {
            gl.glDisable(GL2.GL_DEPTH_TEST);
        }
    }
}
