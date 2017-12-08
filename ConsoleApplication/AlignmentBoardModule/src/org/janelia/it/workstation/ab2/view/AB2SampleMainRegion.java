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

    int w0;
    int h0;
    int x0;
    int y0;
    int w1;
    int h1;
    int w2;
    int h2;
    float scale;
    int xCenter;
    int yCenter;
    float xCenterFraction;
    float yCenterFraction;
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
        // OpenGL will center and clip the smaller of the screen dimensions, so we need to find the size of the
        // virtual square we are working with.

        // Initially, assume screenWidth>screenHeight
        w0=screenWidth;
        h0=screenWidth;

        // We need to create virtual pixel position on the virtual square pixel field
        x0=x;
        int yDownFromMiddle=screenHeight/2-y;
        y0=screenWidth/2-yDownFromMiddle;

        // Deal with screenHeight>screenWidth
        if (screenHeight>screenWidth) {
            w0=screenHeight;
            h0=screenHeight;
            y0=y;
            int xDownFromMiddle=screenWidth/2-x;
            x0=screenHeight/2-xDownFromMiddle;
        }

        // The translation, to line up correctly, needs first to take into account scale, so we do scale first.

        // Because we want the dimensions for the main region to be square, we will take the smaller of the two
        w1=height;
        h1=height;
        w2=width;
        h2=width;
        if (height<width) {
            w1=width;
            h1=width;
            w2=height;
            h2=height;
        }

        // For scale, we want the fraction of total, using the result which is smallest
        scale=(float)((1.0*w2)/(1.0*w0));

        // Now base translation on virtual square coordinates
        xCenter=x0+w1/2;
        yCenter=y0+w1/2;

        xCenterFraction=(float)((1.0*xCenter)/(1.0*w0));
        yCenterFraction=(float)((1.0*yCenter)/(1.0*w0));

        xTranslate=2.0f*xCenterFraction-1.0f;
        yTranslate=2.0f*yCenterFraction-1.0f;

        logger.info("x="+x+" y="+y+" width="+width+" height="+height+" screenWidth="+screenWidth+" screenHeight="+screenHeight);

        logger.info("x0="+x0+" y0="+y0);
        logger.info("w0="+w0+" h0="+h0);
        logger.info("w1="+w1+" h1="+h1);
        logger.info("w2="+w2+" h2="+h2);

        logger.info("scale="+scale+" xCenter="+xCenter+" yCenter="+yCenter);
        logger.info("xCenterFraction="+xCenterFraction+" yCenterFraction="+yCenterFraction);
        logger.info("xTranslate="+xTranslate+" yTranslate="+yTranslate);
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
