/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

/**
 *
 * @author fosterl
 */
public class BufferStateException extends Exception {
    public BufferStateException() {
        super("Bad Buffer State.  Avoid further OpenGL Processing.");
    }
}
