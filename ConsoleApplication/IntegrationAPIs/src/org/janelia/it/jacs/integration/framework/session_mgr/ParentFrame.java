/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration.framework.session_mgr;

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
