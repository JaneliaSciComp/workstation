package org.janelia.it.workstation.ab2.renderer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.OrthographicCamera;

import org.janelia.it.workstation.ab2.event.AB2Event;

import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Renderer2D extends AB2RendererD {
    Logger logger = LoggerFactory.getLogger(AB2Renderer2D.class);

    protected OrthographicCamera camera2d;

    Matrix4 vp2d;

    public AB2Renderer2D(GLRegion parentRegion) {
        super(parentRegion);
        setBackgroundColorBuffer();
        resetView();
    }

    @Override
    protected void resetCamera() {
        camera2d = new OrthographicCamera(vantage, viewport);
    }

    public Matrix4 getVp2d() { return new Matrix4(vp2d); }

    @Override
    protected void resetVPMatrix() {
        Matrix4 projectionMatrix2d=new Matrix4(camera2d.getProjectionMatrix());
        Matrix4 viewMatrix2d=new Matrix4(camera2d.getViewMatrix());
        vp2d=viewMatrix2d.multiply(projectionMatrix2d);
    }

    @Override
    public void processEvent(AB2Event event) {
        super.processEvent(event);
    }

}
