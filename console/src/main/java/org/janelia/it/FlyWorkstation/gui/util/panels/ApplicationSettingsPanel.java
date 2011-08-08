package org.janelia.it.FlyWorkstation.gui.util.panels;

import org.janelia.it.FlyWorkstation.gui.framework.navigation_tools.AutoNavigationMgr;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.roles.PrefEditor;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ApplicationSettingsPanel extends JPanel implements PrefEditor {
    private boolean settingsChanged = false;
    private static final String SUBVIEW_FOCUS = "FocusSubviewsUponNavigation";
    JCheckBox subviewFocusCheckBox = new JCheckBox();
    JCheckBox subEditors = new JCheckBox();
    JCheckBox memoryUsage = new JCheckBox();
    JCheckBox navComplete = new JCheckBox();
    SessionMgr sessionMgr = SessionMgr.getSessionMgr();
    MySessionModelListener sessionModelListener = new MySessionModelListener();
    ButtonGroup buttonGroup = new ButtonGroup();
    Map<ButtonModel, String> buttonToLookAndFeel = new HashMap<ButtonModel, String>();

    public ApplicationSettingsPanel(JFrame parentFrame) {
        try {
            sessionMgr.addSessionModelListener(sessionModelListener);
            jbInit();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getDescription() {
        return "Set various browser preferences, including Look and Feel and layout settings.";
    }


    public String getPanelGroup() {
        return PrefController.SYSTEM_EDITOR;
    }

    private void jbInit() throws Exception {
        JPanel pnlLayoutOptions = new JPanel();
        pnlLayoutOptions.setLayout(new BoxLayout(pnlLayoutOptions, BoxLayout.Y_AXIS));
        pnlLayoutOptions.setBorder(new javax.swing.border.TitledBorder("Browser Options"));
        subEditors.setText("Display SubViews When Available");
        subEditors.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });
        subEditors.setSelected((Boolean) sessionMgr.getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY));

        subviewFocusCheckBox.setText("Focus SubViews Upon Navigation");
        subviewFocusCheckBox.setBounds(new Rectangle(25, 199, 222, 19));
        subviewFocusCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SessionMgr.getSessionMgr().setModelProperty(SUBVIEW_FOCUS, subviewFocusCheckBox.isSelected());
            }
        });
        if (SessionMgr.getSessionMgr().getModelProperty(SUBVIEW_FOCUS) == null) {
            SessionMgr.getSessionMgr().setModelProperty(SUBVIEW_FOCUS, Boolean.TRUE);
        }
        else {
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
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(Box.createVerticalStrut(10));
        this.add(pnlLayoutOptions);

        JPanel popupPanel = new JPanel();
        popupPanel.setBorder(new javax.swing.border.TitledBorder("Pop-up Information Options"));
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
        this.add(Box.createVerticalStrut(20));
        this.add(popupPanel);


        JPanel pnlLookAndFeelOptions = new JPanel();
        pnlLookAndFeelOptions.setBorder(new javax.swing.border.TitledBorder("Look and Feel Options"));

        pnlLookAndFeelOptions.setLayout(new BoxLayout(pnlLookAndFeelOptions, BoxLayout.Y_AXIS));
        pnlLookAndFeelOptions.add(Box.createVerticalStrut(20));
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        JRadioButton rb;
        for (UIManager.LookAndFeelInfo info : infos) {
            rb = new JRadioButton(info.getName());
            rb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    settingsChanged = true;
                }
            });
            if (UIManager.getLookAndFeel().getName().equals(info.getName())) rb.setSelected(true);
            buttonGroup.add(rb);
            buttonToLookAndFeel.put(rb.getModel(), info.getClassName());
            pnlLookAndFeelOptions.add(rb);
            pnlLookAndFeelOptions.add(Box.createVerticalStrut(15));
        }
        this.add(Box.createVerticalStrut(20));
        this.add(pnlLookAndFeelOptions);

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
            if (key.equals(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY)) memoryUsage.setSelected((Boolean) newValue);
            if (key.equals(SUBVIEW_FOCUS)) subviewFocusCheckBox.setSelected((Boolean) newValue);
            if (key.equals(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)) subEditors.setSelected((Boolean) newValue);
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
        if (memoryUsage.isSelected() != (Boolean) sessionMgr.
                getModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY)) {
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY, memoryUsage.isSelected());
        }
        if (subEditors.isSelected() != (Boolean) sessionMgr.
                getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)) {
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY, subEditors.isSelected());
        }
        if (subviewFocusCheckBox.isSelected() != (Boolean) sessionMgr.
                getModelProperty(SUBVIEW_FOCUS)) {
            sessionMgr.setModelProperty(SUBVIEW_FOCUS, subviewFocusCheckBox.isSelected());
        }
        AutoNavigationMgr.getAutoNavigationMgr().showNavigationCompleteMsgs(navComplete.isSelected());
        try {
            SessionMgr.getSessionMgr().setLookAndFeel(buttonToLookAndFeel.get(buttonGroup.getSelection()));
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
        return NO_DELAYED_CHANGES;
    }


}