package org.janelia.it.FlyWorkstation.gui.util.panels;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml.ValidationManager;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.roles.PrefEditor;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.swing_models.CollectionJListModel;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ToolSettingsPanel extends JPanel implements PrefEditor {
    private boolean settingsChanged = false;
    private JFrame parentFrame;
    TitledBorder titledBorder2;

    private static final int PREFERRED_JLIST_HEIGHT = 165;

    private JButton addDirectoryButton;
    private JComboBox validationComboBox;
    private static String fileSep = File.separator;
    private static final String LOCATION_PROP_NAME = "ToolLocations";
    protected File directoryPrefFile = new File(SessionMgr.getSessionMgr().getApplicationOutputDirectory() + fileSep + "userPrefs." + LOCATION_PROP_NAME);

    private JList currentDirectoryJList;
    private CollectionJListModel directoryLocationModel;
    private JButton removeDirectoryButton = new JButton("Remove Selected Directory");

    public ToolSettingsPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        try {
            jbInit();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    public String getName() {
        return "Tool Settings";
    }

    public String getPanelGroup() {
        return PrefController.SYSTEM_EDITOR;
    }

    public String getDescription() {
        return "Set the Login/Password for the Internal EJB Server, XML Directory and XML Service location.";
    }

    /**
     * Defined for the PrefEditor interface.  When the Cancel button is pressed in
     * the Controller frame.
     */
    public void cancelChanges() {
        settingsChanged = false;
    }

    public boolean hasChanged() {
        // If not equal to original values, they have changed.
        return settingsChanged;
    }

    /**
     * Defined for the PrefEditor interface.  When the Apply or OK button is
     * pressed in the Controller frame.
     */
    public String[] applyChanges() {
        List delayedChanges = new ArrayList();
        try {
            if (directoryLocationModel.isModified()) {
                if (ModelMgr.getModelMgr().getNumberOfLoadedOntologies() > 0)
                    delayedChanges.add("Changing the XML Directories");
                setNewDirectoryLocations(directoryLocationModel.getList());
            } // Change required.
        } // End try to save changes.
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        } // End catch for delete
        // End datasource dir selection, apply code

        settingsChanged = false;
        return (String[]) delayedChanges.toArray(new String[delayedChanges.size()]);
    }

    /**
     * This method is required by the interface.
     */
    public void dispose() {
    }

    private void jbInit() throws Exception {
        this.setLayout(null);
        titledBorder2 = new TitledBorder("Workstation Login Information");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        addDirectoryButton = new JButton("Add to Current Directories");
        addDirectoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showNewXmlDirectoryChooser();
            } // End method
        });
        addDirectoryButton.setRequestFocusEnabled(false);

        List directoryLocationCollection = getExistingDirectoryLocations();
        directoryLocationModel = new CollectionJListModel(directoryLocationCollection);
        currentDirectoryJList = new JList(directoryLocationModel);
        ActionListener rdListener = new ModelRemovalListener(currentDirectoryJList);
        removeDirectoryButton.addActionListener(rdListener);
        removeDirectoryButton.setEnabled(directoryLocationModel.getSize() > 0);
        removeDirectoryButton.setRequestFocusEnabled(false);

        String[] choices = ValidationManager.getInstance().getDisplayableValidationChoices();
        validationComboBox = new JComboBox(choices);
        byte validationSetting = ValidationManager.getInstance().getValidationSetting();
        String validationDisplayString = ValidationManager.getInstance().convertValidationSettingFromByteToString(validationSetting);
        validationComboBox.setSelectedItem(validationDisplayString);
        validationComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                settingsChanged = true;
            }
        });

        JPanel internalButtonPanel = new JPanel();
        internalButtonPanel.setLayout(new BoxLayout(internalButtonPanel, BoxLayout.X_AXIS));
        internalButtonPanel.add(Box.createHorizontalStrut(5));
        internalButtonPanel.add(addDirectoryButton);
        internalButtonPanel.add(Box.createHorizontalStrut(10));
        internalButtonPanel.add(removeDirectoryButton);
        internalButtonPanel.add(Box.createHorizontalGlue());

        JPanel internalComboBoxPanel = new JPanel();
        internalComboBoxPanel.setLayout(new BoxLayout(internalComboBoxPanel, BoxLayout.X_AXIS));
        internalComboBoxPanel.add(Box.createHorizontalStrut(5));
        internalComboBoxPanel.add(validationComboBox);
        internalComboBoxPanel.add(Box.createHorizontalGlue());

        JPanel internalValidationPanel = new JPanel();
        internalValidationPanel.setLayout(new GridLayout(2, 1));
        internalValidationPanel.add(new JLabel("Validation Options"));
        internalValidationPanel.add(internalComboBoxPanel);

        JPanel xmlDirectoryPanel = new JPanel();
        xmlDirectoryPanel.setLayout(new BoxLayout(xmlDirectoryPanel, BoxLayout.Y_AXIS));
        xmlDirectoryPanel.setBorder(new TitledBorder("XML Directory"));
        JPanel currentDirPanel = new JPanel();
        currentDirPanel.setLayout(new BoxLayout(currentDirPanel, BoxLayout.X_AXIS));
        JScrollPane directoryScroll = new JScrollPane(currentDirectoryJList);
        directoryScroll.setViewportBorder(new BevelBorder(BevelBorder.LOWERED));
        currentDirPanel.add(directoryScroll);
        xmlDirectoryPanel.add(currentDirPanel);
        xmlDirectoryPanel.add(Box.createVerticalStrut(5));
        xmlDirectoryPanel.add(internalButtonPanel);
        xmlDirectoryPanel.add(Box.createVerticalStrut(5));
        xmlDirectoryPanel.add(internalValidationPanel);
        int preferredWidthOfDirPanel = xmlDirectoryPanel.getWidth();
        xmlDirectoryPanel.setPreferredSize(new Dimension(preferredWidthOfDirPanel, PREFERRED_JLIST_HEIGHT));

        add(xmlDirectoryPanel);

        add(Box.createVerticalStrut(10));

    }

    private void showNewXmlDirectoryChooser() {
        JFileChooser chooser = null;

        if (directoryLocationModel.getSize() == 0) chooser = new FileChooser();
        else chooser = new FileChooser(new File((String) directoryLocationModel.findLast()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        int returnVal = chooser.showOpenDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            settingsChanged = true;
            File[] files = chooser.getSelectedFiles();
            for (int i = 0; i < files.length; i++) {
                directoryLocationModel.add(files[i].getAbsolutePath());
            } // For all files.
            currentDirectoryJList.updateUI();
            removeDirectoryButton.setEnabled(true);
        } // Got approved.

    } // End method

    /**
     * Gets the old location settings.
     */
    private List getExistingDirectoryLocations() {
        List returnCollection = new ArrayList();

        /** @todo when possible change this to use Model Property implementation. */
        // Set the default directory from a preset preference if possible.
        //
        ObjectInputStream istream = null;
        try {

            String nextDirectory = null;
            if (directoryPrefFile.canRead() && directoryPrefFile.exists()) {
                FileInputStream fis = new FileInputStream(directoryPrefFile);
                istream = new ObjectInputStream(fis);
                while (null != (nextDirectory = (String) istream.readObject())) {
                    returnCollection.add(nextDirectory);
                } // For all directories.

            } // Permission granted.

        } // End try
        catch (Exception ex) {
        } // End catch block for pref file open exceptions.
        finally {
            try {
                istream.close();
            } // Close up shop
            catch (Exception ex) {
                // Do nothing.
            } // End catch for closing
        } // After all is said and done...

        return returnCollection;
    } // End method

    /**
     * Sets the user's new pref.
     */
    private void setNewDirectoryLocations(List locationList) {
        /** @todo when possible change this to use Model Property implementation. */

        // Now attempt to writeback the user's currently-selected directory as the
        // new preference for reading XML files.
        //
        try {
            if (locationList != null) {
                ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(directoryPrefFile));
                for (Object aLocationList : locationList) {
                    ostream.writeObject(aLocationList);
                } // For all directories.
                ostream.close();
            } // Permission granted.
            else {
                SessionMgr.getSessionMgr().handleException(new IllegalArgumentException("XML Directory List is null or Cannot Write " + directoryPrefFile.getAbsoluteFile()));
            } // Not granted
        } // End try block.
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(new IllegalArgumentException("XML Directory Prefs " + directoryPrefFile.getAbsoluteFile() + " File Cannot be Written"));
        } // End catch block for writeback of preferred directory.

    } // End method

    /**
     * Listens for removal button actions against JLists.  JLists
     * must have CollectionJListModel models!
     */
    public class ModelRemovalListener implements ActionListener {

        JList widgetForRemoval = null;

        ModelRemovalListener(JList widgetForRemoval) {
            this.widgetForRemoval = widgetForRemoval;
        } // End constructor

        public void actionPerformed(ActionEvent ae) {
            // Looking for button event.
            JButton button;
            if (ae.getSource() instanceof JButton) button = (JButton) ae.getSource();
            else return;

            if (widgetForRemoval.getSelectedValues() != null) {
                Object[] removables = widgetForRemoval.getSelectedValues();
                CollectionJListModel listModel = (CollectionJListModel) widgetForRemoval.getModel();
                for (Object removable : removables) listModel.remove(removable);

                button.setEnabled(listModel.getSize() > 0);

                settingsChanged = true;

            } // One or more items has been selected.
        } // End method
    }
}
