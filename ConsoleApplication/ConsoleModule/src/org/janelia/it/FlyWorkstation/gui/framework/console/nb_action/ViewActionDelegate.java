/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import javax.swing.JCheckBoxMenuItem;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

/**
 *
 * @author fosterl
 */
public class ViewActionDelegate {
    public void toggleDataPanel() {
        Browser browser = SessionMgr.getBrowser();
        browser.toggleViewComponentState(Browser.VIEW_OUTLINES);
    }
    
    public void toggleOntology() {
        Browser browser = SessionMgr.getBrowser();
        browser.toggleViewComponentState(Browser.VIEW_ONTOLOGY);
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
}
