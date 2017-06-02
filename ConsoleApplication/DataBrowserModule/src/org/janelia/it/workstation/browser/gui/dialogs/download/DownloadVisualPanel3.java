package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DownloadVisualPanel3 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel3.class);

    public static final String FILE_PATTERN_PROP_NAME = "LAST_USER_FILE_PATTERN";
    
    private static final String FILE_PATTERN_HELP =
            "<html><font color='#959595' size='-1'>The naming pattern can use any attribute on the items being downloaded.<br>"
            + "It may also use the following special attributes: {Sample Name}, {Result Name}, {File Name}, {Extension}<br>"
            + "Each attribute may include multiple names as a fallback, e.g.: {Fly Core Alias|Line}"
            + "</font></html>";
    
    public static final String[] STANDARD_FILE_PATTERNS = {
            "{Sample Name}/{Result Name|\"Image\"}-{File Name}", 
            "{Line}/{Sample Name}"
    };

    private DownloadWizardPanel3 wizardPanel;
    private final Debouncer debouncer = new Debouncer();
    
    // GUI
    private GroupedKeyValuePanel attrPanel;
    private JCheckBox flattenStructureCheckbox;
    private JComboBox<String> filePatternCombo;
    private JLabel downloadItemCountLabel;
    private JList<DownloadFileItem> downloadItemList;

    // Inputs
    private List<DownloadObject> downloadObjects;
    private List<ArtifactDescriptor> artifactDescriptors;
    private String outputExtension;
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
        this.artifactDescriptors = state.getArtifactDescriptors();
        this.outputExtension = state.getOutputFormat();
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
        String userFilePattern = (String)ConsoleApp.getConsoleApp().getModelProperty(FILE_PATTERN_PROP_NAME);
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
        
        JLabel downloadDirLabel = new JLabel(DownloadFileItem.workstationImagesDir.getAbsolutePath());
        attrPanel.addItem("Destination folder", downloadDirLabel);
        
        downloadItemList = new JList<>(new DefaultListModel<DownloadFileItem>());
        downloadItemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadItemList.setLayoutOrientation(JList.VERTICAL);
        attrPanel.addItem("Preview files", new JScrollPane(downloadItemList), "width 200:600:2000, height 50:200:1000, grow");

        removeAll();
        add(attrPanel, BorderLayout.CENTER);
        
        populateDownloadItemList();
    }

    private void populateDownloadItemList() {

        if (!debouncer.queue()) {
            log.debug("Skipping populateDownloadItemList, since there is an operation already in progress");
            return;
        }

        downloadItems.clear();
        final boolean flattenStructure = isFlattenStructure();
        final String filenamePattern = getFilenamePattern();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                for(DownloadObject downloadObject : downloadObjects) {
                    DomainObject domainObject = downloadObject.getDomainObject();
                    log.debug("Inspecting download object '{}'", domainObject);
                    
                    for (ArtifactDescriptor artifactDescriptor : artifactDescriptors) {
                        log.debug("  Checking artifact descriptor '{}'", artifactDescriptor);

                        for (HasFiles hasFiles : artifactDescriptor.getFileSources(domainObject)) {
                            log.debug("    Checking source item '{}'", hasFiles);
                            for (FileType fileType : artifactDescriptor.getSelectedFileTypes()) {
                                log.debug("      Adding item for file type '{}'", fileType);
                                DownloadFileItem downloadItem = new DownloadFileItem(downloadObject.getFolderPath(), domainObject);
                                downloadItem.init(hasFiles, fileType, fileType.is3dImage()?outputExtension:null, splitChannels, flattenStructure, filenamePattern);
                                downloadItems.add(downloadItem);
                            }
                        }
                    }
                }    
            }
            
            @Override
            protected void hadSuccess() {
                
                int count = 0;
                DefaultListModel<DownloadFileItem> dlm = (DefaultListModel<DownloadFileItem>) downloadItemList.getModel();
                dlm.removeAllElements();
                for (DownloadFileItem downloadItem : downloadItems) {
                    count += downloadItem.getSourceFile()!=null ? 1 : 0;
                    dlm.addElement(downloadItem);
                    log.debug("Adding item "+downloadItem.getTargetFile());
                }
                
                // Update GUI
                downloadItemList.updateUI();
                downloadItemCountLabel.setText(count+" files");
                
                // Update Wizard
                triggerValidation();
                
                debouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                debouncer.failure();
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
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
