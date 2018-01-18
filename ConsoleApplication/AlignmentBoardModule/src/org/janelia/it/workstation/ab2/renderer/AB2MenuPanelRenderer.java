package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.ColorBox2DActor;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2BlackModeRequestEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2ImageControlRequestOpenEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.event.AB2WhiteModeRequestEvent;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;

public class AB2MenuPanelRenderer extends AB2Renderer2D {

    private ColorBox2DActor backgroundPanel;
    private TextLabelActor ab2TextLabel;

    private ColorBoxButtonActor blackModeBox;
    private ColorBoxButtonActor whiteModeBox;

    private GLShaderActionSequence menuPanelDrawSequence;
    private GLShaderActionSequence menuPanelPickSequence;

    private GLShaderActionSequence labelSequence;

    private int x;
    private int y;
    private int width;
    private int height;
    private int screenWidth;
    private int screenHeight;

    private class ColorBoxButtonActor extends ColorBox2DActor {

        AB2Event signalEvent;

        public ColorBoxButtonActor(AB2MenuPanelRenderer renderer, int actorId, Vector3 v0, Vector3 v1,
                                                   Vector4 color, Vector4 hoverColor, Vector4 selectColor,
                                   AB2Event signalEvent) {
            super(renderer, actorId, v0, v1, color, hoverColor, selectColor);
            this.signalEvent=signalEvent;
            setHoverable(true);
        }

        @Override
        public void processEvent(AB2Event event) {
            super.processEvent(event);
            if (event instanceof AB2MouseClickedEvent) {
                AB2Controller.getController().processEvent(signalEvent);
            }
        }

    }



    public AB2MenuPanelRenderer(int x, int y, int width, int height, int screenWidth, int screenHeight, GLRegion parentRegion) {
        super(parentRegion);

        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        menuPanelDrawSequence = new GLShaderActionSequence(this.getClass().getName()+ " Draw");
        menuPanelDrawSequence.setShader(new AB2Basic2DShader());
        addDrawShaderActionSequence(menuPanelDrawSequence);

        menuPanelPickSequence = new GLShaderActionSequence(this.getClass().getName()+" Pick");
        menuPanelPickSequence.setShader(new AB2PickShader());
        addPickShaderActionSequence(menuPanelPickSequence);

        labelSequence = new GLShaderActionSequence(this.getClass().getName()+" Label");
        labelSequence.setShader(new AB2Text2DShader());
        addDrawShaderActionSequence(labelSequence);

    }

    @Override
    public void init(GL4 gl) {
        createBackgroundPanel(x, y, width, height, screenWidth, screenHeight);
        createAb2TextLabel(getAb2LabelX(), getAb2LabelY(), screenWidth, screenHeight);
        createBlackModeButton();
        createWhiteModeButton();
        super.init(gl);
        initialized=true;
    }

    private void createBlackModeButton() {
        int size=getModeButtonSize()/2;
        int xC=getBlackModeX();
        int yC=getBlackModeY();
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(xC-size, yC-size, size*2, size*2,
                screenWidth, screenHeight, AB2Properties.MENU_MODE_BUTTON_Z);
        blackModeBox=new ColorBoxButtonActor(this, AB2Controller.getController().getNextPickIndex(), normed2dPositions[0],
                normed2dPositions[1], AB2Properties.MENU_BLACK_MODE_COLOR, AB2Properties.MENU_BLACK_MODE_HOVER,
                AB2Properties.MENU_BLACK_MODE_COLOR, new AB2BlackModeRequestEvent());
        menuPanelDrawSequence.getActorSequence().add(blackModeBox);
        menuPanelPickSequence.getActorSequence().add(blackModeBox);
    }

    private void createWhiteModeButton() {
        int size=getModeButtonSize()/2;
        int xC=getWhiteModeX();
        int yC=getWhiteModeY();
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(xC-size, yC-size, size*2, size*2,
                screenWidth, screenHeight, AB2Properties.MENU_MODE_BUTTON_Z);
        whiteModeBox=new ColorBoxButtonActor(this, AB2Controller.getController().getNextPickIndex(), normed2dPositions[0],
                normed2dPositions[1], AB2Properties.MENU_WHITE_MODE_COLOR, AB2Properties.MENU_WHITE_MODE_HOVER,
                AB2Properties.MENU_WHITE_MODE_COLOR, new AB2WhiteModeRequestEvent());
        menuPanelDrawSequence.getActorSequence().add(whiteModeBox);
        menuPanelPickSequence.getActorSequence().add(whiteModeBox);
    }

    private int getAb2LabelX() {
        return x+25;
    }

    private int getAb2LabelY() {
        return y+(height/2);
    }

    private int getModeButtonSize() { return 20; }

    private int getBlackModeX() { return x+width-100; }
    private int getBlackModeY() { return y+(height/2); }

    private int getWhiteModeX() { return x+width-125; }
    private int getWhiteModeY() { return y+(height/2); }

    private void repositionModeButtons() {
        int size=getModeButtonSize()/2;

        int xBC=getBlackModeX();
        int yBC=getBlackModeY();

        int xWC=getWhiteModeX();
        int yWC=getWhiteModeY();

        //logger.info("x="+x+" y="+y+" width="+width+" height="+height+" xBC="+xBC+" yBC="+yBC+" screenWidth="+screenWidth+" screenHeight="+screenHeight);

        Vector3[] blackNormed2dPositions=getNormed2DPositionsFromScreenCoordinates(xBC-size, yBC-size, size*2, size*2,
                screenWidth, screenHeight, AB2Properties.MENU_MODE_BUTTON_Z);

        Vector3[] whiteNormed2dPositions=getNormed2DPositionsFromScreenCoordinates(xWC-size, yWC-size, size*2, size*2,
                screenWidth, screenHeight, AB2Properties.MENU_MODE_BUTTON_Z);

        blackModeBox.updateVertices(blackNormed2dPositions[0], blackNormed2dPositions[1]);
        whiteModeBox.updateVertices(whiteNormed2dPositions[0], whiteNormed2dPositions[1]);
    }


    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight, AB2Properties.MENU_BACKGROUND_Z);
        backgroundPanel.updateVertices(normed2dPositions[0], normed2dPositions[1]);

        Vector3 labelPosition=getNormedCenterPositionFromScreenCoordinates(getAb2LabelX(), getAb2LabelY(), screenWidth, screenHeight, AB2Properties.AB2_TEXT_LABEL_Z);
        ab2TextLabel.setCenterPosition(labelPosition);

        repositionModeButtons();
    }

    private void createAb2TextLabel(int x, int y, int screenWidth, int screenHeight) {
        Vector3 labelPosition=getNormedCenterPositionFromScreenCoordinates(x, y, screenWidth, screenHeight, AB2Properties.AB2_TEXT_LABEL_Z);
        ab2TextLabel=new TextLabelActor(this, AB2Controller.getController().getNextPickIndex(), "AB2",
                labelPosition, AB2Properties.AB2_TEXT_LABEL_FOREGROUND, AB2Properties.AB2_TEXT_LABEL_BACKGROUND, TextLabelActor.Orientation.NORMAL);
        labelSequence.getActorSequence().add(ab2TextLabel);
    }

    private void createBackgroundPanel(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector3[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight, AB2Properties.MENU_BACKGROUND_Z);

//        logger.info("backgroundPanel v0="+normed2dPositions[0].get(0)+" "+normed2dPositions[0].get(1)+" v1="+
//                normed2dPositions[1].get(0)+" "+normed2dPositions[1].get(1));

        backgroundPanel=new ColorBox2DActor(this, AB2Controller.getController().getNextPickIndex(),
                normed2dPositions[0], normed2dPositions[1], AB2Properties.MENU_COLOR, AB2Properties.MENU_HOVER_COLOR, AB2Properties.MENU_SELECT_COLOR);
        menuPanelDrawSequence.getActorSequence().add(backgroundPanel);
    }

}
