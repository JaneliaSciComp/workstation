package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import antlr.collections.impl.Vector;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.actor.ColorBox2DActor;
import org.janelia.it.workstation.ab2.actor.HorizontalDualSliderActor;
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
    private HorizontalDualSliderActor rangeSlider;

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
            setHoverable(true);
            logger.info(" --*-- actorId="+actorId);
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

        @Override
        public void setSelect() { } // do nothing

        @Override
        public void releaseHover() {
            //logger.info(">>> RELEASE HOVER");
            super.releaseHover();
        }

        @Override
        public void setHover() {
            //logger.info(">>> SET HOVER");
            super.setHover();
        }

    }

    private class ImageControlOpenCloseActor extends OpenCloseActor {

        public ImageControlOpenCloseActor(AB2ImageControlPanelRenderer renderer, int actorId, Vector3 v0, Vector3 v1,
                                          Vector4 foregroundColor, Vector4 backgroundColor, Vector4 hoverColor, Vector4 selectColor) {
            super(renderer, actorId, v0, v1, foregroundColor, backgroundColor, hoverColor, selectColor);
            setHoverable(true);
            setDisplay(false);
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

        @Override
        public void setSelect() { } // do nothing

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
        createRangeSlider();
        super.init(gl);
        initialized=true;
    }

    public void setOpen(boolean isOpen) {
        //logger.info("setOpen()="+isOpen);
        if (isOpen) {
            //logger.info("  -- opening, background hoverable=false");
            backgroundPanel.setColor(AB2Properties.IMAGE_CONTROL_PANEL_COLOR);
            backgroundPanel.setHoverColor(AB2Properties.IMAGE_CONTROL_PANEL_COLOR);
            backgroundPanel.setHoverable(false);
            openCloseActor.setDisplay(true);
            openCloseActor.setOpen(false);
            rangeSlider.setDisplay(true);
        } else {
            //logger.info("  -- closing, background hoverable=true");
            backgroundPanel.setColor(AB2Properties.IMAGE_CONTROL_PANEL_COLOR);
            backgroundPanel.setHoverColor(AB2Properties.IMAGE_CONTROL_PANEL_HOVER_COLOR);
            backgroundPanel.setHoverable(true);
            openCloseActor.setDisplay(false);
            openCloseActor.setOpen(true);
            rangeSlider.setDisplay(false);
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

        // open close
        Vector3[] openCloseVertices=getOpenCloseVertices(x, y, width, height, screenWidth, screenHeight);
        openCloseActor.updatePosition(openCloseVertices[0], openCloseVertices[1]);

        // range slider
        Vector3[] rangeVertices=getRangeSliderVertices(x, y, width, height, screenWidth, screenHeight);
        rangeSlider.updateVertices(rangeVertices[0], rangeVertices[1]);
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

    private Vector3[] getRangeSliderVertices(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight,
                AB2Properties.IMAGE_CONTROL_PANEL_BACKGROUND_Z);

        float h1=normed2dPositions[1].getY()-normed2dPositions[0].getY();
        float w1=normed2dPositions[1].getX()-normed2dPositions[0].getX();

        float ch=normed2dPositions[0].getY()+(h1/2.0f);
        float cw=normed2dPositions[0].getX()+(w1/2.0f);

        float h2=h1*0.5f;
        float w2=w1*0.5f;

        Vector3 v0=new Vector3(cw-w2/2f, ch-h2/2f, normed2dPositions[0].getZ()-0.1f);
        Vector3 v1=new Vector3( cw+w2/2f, ch+h2/2f, normed2dPositions[0].getZ()-0.1f);
        normed2dPositions[0]=v0;
        normed2dPositions[1]=v1;

        return normed2dPositions;
    }

    private void createRangeSlider() {
        Vector3[] rangeSliderVertices=getRangeSliderVertices(x, y, width, height, screenWidth, screenHeight);

        AB2Controller controller=AB2Controller.getController();
        rangeSlider = new HorizontalDualSliderActor(this, controller.getNextPickIndex(), controller.getNextPickIndex(),
                controller.getNextPickIndex(), rangeSliderVertices[0], rangeSliderVertices[1], AB2Properties.IMAGE_CONTROL_RANGE_SLIDER_BACKGROUND_COLOR,
                AB2Properties.IMAGE_CONTROL_RANGE_SLIDER_GUIDE_COLOR, AB2Properties.IMAGE_CONTROL_RANGE_SLIDER_SLIDER_COLOR,
                AB2Properties.IMAGE_CONTROL_RANGE_SLIDER_SLIDER_HOVER_COLOR);

        rangeSlider.setDisplay(false);
        rangeSlider.setHoverable(rangeSlider.getSlider1Id());
        rangeSlider.setHoverable(rangeSlider.getSlider2Id());
        rangeSlider.setSelectable(rangeSlider.getSlider1Id());
        rangeSlider.setSelectable(rangeSlider.getSlider2Id());
        rangeSlider.setDraggable(rangeSlider.getSlider1Id());
        rangeSlider.setDraggable(rangeSlider.getSlider2Id());

        panelDrawSequence.getActorSequence().add(rangeSlider);
        panelPickSequence.getActorSequence().add(rangeSlider);
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
