package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2EventHandler;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer;

public abstract class GLRegion implements GLSelectable {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int screenWidth;
    protected int screenHeight;

    protected int minimumWidth=0;
    protected int minimumWHeight=0;

    protected List<AB2Renderer> renderers=new ArrayList<>();

    public void setMinimumDimensions(int mininumWidth, int minimumHeight) {
        this.minimumWidth=mininumWidth;
        this.minimumWHeight=minimumHeight;
    }

    public void init(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        for (AB2Renderer renderer : renderers) {
            renderer.init(gl);
        }
    }

    public void dispose(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        for (AB2Renderer renderer : renderers) {
            renderer.dispose(gl);
        }
    }

    public void display(GLAutoDrawable drawable) {
        if ( (minimumWidth>0 && width<minimumWidth) ||
                (minimumWHeight>0 && height<minimumWHeight) ) {
            return;
        }
        final GL4 gl=drawable.getGL().getGL4();
        for (AB2Renderer renderer : renderers) {
            renderer.display(gl);
        }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;
        reshape(drawable);
    }

    protected abstract void reshape(GLAutoDrawable drawable);

    public static float[] computeOffsetParameters(int x, int y, int width, int height, int screenWidth, int screenHeight) {
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
        float[] parameters=new float[3];

        double xfr = (1.0 * x)/(1.0 * screenWidth);
        double yfr = (1.0 * y)/(1.0 * screenHeight);

        double xns = xfr * 2.0 - 1.0;
        double yns = yfr * 2.0 - 1.0;

        double xlr = (1.0 * width) / (1.0 * screenWidth);
        double ylr = (1.0 * height) / (1.0 * screenHeight);

        // Note: because there is a X2 for nsc, and then a 1/2 for half length, these cancel, can just use xlr,ylr
        float xTranslate = (float)(xns + xlr);
        float yTranslate = (float)(yns + ylr);

        double widthScale = (1.0 * width) / (1.0 * screenWidth);
        double heightScale = (1.0 * height) / (1.0 * screenHeight);

        float scale = (float)widthScale;
        if (heightScale < scale) {
            scale = (float)heightScale;
        }

        parameters[0]=xTranslate;
        parameters[1]=yTranslate;
        parameters[2]=scale;
        return parameters;
    }


    public static Matrix4 getOffsetPostProjectionMatrix(float xTranslate, float yTranslate, float scale) {
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
        Matrix4 prMatrix=scaleMatrix.multiply(translationMatrix);
        return prMatrix;
    }

    public static int[] getXYBounds(int x, int y, int width, int height) {
        int bX0=x;
        int bY0=y;
        int bX1=x+width;
        int bY1=y+height;
        return new int[] { bX0, bY0, bX1, bY1 };
    }

    public void processEvent(AB2Event event) {}

    public void setHover(int actorId) {}

    public void releaseHover() {}

}
