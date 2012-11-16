/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.FlyWorkstation.gui.dataview;

import javax.swing.JFrame;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

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
    	
        FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
        
        // This initializes the EJBFactory
        SessionMgr.getSessionMgr();
        
        String provider = EJBFactory.getAppServerName();

        mainFrame = new DataviewFrame();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setTitle("JACS Data Viewer (Data Source="+ provider + ")");
        mainFrame.pack();
        mainFrame.setVisible(true);
        
    }
}
