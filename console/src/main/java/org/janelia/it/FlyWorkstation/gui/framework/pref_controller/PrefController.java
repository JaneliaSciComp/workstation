package org.janelia.it.FlyWorkstation.gui.framework.pref_controller;

import org.janelia.it.FlyWorkstation.gui.framework.roles.PrefEditor;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

/**
 * This is designed so that anyone who introduces a class and needs a user interface
 * to update settings for that class, may define a UI panel and register it with the
 * SessionMgr.  The SessionMgr will then turn around and call the registry method
 * of this Controller.  The developer may then bring up the Preferences frame
 * directly to their specific interface or use the default one.
 * <p/>
 * Upon registration, the name of the tab will be the string used as the key.
 */

public class PrefController {

    private static PrefController prefController = new PrefController();
    // This holds them pre-Construction.
    private Hashtable<Object, Constructor> prefEditorsMap = new Hashtable<Object, Constructor>();
    // This holds them ordered post-Construction.
    private Map<String, Component> orderedEditorMap = new TreeMap<String, Component>(new MyComparator());
    private HashMap<Object, String> classToNameMap = new HashMap<Object, String>();

    private static final String DEFAULT = "Default";
    public static final String SYSTEM_EDITOR = "System";
    public static final String TOOLS_EDITOR = "Tools";
    // List and offer the Panel Categories
    private JFrame parentFrame = null;
    private JDialog mainDialog;
    private JButton cancelButton = new JButton();
    private JTabbedPane tabPane = new JTabbedPane();
    private JButton applyButton = new JButton();
    private int screenWidth, screenHeight;
    JButton okButton = new JButton();
    JPanel dummyPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel tabPanePanel = new JPanel();
    private static int MIN_DIALOG_WIDTH = 550;

    private PrefController() {
        try {
            jbInit();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Singleton enforcement.

    /**
     * Getter for the Singleton.
     */
    public static PrefController getPrefController() {
        return prefController;
    }


    /**
     * Call to bring up the Preferences frame on a specific interface.
     */
    public void getPrefInterface(Object key, JFrame parentFrame) {
        String keyName = "";
        String prefLevel = "";
        this.parentFrame = parentFrame;
        tabPane.removeAll();
        if (mainDialog == null) {
            mainDialog = new JDialog(parentFrame, true);
            buttonPanel.validate();
            buttonPanel.setPreferredSize(new Dimension(MIN_DIALOG_WIDTH, (int) buttonPanel.getPreferredSize().getHeight()));
            mainDialog.getContentPane().setLayout(new BorderLayout());
            mainDialog.getContentPane().add(dummyPanel, BorderLayout.NORTH);
            mainDialog.getContentPane().add(tabPanePanel, BorderLayout.CENTER);
            mainDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        }
        Component component;
        Constructor constructor;
        try {
            if (!(key instanceof String)) {
                // Get the prefLevel of the panel requested.
                constructor = prefEditorsMap.get(key);
                component = (Component) constructor.newInstance(new Object[]{parentFrame});
                prefLevel = ((PrefEditor) component).getPanelGroup();
                keyName = component.getName();
            }
            else prefLevel = (String) key;
            // Go through the panels registered and pull out all those that belong
            // to the target prefLevel.
            for (Object handle : prefEditorsMap.keySet()) {
                constructor = prefEditorsMap.get(handle);
                component = (Component) constructor.newInstance(new Object[]{parentFrame});
                if (component.getName() == null) {
                    component.setName(handle.toString());
                }
                if (((PrefEditor) component).getPanelGroup().equals(prefLevel)) {
                    orderedEditorMap.put((component).getName(), component);
                    classToNameMap.put(handle, component.getName());
                }
            }
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
        if (keyName.equals("")) keyName = DEFAULT;
        redrawTabs(keyName, prefLevel);
        mainDialog.pack();
        centerDialog();
        mainDialog.getRootPane().setDefaultButton(okButton);
        mainDialog.setVisible(true);
    }

    private void redrawTabs(String selectedKeyName, String selectedTabGroup) {
        String tmpName;
        Component tmpComponent;
        tabPane.removeAll();
        for (Object o : orderedEditorMap.keySet()) {
            tmpName = (String) o;
            tmpComponent = orderedEditorMap.get(tmpName);
            if (((PrefEditor) tmpComponent).getPanelGroup().equals(selectedTabGroup)) {
                tabPane.addTab(tmpName, null, tmpComponent, ((PrefEditor) tmpComponent).getDescription());
            }
        }
        if (!selectedKeyName.equals(DEFAULT)) {
            for (int x = 0; x < tabPane.getTabCount(); x++) {
                if (tabPane.getTitleAt(x).equalsIgnoreCase(selectedKeyName)) {
                    tabPane.setSelectedIndex(x);
                    break;
                }
            }
        }
        else tabPane.setSelectedIndex(0);
        mainDialog.setTitle("Preferences: " + ((PrefEditor) tabPane.getSelectedComponent()).getPanelGroup());
    }

    /**
     * Method to register your interface with the controller.
     */
    public void registerPreferenceInterface(Object interfaceKey, Class interfaceClass) throws Exception {
        if (validatePrefEditorClass(interfaceClass)) {
            prefEditorsMap.put(interfaceKey, interfaceClass.getConstructor(new Class[]{JFrame.class}));
        }
        else throw new Exception("Class passed for PrefEditor is not acceptable");
    }


    /**
     * Enforces PrefEditor role for interfaces.  Anything that is not a PrefEditor
     * interface will not be registered.
     */
    private boolean validatePrefEditorClass(Class prefEditor) {
        Class[] interfaces = prefEditor.getInterfaces();
        boolean editorSupported = false;
        for (Class anInterface : interfaces) {
            if (anInterface == PrefEditor.class) {
                editorSupported = true;
                break;
            }
        }
        if (!editorSupported) {
            System.out.println("ERROR! - PrefEditor passed (" + prefEditor + ")is not a PrefController editor!");
            return false;
        }

        Class testClass = prefEditor;
        while (testClass != Object.class && testClass != Component.class) {
            testClass = testClass.getSuperclass();
        }
        if (testClass == Object.class) {
            System.out.println("ERROR! - PrefEditor passed (" + prefEditor + ") is not a java.awt.Component!");
            return false;
        }

        try {
            prefEditor.getConstructor(new Class[]{JFrame.class});
        }
        catch (NoSuchMethodException nsme) {
            System.out.println("ERROR! - PrefEditor passed (" + prefEditor + ") does not have a constructor that takes a JFrame.");
            return false;
        }
        return true;
    }


    /**
     * Removes the tab from the pane and also removes the item from the
     * registry.
     */
    public void deregisterPreferenceInterface(Object interfaceKey) {
        prefEditorsMap.remove(interfaceKey);
        String tmpString = classToNameMap.get(interfaceKey);
        if (tmpString != null) {
            tabPane.remove(orderedEditorMap.get(tmpString));
            classToNameMap.remove(interfaceKey);
            orderedEditorMap.remove(tmpString);
        }
        tabPane.repaint();
    }


    private void jbInit() throws Exception {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;
        dummyPanel.setLayout(new BoxLayout(dummyPanel, BoxLayout.X_AXIS));
        dummyPanel.add(Box.createVerticalStrut(10));

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed();
            }
        });
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyButton_actionPerformed();
            }
        });
        applyButton.setText("Apply");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed();
            }
        });
        okButton.setText("OK");
        okButton.setDefaultCapable(true);
        okButton.setRequestFocusEnabled(true);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        tabPanePanel.setLayout(new BoxLayout(tabPanePanel, BoxLayout.X_AXIS));
        tabPanePanel.add(Box.createHorizontalStrut(10));
        tabPanePanel.add(tabPane);
        tabPanePanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(Box.createVerticalStrut(50));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(applyButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        tabPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                okButton.requestFocus();
            }
        });
    }


    /**
     * Tell all interfaces that their possible changes were cancelled, remove the
     * tabs, and close the frame.
     */
    private void cancelButton_actionPerformed() {
        cancelDialog();
    }


    /**
     * Tell the interface that it's possible changes were applied
     * and keep the frame open.
     */
    private void applyButton_actionPerformed() {
        propagateApplyChanges();
        SessionMgr.getSessionMgr().loginUser();
        if (!SessionMgr.getSessionMgr().isLoggedIn()) {
            Object[] options = {"Fix Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please correct your login information.", "Login Information Invalid",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                return;
            }
            else {
                SessionMgr.getSessionMgr().systemExit();
            }

        }
    }

    /**
     * Tell the interface that it's possible changes were applied, remove the tabs,
     * and close the frame.
     */
    private void okButton_actionPerformed() {
        propagateApplyChanges();
        SessionMgr.getSessionMgr().loginUser();
        if (!SessionMgr.getSessionMgr().isLoggedIn()) {
            Object[] options = {"Fix Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please correct your login information.", "Login Information Invalid", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                return;
            }
            else {
                SessionMgr.getSessionMgr().systemExit();
            }

        }
        tabPane.removeAll();
        // This will clear out the panels and nuke the listeners.
        for (Object o : orderedEditorMap.values()) {
            ((PrefEditor) o).dispose();
        }
        orderedEditorMap = new TreeMap<String, Component>(new MyComparator());
        mainDialog.setVisible(false);
        if (null!=parentFrame) {parentFrame.repaint();}
    }


    /**
     * Go through each interface and call applyChanges for each one.
     */
    private void propagateApplyChanges() {
        String[] delayedApplication;

        for (int x = 0; x < tabPane.getComponentCount(); x++) {
            try {
                if (((PrefEditor) tabPane.getComponentAt(x)).hasChanged()) {
                    delayedApplication = ((PrefEditor) tabPane.getComponentAt(x)).applyChanges();
                    if (delayedApplication != null && delayedApplication.length > 0) {
                        List<String> msgList = new ArrayList<String>();
                        msgList.add("The following changes from " + tabPane.getComponentAt(x).getName() + " will not take effect until the next session:");
                        msgList.addAll(Arrays.asList(delayedApplication));
                        JOptionPane.showMessageDialog(mainDialog, msgList.toArray());
                    }
                }
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
    }


    /**
     * Tell all interfaces that their possible changes were cancelled, remove the
     * tabs, and close the frame.
     */
    private void cancelDialog() {
        if (!SessionMgr.getSessionMgr().isLoggedIn()) {
            Object[] options = {"Fix Login", "Exit Program"};
            final int answer = JOptionPane.showOptionDialog(null, "Please correct your login information.", "Login Information Invalid",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                return;
            }
            else {
                SessionMgr.getSessionMgr().systemExit();
            }

        }
        for (int x = 0; x < tabPane.getComponentCount(); x++) {
            if (((PrefEditor) tabPane.getComponentAt(x)).hasChanged())
                ((PrefEditor) tabPane.getComponentAt(x)).cancelChanges();
        }
        tabPane.removeAll();
        // This will clear out the panels and nuke the listeners.
        for (Object o : orderedEditorMap.values()) {
            ((PrefEditor) o).dispose();
        }
        orderedEditorMap = new TreeMap<String, Component>(new MyComparator());
        mainDialog.setVisible(false);
        parentFrame.repaint();
    }


    /**
     * Helps to ensure good window placement.
     */
    private void centerDialog() {
        //Center the window
        Dimension frameSize = mainDialog.getSize();
        if (frameSize.height > screenHeight) {
            frameSize.height = screenHeight;
        } // Adjust for screen height.

        if (frameSize.width > screenWidth) {
            frameSize.width = screenWidth;
        } // Adjust for screen width.

        mainDialog.setLocation((screenWidth - frameSize.width) / 2, (screenHeight - frameSize.height) / 2);
    }


    private class MyComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    }

}