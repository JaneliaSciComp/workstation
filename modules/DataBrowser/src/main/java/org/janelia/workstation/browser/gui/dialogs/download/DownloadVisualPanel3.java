package org.janelia.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.access.domain.ChanSpecUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public final class DownloadVisualPanel3 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel3.class);

    public static final String FILE_PATTERN_PROP_NAME = "LAST_USER_FILE_PATTERN";
    
    private static final int MAX_LOG_ITEMS = 50;
    
    private static final String FILE_PATTERN_HELP =
            "<html><font color='#959595' size='-1'>The file naming pattern allows you to customize how the files are saved on your local disk. It can use any attribute on the items being downloaded.<br>"
            + "It may also use the following special attributes: {Sample Name}, {Result Name}, {File Name}, {Extension}, {Index}, {Folders}<br>"
            + "Each attribute may include multiple names as a fallback, e.g.: {Fly Core Alias|VT Line|Line}, or a default value {Fly Core Alias|\"NoAlias\"}"
            + "</font></html>";
    
    public static final String[] STANDARD_FILE_PATTERNS = {
            "{Sample Name}/{Result Name|\"Image\"}-{File Name}", 
            "{Line}/{Sample Name}"
    };

    private DownloadWizardPanel3 wizardPanel;
    private final Debouncer debouncer = new Debouncer();
    
    // GUI
    private JPanel mainPane;
    private GroupedKeyValuePanel attrPanel;
    private JCheckBox flattenStructureCheckbox;
    private JComboBox<String> filePatternCombo;
    private JLabel downloadItemCountLabel;
    private JList<DownloadFileItem> downloadItemList;

    // Inputs
    private List<DownloadObject> downloadObjects;
    private List<ArtifactDescriptor> artifactDescriptors;
    private Map<String,String> outputExtensions;
    private boolean splitChannels;
    private boolean flattenStructure;
    private String filenamePattern;
    
    // Outputs
    private List<DownloadFileItem> downloadItems = new ArrayList<>();

    public DownloadVisualPanel3(DownloadWizardPanel3 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());
    }

    @Override
    public String getName() {
        return "File Naming";
    }

    // If anything changes, we need to recalculate the file download list
    private ItemListener changeListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED || e.getSource() instanceof JCheckBox) {
                populateDownloadItemList();
            }
        }
    };
    
    public void init(DownloadWizardState state) {

        this.downloadObjects = state.getDownloadObjects();
        this.artifactDescriptors = state.getSelectedArtifactDescriptors();
        this.outputExtensions = state.getOutputExtensions();
        this.splitChannels = state.isSplitChannels();
        this.flattenStructure = state.isFlattenStructure();
        this.filenamePattern = state.getFilenamePattern();

        attrPanel = new GroupedKeyValuePanel("wrap 2, ins 10, fill", "[growprio 0]0[growprio 1, grow]", "[][][][][][growprio 200]");

        flattenStructureCheckbox = new JCheckBox();
        flattenStructureCheckbox.setSelected(flattenStructure);
        flattenStructureCheckbox.addItemListener(changeListener);
        attrPanel.addItem("Flatten folder structure", flattenStructureCheckbox);

        filePatternCombo = new JComboBox<>();
        filePatternCombo.setEditable(true);
        filePatternCombo.setToolTipText("Select a standard file naming pattern, or enter your own.");
        DefaultComboBoxModel<String> fpmodel = (DefaultComboBoxModel<String>) filePatternCombo.getModel();
        String userFilePattern = (String)FrameworkImplProvider.getModelProperty(FILE_PATTERN_PROP_NAME);
        if (userFilePattern!=null) {
            fpmodel.addElement(userFilePattern);
        }
        for (String pattern : STANDARD_FILE_PATTERNS) {
            fpmodel.addElement(pattern);
        }
        if (filenamePattern != null) {
            fpmodel.setSelectedItem(filenamePattern);
        }
        filePatternCombo.addItemListener(changeListener);

        attrPanel.addItem("Naming pattern", filePatternCombo, "width 200:300:600, grow");

        attrPanel.addItem("", new JLabel(FILE_PATTERN_HELP), "width 200:800:1000, grow");

        downloadItemCountLabel = new JLabel();
        attrPanel.addItem("File count", downloadItemCountLabel);
        
        Path workstationImagesDir = Utils.getDownloadsDir();
        JLabel downloadDirLabel = new JLabel(workstationImagesDir.toString());
        
        String chooseFileText = null;
        ImageIcon chooseFileIcon = null;
        try {
            chooseFileIcon = UIUtils.getClasspathImage("magnifier.png");
        } 
        catch (FileNotFoundException e) {
            log.warn("Failed to load button icon", e);
            chooseFileText = "...";
        }
        
        JButton chooseFileButton = new JButton(chooseFileText, chooseFileIcon);
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Path downloadsDir = Utils.getDownloadsDir();
                if (!Files.exists(downloadsDir)) {
                    downloadsDir = null;
                }
                JFileChooser fileChooser = new JFileChooser() {
                    @Override
                    public void approveSelection() {
                        log.info("User selected download directory: {}",getSelectedFile());
                        super.approveSelection();
                    }
                };
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (downloadsDir!=null) {
                    fileChooser.setCurrentDirectory(downloadsDir.toFile());
                }
                int returnVal = fileChooser.showOpenDialog(DownloadVisualPanel3.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String downloadDirPath = fileChooser.getSelectedFile().getAbsolutePath();
                    Utils.setDownloadsDir(downloadDirPath);
                    downloadDirLabel.setText(downloadDirPath);
                    // Recalculate download paths
                    populateDownloadItemList();
                }
            }
        });

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.LINE_AXIS));
        filePanel.add(downloadDirLabel);
        filePanel.add(Box.createRigidArea(new Dimension(5,  0)));
        filePanel.add(chooseFileButton);
        attrPanel.addItem("Destination folder", filePanel);
        
        downloadItemList = new JList<>(new DefaultListModel<DownloadFileItem>());
        downloadItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadItemList.setLayoutOrientation(JList.VERTICAL);

        mainPane = new JPanel(new BorderLayout());
        mainPane.add(new JLabel(Icons.getLoadingIcon()));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(mainPane);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Increase scroll speed

        attrPanel.addItem("Preview files", scrollPane, "width 200:600:2000, height 50:200:1000, grow");
        
        removeAll();
        add(attrPanel, BorderLayout.CENTER);
        
        populateDownloadItemList();
    }

    private void populateDownloadItemList() {

        if (!debouncer.queue()) {
            log.debug("Skipping populateDownloadItemList, since there is an operation already in progress");
            return;
        }

        filePatternCombo.setEnabled(false);
        mainPane.removeAll();
        mainPane.add(new JLabel(Icons.getLoadingIcon()));

        final boolean flattenStructure = isFlattenStructure();
        final String filenamePattern = getFilenamePattern();
        
        SimpleWorker worker = new SimpleWorker() {

            private List<DownloadFileItem> downloadFileItems = new ArrayList<>();
            
            @Override
            protected void doStuff() throws Exception {
                this.downloadFileItems = createDownloadFileItems(flattenStructure, filenamePattern, this);
            }
            
            @Override
            protected void hadSuccess() {

                downloadItems = downloadFileItems;
                
                int count = 0;
                DefaultListModel<DownloadFileItem> dlm = (DefaultListModel<DownloadFileItem>) downloadItemList.getModel();
                dlm.removeAllElements();
                for (DownloadFileItem downloadItem : downloadFileItems) {
                    count += downloadItem.getSourceFile()!=null ? 1 : 0;
                    dlm.addElement(downloadItem);
                    log.debug("Adding item "+downloadItem);
                }
                
                // Update GUI
                downloadItemCountLabel.setText(count+" files");
                
                filePatternCombo.setEnabled(true);
                mainPane.removeAll();
                mainPane.add(downloadItemList);
                mainPane.updateUI();
                
                // Update Wizard
                triggerValidation();
                
                debouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                debouncer.failure();
                FrameworkImplProvider.handleException(error);
            }
        };

        worker.execute();
    }
    
    private List<DownloadFileItem> createDownloadFileItems(boolean flattenStructure, String filenamePattern, Progress progress) throws Exception {
        
        List<DownloadFileItem> downloadFileItems = new ArrayList<>();
        
        Map<Integer,FileType> colorDepthTypes = new HashMap<>();
        colorDepthTypes.put(0, FileType.ColorDepthMip1);
        colorDepthTypes.put(1, FileType.ColorDepthMip2);
        colorDepthTypes.put(2, FileType.ColorDepthMip3);
        colorDepthTypes.put(3, FileType.ColorDepthMip4);
        
        int i = 0;
        int index = 0;
        
        Set<Path> paths = new HashSet<>();
        
        for(DownloadObject downloadObject : downloadObjects) {
            DomainObject domainObject = downloadObject.getDomainObject();
            log.debug("-------------------------------------------------------------------------");
            log.debug("Inspecting download object '{}'", domainObject);
            
            for (ArtifactDescriptor artifactDescriptor : artifactDescriptors) {
                log.debug("  Checking artifact descriptor '{}'", artifactDescriptor);

                for (HasFiles hasFiles : artifactDescriptor.getFileSources(domainObject)) {
                    log.debug("    Checking source item '{}'", hasFiles);
                    
                    // Replace aggregate with individual color depth MIPs
                    List<FileType> selectedFileTypes = new ArrayList<>(artifactDescriptor.getSelectedFileTypes());
                    if (selectedFileTypes.contains(FileType.ColorDepthMips)) {
                        selectedFileTypes.remove(FileType.ColorDepthMips);
                        if (hasFiles instanceof SampleAlignmentResult) {
                            SampleAlignmentResult alignment = (SampleAlignmentResult)hasFiles;
                            List<Integer> signalChans = ChanSpecUtils.getSignalChannelIndexList(alignment.getChannelSpec());
                            for(Integer signalChan : signalChans) {
                                selectedFileTypes.add(colorDepthTypes.get(signalChan));
                            }
                        }
                        else {
                            throw new IllegalStateException("Color depth MIPs selected for unaligned data");
                        }
                    }
                    
                    for (FileType fileType : selectedFileTypes) {
                        log.debug("      Adding item for file type '{}'", fileType);
                        
                        DownloadFileItem downloadItem = new DownloadFileItem(downloadObject.getFolderPath(), domainObject, index++);
                        downloadItem.init(artifactDescriptor, hasFiles, fileType, outputExtensions, splitChannels && fileType.is3dImage(), flattenStructure, filenamePattern, paths);

                        if (downloadItem.getError()==null) {
                            if (index < MAX_LOG_ITEMS) {
                                log.info("Download {}, descriptor={}, fileSource={}", domainObject, artifactDescriptor, hasFiles);
                                log.info("         {} to {}", fileType, downloadItem.getTargetFile());
                            }
                            else if (index == MAX_LOG_ITEMS) {
                                log.info("File list logging truncated after {} items", MAX_LOG_ITEMS);
                            }
                            downloadFileItems.add(downloadItem);
                            paths.add(downloadItem.getTargetFile());
                        }
                    }
                }
            }
            
            progress.setProgress(i++, downloadObjects.size());
        }

        Collections.sort(downloadFileItems, new Comparator<DownloadFileItem>() {
            @Override
            public int compare(DownloadFileItem o1, DownloadFileItem o2) {
                ComparisonChain chain = ComparisonChain.start()
                        .compare(o1.getPath(), o2.getPath(), Ordering.natural().nullsFirst())
                        .compare(o1.getPrefix(), o2.getPrefix(), Ordering.natural().nullsFirst())
                        .compare(o1.getNumber(), o2.getNumber(), Ordering.natural().nullsFirst())
                        .compare(o1.getExtension(), o2.getExtension(), Ordering.natural().nullsFirst());
                return chain.result();
            }
        });
        
        for (DownloadFileItem downloadFileItem : downloadFileItems) {
            log.info(downloadFileItem.toString());
        }
        
        return downloadFileItems;
    }
     
    public boolean isFlattenStructure() {
        return flattenStructureCheckbox.isSelected();
    }
    
    public String getFilenamePattern() {
        DefaultComboBoxModel<String> fpmodel = (DefaultComboBoxModel<String>) filePatternCombo.getModel();
        return (String)fpmodel.getSelectedItem();
    }
    
    public List<DownloadFileItem> getDownloadItems() {
        return downloadItems;
    }

    private void triggerValidation() {
        wizardPanel.fireChangeEvent();
    }
}
