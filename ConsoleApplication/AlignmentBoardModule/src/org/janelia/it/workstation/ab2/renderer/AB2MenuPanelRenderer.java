package org.janelia.it.workstation.ab2.renderer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.it.workstation.ab2.actor.ColorBox2DActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.view.AB2Properties;

public class AB2MenuPanelRenderer extends AB2Renderer2D {

    private ColorBox2DActor backgroundPanel;
    private GLShaderActionSequence menuPanelSequence;

    public AB2MenuPanelRenderer() {
        menuPanelSequence = new GLShaderActionSequence(this.getClass().getName());
    }


    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        if (backgroundPanel==null) {
            createBackgroundPanel(x, y, width, height, screenWidth, screenHeight);
        } else {
            backgroundPanel
        }
    }

    private void createBackgroundPanel(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Vector2[] normed2dPositions=getNormed2DPositionsFromScreenCoordinates(x, y, width, height, screenWidth, screenHeight);
        backgroundPanel=new ColorBox2DActor(this, AB2Controller.getController().getNextPickIndex(),
                normed2dPositions[0], normed2dPositions[1], AB2Properties.MENU_COLOR, AB2Properties.MENU_HOVER_COLOR, AB2Properties.MENU_SELECT_COLOR);
    }

}
