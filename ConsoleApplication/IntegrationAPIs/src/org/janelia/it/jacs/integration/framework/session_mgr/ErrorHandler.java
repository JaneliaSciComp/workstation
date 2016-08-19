/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.integration.framework.session_mgr;

/**
 * Implement this to create something capable of handling exceptions at the
 * whole-application level. Call the impl when the code has given up on solving
 * the immediate problem.  This is for "system errors".
 *
 * @author fosterl
 */
public interface ErrorHandler {
    public static final String LOOKUP_PATH = "ErrorHandler/Location/Nodes";
    void handleException(Throwable ex);
}
