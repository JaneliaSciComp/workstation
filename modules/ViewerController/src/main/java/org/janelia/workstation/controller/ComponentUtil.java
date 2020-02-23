package org.janelia.workstation.controller;

import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;

/**
 * Utilities relay to avoid direct dependencies on things like the top component.
 * 
 * @author fosterl
 */
public class ComponentUtil {
    private static JComponent mainLvvWindow;
    
    public static JComponent getMainWindow() {
        if ( mainLvvWindow == null ) {
            popuplateMainWin();
        }
        return mainLvvWindow;
    }

    private static void popuplateMainWin() {
        try {
            Runnable runnable = new Runnable() {
                public void run() {
                    //mainLvvWindow = LargeVolumeViewerTopComponent.getInstance();
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeAndWait(runnable);
            }
        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }
}
