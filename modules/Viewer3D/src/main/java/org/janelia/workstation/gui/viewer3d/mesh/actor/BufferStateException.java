package org.janelia.workstation.gui.viewer3d.mesh.actor;

/**
 *
 * @author fosterl
 */
public class BufferStateException extends Exception {
    public BufferStateException() {
        super("Bad Buffer State.  Avoid further OpenGL Processing.");
    }
}
