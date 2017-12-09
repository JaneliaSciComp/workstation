package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2SampleRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleMainRegion extends GLRegion {
    Logger logger = LoggerFactory.getLogger(AB2SampleMainRegion.class);


    private AB2SampleRenderer sampleRenderer=new AB2SampleRenderer();

    public AB2SampleMainRegion() {
        renderers.add(sampleRenderer);
    }

    public AB2SampleRenderer getSampleRenderer() {
        return sampleRenderer;
    }

    float scale;
    float xTranslate;
    float yTranslate;

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        final GL4 gl=drawable.getGL().getGL4();
        // todo: fix this to use proper values for sampleRenderer.reshape()
        sampleRenderer.reshape(gl, x, y, screenWidth, screenHeight, screenWidth, screenHeight);
        computeOffsetParameters(x, y, width, height, screenWidth, screenHeight);
        sampleRenderer.setVoxel3DActorPostRotationalMatrix(getPrMatrix(x, y, width, height, screenWidth, screenHeight));
        int[] xyBounds=getXYBounds(x, y, width, height, screenWidth, screenHeight);
        sampleRenderer.setVoxel3DxyBounds(xyBounds[0], xyBounds[1], xyBounds[2], xyBounds[3]);
    }

    private void computeOffsetParameters(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        // OpenGL expands the -1,1 range for X,Y to cover the entire screen regardless of aspect ratio, so
        // since we are adding a "final stage" transform, we need to keep this in mind.

        // Here is our strategy:
        //
        // 1 - Renormalize the target positions for each axis, translating pixels to the -1,1 GL XY range, for
        //     both the lower-left position of the desired box, and the X and Y lengths of each side.
        //
        // 2 - Using the 1/2 distance of each side length in normalized screen coordinates, add this to the target
        //     lower-left positions to get the actual center point of the box.
        //
        // 3 - Compute the translation components to recenter the image on the center of the target box.
        //
        // 4 - We want the scale to be smaller of either case, by comparing the scale difference between
        //     using one aspect side or the other.

        double xfr = (1.0 * x)/(1.0 * screenWidth);
        double yfr = (1.0 * y)/(1.0 * screenHeight);

        double xns = xfr * 2.0 - 1.0;
        double yns = yfr * 2.0 - 1.0;

        double xlr = (1.0 * width) / (1.0 * screenWidth);
        double ylr = (1.0 * height) / (1.0 * screenHeight);

        // Note: because there is a X2 for nsc, and then a 1/2 for half length, these cancel, can just use xlr,ylr
        xTranslate = (float)(xns + xlr);
        yTranslate = (float)(yns + ylr);

        double widthScale = (1.0 * width) / (1.0 * screenWidth);
        double heightScale = (1.0 * height) / (1.0 * screenHeight);

        scale = (float)widthScale;
        if (heightScale < scale) {
            scale = (float)heightScale;
        }

    }


    public Matrix4 getPrMatrix(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Matrix4 translationMatrix = new Matrix4();
        translationMatrix.set(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                xTranslate, yTranslate, 0.0f, 1.0f);
        Matrix4 scaleMatrix = new Matrix4();
        scaleMatrix.set(
                scale, 0.0f, 0.0f, 0.0f,
                0.0f, scale, 0.0f, 0.0f,
                0.0f, 0.0f, scale, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f);
        //Matrix4 modelMatrix=translationMatrix.multiply(scaleMatrix); - NOT CORRECT
        Matrix4 modelMatrix=scaleMatrix.multiply(translationMatrix);

        return modelMatrix;
    }

    public int[] getXYBounds(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        int bX0=x;
        int bY0=y;
        int bX1=x+width;
        int bY1=y+height;
        return new int[] { bX0, bY0, bX1, bY1 };
    }

}
