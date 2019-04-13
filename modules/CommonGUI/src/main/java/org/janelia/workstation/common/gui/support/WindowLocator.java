package org.janelia.workstation.common.gui.support;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.janelia.workstation.integration.util.FrameworkAccess;
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
                FrameworkAccess.handleException(ex);
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
                    component[ 0 ] = WindowManager.getDefault().findTopComponent(frameName);
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
        return component[ 0 ];
    }
    
    /**
     * Activate the given top component in the given mode, and return it. 
     * @param frameName
     * @param modeName
     * @return 
     */
    public static TopComponent activateAndGet(final String frameName, final String modeName) {
        TopComponent win = WindowLocator.getByName(frameName);
        if (win != null) {
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

    public static TopComponent makeVisibleAndGet(final String frameName) {
        TopComponent topComponent = WindowLocator.getByName(frameName);
        if (topComponent != null) {
            if (!topComponent.isOpened()) {
                topComponent.open();
            }
            if (topComponent.isOpened()) {
                topComponent.requestActive();
            }

            topComponent.setVisible(true);
        }
        return topComponent;
    }
}
