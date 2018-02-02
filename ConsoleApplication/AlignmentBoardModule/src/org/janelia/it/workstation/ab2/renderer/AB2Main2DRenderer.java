package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Main2DRenderer extends AB2Renderer2D {
    Logger logger = LoggerFactory.getLogger(AB2Main2DRenderer.class);

    int x;
    int y;
    int width;
    int height;
    int screenWidth;
    int screenHeight;

    private AB2Controller controller;

    private TextLabelActor messageActor;
    private GLShaderActionSequence textShaderDrawSequence;


    public AB2Main2DRenderer(int x, int y, int width, int height, int screenWidth, int screenHeight, GLRegion parentRegion) {
        super(parentRegion);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;

        controller = AB2Controller.getController();

        textShaderDrawSequence = new GLShaderActionSequence("Text");
        textShaderDrawSequence.setShader(new AB2Text2DShader());
        addDrawShaderActionSequence(textShaderDrawSequence);

    }

    @Override
    public void init(GL4 gl) {
        createTextMessage(gl);
        super.init(gl);
        initialized=true;
    }

    private void createTextMessage(GL4 gl) {
        float centerX = (float) ((((width / 2) + x) * 1.0) / (1.0 * screenWidth));
        float centerY = (float) ((((height / 2) + y) * 1.0) / (1.0 * screenHeight));
        messageActor = new TextLabelActor(
                this,
                controller.getNextPickIndex(),
                "<no message>",
                new Vector3(centerX, centerY, AB2Properties.MAIN_RENDERER_MESSAGE_Z),
                AB2Properties.MAIN_RENDERER_MESSAGE_TEXT_COLOR,
                AB2Properties.MAIN_RENDERER_MESSAGE_BACKGROUND_COLOR,
                TextLabelActor.Orientation.NORMAL);
        messageActor.setDisplay(false);
        textShaderDrawSequence.getActorSequence().add(messageActor);
    }

    public void updateTextMessage(String message) {
        messageActor.setText(message);
        messageActor.setDisplay(true);
        controller.setNeedsRepaint(true);
    }


    public void displayTextMessage() {
        messageActor.setDisplay(true);
    }

    public void hideTextMessage() {
        messageActor.setDisplay(false);
    }


    public void clearActors() {
        clearActionSequenceActors(textShaderDrawSequence);
        messageActor = null;
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;

        super.reshape(gl, x, y, width, height, screenWidth, screenHeight);
        float centerX = (float) ((((width / 2) + x) * 1.0) / (1.0 * screenWidth));
        float centerY = (float) ((((height / 2) + y) * 1.0) / (1.0 * screenHeight));
        messageActor.setCenterPosition(new Vector3(centerX, centerY, AB2Properties.MAIN_RENDERER_MESSAGE_Z));
    }

    @Override
    public void processEvent(AB2Event event) {
        super.processEvent(event);

    }

    @Override
    public void display(GL4 gl) {
        super.display(gl);
    }

}

