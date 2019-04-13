package org.janelia.workstation.integration.framework.system;

import javax.swing.JFrame;

/**
 * Returns the main frame of the application, suitable as parent to any
 * dialog or other frame; especially JOPtionPane.
 *
 * @author fosterl
 */
public interface ParentFrame {
    public static final String LOOKUP_PATH = "ParentFrame/Location/Nodes";
    JFrame getMainFrame();
}
