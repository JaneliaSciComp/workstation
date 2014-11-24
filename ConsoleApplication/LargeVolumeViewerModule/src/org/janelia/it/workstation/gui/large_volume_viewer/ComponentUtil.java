/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

/**
 * Utilities relay to avoid direct dependencies on things like the top component.
 * 
 * @author fosterl
 */
public class ComponentUtil {
    private static JComponent mainLvvWindow;
    
    public static JComponent getLVVMainWindow() {
        if ( mainLvvWindow == null ) {
            popuplateMainWin();
        }
        return mainLvvWindow;
    }

    private static void popuplateMainWin() {
        try {
            Runnable runnable = new Runnable() {
                public void run() {
                    mainLvvWindow = LargeVolumeViewerTopComponent.findThisComponent();
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeAndWait(runnable);
            }
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }
}
