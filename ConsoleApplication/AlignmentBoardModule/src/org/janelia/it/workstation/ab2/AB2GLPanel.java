package org.janelia.it.workstation.ab2;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;

import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2AwtActionEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseDraggedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseEnteredEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseExitedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseMovedEvent;
import org.janelia.it.workstation.ab2.event.AB2MousePressedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseReleasedEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseWheelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2GLPanel extends GLJPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {

    private static final Logger logger = LoggerFactory.getLogger(AB2GLPanel.class);
    protected static GLProfile profile = null;
    protected static GLCapabilities capabilities = null;

    private AB2Controller controller;

    static {
        try {
            profile = GLProfile.get(GLProfile.GL4);
            capabilities = new GLCapabilities(profile);
        } catch ( Throwable th ) {
            profile = null;
            capabilities = null;
            logger.warn("JOGL is unavailable. No 3D images will be shown.");
        }
    }

    public AB2GLPanel(int width, int height, AB2Controller controller) {
        super(capabilities);
        this.controller=controller;
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setPreferredSize( new Dimension( width, height ) );
        addGLEventListener(controller);
    }

    @Override
    public Dimension getPreferredSize() {
        return super.getPreferredSize();
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        controller.processEvent(new AB2MousePressedEvent(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        controller.processEvent(new AB2MouseReleasedEvent(e));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        controller.processEvent(new AB2MouseEnteredEvent(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        controller.processEvent(new AB2MouseExitedEvent(e));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        controller.processEvent(new AB2AwtActionEvent(arg0));
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        controller.processEvent(new AB2MouseDraggedEvent(event));
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        controller.processEvent(new AB2MouseMovedEvent(event));
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        controller.processEvent(new AB2MouseClickedEvent(event));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        controller.processEvent(new AB2MouseWheelEvent(event));
    }

}
