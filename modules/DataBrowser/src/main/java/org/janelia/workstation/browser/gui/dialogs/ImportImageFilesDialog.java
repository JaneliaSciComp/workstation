package org.janelia.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileFilter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.core.filecache.RemoteLocation;
import org.janelia.workstation.core.filecache.WebDavUploader;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.browser.nodes.NodeUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.util.TimebasedIdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for importing images using the file import service. 
 * 
 * @author kimmelr
 * @author goinac
 * @author rokickik
 */
public class ImportImageFilesDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(ImportImageFilesDialog.class);
    
    private static final int MAX_IMPORT_GB = 100;
    
    private static final String PREF_IMPORT_SOURCE_DIR = "FileImport.SourceFolder";
    private static final String PREF_IMPORT_TARGET_FOLDER = "FileImport.TargetFolder";
    private static final String PREF_IMPORT_MOVIES = "FileImport.Movies";
    private static final String PREF_IMPORT_MIPS = "FileImport.MIPs";
    private static final String PREF_IMPORT_HIST = "FileImport.Hist";
    private static final String PREF_IMPORT_GAMMA = "FileImport.Gamma";
    private static final String PREF_IMPORT_LEGENDS = "FileImport.Legends";
    private static final String PREF_IMPORT_STORAGE_TIER = "FileImport.StorageTier";
    
    private static final String TOOLTIP_TOP_LEVEL_FOLDER =
            "Name of the folder in which data should be loaded with the data.";
    private static final String TOOLTIP_INPUT_DIR =
            "Directory of the tree that should be loaded into the database.";

    private static final String NRS_STORAGE = "NRS";
    private static final String JADE_STORAGE = "Jade";

    private static final Map<String, String> STORAGE_TAGS = ImmutableMap.of(
            NRS_STORAGE, ConsoleProperties.getString("console.upload.StorageTags.nrs"),
            JADE_STORAGE, ConsoleProperties.getString("console.upload.StorageTags.jade")
    );

    private GroupedKeyValuePanel attrPanel;
    private JTextField folderField;
    private TreeNode rootFolder;
    private JTextField pathTextField;
    private JComboBox<String> storageChoice;
    private FilenameFilter selectedChildrenFilter;
    private JCheckBox histCheckbox;
    private JCheckBox gammaCheckbox;
    private JCheckBox legendsCheckbox;
    private JCheckBox generateMipsCheckbox;
    private JCheckBox generateMoviesCheckbox;
    private JButton okButton;

    public ImportImageFilesDialog() {

        setTitle("Import Image Files");

        this.attrPanel = new GroupedKeyValuePanel();

        attrPanel.addSeparator("File Import");

        this.pathTextField = new JTextField(40);
        pathTextField.setToolTipText(TOOLTIP_INPUT_DIR);
        
        String chooseFileText = null;
        ImageIcon chooseFileIcon = null;
        try {
            chooseFileIcon = UIUtils.getClasspathImage("magnifier.png");
        } 
        catch (FileNotFoundException e) {
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
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public String getDescription() {
                        return "TIFF or Vaa3D";
                    }
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || isSupportedFileType(f);
                    }
                });
                
                fileChooser.setCurrentDirectory(currentDir);
                int returnVal = fileChooser.showOpenDialog(ImportImageFilesDialog.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    pathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BoxLayout(pathPanel, BoxLayout.LINE_AXIS));
        pathPanel.add(pathTextField);
        pathPanel.add(chooseFileButton);
        attrPanel.addItem("Directory or image to upload", pathPanel);
        
        this.folderField = new JTextField(40);
        folderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        attrPanel.addItem("Target folder name", folderField);

        this.storageChoice = new JComboBox<>(new String[] {NRS_STORAGE, JADE_STORAGE});
        attrPanel.addItem("Storage tier", storageChoice);
        
        attrPanel.addSeparator("Image Stack Processing");

        generateMoviesCheckbox = new JCheckBox("Generate movies");
        generateMoviesCheckbox.setSelected(true);
        attrPanel.addItem(generateMoviesCheckbox);

        generateMipsCheckbox = new JCheckBox("Generate MIPs (maximum intensity projections)");
        generateMipsCheckbox.setSelected(true);
        attrPanel.addItem(generateMipsCheckbox);
        
        // The remaining components are indented under "generate projections" 
        // and their enabled-ness is controlled by the generate projections checkbox.
        int indent = 50;
        
        histCheckbox = new JCheckBox("Histogram equalization");
        histCheckbox.setSelected(true);
        attrPanel.addItem(histCheckbox, "gapbefore "+indent);
        
        gammaCheckbox = new JCheckBox("Enhance local contrast (CLAHE)");
        gammaCheckbox.setSelected(true);
        attrPanel.addItem(gammaCheckbox, "gapbefore "+indent);
        
        legendsCheckbox = new JCheckBox("Draw intensity scale bars");
        legendsCheckbox.setSelected(false);
        attrPanel.addItem(legendsCheckbox, "gapbefore "+indent);
        
        generateMipsCheckbox.addChangeListener((ChangeEvent e) -> {
            boolean enabled = generateMipsCheckbox.isSelected();
            histCheckbox.setEnabled(enabled);
            gammaCheckbox.setEnabled(enabled);
            legendsCheckbox.setEnabled(enabled);
        });
        
        add(attrPanel, BorderLayout.CENTER);

        this.okButton = new JButton("Import");
        okButton.setToolTipText("Import the selected file or directory");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOkPress();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
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
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);

        selectedChildrenFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return isSupportedFileType(new File(file, name));
            }
        };
    }

    private boolean isSupportedFileType(File f) {
        return f.getName().endsWith(".lsm")
            || f.getName().endsWith(".tif")
            || f.getName().endsWith(".tiff")
            || f.getName().endsWith(".v3draw")
            || f.getName().endsWith(".v3dpbd");
    }
    
    public void showDialog() {

        // Load preferences

        final String importSourceDirectory = (String)
                FrameworkImplProvider.getModelProperty(PREF_IMPORT_SOURCE_DIR);
        if (!StringUtils.isEmpty(importSourceDirectory)) {
            final File importSourceFolder = new File(importSourceDirectory);
            if (importSourceFolder.exists()) {
                pathTextField.setText(importSourceFolder.getAbsolutePath());
            }
        }
        
        final Boolean generateMovies = FrameworkImplProvider.getModelProperty(PREF_IMPORT_MOVIES, true);
        generateMoviesCheckbox.setSelected(generateMovies);

        final Boolean generateMips = FrameworkImplProvider.getModelProperty(PREF_IMPORT_MIPS, true);
        generateMipsCheckbox.setSelected(generateMips);

        final Boolean hist = FrameworkImplProvider.getModelProperty(PREF_IMPORT_HIST, true);
        histCheckbox.setSelected(hist);

        final Boolean gamma = FrameworkImplProvider.getModelProperty(PREF_IMPORT_GAMMA, true);
        gammaCheckbox.setSelected(gamma);

        final Boolean legends = FrameworkImplProvider.getModelProperty(PREF_IMPORT_LEGENDS, false);
        legendsCheckbox.setSelected(legends);
        
        final String storageTier = FrameworkImplProvider.getModelProperty(PREF_IMPORT_STORAGE_TIER, NRS_STORAGE);
        storageChoice.setSelectedItem(storageTier);
        
        try {
            rootFolder = DomainMgr.getDomainMgr().getModel().getDefaultWorkspace();
        } 
        catch (Exception e) {
            throw new RuntimeException("Problem loading default workspace", e);
        }
        
        ActivityLogHelper.logUserAction("ImportImageFilesDialog.showDialog");
        packAndShow();
    }

    private void handleOkPress() {

        String folderName = folderField.getText();
        
        if (StringUtils.isEmpty(folderName)) {
            JOptionPane.showMessageDialog(ImportImageFilesDialog.this, 
                    "Please specify a folder into which the files should be imported.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // save the user preferences for later
        FrameworkImplProvider.setModelProperty(PREF_IMPORT_TARGET_FOLDER, folderName);

        int fileCount = 1;
        double transferMegabytes = 0;
        final File selectedFile = new File(pathTextField.getText());
        List<File> selectedChildren = null;

        String storageTier = (String) storageChoice.getSelectedItem();
        
        if (selectedFile.exists()) {

            // save the user preferences for later
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_SOURCE_DIR, selectedFile.getAbsolutePath());
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_MOVIES, generateMoviesCheckbox.isSelected());
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_MIPS, generateMipsCheckbox.isSelected());
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_HIST, histCheckbox.isSelected());
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_GAMMA, gammaCheckbox.isSelected());
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_LEGENDS, legendsCheckbox.isSelected());
            FrameworkImplProvider.setModelProperty(PREF_IMPORT_STORAGE_TIER, storageTier);
            
            if (selectedFile.isDirectory()) {

                selectedChildren = new ArrayList<File>();
                addSelectedChildren(selectedFile, selectedChildren);
                fileCount = selectedChildren.size();

                if (fileCount == 0) {
                    JOptionPane.showMessageDialog(ImportImageFilesDialog.this, "No eligible import files were found in " +
                            selectedFile.getAbsolutePath() + ".", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                    
                } 
                else {
                    for (File child : selectedChildren) {
                        transferMegabytes += (child.length() / 1000000.0);
                    }
                }
            } 
            else {
                transferMegabytes = selectedFile.length() / 1000000.0;
            }

        } 
        else {
            JOptionPane.showMessageDialog(ImportImageFilesDialog.this, "Please specify a valid file or directory to import.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        MessageFormat form = new MessageFormat(
                "You have selected {0,choice,1#1 file|1<{0,number,integer} files} " +
                "that contain a total of {1,number,#.#} {2}bytes.");
        String msg;

        final double transferGigabytes = transferMegabytes / 1000.0;
        if (transferGigabytes > 0.999999999) {
            msg = form.format(new Object[] { fileCount, transferGigabytes, "giga" });
        }
        else if (transferMegabytes > 0.999999) {
            msg = form.format(new Object[] { fileCount, (int) transferMegabytes, "mega" });
        }
        else if (transferMegabytes > 0.000999) {
            final int transferKilobytes = (int) (transferMegabytes * 1000);
            msg = form.format(new Object[] { fileCount, transferKilobytes, "kilo" });
        }
        else {
            final int transferBytes = (int) (transferMegabytes * 1000000);
            msg = form.format(new Object[] { fileCount, transferBytes, "" });
        }

        final int maxGigabytes = MAX_IMPORT_GB;
        if (transferGigabytes > maxGigabytes) {
            JOptionPane.showMessageDialog(ImportImageFilesDialog.this, msg + "  This exceeds the maximum import limit of " +
                    maxGigabytes + " gigabytes.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
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
            
            StringBuilder options = new StringBuilder();
            if (generateMoviesCheckbox.isSelected()) {
                if (options.length()>0) options.append(":");
                options.append("movies");
            }
            if (generateMipsCheckbox.isSelected()) {
                if (options.length()>0) options.append(":");
                options.append("mips");
                if (histCheckbox.isSelected()) {
                    if (options.length()>0) options.append(":");
                    options.append("hist");
                }
                if (gammaCheckbox.isSelected()) {
                    if (options.length()>0) options.append(":");
                    options.append("gamma");
                }
                if (legendsCheckbox.isSelected()) {
                    if (options.length()>0) options.append(":");
                    options.append("legends");
                }
            }
            
            String storageTags = STORAGE_TAGS.get(storageTier);
            
            // close import dialog and run import in background thread
            this.setVisible(false);
            runImport(selectedFile, 
                    selectedChildren,
                    folderName,
                    rootFolder.getId(), 
                    storageTags,
                    null,
                    options.toString()
            );
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
                    } 
                    else {
                        selectedChildren.add(child);
                    }
                }
            }
        }
    }

    private void runImport(final File selectedFile,
                           final List<File> selectedChildren,
                           final String importFolderName,
                           final Long importFolderId,
                           final String storageTags,
                           final String channelSpec,
                           final String mipsOptions) {
    
        BackgroundWorker executeWorker = new AsyncServiceMonitoringWorker() {

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
                        importFolderId, storageTags,
                        channelSpec,
                        mipsOptions);
                
                setServiceId(taskId);
                
                setStatus("Grid execution");
                
                // Wait until task is finished
                super.doStuff(); 

                if (isCancelled()) throw new CancellationException();
                setStatus("Done importing");
            }

            @Override
            public Callable<Void> getSuccessCallback() {
                return () -> {
                    final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
                    explorer.refresh(true, true, () -> {

                            if (rootFolder!=null) {

                                DomainModel model = DomainMgr.getDomainMgr().getModel();
                                rootFolder = (TreeNode) model.getDomainObject(Reference.createFor(rootFolder));
                                List<DomainObject> children = model.getDomainObjects(rootFolder.getChildren());
                                DomainObject importFolder = null;
                                for (DomainObject child : children) {
                                    if (child.getName().equals(importFolderName)) {
                                        importFolder = child;
                                        break;
                                    }
                                }
                                final Long[] idPath = NodeUtils.createIdPath(rootFolder, importFolder);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        explorer.selectAndNavigateNodeByPath(idPath);
                                        setVisible(false);
                                    }
                                });
                            }

                            return null;
                        }
                    );
                    return null;
                };
            }
        };

        executeWorker.executeWithEvents();
    }

    private Long startImportFilesTask(File selectedFile,
                                      List<File> selectedChildren,
                                      String importTopLevelFolderName,
                                      Long importTopLevelFolderId,
                                      String storageTags,
                                      String channelSpec,
                                      String mipsOptions) throws Exception {

        AsyncServiceClient asyncServiceClient = new AsyncServiceClient();

        log.info("Starting import of {} with options:", selectedFile);
        log.info("selectedChildren: {}", selectedChildren);
        log.info("importTopLevelFolderName: {}", importTopLevelFolderName);
        log.info("importTopLevelFolderId: {}", importTopLevelFolderId);
        log.info("storageTags: {}", storageTags);
        log.info("channelSpec: {}", channelSpec);
        log.info("mipsOptions: {}", mipsOptions);
        
        final WebDavUploader uploader = FileMgr.getFileMgr().getFileUploader();

        final String subjectName = AccessManager.getSubjectName();
        String uploadContext = uploader.createUploadContext(
                "WorkstationFileUpload",
                subjectName,
                storageTags);
        String uploadPath;

        Long guid = TimebasedIdentifierGenerator.generateIdList(1).get(0);
        String storageName = "UserFileImport_"+guid;
        
        if (selectedChildren == null) {
            RemoteLocation uploadedFile = uploader.uploadFile(
                    storageName,
                    uploadContext,
                    storageTags,
                    selectedFile);
            uploadPath = uploadedFile.getStorageURL();
        } 
        else {
            List<RemoteLocation> uploadedFiles = uploader.uploadFiles(
                    storageName,
                    uploadContext,
                    storageTags,
                    selectedChildren,
                    selectedFile);
            // all files should be uploaded to the same storage
            uploadPath = uploadedFiles.stream().findFirst().map(rl -> rl.getStorageURL()).orElseThrow(() -> new IllegalStateException("Invalid upload state " + uploadedFiles));
        }

        ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                .add("-folderName", importTopLevelFolderName);
        if (importTopLevelFolderId != null) {
            serviceArgsBuilder.add("-parentFolderId", importTopLevelFolderId.toString());
        }
        if (!StringUtils.isBlank(channelSpec)) {
            serviceArgsBuilder.add("-mipsChanSpec", channelSpec);
        }
        if (!StringUtils.isBlank(mipsOptions)) {
            serviceArgsBuilder.add("-mipsOptions", mipsOptions);
        }
        serviceArgsBuilder.add("-storageLocation", uploadPath);
        serviceArgsBuilder.add("-cleanStorageOnFailure");
        return asyncServiceClient.invokeService("dataTreeLoad",
                serviceArgsBuilder.build(),
                "LSF_JAVA",
                ImmutableMap.of()
        );
    }

}
