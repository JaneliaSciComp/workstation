package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.nio.IntBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.MatrixManager;

/**
 * Carries out the display task for a Generic Vertex Pointer-using class.
 * @author fosterl
 */
public class GenericVPHelper {
    private MeshViewContext context;
    private String reportString;
    public GenericVPHelper(MeshViewContext context, String reportString) {
        this.context = context;
        this.reportString = reportString;
    }
    
    public void display(
            GLAutoDrawable glDrawable, 
            DirectionalReferenceAxesShader shader, 
            MatrixManager matrixManager,
            int lineBufferHandle,
            int colorBufferHandle, 
            int inxBufferHandle, 
            int lineBufferVertexCount) {
        int previousShader = -1;
        IntBuffer gpuToCpuBuffer = IntBuffer.allocate(1);

        GL2 gl = glDrawable.getGL().getGL2();
        reportError(gl, "Display of %s upon entry");

        // Exchange shader programs.
        gpuToCpuBuffer.rewind();
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, gpuToCpuBuffer);
        if (reportError(gl, "Display of %s get-current-program")) {
            return;
        }
        gpuToCpuBuffer.rewind();
        previousShader = gpuToCpuBuffer.get();
        gl.glUseProgram(shader.getShaderProgram());
        if (reportError(gl, "Display of %s use-program")) {
            return;
        }

        // Rendering characteristics / 'draw' state.
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CW);
        if (reportError(gl, String.format("Display of %s cull-face", reportString))) {
            return;
        }

        if (reportError(gl, String.format("Display of %s lighting 1", reportString))) {
            return;
        }
        gl.glShadeModel(GL2.GL_FLAT);
        if (reportError(gl, String.format("Display of %s lighting 2", reportString))) {
            return;
        }
        gl.glDisable(GL2.GL_LIGHTING);
        if (reportError(gl, String.format("Display of %s lighting 3", reportString))) {
            return;
        }
        setRenderMode(gl, true);

        gl.glEnable(GL2.GL_LINE_SMOOTH);                     // May not be in v2
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);   // May not be in v2

        gl.glLineWidth(1.0f);
        if (reportError(gl, String.format("Display of %s end characteristics", reportString))) {
            return;
        }

        // Deal with positioning matrices here, rather than in the renderer.
        matrixManager.recalculate(gl);

        shader.setUniformMatrix4v(gl, DirectionalReferenceAxesShader.PROJECTION_UNIFORM_NAME, false, context.getPerspectiveMatrix());
        shader.setUniformMatrix4v(gl, DirectionalReferenceAxesShader.MODEL_VIEW_UNIFORM_NAME, false, context.getModelViewMatrix());
        if (reportError(gl, String.format("Display of %s uniforms", reportString))) {
            return;
        }

        // Draw the little lines.
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, lineBufferHandle);
        if (reportError(gl, String.format("Display of %s 1", reportString))) {
            return;
        }

        // 3 floats per coord. Stride is 0, offset to first is 0.
        gl.glEnableVertexAttribArray(shader.getVertexAttribLoc());
        gl.glVertexAttribPointer(shader.getVertexAttribLoc(), 3, GL2.GL_FLOAT, false, 0, 0);
        if (reportError(gl, String.format("Display of %s 3", reportString))) {
            return;
        }

        // Color the little lines.
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferHandle);
        if (reportError(gl, String.format("Display of %s 3a", reportString))) {
            return;
        }

        // 4 byte values per color/coord.  Stride is 0, offset to first is 0.
        gl.glEnableVertexAttribArray(shader.getColorAttribLoc());
        gl.glVertexAttribPointer(shader.getColorAttribLoc(), 4, GL2.GL_UNSIGNED_BYTE, true, 0, 0);
        if (reportError(gl, String.format("Display of %s 3b", reportString))) {
            return;
        }

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        if (reportError(gl, String.format("Display of %s 4.", reportString))) {
            return;
        }

        gl.glDrawElements(GL2.GL_LINES, lineBufferVertexCount, GL2.GL_UNSIGNED_INT, 0);
        if (reportError(gl, String.format("Display of %s 5", reportString))) {
            return;
        }

        // Tear down 'draw' state.
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);   // Prob: not in v2
        gl.glDisable(GL2.GL_LINE_SMOOTH);               // May not be in v2
        if (reportError(gl, String.format("Display of %s 6", reportString))) {
            return;
        }

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

        setRenderMode(gl, false);
        if (reportError(gl, String.format("%s, end of display.", reportString))) {
            return;
        }

        // Switch back to previous shader.
        gl.glUseProgram(previousShader);

    }

    private void setRenderMode(GL2 gl, boolean enable) {
        if (enable) {
            // set blending to enable transparent voxels
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LESS);
            reportError(gl, String.format("Display of %s depth", reportString));
        } else {
            gl.glDisable(GL2.GL_DEPTH_TEST);
        }
    }
}
