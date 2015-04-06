package org.janelia.it.workstation.gui.util;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.top_component.IconPanelTopComponent;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * This acts as an Adapter between the specific implementation, and interface
 * needed.  This includes hiding some details about threading, etc.
 * 
 * @author fosterl
 */
public class WindowLocator {
    private static JFrame mainFrame;
    
    /**
     * Call this if all you need is a parent frame.  Browser will no longer
     * extend JFrame.
     * 
     * @return the main framework window.
     */
    public static JFrame getMainFrame() {        
        if (mainFrame == null) {
            try {                
                Runnable runnable = new Runnable() {
                    public void run() {
                        mainFrame = (JFrame) WindowManager.getDefault().getMainWindow();
                    }
                };
                if ( SwingUtilities.isEventDispatchThread() ) {
                    runnable.run();
                }
                else {
                    SwingUtilities.invokeAndWait( runnable );
                }
            } catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
        return mainFrame;
    }
    
    /**
     * Fetch the component by name.  Cover eventualities of caller origin.
     * @param frameName which to fetch
     * @return found component.
     */
    public static TopComponent getByName( final String frameName ) {
        final TopComponent[] component = new TopComponent[ 1 ];
        try {
            Runnable runnable = new Runnable() {
                public void run() {
                    component[ 0 ] = (TopComponent)WindowManager.getDefault().findTopComponent(frameName);
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
        return component[ 0 ];
    }
    
    /**
     * Activate the given top component in the given mode, and return it. 
     * @param frameName
     * @param modeName
     * @return 
     */
    public static TopComponent activateAndGet(final String frameName, final String modeName) {
        TopComponent win = WindowLocator.getByName(IconPanelTopComponent.PREFERRED_ID);
        if (win!=null) {
            if (!win.isOpened()) {
                Mode mode = WindowManager.getDefault().findMode(modeName);
                if (mode != null) {
                    mode.dockInto(win);
                }
            }
            win.requestActive();
        }
        return win;
    }

}
