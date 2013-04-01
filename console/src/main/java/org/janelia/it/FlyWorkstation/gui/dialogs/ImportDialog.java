package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/23/12
 * Time: 2:05 PM
 */
public class ImportDialog extends ModalDialog {

    private static final String DEFAULT_FOLDER_NAME = " Imported Data";

    public static final String IMPORT_TARGET_FOLDER = "FileImport.TargetFolder";
    public static final String IMPORT_SOURCE_FOLDER = "FileImport.SourceFolder";
    private static final String TOOLTIP_INPUT_DIR       = "Directory of the tree that should be loaded into the database";
    private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder which should be loaded with the data";

    private static final int REFRESH_DELAY = 3000;

    private final JTextField folderField;
    private JTextField pathTextField;
    private JFileChooser fileChooser;

    private long taskID;

    private Timer refreshTimer;

    public ImportDialog(String title) {
        setTitle(title);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new VerticalLayout(5));

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createTitledBorder("Basic Options"));

        JLabel topLevelFolderLabel = new JLabel("Folder Name");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        folderField = new JTextField(40);
        // Use the previous destination; otherwise, suggest the default user location
        if (null!=SessionMgr.getSessionMgr().getModelProperty(IMPORT_TARGET_FOLDER)&&
            !"".equals(SessionMgr.getSessionMgr().getModelProperty(IMPORT_TARGET_FOLDER))) {
            folderField.setText((String)SessionMgr.getSessionMgr().getModelProperty(IMPORT_TARGET_FOLDER));
        }
        else {
            folderField.setText(SessionMgr.getUsername()+DEFAULT_FOLDER_NAME);
        }
        folderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderLabel.setLabelFor(folderField);
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 5;
        c.gridx = 0; c.gridy = 0;
        attrPanel.add(topLevelFolderLabel, c);
        c.gridx = 1;
        attrPanel.add(folderField, 1);

        String pathText="";
        // Figure out the user path preference
        if (null!=SessionMgr.getSessionMgr().getModelProperty(IMPORT_SOURCE_FOLDER)&&
                !"".equals(SessionMgr.getSessionMgr().getModelProperty(IMPORT_SOURCE_FOLDER))) {
            pathText=((String)SessionMgr.getSessionMgr().getModelProperty(IMPORT_TARGET_FOLDER));
        }
        else {
            pathText = ConsoleProperties.getString("remote.defaultLinuxPath");
        }
        pathTextField = new JTextField(40);
        pathTextField.setText(pathText);
        setSize(400, 400);
        File pathTest = new File(pathText);
        c.gridx = 0; c.gridy = 1;
        JLabel pathLabel = new JLabel("Path:");
        pathLabel.setToolTipText(TOOLTIP_INPUT_DIR);
        attrPanel.add(new JLabel("Path:"), c);
        c.gridx = 1;
        attrPanel.add(pathTextField, c);
        if (pathTest.exists() && pathTest.canRead()) {
            fileChooser  = new JFileChooser(pathTest);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            JButton _filePathButton = null;
            try {
                _filePathButton = new JButton(Utils.getClasspathImage("magnifier.png"));
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (_filePathButton != null) {
                _filePathButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = fileChooser.showOpenDialog(ImportDialog.this);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                        }
                    }
                });
            }
            c.gridx = 2;
            attrPanel.add(_filePathButton, c);
        }

        JPanel advancedOptionsPanel = new JPanel();
        advancedOptionsPanel.setLayout(new GridBagLayout());
        advancedOptionsPanel.setBorder(BorderFactory.createTitledBorder("Advanced Options"));
        advancedOptionsPanel.add(new JLabel("File Types:"));
        advancedOptionsPanel.add(getMimeTypeChooser());
        advancedOptionsPanel.add(new JLabel("Entity Types"));
        advancedOptionsPanel.add(getEntityTypeChooser());

        mainPanel.add(attrPanel);
//        mainPanel.add(advancedOptionsPanel);

        add(mainPanel, BorderLayout.CENTER);
//        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/3, 2, 6, 6, 6, 6);



        JButton okButton = new JButton("Import");
        okButton.setToolTipText("Import a directory or file");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = pathTextField.getText();
                if (null==path || "".equals(path) || !(path.startsWith("/groups/") || path.startsWith("/archive/"))) {
                    JOptionPane.showMessageDialog(ImportDialog.this,
                            "Please define a valid network path (/groups/...)", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                runImport();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);

        add(buttonPane, BorderLayout.SOUTH);

    }

    public void runImport() {
        Utils.setWaitingCursor(this);

        SimpleWorker executeWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                startImport();
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(ImportDialog.this);
                Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
                browser.setPerspective(Perspective.ImageBrowser);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
                Utils.setDefaultCursor(ImportDialog.this);
                JOptionPane.showMessageDialog(ImportDialog.this,
                        "Error submitting job: "+error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        executeWorker.execute();
    }

    public void showDialog() {
        packAndShow();
    }

    public void setTargetFolder(Entity parentEntity) {
        folderField.setText(parentEntity.getName());
    }

    private JComboBox getMimeTypeChooser() {
        JComboBox mimeSelector = new JComboBox();
//        Field[] fields = MediaType.class.getFields();
//        for (Field field : fields) {
//            if (field.getClass().getName().equals(MediaType.class.getName())) {
//                try {
//                    mimeSelector.addItem(((MediaType)Class.forName(field.getClass().getName()).newInstance()).subtype());
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        return mimeSelector;
    }

    private JComboBox getEntityTypeChooser() {
        JComboBox types = new JComboBox();
        Field[] fields = EntityConstants.class.getFields();
        for (Field field : fields) {
            if (field.getName().startsWith("TYPE_")) {types.addItem(field.getName());}
        }
        return types;
    }

    private void startImport() {
        try {
            String process;
            Task task;
            String owner = SessionMgr.getSubjectKey();
            process = "FileTreeLoader";

            String path = pathTextField.getText().trim();
            String folderName = folderField.getText().trim();
            task = new FileTreeLoaderPipelineTask(new HashSet<Node>(),
                    owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), path, folderName);
            task.setJobName("Import Files Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
            taskID = task.getObjectId();
            // Before firing off, save the user preferences for later.
            SessionMgr.getSessionMgr().setModelProperty(IMPORT_TARGET_FOLDER, folderField.getText().trim());
            SessionMgr.getSessionMgr().setModelProperty(IMPORT_SOURCE_FOLDER, pathTextField.getText().trim());

            // Submit the job
            // todo Should do this the right way and not use the explicit method
//            ((ConsoleMenuBar)(SessionMgr.getBrowser().getJMenuBar())).modifyImageState(true);
            ModelMgr.getModelMgr().submitJob(process, task.getObjectId());
            // todo remove this thread sleep
//            Thread.sleep(10000);
            refreshTimer = new Timer(REFRESH_DELAY, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Task importCompleteYet = ModelMgr.getModelMgr().getTaskById(taskID);
                        if (importCompleteYet.isDone()) {
                            refreshTimer.stop();
//                            ((ConsoleMenuBar)(SessionMgr.getBrowser().getJMenuBar())).modifyImageState(false);
                            SessionMgr.getBrowser().getEntityOutline().refresh();
                        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
            refreshTimer.setInitialDelay(0);
            refreshTimer.start();

        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
