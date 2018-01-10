package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.actor.ColorBox2DActor;
import org.janelia.it.workstation.ab2.actor.OpenCloseActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2ImageControlRequestCloseEvent;
import org.janelia.it.workstation.ab2.event.AB2ImageControlRequestOpenEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2ImageControlPanelRenderer extends AB2Renderer2D {

    Logger logger = LoggerFactory.getLogger(AB2ImageControlPanelRenderer.class);

    private ImageControlBackgroundColorBoxActor backgroundPanel;
    private OpenCloseActor openCloseActor;

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

        public ImageControlBackgroundColorBoxActor(AB2ImageControlPanelRenderer renderer, int actorId, Vector3 v0, Vector3 v1,
                                                   Vector4 color, Vector4 hoverColor, Vector4 selectColor) {
            super(renderer, actorId, v0, v1, color, hoverColor, selectColor);
        }

        @Override
        public void processEvent(AB2Event event) {
            //logger.info("ImageControlBackgroundColorBoxActor - processEvent() event="+event.getClass().getName());
            AB2ImageControlPanelRenderer imageControlPanelRenderer=(AB2ImageControlPanelRenderer)renderer;
            super.processEvent(event);
            if (event instanceof AB2MouseClickedEvent) {
                if (!imageControlPanelRenderer.isOpen()) {
                    renderer.processEvent(new AB2ImageControlRequestOpenEvent());
                }
            }
        }
    }

    private class ImageControlOpenCloseActor extends OpenCloseActor {

        public ImageControlOpenCloseActor(AB2ImageControlPanelRenderer renderer, int actorId, Vector3 v0, Vector3 v1,
                                          Vector4 foregroundColor, Vector4 backgroundColor, Vector4 hoverColor, Vector4 selectColor) {
            super(renderer, actorId, v0, v1, foregroundColor, backgroundColor, hoverColor, selectColor);
        }

        @Override
        public void processEvent(AB2Event event) {
            //logger.info("ImageControlBackgroundColorBoxActor - processEvent() event="+event.getClass().getName());
            AB2ImageControlPanelRenderer imageControlPanelRenderer=(AB2ImageControlPanelRenderer)renderer;
            super.processEvent(event);
            if (event instanceof AB2MouseClickedEvent) {
                if (imageControlPanelRenderer.isOpen()) {
                    renderer.processEvent(new AB2ImageControlRequestCloseEvent());
                }
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
        createOpenCloseActor();
        super.init(gl);
        initialized=true;
    }

    public void setOpen(boolean isOpen) {
        if (isOpen) {
            backgroundPanel.setColor(AB2Properties.IMAGE_CONTROL_PANEL_COLOR);
            //backgroundPanel.setColor(new Vector4(1.0f, 0.0f, 0.0f, 1.0f));
            backgroundPanel.setHoverColor(AB2Properties.IMAGE_CONTROL_PANEL_COLOR);
            backgroundPanel.setSelectable(false);
            openCloseActor.setDisplay(true);
            openCloseActor.setOpen(false);
            openCloseActor.setSelectable(true);
        } else {
            backgroundPanel.setColor(AB2Properties.IMAGE_CONTROL_PANEL_COLOR);
            backgroundPanel.setHoverColor(AB2Properties.IMAGE_CONTROL_PANEL_HOVER_COLOR);
            backgroundPanel.setSelectable(true);
            openCloseActor.setDisplay(false);
            openCloseActor.setOpen(true);
            openCloseActor.setSelectable(false);
        }
        this.isOpen=isOpen;
    }

    public boolean isOpen() { return isOpen; }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        // backgroundPanel
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight,
                AB2Properties.IMAGE_CONTROL_PANEL_BACKGROUND_Z);
        backgroundPanel.updateVertices(normed2dPositions[0], normed2dPositions[1]);

        Vector3[] openCloseVertices=getOpenCloseVertices(x, y, width, height, screenWidth, screenHeight);
        openCloseActor.updatePosition(openCloseVertices[0], openCloseVertices[1]);
    }

    private Vector3[] getOpenCloseVertices(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector3[] result=new Vector3[2];

        float widthFraction = (float)(AB2Properties.IMAGE_CONTROL_OPENCLOSE_PIXEL_SIZE * 1.0 / (1.0 * screenWidth));
        float heightFraction = (float)(AB2Properties.IMAGE_CONTROL_OPENCLOSE_PIXEL_SIZE * 1.0 / (1.0 * screenHeight));

        Vector3 v0=new Vector3( ((x+width)*1.0f)/(1.0f*screenWidth) - widthFraction, ((y+height)*1.0f)/(1.0f*screenHeight) - heightFraction,
                AB2Properties.IMAGE_CONTROL_OPENCLOSE_Z);
        Vector3 v1=new Vector3( ((x+width)*1.0f)/(1.0f*screenWidth), ((y+height)*1.0f)/(1.0f*screenHeight), AB2Properties.IMAGE_CONTROL_OPENCLOSE_Z);

        //logger.info("getOpenCloseVertices() v0="+v0.getX()+" "+v0.getY()+" "+v0.getZ()+", v1="+v1.getX()+" "+v1.getY()+" "+v1.getZ());

        result[0]=v0;
        result[1]=v1;

        return result;
    }

    private void createOpenCloseActor() {

        Vector3[] openCloseVertices=getOpenCloseVertices(x, y, width, height, screenWidth, screenHeight);

        openCloseActor=new ImageControlOpenCloseActor(this, AB2Controller.getController().getNextPickIndex(), openCloseVertices[0],
                openCloseVertices[1], AB2Properties.IMAGE_CONTROL_OPENCLOSE_FOREGROUND_COLOR,
                AB2Properties.IMAGE_CONTROL_OPENCLOSE_BACKGROUND_COLOR,
                AB2Properties.IMAGE_CONTROL_OPENCLOSE_HOVER_COLOR, AB2Properties.IMAGE_CONTROL_OPENCLOSE_SELECT_COLOR);
        panelDrawSequence.getActorSequence().add(openCloseActor);
        panelPickSequence.getActorSequence().add(openCloseActor);
        }

    private void createBackgroundPanel(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight,
                AB2Properties.IMAGE_CONTROL_PANEL_BACKGROUND_Z);

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
        if (event instanceof AB2ImageControlRequestOpenEvent) {
            parentRegion.processEvent(event);
        } else if (event instanceof AB2ImageControlRequestCloseEvent) {
            parentRegion.processEvent(event);
        }
    }

}
