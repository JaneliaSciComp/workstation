package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.actor.ColorBox2DActor;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.event.AB2RegionManagerResizeNeededEvent;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2ImageControlPanelRenderer extends AB2Renderer2D {

    Logger logger = LoggerFactory.getLogger(AB2ImageControlPanelRenderer.class);

    private ImageControlBackgroundColorBoxActor backgroundPanel;

    private GLShaderActionSequence panelDrawSequence;
    private GLShaderActionSequence panelPickSequence;

    private int x;
    private int y;
    private int width;
    private int height;
    private int screenWidth;
    private int screenHeight;

    private boolean isOpen=false;

    private class ImageControlBackgroundColorBoxActor extends ColorBox2DActor {

        public ImageControlBackgroundColorBoxActor(AB2Renderer2D renderer, int actorId, Vector3 v0, Vector3 v1,
                                                   Vector4 color, Vector4 hoverColor, Vector4 selectColor) {
            super(renderer, actorId, v0, v1, color, hoverColor, selectColor);
        }

        @Override
        public void processEvent(AB2Event event) {
            //logger.info("ImageControlBackgroundColorBoxActor - processEvent() event="+event.getClass().getName());
            super.processEvent(event);
            if (event instanceof AB2MouseClickedEvent) {
                renderer.processEvent(event);
            }
        }
    }

    public AB2ImageControlPanelRenderer(int x, int y, int width, int height, int screenWidth, int screenHeight, GLRegion parentRegion) {
        super(parentRegion);

        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        panelDrawSequence = new GLShaderActionSequence(this.getClass().getName()+ " Draw");
        panelDrawSequence.setShader(new AB2Basic2DShader());
        addDrawShaderActionSequence(panelDrawSequence);

        panelPickSequence = new GLShaderActionSequence(this.getClass().getName()+" Pick");
        panelPickSequence.setShader(new AB2PickShader());
        addPickShaderActionSequence(panelPickSequence);

    }

    @Override
    public void init(GL4 gl) {
        createBackgroundPanel(x, y, width, height, screenWidth, screenHeight);
        super.init(gl);
        initialized=true;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen=isOpen;
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight,
                AB2Properties.IMAGE_CONTROL_PANEL_BACKGROUND_Z);
        backgroundPanel.updateVertices(normed2dPositions[0], normed2dPositions[1]);

    }

    private void createBackgroundPanel(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight,
                AB2Properties.IMAGE_CONTROL_PANEL_BACKGROUND_Z);

        //logger.info("backgroundPanel v0="+normed2dPositions[0].get(0)+" "+normed2dPositions[0].get(1)+" v1="+
        //        normed2dPositions[1].get(0)+" "+normed2dPositions[1].get(1));

        backgroundPanel=new ImageControlBackgroundColorBoxActor(this, AB2Controller.getController().getNextPickIndex(),
                normed2dPositions[0], normed2dPositions[1], AB2Properties.IMAGE_CONTROL_PANEL_COLOR,
                AB2Properties.IMAGE_CONTROL_PANEL_HOVER_COLOR, AB2Properties.IMAGE_CONTROL_PANEL_SELECT_COLOR);
        panelDrawSequence.getActorSequence().add(backgroundPanel);
        panelPickSequence.getActorSequence().add(backgroundPanel);
    }

    @Override
    public void processEvent(AB2Event event) {
        //logger.info("processEvent() event="+event.getClass().getName());
        super.processEvent(event);
        if (event instanceof AB2MouseClickedEvent) {
            parentRegion.processEvent(event);
        }
    }

}