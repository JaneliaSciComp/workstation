/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.console.nb_action;

import javax.swing.JCheckBoxMenuItem;

import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author fosterl
 */
public class ViewActionDelegate {
    /**
     * This makes the entire "explorer" mode (NB term) unseen by user.
     */
    public void toggleDataPanel( boolean booleanState ) {
        String sourceMode = booleanState ? "leftSlidingSide" : "explorer";
        String targetMode = booleanState ? "explorer" : "leftSlidingSide";
        changeModes(sourceMode, targetMode);
            
    }

    /**
     * This makes the entire "properties" mode (NB term) unseen by user.
     */
    public void toggleOntology( boolean booleanState ) {
        String sourceMode = booleanState ? "rightSlidingSide" : "properties";
        String targetMode = booleanState ? "properties" : "rightSlidingSide";
        changeModes(sourceMode, targetMode);
    }
    
    /**
     * Not called until/unless supported.  Preserving this code for future
     * perusal.
     */
    public void linkLeftRightViewers() {
        final JCheckBoxMenuItem linkViewersMenuItem = 
                new JCheckBoxMenuItem("Link Left/Right Viewers", true);
        Browser browser = SessionMgr.getBrowser();
        browser.setIsViewersLinked(linkViewersMenuItem.isSelected());
    }
    
    public void resetWindow() {
        SessionMgr.getBrowser().resetBrowserPosition();
    }

    private void changeModes(String sourceMode, String targetMode) {
        Mode oldMode = WindowManager.getDefault().findMode( sourceMode );
        Mode newMode = WindowManager.getDefault().findMode( targetMode );
        if ( oldMode != null  &&  newMode != null ) {
            TopComponent[] allTopComponents =
                    WindowManager.getDefault().getOpenedTopComponents(oldMode);
            for ( TopComponent tc: allTopComponents ) {
                Mode tcMode = WindowManager.getDefault().findMode(tc);
                if ( tcMode.equals( oldMode ) ) {
                    newMode.dockInto(tc);
                }
            }
        }
    }
    
}
