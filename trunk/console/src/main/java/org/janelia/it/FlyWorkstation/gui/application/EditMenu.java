package org.janelia.it.FlyWorkstation.gui.application;

import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;

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
 */
public class EditMenu extends JMenu {
    private JMenuItem menuUnDo;
    private JMenuItem menuReDo;
    private JMenuItem menuCut;
    private JMenuItem menuCopy;
    private JMenuItem menuPaste;
    private JMenuItem menuPrefSystem;
    //    private JMenuItem menuPrefSubView;
//    private JMenuItem menuPrefMainView;
    private JMenuItem menuPrefExport;
    private JMenuItem menuPrefImport;
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
//        menuUnDo = new JMenuItem("Undo", 'U');
//        menuUnDo.setHorizontalTextPosition(SwingConstants.RIGHT);
//        menuUnDo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
//                                                       InputEvent.CTRL_MASK,
//                                                       false));
//        menuUnDo.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                unDo_actionPerformed(e);
//            }
//        });
//        menuUnDo.setEnabled(false);
//        add(menuUnDo);
//
//        menuReDo = new JMenuItem("Redo", 'R');
//        menuReDo.setHorizontalTextPosition(SwingConstants.RIGHT);
//        menuReDo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
//                                                       InputEvent.CTRL_MASK,
//                                                       false));
//
//        menuReDo.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                reDo_actionPerformed(e);
//            }
//        });
//        menuReDo.setEnabled(false);
//        add(menuReDo);
//        add(new JSeparator());
//        cutAction = new MyCutAction();
//        cutAction.putValue(Action.NAME, "Cut");
//        menuCut = new JMenuItem(cutAction);
//        menuCut.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.Event.META_MASK));
//        add(menuCut);
//
//        copyAction = new MyCopyAction();
//        copyAction.putValue(Action.NAME, "Copy");
//        menuCopy = new JMenuItem(copyAction);
//        menuCopy.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.Event.META_MASK));
//        add(menuCopy);
//
//        pasteAction = new MyPasteAction();
//        pasteAction.putValue(Action.NAME, "Paste");
//        menuPaste = new JMenuItem(pasteAction);
//        menuPaste.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.Event.META_MASK));
//        add(menuPaste);
//
        menuSetPreferences = new JMenu("Preferences");
        menuSetPreferences.setMnemonic('P');
        add(menuSetPreferences);

//        menuPrefMainView = new JMenuItem("Genomic Axis Annotation View...", 'M');
//        menuPrefMainView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,
//                                                               InputEvent.CTRL_MASK,
//                                                               false));
//        menuPrefMainView.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                establishPrefController(
//                        PrefController.GENOMIC_AXIS_ANNOTATION_VIEW_EDITOR);
//            }
//        });
//        menuSetPreferences.add(menuPrefMainView);
//
//        menuPrefSubView = new JMenuItem("SubViews...", 'V');
//        menuPrefSubView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2,
//                                                              InputEvent.CTRL_MASK,
//                                                              false));
//        menuPrefSubView.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                establishPrefController(PrefController.SUB_VIEW_EDITOR);
//            }
//        });
//        menuSetPreferences.add(menuPrefSubView);

        menuPrefSystem = new JMenuItem("System...", 'S');
        menuPrefSystem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_MASK, false));
        menuPrefSystem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                establishPrefController(PrefController.APPLICATION_EDITOR);
            }
        });
        menuSetPreferences.add(menuPrefSystem);

        menuPrefExport = new JMenuItem("Export Preference File...", 'x');
        menuPrefExport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String targetDir = userHomeDir;

                    if (SessionMgr.getSessionMgr().getModelProperty(EXPORT_IMPORT_LOCATION) != null) {
                        targetDir = (String) SessionMgr.getSessionMgr().getModelProperty(EXPORT_IMPORT_LOCATION);
                    }

                    FileChooser tmpExportChooser = new FileChooser(userHomeDir);
                    tmpExportChooser.setDialogTitle("Select File To Export");

                    int ans = tmpExportChooser.showDialog(EditMenu.this.browser, "OK");

                    if (ans == FileChooser.CANCEL_OPTION) {
                        return;
                    }

                    File targetToExport = tmpExportChooser.getSelectedFile();

                    if (targetToExport == null) {
                        return;
                    }

                    FileChooser tmpDestChooser = new FileChooser(targetDir);
                    tmpDestChooser.setDialogTitle("Select File Destination");
                    tmpDestChooser.setFileSelectionMode(FileChooser.DIRECTORIES_ONLY);
                    ans = tmpDestChooser.showDialog(EditMenu.this.browser, "OK");

                    if (ans == FileChooser.CANCEL_OPTION) {
                        return;
                    }

                    // Copy file to targetDir here.
                    String destDir = tmpDestChooser.getSelectedFile().getAbsolutePath();

                    if ((destDir == null) || destDir.equals("")) {
                        return;
                    }

                    File newFile = new File(destDir + fileSep + targetToExport.getName());
                    copyFile(targetToExport, newFile);

                    /**
                     * Save preference if the user has changed export/import directory.
                     * Assuming that exports and imports occur in from the same directory.
                     */
                    if ((destDir != null) && !destDir.equals(targetDir)) {
                        SessionMgr.getSessionMgr().setModelProperty(EXPORT_IMPORT_LOCATION, destDir);
                    }
                }
                catch (Exception ex) {
                    SessionMgr.getSessionMgr().handleException(ex);
                }
            }
        });
        menuSetPreferences.add(menuPrefExport);

        menuPrefImport = new JMenuItem("Import Preference File...", 'I');
        menuPrefImport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String targetDir = userHomeDir;

                    if (SessionMgr.getSessionMgr().getModelProperty(EXPORT_IMPORT_LOCATION) != null) {
                        targetDir = (String) SessionMgr.getSessionMgr().getModelProperty(EXPORT_IMPORT_LOCATION);
                    }

                    FileChooser tmpImportChooser = new FileChooser(targetDir);
                    tmpImportChooser.setDialogTitle("Select File To Import");

                    int ans = tmpImportChooser.showDialog(EditMenu.this.browser, "OK");

                    if (ans == FileChooser.CANCEL_OPTION) {
                        return;
                    }

                    File targetToImport = tmpImportChooser.getSelectedFile();

                    if (targetToImport == null) {
                        return;
                    }

                    String destDir = userHomeDir + fileSep;
                    File newFile = new File(destDir + targetToImport.getName());
                    copyFile(targetToImport, newFile);

                    /**
                     * Save preference if the user has changed export/import directory.
                     * Assuming that exports and imports occur in from the same directory.
                     */
                    String newDir = tmpImportChooser.getCurrentDirectory().getAbsolutePath();

                    if ((newDir != null) && !newDir.equals(targetDir)) {
                        SessionMgr.getSessionMgr().setModelProperty(EXPORT_IMPORT_LOCATION, newDir);
                    }
                }
                catch (Exception ex) {
                    SessionMgr.getSessionMgr().handleException(ex);
                }
            }
        });
        menuSetPreferences.add(menuPrefImport);

//        ModifyManager.getModifyMgr().addObserver(new CommandObserver());
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

//    private void unDo_actionPerformed(ActionEvent e) {
//        //  try{
//        ModifyManager.getModifyMgr().undoCommand();
//
//        /*
//                            }catch(Exception ex){
//              JOptionPane.showMessageDialog(browser,
//              ex.getMessage() ,
//              "Warning!", JOptionPane.PLAIN_MESSAGE);
//        }
//        */
//    }
//
//    private void reDo_actionPerformed(ActionEvent e) {
//        // try{
//        ModifyManager.getModifyMgr().redoCommand();
//
//        /*
//        }catch(Exception ex){
//         JOptionPane.showMessageDialog(browser,
//         ex.getMessage() ,
//         "Warning!", JOptionPane.PLAIN_MESSAGE);
//
//        }
//        */
//    }

//    class CommandObserver extends ModifyManagerObserverAdapter {
//        public void noteCanUndo(String undoString) {
//            if (undoString != null) {
//                menuUnDo.setText("Undo " + undoString);
//            } else {
//                menuUnDo.setText("Undo");
//            }
//
//            menuUnDo.setEnabled(true);
//        }
//
//        public void noteCanRedo(String redoString) {
//            if (redoString != null) {
//                menuReDo.setText("Redo " + redoString);
//            } else {
//                menuReDo.setText("Redo");
//            }
//
//            menuReDo.setEnabled(true);
//        }
//
//        public void noteNoUndo() {
//            menuUnDo.setText("Undo");
//            menuUnDo.setEnabled(false);
//        }
//
//        public void noteNoRedo() {
//            menuReDo.setText("Redo");
//            menuReDo.setEnabled(false);
//        }
//    }

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