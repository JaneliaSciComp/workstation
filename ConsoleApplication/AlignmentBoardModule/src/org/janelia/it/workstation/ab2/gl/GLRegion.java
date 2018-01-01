package org.janelia.it.workstation.ab2.gl;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2EventHandler;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer;
import org.janelia.it.workstation.ab2.view.AB2SampleMainRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GLRegion implements GLSelectable {
    Logger logger = LoggerFactory.getLogger(GLRegion.class);

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

    public boolean containsPoint(Point point) {
        logger.info("containsPoint : pxy="+point.x+" "+point.y+" , x="+x+" y="+y+" width="+width+" height="+height);
        int xTest=point.x;
        if (xTest>=x && xTest<(x+width)) {
            int yTest=point.y;
            if (yTest>=y && yTest<(y+height)) {
                logger.info("containsPoint=true");
                return true;
            }
        }
        logger.info("containsPoint=false");
        return false;
    }

    protected abstract void reshape(GLAutoDrawable drawable);

    public void processEvent(AB2Event event) {}

    public void setHover(int actorId) {}

    public void releaseHover() {}

    public void setSelect() {}

    public void releaseSelect() {}

    public void setDrag() {}

    public void releaseDrag() {}

}
