package org.janelia.workstation.common.gui.options;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.text.DefaultFormatter;

import org.janelia.workstation.core.api.LocalCacheMgr;
import org.janelia.workstation.core.api.LocalPreferenceMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.panels.MemorySettingPanel;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ApplicationOptionsPanel extends javax.swing.JPanel {

    private static final Logger log = LoggerFactory.getLogger(ApplicationOptionsPanel.class);
    
    private final ApplicationOptionsPanelController controller;

    private final GroupedKeyValuePanel mainPanel;
    private MemorySettingPanel memoryPanel;

    private JCheckBox autoDownloadUpdates;
    private JCheckBox showReleaseNotesOnStartup;
    private JCheckBox showStartPageOnStartup;
    private JCheckBox useRunAsUserPreferences;
    private JCheckBox useHTTPForTileAccess;
    private JCheckBox showHortaControlCenterOnStartup;
    private JRadioButton fileCacheEnabledRadioButton;
    private JRadioButton fileCacheDisabledRadioButton;
    private JSpinner fileCacheSpinner;
    private JProgressBar fileCacheUsageBar;
    private JButton fileCacheClearButton;
    private JLabel errorLabel;
    
    ApplicationOptionsPanel(final ApplicationOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        
        this.mainPanel = new GroupedKeyValuePanel();

        // General options
        
        mainPanel.addSeparator("General");

        autoDownloadUpdates = new JCheckBox("Download updates automatically");
        autoDownloadUpdates.addActionListener((e) -> controller.changed());
        mainPanel.addItem(autoDownloadUpdates);
        
        showReleaseNotesOnStartup = new JCheckBox("Show release notes after update");
        showReleaseNotesOnStartup.addActionListener((e) -> controller.changed());
        mainPanel.addItem(showReleaseNotesOnStartup);

        showStartPageOnStartup = new JCheckBox("Show start page on startup");
        showStartPageOnStartup.addActionListener((e) -> controller.changed());
        mainPanel.addItem(showStartPageOnStartup);
        
        useRunAsUserPreferences = new JCheckBox("Use preferences from Run As user");
        useRunAsUserPreferences.addActionListener((e) -> controller.changed());
        if (AccessManager.getAccessManager().isAdmin()) {
            mainPanel.addItem(useRunAsUserPreferences);
        }

        useHTTPForTileAccess = new JCheckBox("Use http for tile access");
        useHTTPForTileAccess.addActionListener((e) -> controller.changed());
        mainPanel.addItem(useHTTPForTileAccess);

        showHortaControlCenterOnStartup = new JCheckBox("Show Horta Control Center on startup");
        showHortaControlCenterOnStartup.addActionListener((e) -> controller.changed());
        mainPanel.addItem(showHortaControlCenterOnStartup);

        // Memory

        mainPanel.addSeparator("Memory Management");

        memoryPanel = new MemorySettingPanel();
        memoryPanel.setSettingListener(
            new MemorySettingPanel.SettingListener() {
                @Override
                public void settingChanged() {
                    controller.changed();
                }
            }
        );
        mainPanel.addItem("Max Memory (GB)", memoryPanel);

        // Cache

        JPanel fileCachePanel = buildFileCachePanel();
        mainPanel.addItem("Local Disk Cache", fileCachePanel);
        
        errorLabel = new JLabel();
        errorLabel.setIcon(Icons.getIcon("error.png"));
        errorLabel.setVisible(false);
        errorLabel.setForeground(Color.red);
        mainPanel.addItem(errorLabel, "gaptop 10");
                
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    private JPanel buildFileCachePanel() {
        
        JPanel fileCachePanel = new JPanel();

        fileCachePanel.setLayout(new GridBagLayout());
        fileCachePanel.setMaximumSize(new Dimension(600, 200));

        // ---------------------
        fileCacheEnabledRadioButton = new JRadioButton("Enabled");
        fileCacheEnabledRadioButton.addActionListener((e) -> { controller.changed(); });
        
        fileCacheDisabledRadioButton = new JRadioButton("Disabled");
        fileCacheDisabledRadioButton.addActionListener((e) -> { controller.changed(); });

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
            new SpinnerNumberModel(LocalPreferenceMgr.DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY,
                    LocalPreferenceMgr.MIN_FILE_CACHE_GIGABYTE_CAPACITY,
                    LocalPreferenceMgr.MAX_FILE_CACHE_GIGABYTE_CAPACITY,
                    1));
        fileCacheSpinner.setMaximumSize(new Dimension(200, 100));
        fileCacheSpinner.addChangeListener(new ChangeListener() {
            private Object lastValue;
            @Override
            public void stateChanged(ChangeEvent evt) {
                if (lastValue != null && !fileCacheSpinner.getValue().equals(lastValue)) {
                    controller.changed();
                }
                lastValue = fileCacheSpinner.getValue();
            }
        });

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
                LocalCacheMgr.getInstance().clearFileCache();
                updateFileCacheComponents(false);
            }
        });

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 2;
        fileCachePanel.add(fileCacheClearButton, c);
        
        return fileCachePanel;
    }
    
    private void updateFileCacheComponents(final boolean waitForReload) {

        final int capacity = LocalPreferenceMgr.getInstance().getFileCacheGigabyteCapacity();

        fileCacheSpinner.setValue(capacity);

        if (LocalPreferenceMgr.getInstance().isCacheAvailable()) {
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
                    double usage = LocalCacheMgr.getInstance().getFileCacheGigabyteUsage();
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
                    FrameworkAccess.handleException(error);
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

    boolean valid() {
        boolean valid = true;
        String memError = memoryPanel.getError();
        if (memError != null) {
            valid = false;
            errorLabel.setText(memError);
        }
        errorLabel.setVisible(!valid);
        return valid;
    }

    void load() {
        
        log.info("Loading application settings...");
        ApplicationOptions options = ApplicationOptions.getInstance();
        autoDownloadUpdates.setSelected(options.isAutoDownloadUpdates());
        showReleaseNotesOnStartup.setSelected(options.isShowReleaseNotes());
        showStartPageOnStartup.setSelected(options.isShowStartPageOnStartup());
        useRunAsUserPreferences.setSelected(options.isUseRunAsUserPreferences());
        useHTTPForTileAccess.setSelected(options.isUseHTTPForTileAccess());
        showHortaControlCenterOnStartup.setSelected(options.isShowHortaOnStartup());
        memoryPanel.setMemorySetting(Utils.getMemoryAllocation());
        
        updateFileCacheComponents(false);
    }
    
    void store() {
        
        log.info("Saving application settings...");
        
        // General
        ApplicationOptions options = ApplicationOptions.getInstance();
        options.setAutoDownloadUpdates(autoDownloadUpdates.isSelected());
        options.setShowReleaseNotes(showReleaseNotesOnStartup.isSelected());
        options.setShowStartPageOnStartup(showStartPageOnStartup.isSelected());
        options.setUseRunAsUserPreferences(useRunAsUserPreferences.isSelected());
        options.setUseHTTPForTileAccess(useHTTPForTileAccess.isSelected());
        options.setShowHortaControlCenterOnStartup(showHortaControlCenterOnStartup.isSelected());

        // Memory
        String error = memoryPanel.getError();
        if (error == null) {
            try {
                Utils.setMemoryAllocation(memoryPanel.getMemorySetting());
            }
            catch (IOException e) {
                FrameworkAccess.handleException(e);
            }
        }
       
        // Cache
        
        Boolean cacheDisabled = fileCacheDisabledRadioButton.isSelected();
        Integer cacheCapacity = (Integer) fileCacheSpinner.getValue();
        
        final boolean cacheDisabledChanged =
                ! cacheDisabled.equals(FrameworkAccess.getModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY));
        if (cacheDisabledChanged) {
            log.info("Saving file cache disabled setting: "+cacheDisabled);
            LocalPreferenceMgr.getInstance().setFileCacheDisabled(cacheDisabled);
        }
        
        final boolean cacheCapacityChanged = ! cacheCapacity.equals(LocalPreferenceMgr.getInstance().getFileCacheGigabyteCapacity());
        if (cacheCapacityChanged) {
            log.info("Saving cache capacity setting: "+cacheCapacity);
            LocalPreferenceMgr.getInstance().setFileCacheGigabyteCapacity(cacheCapacity);
        }
        
        if (cacheDisabledChanged || cacheCapacityChanged) {
            updateFileCacheComponents(true);
        }
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
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
