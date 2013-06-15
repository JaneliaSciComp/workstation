package org.janelia.it.FlyWorkstation.gui.dialogs;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavUploader;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/23/12
 * Time: 2:05 PM
 */
public class ImportDialog extends ModalDialog {

    public static final String IMPORT_TARGET_FOLDER = "FileImport.TargetFolder";
    public static final String IMPORT_SOURCE_FOLDER = "FileImport.SourceFolder";

    private static final Logger LOG = LoggerFactory.getLogger(ImportDialog.class);

    private static final String DEFAULT_FOLDER_NAME = " Imported Data";

    private static final String TOOLTIP_TOP_LEVEL_FOLDER =
            "Name of the folder in which data should be loaded with the data.";
    private static final String TOOLTIP_INPUT_DIR =
            "Directory of the tree that should be loaded into the database.";

    private JTextField folderField;
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

        JLabel topLevelFolderLabel = new JLabel("Target Folder Name:");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        folderField = new JTextField(40);
        // Use the previous destination; otherwise, suggest the default user location
        folderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderLabel.setLabelFor(folderField);

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
            LOG.warn("failed to load button icon", e);
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
        attrPanel.add(topLevelFolderLabel, c);

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
                    LOG.error("import dialog failure", e1);
                    JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
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

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();

        String importTopLevelFolderName;
        if (rootedEntity == null) {
            folderField.setEnabled(true);
            folderEntityId = null;
            final String modelName = (String)
                    sessionMgr.getModelProperty(IMPORT_TARGET_FOLDER);
            if ((modelName != null) && (modelName.trim().length() > 0)) {
                importTopLevelFolderName = modelName;
            } else {
                importTopLevelFolderName = SessionMgr.getUsername() + DEFAULT_FOLDER_NAME;
            }
        } else {
            folderField.setEnabled(false);
            importTopLevelFolderName = rootedEntity.getName();
            folderEntityId = rootedEntity.getEntityId();
        }

        folderField.setText(importTopLevelFolderName);

        packAndShow();
    }

    private void handleOkPress() throws Exception {

        final String importTopLevelFolderName = folderField.getText();

        if (isEmpty(importTopLevelFolderName)) {
            throw new IllegalArgumentException(
                    "Please specify a folder into which the files should be imported.");
        }

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();

        // save the user preferences for later
        sessionMgr.setModelProperty(IMPORT_TARGET_FOLDER, importTopLevelFolderName);

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
            runImport(selectedFile, selectedChildren, importTopLevelFolderName, folderEntityId);
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
                           final String importTopLevelFolderName,
                           final Long importTopLevelFolderId) {

        SimpleWorker executeWorker = new SimpleWorker() {

            private ImportProgressMonitor importProgressMonitor;

            @Override
            protected void doStuff() throws Exception {
                importProgressMonitor = new ImportProgressMonitor();
                importFiles(selectedFile,
                            selectedChildren,
                            importTopLevelFolderName,
                            importTopLevelFolderId,
                            importProgressMonitor);
            }

            @Override
            protected void hadSuccess() {
                closeMonitorAndRefreshEnityOutline();
                JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
                                              "Your files were imported succesfully.",
                                              "Import Complete",
                                              JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void hadError(Throwable error) {
                LOG.error("failed import", error);
                closeMonitorAndRefreshEnityOutline();
                ModelMgr.getModelMgr().handleException(error);
            }

            private void closeMonitorAndRefreshEnityOutline() {
                importProgressMonitor.close();
                final Browser browser = SessionMgr.getBrowser();
                final EntityOutline entityOutline = browser.getEntityOutline();
                entityOutline.refresh(true, true, null);
            }
        };

        executeWorker.execute();
    }

    private void importFiles(File selectedFile,
                             List<File> selectedChildren,
                             String importTopLevelFolderName,
                             Long importTopLevelFolderId,
                             ImportProgressMonitor importProgressMonitor) throws Exception {

        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        final ModelMgr modelMgr = ModelMgr.getModelMgr();
        final WebDavUploader uploader = new WebDavUploader(sessionMgr.getWebDavClient());

        importProgressMonitor.startTransfer();

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
        final long taskID = task.getObjectId();

        importProgressMonitor.startPipeline();

        // Submit the job
        modelMgr.submitJob(process, task);

        // poll for task status on the simple worker thread (not EDT thread)
        Task refreshedTask = task;
        for (int i = 0; i < importProgressMonitor.getMaxRefreshCount(); i++) {
            Thread.sleep(importProgressMonitor.getRefreshInterval());
            refreshedTask = modelMgr.getTaskById(taskID);
            if (refreshedTask.isDone()) {
                final Event lastEvent = refreshedTask.getLastEvent();
                final String lastEventType = lastEvent.getEventType();
                if (Event.ERROR_EVENT.equals(lastEventType)) {
                    throw new IllegalStateException("import task (ID: " + taskID +
                                                    ") failed");
                } else if (Event.CANCELED_EVENT.equals(lastEventType)) {
                    throw new IllegalStateException("import task (ID: " + taskID +
                                                    ") was cancelled");
                }
                break;
            }
        }

        if (! refreshedTask.isDone()) {
            throw new IllegalStateException("import task (ID: " + taskID +
                                            ") is taking too long to complete");
        }
    }

    private class ImportProgressMonitor implements ActionListener {

        private long startTime;
        private Long pipelineStartTime;
        private int refreshInterval;
        private int refreshCount;
        private int maxRefreshCount;
        private Timer refreshTimer;
        private ProgressMonitor progressMonitor;

        public ImportProgressMonitor() {
            this.startTime = System.currentTimeMillis();
            this.pipelineStartTime = null;
            this.refreshInterval = 3000;
            this.refreshCount = 0;
            final double refreshesPerSecond = 1000.0 / this.refreshInterval;
            final double refreshesPerMinute = 60 * refreshesPerSecond;
            this.maxRefreshCount = (int) (60 * refreshesPerMinute); // 60 minutes
            this.refreshTimer = new Timer(this.refreshInterval, this);
            this.refreshTimer.setInitialDelay(0);
            // hack: use long first message (and display immediately) to size dialog
            this.progressMonitor =
                    new ProgressMonitor(SessionMgr.getBrowser(),
                                        "Import",
                                        "Transferring Files to JACS Server for Pipeline Load (starting)",
                                        0, 40);
            this.progressMonitor.setMillisToDecideToPopup(0);
            this.progressMonitor.setMillisToPopup(0);
        }

        private int getRefreshInterval() {
            return refreshInterval;
        }

        private int getMaxRefreshCount() {
            return maxRefreshCount;
        }

        public void startTransfer() {
            this.startTime = System.currentTimeMillis();
            this.refreshTimer.start();
        }

        public void startPipeline() {
            this.pipelineStartTime = System.currentTimeMillis();
        }

        public void close() {
            refreshTimer.stop();
            progressMonitor.close();
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (progressMonitor.isCanceled()) {

                refreshTimer.stop();

            } else {

                long phaseStart;
                String note;
                if (pipelineStartTime == null) {
                    phaseStart = startTime;
                    note = "Transferring Files to JACS Server for Pipeline Load";
                } else {
                    phaseStart = pipelineStartTime;
                    note = "JACS Server Pipeline Loading Files";
                }

                final long seconds = (System.currentTimeMillis() - phaseStart) / 1000;
                if (seconds > 0) {
                    progressMonitor.setNote(note + " (" + seconds + "s)");
                } else {
                    progressMonitor.setNote(note + " (starting)");
                }

                refreshCount++;
                if (refreshCount < progressMonitor.getMaximum()) {
                    progressMonitor.setProgress(refreshCount);
                }
            }
        }
    }
}
