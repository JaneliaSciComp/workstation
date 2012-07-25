/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/23/11
 * Time: 9:18 AM
 */
package org.janelia.it.FlyWorkstation.gui.dataview;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;

import javax.swing.*;

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
    	
        final ModelMgr modelMgr = ModelMgr.getModelMgr();
        modelMgr.registerFacadeManagerForProtocol(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
        
        // Make sure the user knows what server they're getting data from (which implies database Prod or Dev)
        String provider = ConsoleProperties.getInstance().getProperty("provider.url");
        provider = provider.substring(provider.indexOf("//")+2, provider.lastIndexOf(":"));

        mainFrame = new DataviewFrame();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setTitle("JACS Data Viewer (Data Source="+ provider + ")");
        mainFrame.pack();
        mainFrame.setVisible(true);
        
    }
}
