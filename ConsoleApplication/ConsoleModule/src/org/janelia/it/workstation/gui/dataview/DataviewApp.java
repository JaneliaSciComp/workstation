/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.workstation.gui.dataview;

import javax.swing.JFrame;

/**
 * An graphical interface for viewing the Entity model: EntityTypes, EntityAttributes, Entities and EntityData.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataviewApp {

    private static DataviewFrame mainFrame;

    public static DataviewFrame getMainFrame() {
        return mainFrame;
    }

    public static void main(final String[] args) throws Exception {
        newDataviewer();
    }

    private static void newDataviewer() throws Exception {
    	
        org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager.registerFacade(org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager.getEJBProtocolString(), org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager.class, "JACS EJB Facade Manager");
        
        // This initializes the EJBFactory
        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr();
        
        String provider = org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory.getAppServerName();

        mainFrame = new DataviewFrame();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setTitle("JACS Data Viewer (Data Source="+ provider + ")");
        mainFrame.pack();
        mainFrame.setVisible(true);
        
    }
}
