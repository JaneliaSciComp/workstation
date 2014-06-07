package org.janelia.it.workstation.gui.util.panels;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.framework.roles.PrefEditor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.filestore.PathTranslator;
import org.janelia.it.workstation.shared.util.PropertyConfigurator;

public class SystemSettingsPanel extends JPanel implements PrefEditor {
	
    private String jacsDataPath = "";
    private String jacsInteractiveServer = "";
    private String jacsPipelineServer = "";
    
    private boolean settingsChanged = false;
    
    private JPanel dataSourcePanel = new JPanel();
    
    private JTextField jacsDataPathField = new JTextField();
    private JTextField jacsInteractiveServerField = new JTextField();
    private JTextField jacsPipelineServerField = new JTextField();
    
    
    public SystemSettingsPanel(JFrame parentFrame) {
        try {
        	System.out.println("Reading data source settings ");
        	
            jacsDataPath = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY);
            if (jacsDataPath == null) jacsDataPath = "";
            jacsInteractiveServer = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.JACS_INTERACTIVE_SERVER_PROPERTY);
            if (jacsInteractiveServer == null) jacsInteractiveServer = "";
            jacsPipelineServer = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.JACS_PIPELINE_SERVER_PROPERTY);
            if (jacsPipelineServer == null) jacsPipelineServer = "";
            
            System.out.println("jacsDataPath="+jacsDataPath);
            System.out.println("jacsInteractiveServer="+jacsInteractiveServer);
            System.out.println("jacsPipelineServer="+jacsPipelineServer);
            
            jbInit();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    public String getName() {
        return "System Settings";
    }

    public String getPanelGroup() {
        return PrefController.APPLICATION_EDITOR;
    }

    public String getDescription() {
        return "Settings for accessing the database and file system.";
    }

    /**
     * Defined for the PrefEditor interface.  When the Cancel button is pressed in
     * the Controller frame.
     */
    public void cancelChanges() {
        if (jacsDataPath == null || jacsInteractiveServer == null || jacsPipelineServer == null) {
            PropertyConfigurator.getProperties().setProperty(SessionMgr.JACS_DATA_PATH_PROPERTY, "NoDataPathProperty");
            PropertyConfigurator.getProperties().setProperty(SessionMgr.JACS_INTERACTIVE_SERVER_PROPERTY, "NoInteractiveServerProperty");
            PropertyConfigurator.getProperties().setProperty(SessionMgr.JACS_PIPELINE_SERVER_PROPERTY, "NoPipelineServerProperty");
        }
        settingsChanged = false;
    }

    public boolean hasChanged() {
        // If not equal to original values, they have changed.
        if (!jacsDataPath.equals(new String(jacsDataPathField.getText().trim())) 
        		|| !jacsInteractiveServer.equals(new String(jacsInteractiveServerField.getText().trim())) 
        		|| !jacsPipelineServer.equals(new String(jacsPipelineServerField.getText().trim())))
            settingsChanged = true;
        return settingsChanged;
    }

    /**
     * Defined for the PrefEditor interface.  When the Apply or OK button is
     * pressed in the Controller frame.
     */
    public String[] applyChanges() {
        List delayedChanges = new ArrayList();
        jacsDataPath = jacsDataPathField.getText().trim();
        jacsInteractiveServer = jacsInteractiveServerField.getText().trim();
        jacsPipelineServer = jacsPipelineServerField.getText().trim();
        
        if ((!jacsDataPath.equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY)))
                || (!jacsInteractiveServer.equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.JACS_INTERACTIVE_SERVER_PROPERTY)))
                || (!jacsPipelineServer.equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.JACS_PIPELINE_SERVER_PROPERTY)))) {
            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY, jacsDataPath);
            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.JACS_INTERACTIVE_SERVER_PROPERTY, jacsInteractiveServer);
            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.JACS_PIPELINE_SERVER_PROPERTY, jacsPipelineServer);
            
            PathTranslator.initFromModelProperties(SessionMgr.getSessionMgr().getSessionModel());
            EJBFactory.initFromModelProperties(SessionMgr.getSessionMgr().getSessionModel());
            SessionMgr.getSessionMgr().loginSubject();
        }
        settingsChanged = false;
        return (String[]) delayedChanges.toArray(new String[delayedChanges.size()]);
    }

    /**
     * This method is required by the interface.
     */
    public void dispose() {
    }

    private void jbInit() throws Exception {
    	
        setPreferredSize(new Dimension(300,300));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        jacsDataPathField = new JTextField(jacsDataPath, 40);
        jacsDataPathField.setMaximumSize(jacsDataPathField.getPreferredSize());

        jacsInteractiveServerField = new JTextField(jacsInteractiveServer, 40);
        jacsInteractiveServerField.setMaximumSize(jacsInteractiveServerField.getPreferredSize());

        jacsPipelineServerField = new JTextField(jacsPipelineServer, 40);
        jacsPipelineServerField.setMaximumSize(jacsPipelineServerField.getPreferredSize());
        
        TitledBorder titledBorder = new TitledBorder("Data Access Settings");
        dataSourcePanel.setLayout(new SpringLayout());
        dataSourcePanel.setBorder(titledBorder);

        JLabel nameLabel = new JLabel("* JACS Data Mount:");
        nameLabel.setLabelFor(jacsDataPathField);
        dataSourcePanel.add(nameLabel);
        dataSourcePanel.add(jacsDataPathField);
        
        nameLabel = new JLabel("* JACS Interactive Server:");
        nameLabel.setLabelFor(jacsInteractiveServerField);
        dataSourcePanel.add(nameLabel);
        dataSourcePanel.add(jacsInteractiveServerField);
        
        nameLabel = new JLabel("* JACS Pipeline Server:");
        nameLabel.setLabelFor(jacsPipelineServerField);
        dataSourcePanel.add(nameLabel);
        dataSourcePanel.add(jacsPipelineServerField);
                
        SpringUtilities.makeCompactGrid(dataSourcePanel, dataSourcePanel.getComponentCount()/2, 2, 6, 6, 6, 6);
        
        JPanel notePanel = new JPanel();
        notePanel.setMaximumSize(new Dimension(600,100));
        notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.X_AXIS));
        notePanel.add(new JLabel("* indicates a required field"));

        add(Box.createVerticalStrut(10));
        add(dataSourcePanel);
        add(Box.createVerticalGlue());
        add(notePanel);
    }
}
