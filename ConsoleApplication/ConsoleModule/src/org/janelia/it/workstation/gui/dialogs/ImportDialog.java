package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.swing.*;

import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.util.filecache.WebDavUploader;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/23/12
 * Time: 2:05 PM
 */
public class ImportDialog extends ModalDialog {

    public static final String IMPORT_TARGET_FOLDER = "FileImport.TargetFolder";
    public static final String IMPORT_SOURCE_FOLDER = "FileImport.SourceFolder";

    private static final Logger log = LoggerFactory.getLogger(ImportDialog.class);

    private static final String DEFAULT_FOLDER_NAME = "Imported Data";

    private static final String TOOLTIP_TOP_LEVEL_FOLDER =
            "Name of the folder in which data should be loaded with the data.";
    private static final String TOOLTIP_INPUT_DIR =
            "Directory of the tree that should be loaded into the database.";

    private JTextField folderField;
    private RootedEntity rootedEntity;
    private Long folderEntityId;
    private JTextField pathTextField;
    private FilenameFilter selectedChildrenFilter;

    public ImportDialog(String title) {
        setTitle(title);
        setSize(400, 400);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new VerticalLayout(5));

        JPanel attrPanel = new JPanel();
        attrPanel.setLayout(new GridBagLayout());

        JLabel targetFolderNameLabel = new JLabel("Target Folder Name:");
        targetFolderNameLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        folderField = new JTextField(40);
        // Use the previous destination; otherwise, suggest the default user location
        folderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        targetFolderNameLabel.setLabelFor(folderField);

        JLabel pathLabel = new JLabel("Directory or File:");
        pathLabel.setToolTipText(TOOLTIP_INPUT_DIR);

        pathTextField = new JTextField(40);

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        final String importSourceFolderName = (String)
                sessionMgr.getModelProperty(IMPORT_SOURCE_FOLDER);
        if (! isEmpty(importSourceFolderName)) {
            final File importSourceFolder = new File(importSourceFolderName.trim());
            if (importSourceFolder.exists()) {
                pathTextField.setText(importSourceFolder.getAbsolutePath());
            }
        }

        String chooseFileText = null;
        ImageIcon chooseFileIcon = null;
        try {
            chooseFileIcon = Utils.getClasspathImage("magnifier.png");
        } catch (FileNotFoundException e) {
            log.warn("failed to load button icon", e);
            chooseFileText = "...";
        }

        JButton chooseFileButton = new JButton(chooseFileText, chooseFileIcon);
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File currentDir = new File(pathTextField.getText());
                if (! currentDir.exists()) {
                    currentDir = null;
                }
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setCurrentDirectory(currentDir);
                int returnVal = fileChooser.showOpenDialog(ImportDialog.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 5;
        c.gridx = 0;
        c.gridy = 0;
        attrPanel.add(targetFolderNameLabel, c);

        c.gridx = 1;
        attrPanel.add(folderField, 1);

        c.gridx = 0;
        c.gridy = 1;
        attrPanel.add(pathLabel, c);

        c.gridx = 1;
        attrPanel.add(pathTextField, c);

        c.gridx = 2;
        attrPanel.add(chooseFileButton, c);

        mainPanel.add(attrPanel);
        add(mainPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("Import");
        okButton.setToolTipText("Import a directory or file.");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    handleOkPress();
                } catch (Exception e1) {
                    log.error("import dialog failure", e1);
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                                                  e1.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog.");
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

        selectedChildrenFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir,
                                  String name) {
                // exclude '.' directories and files (including '.DS_Store' files)
                return (name.charAt(0) != '.');
            }
        };
    }

    public void showDialog(RootedEntity rootedEntity) {

        this.rootedEntity = rootedEntity;
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();

        String folderName;
        if (rootedEntity == null) {
            folderField.setEnabled(true);
            folderEntityId = null;
            final String modelName = (String)
                    sessionMgr.getModelProperty(IMPORT_TARGET_FOLDER);
            if ((modelName != null) && (modelName.trim().length() > 0)) {
                folderName = modelName;
            } else {
                folderName = SessionMgr.getUsername() + " " + DEFAULT_FOLDER_NAME;
            }
        } else {
            

            String rootEntityType = rootedEntity.getEntity().getEntityTypeName();
            if (EntityConstants.TYPE_SAMPLE.equals(rootEntityType)) {                
                folderField.setEnabled(false);
                folderName = rootedEntity.getName()+"/"+DEFAULT_FOLDER_NAME;
                folderEntityId = rootedEntity.getEntityId();
            }
            else {
                folderField.setEnabled(false);
                folderName = rootedEntity.getName();
                folderEntityId = rootedEntity.getEntityId();
            }
        }

        folderField.setText(folderName);

        packAndShow();
    }

    private void handleOkPress() throws Exception {

        String folderName = folderField.getText();

        if (folderName.contains("/")) {
            folderName = folderName.split("/")[1];
        }
        
        if (isEmpty(folderName)) {
            throw new IllegalArgumentException(
                    "Please specify a folder into which the files should be imported.");
        }

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();

        // save the user preferences for later
        sessionMgr.setModelProperty(IMPORT_TARGET_FOLDER, folderName);

        int fileCount = 1;
        double transferMegabytes = 0;
        final File selectedFile = new File(pathTextField.getText());
        List<File> selectedChildren = null;

        if (selectedFile.exists()) {

            // save the user preferences for later
            sessionMgr.setModelProperty(IMPORT_SOURCE_FOLDER, selectedFile.getAbsolutePath());

            if (selectedFile.isDirectory()) {

                selectedChildren = new ArrayList<File>();
                addSelectedChildren(selectedFile, selectedChildren);
                fileCount = selectedChildren.size();

                if (fileCount == 0) {
                    throw new IllegalArgumentException(
                            "No eligible import files were found in " +
                            selectedFile.getAbsolutePath() + ".");
                } else {
                    for (File child : selectedChildren) {
                        transferMegabytes += (child.length() / 1000000.0);
                    }
                }

            } else {
                transferMegabytes = selectedFile.length() / 1000000.0;
            }

        } else {

            throw new IllegalArgumentException(
                    "Please specify a valid file or directory to import.");

        }

        MessageFormat form = new MessageFormat(
                "You have selected {0,choice,1#1 file|1<{0,number,integer} files} " +
                "that contain a total of {1,number,#.#} {2}bytes.");
        String msg;

        final double transferGigabytes = transferMegabytes / 1000.0;
        if (transferGigabytes > 0.999999999) {
            msg = form.format(new Object[] {fileCount, transferGigabytes, "giga"});
        } else if (transferMegabytes > 0.999999) {
            msg = form.format(new Object[] {fileCount, (int) transferMegabytes, "mega"});
        } else if (transferMegabytes > 0.000999) {
            final int transferKilobytes = (int) (transferMegabytes * 1000);
            msg = form.format(new Object[] {fileCount, transferKilobytes, "kilo"});
        } else {
            final int transferBytes = (int) (transferMegabytes * 1000000);
            msg = form.format(new Object[] {fileCount, transferBytes, ""});
        }

        final int maxGigabytes = 20;
        if (transferGigabytes > maxGigabytes) {
            throw new IllegalArgumentException(
                    msg + "  This exceeds the maximum import limit of " +
                    maxGigabytes + " gigabytes.");
        }

        boolean continueWithImport = true;
        if ((transferGigabytes > 1) || (fileCount > 9)) {
            msg = msg + "  Do you wish to continue with the import?";
            final int areYouSure = JOptionPane.showConfirmDialog(this,
                                                                 msg,
                                                                 "Confirm Large Import",
                                                                 JOptionPane.YES_NO_OPTION);
            continueWithImport = (areYouSure == JOptionPane.YES_OPTION);
        }

        if (continueWithImport) {
            // close import dialog and run import in background thread
            this.setVisible(false);
            runImport(selectedFile, selectedChildren, folderName, folderEntityId);
        }
    }

    private void addSelectedChildren(File directory,
                                     List<File> selectedChildren) {
        if (directory.isDirectory()) {
            final File[] directoryFiles = directory.listFiles(selectedChildrenFilter);
            if (directoryFiles != null) {
                for (File child : directoryFiles) {
                    if (child.isDirectory()) {
                        addSelectedChildren(child, selectedChildren);
                    } else {
                        selectedChildren.add(child);
                    }
                }
            }
        }
    }

    private boolean isEmpty(String value) {
        return ((value == null) || (value.trim().length() == 0));
    }

    private void runImport(final File selectedFile,
                           final List<File> selectedChildren,
                           final String importFolderName,
                           final Long importFolderId) {

        try {            
            org.janelia.it.workstation.shared.workers.BackgroundWorker executeWorker = new TaskMonitoringWorker() {
    
                @Override
                public String getName() {
                    return "Import "+selectedFile.getName();
                }
    
                @Override
                protected void doStuff() throws Exception {

                    setStatus("Submitting task");
                    
                    Long taskId = startImportFilesTask(selectedFile,
                                selectedChildren,
                                importFolderName,
                                importFolderId);
                    
                    setTaskId(taskId);
                    
                    setStatus("Grid execution");
                    
                    // Wait until task is finished
                    super.doStuff(); 
                    
                    if (isCancelled()) throw new CancellationException();
                    setStatus("Done importing");
                }
                
                @Override
                public Callable<Void> getSuccessCallback() {
                    return new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            final Browser browser = SessionMgr.getBrowser();
                            final org.janelia.it.workstation.gui.framework.outline.EntityOutline entityOutline = browser.getEntityOutline();
                            entityOutline.totalRefresh(true, new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    
                                    SimpleWorker worker = new SimpleWorker() {
                                        
                                        @Override
                                        protected void doStuff() throws Exception {
                                            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().refreshChildren(rootedEntity.getEntity());
                                        }
                                        
                                        @Override
                                        protected void hadSuccess() {
                                            RootedEntity importFolder = rootedEntity.getChildByName(importFolderName);
                                            if (importFolder!=null) {
                                                SessionMgr.getBrowser().getEntityOutline().expandByUniqueId(importFolder.getUniqueId());
                                            }
                                        }
                                        
                                        @Override
                                        protected void hadError(Throwable error) {
                                            SessionMgr.getSessionMgr().handleException(error);
                                        }
                                    };
                                    
                                    worker.execute();
                                    
                                    return null;
                                }
                            });
                            return null;
                        }
                    };
                }
            };
    
            executeWorker.executeWithEvents();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        
    }

    private long startImportFilesTask(File selectedFile,
                             List<File> selectedChildren,
                             String importTopLevelFolderName,
                             Long importTopLevelFolderId) throws Exception {

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        final org.janelia.it.workstation.api.entity_model.management.ModelMgr modelMgr = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr();
        final WebDavUploader uploader = new WebDavUploader(sessionMgr.getWebDavClient());

        String uploadPath;
        if (selectedChildren == null) {
            uploadPath = uploader.uploadFile(selectedFile);
        } else {
            uploadPath = uploader.uploadFiles(selectedChildren, selectedFile);
        }

        final String owner = SessionMgr.getSubjectKey();
        final String process = "FileTreeLoader";
        final boolean filesUploadedFlag = true;
        Task task = new FileTreeLoaderPipelineTask(new HashSet<Node>(),
                                                   owner,
                                                   new ArrayList<Event>(),
                                                   new HashSet<TaskParameter>(),
                                                   uploadPath,
                                                   importTopLevelFolderName,
                                                   filesUploadedFlag,
                                                   importTopLevelFolderId);
        task.setJobName("Import Files Task");
        task = modelMgr.saveOrUpdateTask(task);

        // Submit the job
        modelMgr.submitJob(process, task);
        
        return task.getObjectId();
    }
}
