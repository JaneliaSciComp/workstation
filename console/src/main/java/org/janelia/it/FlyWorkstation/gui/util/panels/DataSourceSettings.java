package org.janelia.it.FlyWorkstation.gui.util.panels;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml.ValidationManager;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml.XmlServiceFacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.roles.PrefEditor;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.swing_models.CollectionJListModel;
import org.janelia.it.FlyWorkstation.shared.util.PropertyConfigurator;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextField;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataSourceSettings extends JPanel implements PrefEditor {
   private String userLogin = new String("");
   private String userPassword = new String("");
   private boolean settingsChanged = false;
   private JFrame parentFrame;
   JPanel ejbPanel = new JPanel();
   JPasswordField passwordTextField;
   JLabel passwordLabel = new JLabel("Password:");
   JCheckBox saveCheckBox;
   JLabel loginLabel = new JLabel("User Name:");
   JTextField loginTextField = new StandardTextField();
   TitledBorder titledBorder2;

   private static final String LOCATION_PROP_NAME = "XmlGenomeVersionLocation";
   private static final int PREFERRED_JLIST_HEIGHT = 165;

   private JButton addDirectoryButton;
   private JComboBox validationComboBox;
   private static String fileSep = File.separator;
   protected File directoryPrefFile =
      new File(System.getProperty("user.home") + fileSep + "x" + fileSep + "GenomeBrowser" + fileSep + "userPrefs." + LOCATION_PROP_NAME);

   private JList currentDirectoryJList;
   private CollectionJListModel directoryLocationModel;
   private static final int VERY_WIDE = 800;
   private JList urlJList = null;
   private CollectionJListModel urlLocationModel;
   private JButton removeUrlButton = new JButton("Remove Selected URL");
   private JButton addUrlButton = new JButton("Add to Current URLs");
   private JTextField addUrlField = new StandardTextField();
   private JButton removeDirectoryButton = new JButton("Remove Selected Directory");
   private static final int MAX_DIR_LENGTH = 60;

   public DataSourceSettings(JFrame parentFrame) {
      this.parentFrame = parentFrame;
      try {
         userLogin = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
         if (userLogin == null)
            userLogin = "";
         userPassword = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_PASSWORD);
         if (userPassword == null)
            userPassword = "";
         jbInit();
      }
      catch (Exception ex) {
         SessionMgr.getSessionMgr().handleException(ex);
      }
   }

   public String getName() {
      return "Data Source Settings";
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
      if (userLogin == null || userPassword == null) {
         PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_NAME, "NoUserLogin");
         PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_PASSWORD, "NoUserPassword");
      }
      settingsChanged = false;
   }

   public boolean hasChanged() {
      // If not equal to original values, they have changed.
      if (!userLogin.equals(loginTextField.getText().trim()) || !userPassword.equals(new String(passwordTextField.getPassword())))
         settingsChanged = true;
      return settingsChanged;
   }

   /**
    * Defined for the PrefEditor interface.  When the Apply or OK button is
    * pressed in the Controller frame.
    */
   public String[] applyChanges() {
      List delayedChanges = new ArrayList();
      userLogin = loginTextField.getText().trim();
      userPassword = new String(passwordTextField.getPassword());
// JCVI LLF, 10/19/2006
//      if ((!userLogin.equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME)))
//         || (!userPassword.equals(SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_PASSWORD)))) {
//         if (saveCheckBox.isSelected()) {
//            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.USER_NAME, userLogin);
//            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.USER_PASSWORD, userPassword);
//         }
//         PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_NAME, userLogin);
//         PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_PASSWORD, userPassword);
//         // End login apply code
//
//         // Begin Datasource directory selection apply code
//         List list = FacadeManager.getInUseProtocolStrings();
//         for (Iterator it = list.iterator(); it.hasNext();) {
//            if (it.next().equals(FacadeManager.getEJBProtocolString()))
//               delayedChanges.add("Changing the User Login while currently Logged in");
//         }
//         FacadeManager.addProtocolToUseList(FacadeManager.getEJBProtocolString());
//      }
      try {
         if (directoryLocationModel.isModified()) {
            if (ModelMgr.getModelMgr().getNumberOfLoadedOntologies() > 0)
               delayedChanges.add("Changing the XML Directories");
            setNewDirectoryLocations(directoryLocationModel.getList());
         } // Change required.

         String userChosenValidation = (String) validationComboBox.getSelectedItem();
//         if (!userChosenValidation.equals(ValidationManager.getInstance().getDisplayableValidationSetting())) {
//            ValidationManager.getInstance().setDisplayableValidationSetting(userChosenValidation);
//            delayedChanges.add("Changing XML File Validation Preference");
//         } // Change required.

      } // End try to save changes.
      catch (Exception ex) {
         SessionMgr.getSessionMgr().handleException(ex);
      } // End catch for delete
      // End datasource dir selection, apply code

      if (urlLocationModel.isModified())
         delayedChanges.add("Changing the XML Service URLs");

      setNewUrlLocations(urlLocationModel.getList());
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
      passwordTextField = new JPasswordField(userPassword, 10);
      passwordTextField.setMaximumSize(new Dimension(100, 20));
      passwordTextField.setMinimumSize(new Dimension(100, 20));
      passwordTextField.setSize(100, 20);
      passwordTextField.addFocusListener(new FocusListener() {
         public void focusGained(FocusEvent e) {
            if (e.getSource() == passwordTextField)
               passwordTextField.selectAll();
         }
         public void focusLost(FocusEvent e) {
         }
      });
      saveCheckBox = new JCheckBox("Save Login Information");
      loginTextField = new StandardTextField(userLogin, 10);
      loginTextField.setMaximumSize(new Dimension(100, 20));
      loginTextField.addFocusListener(new FocusListener() {
         public void focusGained(FocusEvent e) {
            if (e.getSource() == loginTextField)
               loginTextField.selectAll();
         }
         public void focusLost(FocusEvent e) {
         }
      });
      titledBorder2 = new TitledBorder("Workstation Login Information");
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      ejbPanel.setBorder(titledBorder2);
      ejbPanel.setLayout(new BoxLayout(ejbPanel, BoxLayout.Y_AXIS));
      saveCheckBox.setSelected(true);
      JPanel userPassPanel = new JPanel();
      userPassPanel.setLayout(new BoxLayout(userPassPanel, BoxLayout.X_AXIS));
      userPassPanel.add(loginLabel);
      userPassPanel.add(Box.createHorizontalStrut(10));
      userPassPanel.add(loginTextField);
      userPassPanel.add(Box.createHorizontalStrut(30));
      userPassPanel.add(passwordLabel);
      userPassPanel.add(Box.createHorizontalStrut(5));
      userPassPanel.add(passwordTextField);
      JPanel checkPanel = new JPanel();
      checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.X_AXIS));
      checkPanel.add(saveCheckBox);
      ejbPanel.add(Box.createVerticalStrut(10));
      ejbPanel.add(userPassPanel);
      ejbPanel.add(Box.createVerticalStrut(10));
      ejbPanel.add(checkPanel);
      ejbPanel.add(Box.createVerticalStrut(5));

      add(Box.createVerticalStrut(10));
      add(ejbPanel);
      add(Box.createVerticalStrut(10));

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

      // "Add" panel.  Button and field to let user create a new URL and add it.
      addUrlButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            addUrlButtonActionPerformed(ae);
         }
      });
      addUrlButton.setRequestFocusEnabled(false);

      // 'Remove' sub-panel. Combobox and button.
      List urlLocationCollection = getExistingUrlLocations();
      urlLocationModel = new CollectionJListModel(urlLocationCollection);
      urlJList = new JList(urlLocationModel);
      JScrollPane urlScroll = new JScrollPane(urlJList);
      urlScroll.setViewportBorder(new BevelBorder(BevelBorder.LOWERED));
      ActionListener ruListener = new ModelRemovalListener(urlJList);
      removeUrlButton.addActionListener(ruListener);

      removeUrlButton.setEnabled(urlLocationModel.getSize() > 0);
      if (urlLocationModel.getSize() <= 0)
         removeUrlButton.setEnabled(false);
      removeUrlButton.setRequestFocusEnabled(false);

      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
      buttonPanel.add(addUrlButton);
      buttonPanel.add(Box.createHorizontalStrut(5));
      buttonPanel.add(removeUrlButton);
      buttonPanel.add(Box.createHorizontalGlue());

      JLabel addLabel = new JLabel("New URL: ");
      JPanel addPanel = new JPanel();
      addUrlField.setMaximumSize(new Dimension(200, 20));
      addUrlField.setMinimumSize(new Dimension(200, 20));
      addUrlField.setSize(200, 20);
      addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.X_AXIS));
      addPanel.add(Box.createHorizontalStrut(5));
      addPanel.add(addLabel);
      addPanel.add(Box.createHorizontalStrut(2));
      addPanel.add(addUrlField);

      JPanel xmlServicePanel = new JPanel();
      xmlServicePanel.setLayout(new BoxLayout(xmlServicePanel, BoxLayout.Y_AXIS));
      xmlServicePanel.setBorder(new TitledBorder("XML Service"));
      JPanel labelPanel = new JPanel();
      labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
      labelPanel.add(new JLabel("Current URLs:"));
      labelPanel.add(Box.createHorizontalGlue());
      xmlServicePanel.add(labelPanel);
      xmlServicePanel.add(urlScroll);
      xmlServicePanel.add(Box.createVerticalStrut(5));
      xmlServicePanel.add(buttonPanel);
      xmlServicePanel.add(Box.createVerticalStrut(5));
      xmlServicePanel.add(addPanel);
      xmlServicePanel.add(Box.createVerticalStrut(5));
      int preferredWidthOfUrlPanel = xmlServicePanel.getWidth();
      xmlServicePanel.setPreferredSize(new Dimension(preferredWidthOfUrlPanel, PREFERRED_JLIST_HEIGHT));

      Box contentBox = Box.createVerticalBox();
      contentBox.add(Box.createVerticalStrut(5));
      contentBox.add(xmlServicePanel);
      contentBox.add(Box.createVerticalStrut(5));
      add(contentBox);
      add(Box.createVerticalStrut(10));
   }

   private void addUrlButtonActionPerformed(ActionEvent ae) {
      try {
         URL url = new URL(addUrlField.getText());
         settingsChanged = true;
         String valueToAdd = url.toString(); //addUrlField.getText().trim();
         if ((valueToAdd != null) && (valueToAdd.length() > 0)) {
            urlLocationModel.add(valueToAdd);
            removeUrlButton.setEnabled(true);
         } // User entered something.
      }
      catch (MalformedURLException ex) {
         JOptionPane.showMessageDialog(DataSourceSettings.this, "The typed URL is not valid", "Error", JOptionPane.ERROR_MESSAGE);
         return;
      }
      this.repaint();
   }

   private void showNewXmlDirectoryChooser() {
      JFileChooser chooser = null;

      if (directoryLocationModel.getSize() == 0)
         chooser = new FileChooser();
      else
         chooser = new FileChooser(new File((String) directoryLocationModel.findLast()));
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

   /** Gets the old location settings. */
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

   /** Sets the user's new pref. */
   private void setNewDirectoryLocations(List locationList) {
      /** @todo when possible change this to use Model Property implementation. */

      // Now attempt to writeback the user's currently-selected directory as the
      // new preference for reading XML files.
      //
      try {
         if (locationList != null) {
            ObjectOutputStream ostream = new ObjectOutputStream(new FileOutputStream(directoryPrefFile));
            for (Iterator it = locationList.iterator(); it.hasNext();) {
               ostream.writeObject(it.next());
            } // For all directories.
            ostream.close();
         } // Permission granted.
         else {
            SessionMgr.getSessionMgr().handleException(
               new IllegalArgumentException("XML Directory List is null or Cannot Write " + directoryPrefFile.getAbsoluteFile()));
         } // Not granted
      } // End try block.
      catch (Exception ex) {
         SessionMgr.getSessionMgr().handleException(
            new IllegalArgumentException("XML Directory Prefs " + directoryPrefFile.getAbsoluteFile() + " File Cannot be Written"));
      } // End catch block for writeback of preferred directory.

   } // End method

   /** Sets the user's new prefs. */
   private void setNewUrlLocations(List newLocs) {
      XmlServiceFacadeManager.setNewLocations(newLocs);
   } // End method

   /** Gets the old settings. */
   private List getExistingUrlLocations() {
      return XmlServiceFacadeManager.getExistingLocations();
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
         JButton button = null;
         if (ae.getSource() instanceof JButton)
            button = (JButton) ae.getSource();
         else
            return;

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
