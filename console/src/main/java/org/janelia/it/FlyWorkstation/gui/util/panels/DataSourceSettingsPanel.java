package org.janelia.it.FlyWorkstation.gui.util.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.text.DefaultFormatter;

import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.roles.PrefEditor;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.PropertyConfigurator;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextField;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceSettingsPanel extends JPanel implements PrefEditor {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceSettingsPanel.class);
    
    private String userLogin = "";
    private String userPassword = "";
    private String userEmail = "";
    private String runAsUser = "";
    private Boolean cacheDisabled;
    private Integer cacheCapacity;
    private String downloadsDir = "";
    private boolean settingsChanged = false;
    
    private JLabel requiredField = new JLabel("* indicates a required field");
    
    private JPanel loginPanel = new JPanel();
    private JPasswordField passwordTextField;
    private JLabel passwordLabel = new JLabel("* Password:");
    private JLabel loginLabel = new JLabel("* User Name:");
    private JTextField loginTextField;
    private JPanel runAsPanel;
    private JLabel runAsLabel = new JLabel("Run As User:");
    private JTextField runAsTextField;

    private JPanel emailPanel;
    private JLabel emailLabel = new JLabel("* Email Address:");
    private JTextField emailTextField;

    private JRadioButton fileCacheEnabledRadioButton;
    private JRadioButton fileCacheDisabledRadioButton;
    private JSpinner fileCacheSpinner;
    private JProgressBar fileCacheUsageBar;
    private JButton fileCacheClearButton;

    private JLabel downloadsDirLabel = new JLabel("* Downloads Dir:");
    private JTextField downloadsDirField = new JTextField(40);
    
    public DataSourceSettingsPanel(@SuppressWarnings("UnusedParameters")
                                   JFrame parentFrame) {
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        try {
            userLogin = (String) getModelProperty(SessionMgr.USER_NAME, "");
            userPassword = (String) getModelProperty(SessionMgr.USER_PASSWORD, "");
            userEmail = (String) getModelProperty(SessionMgr.USER_EMAIL, "");
            cacheDisabled = (Boolean) getModelProperty(SessionMgr.FILE_CACHE_DISABLED_PROPERTY, false);
            cacheCapacity = (Integer) getModelProperty(SessionMgr.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY,
                                                   SessionMgr.MIN_FILE_CACHE_GIGABYTE_CAPACITY);
            runAsUser = (String) getModelProperty(SessionMgr.RUN_AS_USER, "");
            downloadsDir = SystemInfo.getDownloadsDir().getAbsolutePath();
            jbInit();
        }
        catch (Exception ex) {
            sessionMgr.handleException(ex);
        }
    }

    public String getName() {
        return "Data Source Settings";
    }

    public String getPanelGroup() {
        return PrefController.APPLICATION_EDITOR;
    }

    public String getDescription() {
        return "Set the Login, Password and Email for the Internal EJB Server, XML Directory and XML Service location.";
    }

    /**
     * Defined for the PrefEditor interface.  When the Cancel button is pressed in
     * the Controller frame.
     */
    public void cancelChanges() {
        if (userLogin == null || userPassword == null || userEmail == null) {
            PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_NAME, "NoUserLogin");
            PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_PASSWORD, "NoUserPassword");
            PropertyConfigurator.getProperties().setProperty(SessionMgr.USER_EMAIL, "NoUserEmail");
            PropertyConfigurator.getProperties().setProperty(SessionMgr.RUN_AS_USER, "NoRunAsUser");
        }
        settingsChanged = false;
    }

    public boolean hasChanged() {
        // If not equal to original values, they have changed.
        if (!userLogin.equals(loginTextField.getText().trim()) ||
            !userPassword.equals(new String(passwordTextField.getPassword())) ||
            !userEmail.equals(emailTextField.getText().trim()) ||
            !runAsUser.equals(runAsTextField.getText().trim()) ||
            !cacheDisabled.equals(fileCacheDisabledRadioButton.isSelected()) ||
            !cacheCapacity.equals(fileCacheSpinner.getValue()) || 
            !downloadsDir.equals(downloadsDirField.getText().trim()))
            settingsChanged = true;
        return settingsChanged;
    }

    /**
     * Defined for the PrefEditor interface.  When the Apply or OK button is
     * pressed in the Controller frame.
     */
    public String[] applyChanges() {
        userLogin = loginTextField.getText().trim();
        userPassword = new String(passwordTextField.getPassword());
        userEmail = emailTextField.getText().trim();
        runAsUser = runAsTextField.getText().trim();
        cacheDisabled = fileCacheDisabledRadioButton.isSelected();
        cacheCapacity = (Integer) fileCacheSpinner.getValue();
        downloadsDir = downloadsDirField.getText().trim();
        
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();

        final boolean cacheDisabledChanged =
                ! cacheDisabled.equals(sessionMgr.getModelProperty(SessionMgr.FILE_CACHE_DISABLED_PROPERTY));
        if (cacheDisabledChanged) {
            sessionMgr.setFileCacheDisabled(cacheDisabled);
        }

        final boolean cacheCapacityChanged =
                ! cacheCapacity.equals(sessionMgr.getFileCacheGigabyteCapacity());
        if (cacheCapacityChanged) {
            sessionMgr.setFileCacheGigabyteCapacity(cacheCapacity);
        }

        if ((!userLogin.equals(sessionMgr.getModelProperty(SessionMgr.USER_NAME))) ||
            (!userPassword.equals(sessionMgr.getModelProperty(SessionMgr.USER_PASSWORD))) ||
            (!userEmail.equals(sessionMgr.getModelProperty(SessionMgr.USER_EMAIL))) ||
            (!runAsUser.equals(sessionMgr.getModelProperty(SessionMgr.RUN_AS_USER)))) {
            // If the login has changed then wipe out the runAs field and value.
            if ((!userLogin.equals(sessionMgr.getModelProperty(SessionMgr.USER_NAME)))) {
                runAsTextField.setText("");
                runAsUser="";
            }
            log.info("Setting properties in model...");
            sessionMgr.setModelProperty(SessionMgr.RUN_AS_USER, runAsUser);
            sessionMgr.setModelProperty(SessionMgr.USER_NAME, userLogin);
            sessionMgr.setModelProperty(SessionMgr.USER_PASSWORD, userPassword);
            sessionMgr.setModelProperty(SessionMgr.USER_EMAIL, userEmail);
            boolean loginSuccess = SessionMgr.getSessionMgr().loginSubject();
            if (loginSuccess) {
                runAsPanel.setVisible(SessionMgr.authenticatedSubjectIsInGroup("admin"));
            }
            SessionMgr.getBrowser().setPerspective(Perspective.ImageBrowser);
            FacadeManager.addProtocolToUseList(FacadeManager.getEJBProtocolString());
        }

        if (cacheDisabledChanged || cacheCapacityChanged) {
            updateFileCacheComponents(true);
        }
        
        SystemInfo.setDownloadsDir(downloadsDir);

        settingsChanged = false;
        return new String[0];
    }

    /**
     * This method is required by the interface.
     */
    public void dispose() {
    }

    private void jbInit() throws Exception {
        this.setPreferredSize(new Dimension(300,300));
        passwordTextField = new JPasswordField(userPassword, 40);
        passwordTextField.setPreferredSize(new Dimension(60, 30));
        passwordTextField.setSize(100, 20);
        passwordTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (e.getSource() == passwordTextField) passwordTextField.selectAll();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        loginTextField = new StandardTextField(userLogin, 40);
        loginTextField.setPreferredSize(new Dimension(60, 30));
        loginTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (e.getSource() == loginTextField) loginTextField.selectAll();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        emailTextField = new StandardTextField(userEmail, 40);
        emailTextField.setPreferredSize(new Dimension(80, 30));
        emailTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (e.getSource() == emailTextField) emailTextField.selectAll();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        loginPanel.setBorder(new TitledBorder("Workstation Login Information"));
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setMaximumSize(new Dimension(600, 100));
        JPanel userPassPanel = new JPanel();
        userPassPanel.setLayout(new BoxLayout(userPassPanel, BoxLayout.X_AXIS));
        userPassPanel.add(loginLabel);
        userPassPanel.add(Box.createHorizontalStrut(10));
        userPassPanel.add(loginTextField);
        userPassPanel.add(Box.createHorizontalStrut(30));
        userPassPanel.add(passwordLabel);
        userPassPanel.add(Box.createHorizontalStrut(5));
        userPassPanel.add(passwordTextField);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(userPassPanel);
        loginPanel.add(Box.createVerticalStrut(10));

        runAsTextField = new StandardTextField(runAsUser, 40);
        runAsTextField.setPreferredSize(new Dimension(80, 30));
        
        runAsPanel = new JPanel();
        runAsPanel.setLayout(new BoxLayout(runAsPanel, BoxLayout.X_AXIS));
        runAsPanel.add(runAsLabel);
        runAsPanel.add(Box.createHorizontalStrut(10));
        runAsPanel.add(runAsTextField);
        
        if (SessionMgr.authenticatedSubjectIsInGroup("admin")) {    
            loginPanel.add(runAsPanel);
            loginPanel.add(Box.createVerticalStrut(10));
        }

        emailPanel = new JPanel();
        emailPanel.setBorder(new TitledBorder("Email Address"));
        emailPanel.setLayout(new BoxLayout(emailPanel, BoxLayout.X_AXIS));
        emailPanel.setMaximumSize(new Dimension(600, 100));
        JPanel userEmailPanel = new JPanel();
        userEmailPanel.setLayout(new BoxLayout(userEmailPanel, BoxLayout.X_AXIS));
        userEmailPanel.add(emailLabel);
        userEmailPanel.add(Box.createHorizontalStrut(10));
        userEmailPanel.add(emailTextField);
        userEmailPanel.add(Box.createHorizontalStrut(30));
        emailPanel.add(Box.createVerticalStrut(10));
        emailPanel.add(userEmailPanel);
        emailPanel.add(Box.createVerticalStrut(10));

        JPanel fileCachePanel = buildFileCachePanel();

        JPanel notePanel = new JPanel();
        notePanel.setMaximumSize(new Dimension(600,100));
        notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.X_AXIS));
        notePanel.add(requiredField);


        String chooseFileText = null;
        ImageIcon chooseFileIcon = null;
        try {
            chooseFileIcon = Utils.getClasspathImage("magnifier.png");
        } catch (FileNotFoundException e) {
            log.warn("failed to load button icon", e);
            chooseFileText = "...";
        }

        downloadsDirField = new JTextField(40);
        downloadsDirField.setText(downloadsDir);
        downloadsDirLabel.setLabelFor(downloadsDirField);

        JButton chooseFileButton = new JButton(chooseFileText, chooseFileIcon);
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File currentDir = new File(downloadsDirField.getText());
                if (! currentDir.exists()) {
                    currentDir = null;
                }
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setCurrentDirectory(currentDir);
                int returnVal = fileChooser.showOpenDialog(DataSourceSettingsPanel.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    downloadsDirField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        
        JPanel downloadDirPanel = new JPanel();
        downloadDirPanel.setLayout(new BoxLayout(downloadDirPanel, BoxLayout.X_AXIS));
        downloadDirPanel.add(downloadsDirLabel);
        downloadDirPanel.add(downloadsDirField);
        downloadDirPanel.add(chooseFileButton);
        
        JPanel dirsPanel = new JPanel();
        dirsPanel.setLayout(new BoxLayout(dirsPanel, BoxLayout.Y_AXIS));
        dirsPanel.setBorder(new TitledBorder("Local Directories"));
        dirsPanel.add(downloadDirPanel);
        dirsPanel.setMaximumSize(new Dimension(600, 100));
        
        add(Box.createVerticalStrut(10));
        add(loginPanel);
        add(Box.createVerticalStrut(10));
        add(emailPanel);
        add(Box.createVerticalStrut(10));
        add(fileCachePanel);
        add(Box.createVerticalStrut(10));
        add(dirsPanel);
        add(Box.createVerticalGlue());
        add(notePanel);

//        addDirectoryButton = new JButton("Add to Current Directories");
//        addDirectoryButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent ae) {
//                showNewXmlDirectoryChooser();
//            } // End method
//        });
//        addDirectoryButton.setRequestFocusEnabled(false);
//
//        List directoryLocationCollection = getExistingDirectoryLocations();
//        directoryLocationModel = new CollectionJListModel(directoryLocationCollection);
//        currentDirectoryJList = new JList(directoryLocationModel);
//        ActionListener rdListener = new ModelRemovalListener(currentDirectoryJList);
//        removeDirectoryButton.addActionListener(rdListener);
//        removeDirectoryButton.setEnabled(directoryLocationModel.getSize() > 0);
//        removeDirectoryButton.setRequestFocusEnabled(false);
//
//        String[] choices = ValidationManager.getInstance().getDisplayableValidationChoices();
//        validationComboBox = new JComboBox(choices);
//        byte validationSetting = ValidationManager.getInstance().getValidationSetting();
//        String validationDisplayString = ValidationManager.getInstance().convertValidationSettingFromByteToString(validationSetting);
//        validationComboBox.setSelectedItem(validationDisplayString);
//        validationComboBox.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent e) {
//                settingsChanged = true;
//            }
//        });
//
//        JPanel internalButtonPanel = new JPanel();
//        internalButtonPanel.setLayout(new BoxLayout(internalButtonPanel, BoxLayout.X_AXIS));
//        internalButtonPanel.add(Box.createHorizontalStrut(5));
//        internalButtonPanel.add(addDirectoryButton);
//        internalButtonPanel.add(Box.createHorizontalStrut(10));
//        internalButtonPanel.add(removeDirectoryButton);
//        internalButtonPanel.add(Box.createHorizontalGlue());
//
//        JPanel internalComboBoxPanel = new JPanel();
//        internalComboBoxPanel.setLayout(new BoxLayout(internalComboBoxPanel, BoxLayout.X_AXIS));
//        internalComboBoxPanel.add(Box.createHorizontalStrut(5));
//        internalComboBoxPanel.add(validationComboBox);
//        internalComboBoxPanel.add(Box.createHorizontalGlue());
//
//        JPanel internalValidationPanel = new JPanel();
//        internalValidationPanel.setLayout(new GridLayout(2, 1));
//        internalValidationPanel.add(new JLabel("Validation Options"));
//        internalValidationPanel.add(internalComboBoxPanel);
//
//        JPanel xmlDirectoryPanel = new JPanel();
//        xmlDirectoryPanel.setLayout(new BoxLayout(xmlDirectoryPanel, BoxLayout.Y_AXIS));
//        xmlDirectoryPanel.setBorder(new TitledBorder("XML Directory"));
//        JPanel currentDirPanel = new JPanel();
//        currentDirPanel.setLayout(new BoxLayout(currentDirPanel, BoxLayout.X_AXIS));
//        JScrollPane directoryScroll = new JScrollPane(currentDirectoryJList);
//        directoryScroll.setViewportBorder(new BevelBorder(BevelBorder.LOWERED));
//        currentDirPanel.add(directoryScroll);
//        xmlDirectoryPanel.add(currentDirPanel);
//        xmlDirectoryPanel.add(Box.createVerticalStrut(5));
//        xmlDirectoryPanel.add(internalButtonPanel);
//        xmlDirectoryPanel.add(Box.createVerticalStrut(5));
//        xmlDirectoryPanel.add(internalValidationPanel);
//        int preferredWidthOfDirPanel = xmlDirectoryPanel.getWidth();
//        xmlDirectoryPanel.setPreferredSize(new Dimension(preferredWidthOfDirPanel, PREFERRED_JLIST_HEIGHT));
//
////        add(xmlDirectoryPanel);
//
////        add(Box.createVerticalStrut(10));
//
//        // "Add" panel.  Button and field to let user create a new URL and add it.
//        addUrlButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent ae) {
//                addUrlButtonActionPerformed(ae);
//            }
//        });
//        addUrlButton.setRequestFocusEnabled(false);
//
//        // 'Remove' sub-panel. Combobox and button.
//        List urlLocationCollection = getExistingUrlLocations();
//        urlLocationModel = new CollectionJListModel(urlLocationCollection);
//        urlJList = new JList(urlLocationModel);
//        JScrollPane urlScroll = new JScrollPane(urlJList);
//        urlScroll.setViewportBorder(new BevelBorder(BevelBorder.LOWERED));
//        ActionListener ruListener = new ModelRemovalListener(urlJList);
//        removeUrlButton.addActionListener(ruListener);
//
//        removeUrlButton.setEnabled(urlLocationModel.getSize() > 0);
//        if (urlLocationModel.getSize() <= 0) removeUrlButton.setEnabled(false);
//        removeUrlButton.setRequestFocusEnabled(false);
//
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
//        buttonPanel.add(addUrlButton);
//        buttonPanel.add(Box.createHorizontalStrut(5));
//        buttonPanel.add(removeUrlButton);
//        buttonPanel.add(Box.createHorizontalGlue());
//
//        JLabel addLabel = new JLabel("New URL: ");
//        JPanel addPanel = new JPanel();
//        addUrlField.setMaximumSize(new Dimension(200, 20));
//        addUrlField.setMinimumSize(new Dimension(200, 20));
//        addUrlField.setSize(200, 20);
//        addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.X_AXIS));
//        addPanel.add(Box.createHorizontalStrut(5));
//        addPanel.add(addLabel);
//        addPanel.add(Box.createHorizontalStrut(2));
//        addPanel.add(addUrlField);
//
//        JPanel xmlServicePanel = new JPanel();
//        xmlServicePanel.setLayout(new BoxLayout(xmlServicePanel, BoxLayout.Y_AXIS));
//        xmlServicePanel.setBorder(new TitledBorder("XML Service"));
//        JPanel labelPanel = new JPanel();
//        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
//        labelPanel.add(new JLabel("Current URLs:"));
//        labelPanel.add(Box.createHorizontalGlue());
//        xmlServicePanel.add(labelPanel);
//        xmlServicePanel.add(urlScroll);
//        xmlServicePanel.add(Box.createVerticalStrut(5));
//        xmlServicePanel.add(buttonPanel);
//        xmlServicePanel.add(Box.createVerticalStrut(5));
//        xmlServicePanel.add(addPanel);
//        xmlServicePanel.add(Box.createVerticalStrut(5));
//        int preferredWidthOfUrlPanel = xmlServicePanel.getWidth();
//        xmlServicePanel.setPreferredSize(new Dimension(preferredWidthOfUrlPanel, PREFERRED_JLIST_HEIGHT));
//
//        Box contentBox = Box.createVerticalBox();
//        contentBox.add(Box.createVerticalStrut(5));
//        contentBox.add(xmlServicePanel);
//        contentBox.add(Box.createVerticalStrut(5));
//        add(contentBox);
//        add(Box.createVerticalStrut(10));
    }

    private JPanel buildFileCachePanel() {

        JPanel fileCachePanel = new JPanel();

        fileCachePanel.setBorder(new TitledBorder("Local Disk Cache"));
        fileCachePanel.setLayout(new GridBagLayout());
        fileCachePanel.setMaximumSize(new Dimension(600, 200));

        // ---------------------
        fileCacheEnabledRadioButton = new JRadioButton("Enabled");
        fileCacheDisabledRadioButton = new JRadioButton("Disabled");

        JPanel cacheRadioPanel = new JPanel();
        cacheRadioPanel.setLayout(new BoxLayout(cacheRadioPanel, BoxLayout.X_AXIS));

        ButtonGroup group = new ButtonGroup();
        group.add(fileCacheEnabledRadioButton);
        group.add(fileCacheDisabledRadioButton);
        cacheRadioPanel.add(fileCacheEnabledRadioButton);
        cacheRadioPanel.add(Box.createHorizontalStrut(10));
        cacheRadioPanel.add(fileCacheDisabledRadioButton);
        cacheRadioPanel.add(Box.createHorizontalStrut(10));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.5;
        c.weighty = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        fileCachePanel.add(new JLabel("Caching:"), c);

        c.gridx = 1;
        fileCachePanel.add(cacheRadioPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        fileCachePanel.add(new JLabel("Capacity (GB):"), c);

        // ---------------------
        fileCacheSpinner = new JSpinner(
                new SpinnerNumberModel(SessionMgr.MIN_FILE_CACHE_GIGABYTE_CAPACITY,
                        SessionMgr.MIN_FILE_CACHE_GIGABYTE_CAPACITY,
                        SessionMgr.MAX_FILE_CACHE_GIGABYTE_CAPACITY,
                        1));
        fileCacheSpinner.setMaximumSize(new Dimension(200, 100));

        // configure spinner to dis-allow invalid edits
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) fileCacheSpinner.getEditor();
        JFormattedTextField ftf = editor.getTextField();
        JFormattedTextField.AbstractFormatter formatter = ftf.getFormatter();
        DefaultFormatter df = (DefaultFormatter) formatter;
        df.setAllowsInvalid(false);

        c.gridx = 1;
        fileCachePanel.add(fileCacheSpinner, c);

        // ---------------------
        fileCacheUsageBar = new JProgressBar(0, 100);
        fileCacheUsageBar.setUI(new NonAnimatedProgressBarUI());
        fileCacheUsageBar.setBorder(BorderFactory.createLineBorder(fileCacheUsageBar.getForeground()));
        fileCacheUsageBar.setForeground(Color.GRAY);
        fileCacheUsageBar.setBackground(Color.DARK_GRAY);
        fileCacheUsageBar.setStringPainted(true);

        c.gridx = 0;
        c.gridy = 2;
        fileCachePanel.add(new JLabel("Usage:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        fileCachePanel.add(fileCacheUsageBar, c);

        // ---------------------
        fileCacheClearButton = new JButton("Clear Cache");
        fileCacheClearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SessionMgr.getSessionMgr().clearFileCache();
                updateFileCacheComponents(false);
            }
        });

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 2;
        fileCachePanel.add(fileCacheClearButton, c);

        updateFileCacheComponents(false);
        
        return fileCachePanel;
    }

    private void updateFileCacheComponents(final boolean waitForReload) {

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        final int capacity = sessionMgr.getFileCacheGigabyteCapacity();

        fileCacheSpinner.setValue(capacity);

        if (sessionMgr.isFileCacheAvailable()) {

            fileCacheEnabledRadioButton.setSelected(true);
            fileCacheSpinner.setEnabled(true);

            SimpleWorker worker = new SimpleWorker() {
                
                double percentage;
                
                @Override
                protected void doStuff() throws Exception {
                    if (waitForReload) {
                        try {
                            // HACK! - give the cache a chance to reload before refreshing this view
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            log.warn("ignoring exception", e);
                        }
                    }
                    double usage = sessionMgr.getFileCacheGigabyteUsage();
                    this.percentage = (usage / capacity) * 100.0;
                }
                
                @Override
                protected void hadSuccess() {
                    fileCacheUsageBar.setValue((int) percentage);
                    fileCacheUsageBar.setVisible(true);
                    fileCacheClearButton.setEnabled(true);
                }
                
                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            worker.execute();
            
        } 
        else {
            fileCacheDisabledRadioButton.setSelected(true);
            fileCacheSpinner.setEnabled(false);
            fileCacheUsageBar.setVisible(false);
            fileCacheClearButton.setEnabled(false);
        }
    }

    private Object getModelProperty(String key,
                                    Object defaultValue) {
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        Object value = sessionMgr.getModelProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Overrides the default animated progress bar L&F.
     */
    private class NonAnimatedProgressBarUI extends BasicProgressBarUI {

        @Override
        protected Color getSelectionForeground() {
            return getForeground();
        }
        @Override
        protected Color getSelectionBackground() {
            return getForeground();
        }
    }
}
