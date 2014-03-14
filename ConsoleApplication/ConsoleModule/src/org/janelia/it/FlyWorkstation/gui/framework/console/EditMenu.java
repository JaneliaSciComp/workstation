package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.api.entity_model.access.observer.ModifyManagerObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModifyMgr;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


/**
 * This class provides a EditMenu specific to the FlyGraph application.
 * <p/>
 * Initially written by: Peter Davies
 * @Deprecated most items are not on the menu. Others moved to NetBeans menu.
 */
public class EditMenu extends JMenu {
    private JMenuItem menuUnDo;
    private JMenuItem menuReDo;
    private JMenuItem menuCut;
    private JMenuItem menuCopy;
    private JMenuItem menuPaste;
    private JMenuItem menuPrefSystem;
    private JMenuItem menuPrefViewer;
    private JMenu menuSetPreferences;
    private static String fileSep = File.separator;
    private static final String EXPORT_IMPORT_LOCATION = "PreferenceExportImportLocation";
    private String userHomeDir;
    private final Browser browser;
    private Action copyAction;
    private Action cutAction;
    private Action pasteAction;

    public EditMenu(Browser browser) {
        userHomeDir = SessionMgr.getSessionMgr().getApplicationOutputDirectory();
        setText("Edit");
        this.browser = browser;
        this.setMnemonic('E');
        menuUnDo = new JMenuItem("Undo", 'U');
        menuUnDo.setHorizontalTextPosition(SwingConstants.RIGHT);
        menuUnDo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                                                       InputEvent.CTRL_MASK,
                                                       false));
        menuUnDo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                unDo_actionPerformed(e);
            }
        });
        menuUnDo.setEnabled(false);
        add(menuUnDo);

        menuReDo = new JMenuItem("Redo", 'R');
        menuReDo.setHorizontalTextPosition(SwingConstants.RIGHT);
        menuReDo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                                                       InputEvent.CTRL_MASK,
                                                       false));

        menuReDo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reDo_actionPerformed(e);
            }
        });
        menuReDo.setEnabled(false);
        add(menuReDo);
        add(new JSeparator());
        cutAction = new MyCutAction();
        cutAction.putValue(Action.NAME, "Cut");
        menuCut = new JMenuItem(cutAction);
        menuCut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.Event.META_MASK));

        copyAction = new MyCopyAction();
        copyAction.putValue(Action.NAME, "Copy");
        menuCopy = new JMenuItem(copyAction);
        menuCopy.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.Event.META_MASK));

        pasteAction = new MyPasteAction();
        pasteAction.putValue(Action.NAME, "Paste");
        menuPaste = new JMenuItem(pasteAction);
        menuPaste.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.Event.META_MASK));

        menuSetPreferences = new JMenu("Preferences");
        menuSetPreferences.setMnemonic('P');
        add(menuSetPreferences);

        menuPrefSystem = new JMenuItem("System...", 'S');
        menuPrefSystem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_MASK, false));
        menuPrefSystem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                establishPrefController(PrefController.APPLICATION_EDITOR);
            }
        });
        menuSetPreferences.add(menuPrefSystem);

        menuPrefViewer = new JMenuItem("Viewer", 'V');
        menuPrefViewer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                establishPrefController(PrefController.VIEWER_EDITOR);
            }
        });
        menuSetPreferences.add(menuPrefViewer);

    }

    public void setPopupMenuVisible(boolean b) {
        menuCut.setEnabled(cutAction.isEnabled());
        menuCopy.setEnabled(copyAction.isEnabled());
        menuPaste.setEnabled(pasteAction.isEnabled());
        super.setPopupMenuVisible(b);
    }

    private void establishPrefController(String prefLevel) {
        browser.repaint();
        PrefController.getPrefController().getPrefInterface(prefLevel, browser);
    }

    /**
     * This method exists to help the pref file export and import actions.
     */
    private void copyFile(File oldFile, File newFile) {
        try {
            FileReader in = new FileReader(oldFile);
            FileWriter out = new FileWriter(newFile);
            int c;

            while ((c = in.read()) != -1) out.write(c);

            in.close();
            out.close();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    private void unDo_actionPerformed(ActionEvent e) {
        //  try{
        ModifyMgr.getModifyMgr().undoCommand();

        /*
                            }catch(Exception ex){
              JOptionPane.showMessageDialog(browser,
              ex.getMessage() ,
              "Warning!", JOptionPane.PLAIN_MESSAGE);
        }
        */
    }

    private void reDo_actionPerformed(ActionEvent e) {
        // try{
        ModifyMgr.getModifyMgr().redoCommand();

        /*
        }catch(Exception ex){
         JOptionPane.showMessageDialog(browser,
         ex.getMessage() ,
         "Warning!", JOptionPane.PLAIN_MESSAGE);

        }
        */
    }

    class CommandObserver extends ModifyManagerObserverAdapter {
        public void noteCanUndo(String undoString) {
            if (undoString != null) {
                menuUnDo.setText("Undo " + undoString);
            } else {
                menuUnDo.setText("Undo");
            }

            menuUnDo.setEnabled(true);
        }

        public void noteCanRedo(String redoString) {
            if (redoString != null) {
                menuReDo.setText("Redo " + redoString);
            } else {
                menuReDo.setText("Redo");
            }

            menuReDo.setEnabled(true);
        }

        public void noteNoUndo() {
            menuUnDo.setText("Undo");
            menuUnDo.setEnabled(false);
        }

        public void noteNoRedo() {
            menuReDo.setText("Redo");
            menuReDo.setEnabled(false);
        }
    }

    class MyCopyAction extends DefaultEditorKit.CopyAction {
        public boolean isEnabled() {
            return (super.isEnabled() && getFocusedComponent() != null);
        }
    }

    class MyPasteAction extends DefaultEditorKit.PasteAction {
        public boolean isEnabled() {
            return (super.isEnabled() && (getFocusedComponent() != null) && getFocusedComponent().isEditable());
        }
    }

    class MyCutAction extends DefaultEditorKit.CutAction {
        public boolean isEnabled() {
            return (super.isEnabled() && (getFocusedComponent() != null) && getFocusedComponent().isEditable());
        }
    }
}