package org.janelia.workstation.integration.api;

import javax.swing.JFrame;

/**
 * Returns the main frame of the application, suitable as parent to any
 * dialog or other frame, especially JOptionPane.
 *
 * @author fosterl
 */
public interface FrameModel {

    public static final String LOOKUP_PATH = "FrameModel/Location/Nodes";

    JFrame getMainFrame();

}
