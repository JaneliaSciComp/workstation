package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.it.workstation.ab2.actor.ColorBox2DActor;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;

public class AB2MenuPanelRenderer extends AB2Renderer2D {

    private ColorBox2DActor backgroundPanel;
    private TextLabelActor ab2TextLabel;

    private GLShaderActionSequence menuPanelDrawSequence;
    private GLShaderActionSequence menuPanelPickSequence;

    private GLShaderActionSequence labelSequence;

    private int x;
    private int y;
    private int width;
    private int height;
    private int screenWidth;
    private int screenHeight;

    public AB2MenuPanelRenderer(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        super();

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
        super.init(gl);
        initialized=true;
    }

    private int getAb2LabelX() {
        return x+50;
    }

    private int getAb2LabelY() {
        return y+(height/2);
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        Vector2[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight);
        backgroundPanel.updateVertices(normed2dPositions[0], normed2dPositions[1]);

        Vector2 labelPosition=getNormedCenterPositionFromScreenCoordinates(getAb2LabelX(), getAb2LabelY(), screenWidth, screenHeight);
        ab2TextLabel.setCenterPosition(labelPosition);

    }

    private void createAb2TextLabel(int x, int y, int screenWidth, int screenHeight) {
        Vector2 labelPosition=getNormedCenterPositionFromScreenCoordinates(x, y, screenWidth, screenHeight);
        ab2TextLabel=new TextLabelActor(this, AB2Controller.getController().getNextPickIndex(), "AB2",
                labelPosition, AB2Properties.AB2_TEXT_LABEL_FOREGROUND, AB2Properties.AB2_TEXT_LABEL_BACKGROUND, TextLabelActor.Orientation.NORMAL);
        labelSequence.getActorSequence().add(ab2TextLabel);
    }

    private void createBackgroundPanel(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector2[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight);

        logger.info("backgroundPanel v0="+normed2dPositions[0].get(0)+" "+normed2dPositions[0].get(1)+" v1="+
                normed2dPositions[1].get(0)+" "+normed2dPositions[1].get(1));

        backgroundPanel=new ColorBox2DActor(this, AB2Controller.getController().getNextPickIndex(),
                normed2dPositions[0], normed2dPositions[1], AB2Properties.MENU_COLOR, AB2Properties.MENU_HOVER_COLOR, AB2Properties.MENU_SELECT_COLOR);
        menuPanelDrawSequence.getActorSequence().add(backgroundPanel);
    }

}
