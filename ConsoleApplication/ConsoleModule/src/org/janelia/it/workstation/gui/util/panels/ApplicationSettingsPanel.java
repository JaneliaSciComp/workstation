package org.janelia.it.workstation.gui.util.panels;

import org.janelia.it.workstation.gui.framework.navigation_tools.AutoNavigationMgr;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.framework.roles.PrefEditor;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.shared.util.RendererType2D;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ApplicationSettingsPanel extends JPanel implements PrefEditor {
    private boolean settingsChanged = false;
    private static final String SUBVIEW_FOCUS = "FocusSubviewsUponNavigation";
    private MemorySettingPanel pnlMemorySetting;
    JCheckBox subviewFocusCheckBox = new JCheckBox();
    JCheckBox subEditors = new JCheckBox();
    JCheckBox memoryUsage = new JCheckBox();
    JCheckBox navComplete = new JCheckBox();
    JCheckBox unloadImages = new JCheckBox();
    SessionMgr sessionMgr = SessionMgr.getSessionMgr();
    MySessionModelListener sessionModelListener = new MySessionModelListener();
    ButtonGroup buttonLookAndFeelGroup = new ButtonGroup();
    Map<ButtonModel, String> buttonToLafMap = new HashMap<ButtonModel, String>();

    ButtonGroup rendererGroup = new ButtonGroup();
    Map<ButtonModel, String> buttonToRendererMap = new HashMap<ButtonModel, String>();
    
    public ApplicationSettingsPanel(JFrame parentFrame) {
        try {
            sessionMgr.addSessionModelListener(sessionModelListener);
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return "Set various browser preferences, including Look and Feel and layout settings.";
    }

    public String getPanelGroup() {
        return PrefController.APPLICATION_EDITOR;
    }

    private void jbInit() throws Exception {

        setPreferredSize(new Dimension(500, 600));

        JPanel mainPanel = new JPanel();

        // ------------------------------------------------------------------------------------------------------------
        // Browser Options
        // ------------------------------------------------------------------------------------------------------------
        
        JPanel pnlLayoutOptions = new JPanel();
        pnlLayoutOptions.setLayout(new BoxLayout(pnlLayoutOptions, BoxLayout.Y_AXIS));
        pnlLayoutOptions.setBorder(new TitledBorder("Browser Options"));
        subEditors.setText("Display SubViews When Available");
        subEditors.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });
        if (null != sessionMgr.getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)) {
            subEditors.setSelected((Boolean) sessionMgr.getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY));
        }

        subviewFocusCheckBox.setText("Focus SubViews Upon Navigation");
        subviewFocusCheckBox.setBounds(new Rectangle(25, 199, 222, 19));
        subviewFocusCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SessionMgr.getSessionMgr().setModelProperty(SUBVIEW_FOCUS, subviewFocusCheckBox.isSelected());
            }
        });
        if (SessionMgr.getSessionMgr().getModelProperty(SUBVIEW_FOCUS) == null) {
            SessionMgr.getSessionMgr().setModelProperty(SUBVIEW_FOCUS, Boolean.TRUE);
        } else {
            boolean tmpBoolean = (Boolean) SessionMgr.getSessionMgr().getModelProperty(SUBVIEW_FOCUS);
            subviewFocusCheckBox.setSelected(tmpBoolean);
        }
        
        memoryUsage.setText("Display Memory Usage Meter");
        memoryUsage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });

        memoryUsage.setSelected((Boolean) sessionMgr.getModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY));

        pnlLayoutOptions.add(Box.createVerticalStrut(5));
        pnlLayoutOptions.add(subEditors);
        pnlLayoutOptions.add(Box.createVerticalStrut(5));
        pnlLayoutOptions.add(subviewFocusCheckBox);
        pnlLayoutOptions.add(Box.createVerticalStrut(5));
        pnlLayoutOptions.add(memoryUsage);
        pnlLayoutOptions.add(Box.createVerticalStrut(5));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(pnlLayoutOptions);

        // ------------------------------------------------------------------------------------------------------------
        // Pop-ups
        // ------------------------------------------------------------------------------------------------------------
        
        JPanel popupPanel = new JPanel();
        popupPanel.setBorder(new TitledBorder("Pop-up Information Options"));
        navComplete.setText("Show Navigation/Search complete messages");
        navComplete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });
        popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
        popupPanel.add(Box.createVerticalStrut(5));
        navComplete.setSelected(AutoNavigationMgr.getAutoNavigationMgr().isShowingNavigationCompleteMsgs());
        popupPanel.add(navComplete);
        popupPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(popupPanel);

        // ------------------------------------------------------------------------------------------------------------
        // Memory Settings
        // ------------------------------------------------------------------------------------------------------------

        pnlMemorySetting = new MemorySettingPanel();
        pnlMemorySetting.setBorder(new TitledBorder("Max Memory (Gb)"));        
        pnlMemorySetting.setSettingListener(
                new MemorySettingPanel.SettingListener() {
                    @Override
                    public void settingChanged() {                        
                        settingsChanged = true;
                    }
                }
        );
        mainPanel.add(Box.createVerticalStrut(20));        
        
        mainPanel.add(pnlMemorySetting);

        // ------------------------------------------------------------------------------------------------------------
        // Look and Feel
        // ------------------------------------------------------------------------------------------------------------
        
        JPanel pnlLookAndFeelOptions = new JPanel();
        pnlLookAndFeelOptions.setBorder(new TitledBorder("Look and Feel Options"));

        pnlLookAndFeelOptions.setLayout(new BoxLayout(pnlLookAndFeelOptions, BoxLayout.Y_AXIS));
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();

        for (UIManager.LookAndFeelInfo info : infos) {
            JRadioButton rb = new JRadioButton(info.getName());
            rb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    settingsChanged = true;
                }
            }); 
            if (UIManager.getLookAndFeel().getName().equals(info.getName()))
                rb.setSelected(true);
            buttonLookAndFeelGroup.add(rb);
            buttonToLafMap.put(rb.getModel(), info.getClassName());
            pnlLookAndFeelOptions.add(rb);
            // pnlLookAndFeelOptions.add(Box.createVerticalStrut(5));
        }
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(pnlLookAndFeelOptions);

        // ------------------------------------------------------------------------------------------------------------
        // Image Renderer
        // ------------------------------------------------------------------------------------------------------------
        
        JPanel pnlRendererOptions = new JPanel();
        pnlRendererOptions.setBorder(new TitledBorder("2D Image Renderer"));

        pnlRendererOptions.setLayout(new BoxLayout(pnlRendererOptions, BoxLayout.Y_AXIS));

        String selectedRenderer = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_RENDERER_2D);
        for (RendererType2D type : RendererType2D.values()) {
            JRadioButton  rb = new JRadioButton(type.getName());
            rb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    settingsChanged = true;
                }
            });
            if (selectedRenderer.equals(type.name()))
                rb.setSelected(true);
            rendererGroup.add(rb);
            buttonToRendererMap.put(rb.getModel(), type.name());
            pnlRendererOptions.add(rb);
        }
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(pnlRendererOptions);


        // ------------------------------------------------------------------------------------------------------------
        // Image Loading
        // ------------------------------------------------------------------------------------------------------------

        JPanel imageLoadingPanel = new JPanel();
        imageLoadingPanel.setBorder(new TitledBorder("Image Loading Options"));
        unloadImages.setText("Unload images which are not visible on the screen");
        unloadImages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });
        imageLoadingPanel.setLayout(new BoxLayout(imageLoadingPanel, BoxLayout.Y_AXIS));
        imageLoadingPanel.add(Box.createVerticalStrut(5));
        unloadImages.setSelected((Boolean) sessionMgr.getModelProperty(SessionMgr.UNLOAD_IMAGES_PROPERTY));
        imageLoadingPanel.add(unloadImages);
        imageLoadingPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(imageLoadingPanel);

        
        // ------------------------------------------------------------------------------------------------------------
        // Main Panel
        // ------------------------------------------------------------------------------------------------------------
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(mainPanel);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    public void dispose() {
        sessionMgr.removeSessionModelListener(sessionModelListener);
    }

    public String getName() {
        return "Application Settings";
    }

    class MySessionModelListener implements SessionModelListener {
        public void browserAdded(BrowserModel browserModel) {
        }

        public void browserRemoved(BrowserModel browserModel) {
        }

        public void sessionWillExit() {
        }

        public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
            if (key.equals(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY))
                memoryUsage.setSelected((Boolean) newValue);
            if (key.equals(SUBVIEW_FOCUS))
                subviewFocusCheckBox.setSelected((Boolean) newValue);
            if (key.equals(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY))
                subEditors.setSelected((Boolean) newValue);
        }
    }

    public void cancelChanges() {
        settingsChanged = false;
    }

    public boolean hasChanged() {
        return settingsChanged;
    }

    public String[] applyChanges() {
        settingsChanged = false;
        if (memoryUsage.isSelected() != (Boolean) sessionMgr
                .getModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY)) {
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY, memoryUsage.isSelected());
        }
        if (subEditors.isSelected() != (Boolean) sessionMgr.getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)) {
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY, subEditors.isSelected());
        }
        if (subviewFocusCheckBox.isSelected() != (Boolean) sessionMgr.getModelProperty(SUBVIEW_FOCUS)) {
            sessionMgr.setModelProperty(SUBVIEW_FOCUS, subviewFocusCheckBox.isSelected());
        }
        if (pnlMemorySetting.isChanged()) {
            pnlMemorySetting.saveSettings();
        }
        AutoNavigationMgr.getAutoNavigationMgr().showNavigationCompleteMsgs(navComplete.isSelected());

        try {
            String newLaf = buttonToLafMap.get(buttonLookAndFeelGroup.getSelection());
            if (!newLaf.equals(sessionMgr.getModelProperty(SessionMgr.DISPLAY_LOOK_AND_FEEL))) {
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                        "You will need to restart the application to completely update the look and feel.",
                        "Restart recommended", JOptionPane.INFORMATION_MESSAGE);
                sessionMgr.setModelProperty(SessionMgr.DISPLAY_LOOK_AND_FEEL, newLaf);
            }
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
        
        String newRenderer = buttonToRendererMap.get(rendererGroup.getSelection());
        sessionMgr.setModelProperty(SessionMgr.DISPLAY_RENDERER_2D, newRenderer);

        if (unloadImages.isSelected() != (Boolean) sessionMgr.getModelProperty(SessionMgr.UNLOAD_IMAGES_PROPERTY)) {
            sessionMgr.setModelProperty(SessionMgr.UNLOAD_IMAGES_PROPERTY, unloadImages.isSelected());
        }
        
        return NO_DELAYED_CHANGES;
    }

}