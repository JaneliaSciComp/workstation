package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.dialogs.DownloadDialog;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.gui.support.DesktopApi;
import org.janelia.it.workstation.browser.gui.support.DownloadItem;
import org.janelia.it.workstation.browser.gui.support.FileDownloadWorker;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Core",
        id = "org.janelia.it.workstation.browser.gui.dialogs.download.DownloadWizardAction"
)
@ActionRegistration(
        displayName = "#CTL_DownloadWizardAction"
)
@ActionReference(path = "Menu/File", position = 750, separatorAfter = 775)
@Messages("CTL_DownloadWizardAction=Download...")
public final class DownloadWizardAction implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(DownloadWizardAction.class);
    
    private List<? extends DomainObject> inputObjects;
    private ResultDescriptor defaultResultDescriptor;

    private static final Lock COPY_FILE_LOCK = new ReentrantLock();
    private static final int MAX_BROWSE_FILES = 10;
    private Integer applyToAllChoice;
    private int numBrowseFileAttempts = 0;
    
    public DownloadWizardAction() throws Exception {
        List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
        this.inputObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectedIds);
    }
    
    public DownloadWizardAction(List<? extends DomainObject> domainObjects, ResultDescriptor defaultResultDescriptor) {
        this.inputObjects = domainObjects;
        this.defaultResultDescriptor = defaultResultDescriptor;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("DownloadWizardAction.actionPerformed");
        
        Boolean legacy = (Boolean)ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.LEGACY_DOWNLOAD_DIALOG);
        if (legacy!=null && legacy) {
            DownloadDialog dialog = new DownloadDialog();
            dialog.showDialog(inputObjects, defaultResultDescriptor);
            return;
        }
        
        // Hide the default wizard image, which does not look good on our dark background
        UIDefaults uiDefaults = UIManager.getDefaults();
        uiDefaults.put("nb.wizard.hideimage", Boolean.TRUE); 
        
        // Create wizard
        DownloadWizardIterator iterator = new DownloadWizardIterator();
        WizardDescriptor wiz = new WizardDescriptor(iterator);
        iterator.initialize(wiz); 

        // Setup the initial state
        DownloadWizardState state = new DownloadWizardState();
        state.setInputObjects(inputObjects);
        state.setDefaultArtifactDescriptor(new ResultArtifactDescriptor(defaultResultDescriptor));

        // Restore previous state from user's last usage
        String artifactDescriptorString = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "artifactDescriptors", null);
        log.info("Setting last artifactDescriptorString: "+artifactDescriptorString);
        state.setArtifactDescriptorString(artifactDescriptorString);
        
        String outputFormat = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "outputFormat", null);
        log.info("Setting last outputFormat: "+outputFormat);
        state.setOutputFormat(outputFormat);

        boolean splitChannels = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "splitChannels", state.isSplitChannels());
        log.info("Setting last splitChannels: "+splitChannels);
        state.setSplitChannels(splitChannels);

        boolean flattenStructure = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "flattenStructure", state.isFlattenStructure());
        log.info("Setting last flattenStructure: "+flattenStructure);
        state.setFlattenStructure(flattenStructure);

        String filenamePattern = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "filenamePattern", null);
        log.info("Setting last filenamePattern: "+filenamePattern);
        state.setFilenamePattern(filenamePattern);
        
        // Install the state
        wiz.putProperty(DownloadWizardIterator.PROP_WIZARD_STATE, state);

        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
        wiz.setTitle("Download Files");
        
        // Show the wizard
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            // Start downloading 
            DownloadWizardState endState = (DownloadWizardState) wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
            List<DownloadItem> downloadItems = endState.getDownloadItems();
            if (!downloadItems.isEmpty()) {
                download(downloadItems);
            }
        }
    }

    private void download(List<DownloadItem> downloadItems) {

        ActivityLogHelper.logUserAction("DownloadWizardAction.beginDownload");
        
        boolean started = false;
        int remaining = downloadItems.size();
        
        for(final DownloadItem downloadItem : downloadItems) {
            if (downloadItem.getSourceFile()!=null) {
            
                FileDownloadWorker worker = new FileDownloadWorker(downloadItem, COPY_FILE_LOCK);
                if (checkForAlreadyDownloadedFiles(worker, remaining>1)) {
                    worker.startDownload();
                }
                
                started = true;
            }
            remaining--;
        }

        if (!started) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "There are no downloads to start.", "Nothing to do", JOptionPane.PLAIN_MESSAGE);
        }
    }

    private boolean checkForAlreadyDownloadedFiles(FileDownloadWorker worker, boolean showApplyToAll) {

        final DownloadItem downloadItem = worker.getDownloadItem();
        final File targetDir = worker.getTargetDir();
        final String targetExtension = worker.getTargetExtension();
        
        boolean continueWithDownload = true;

        final String targetName = downloadItem.getTargetFile().getName();
        final String basename = FileUtil.getBasename(targetName).replaceAll("#","");

        File[] files = targetDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(basename) && name.endsWith(targetExtension);
            }
        });

        if ((files != null) && (files.length > 0)) {

            Integer chosenOptionIndex = applyToAllChoice;
            if (chosenOptionIndex==null) {

                String abbrName = StringUtils.abbreviate(files[0].getName(), 40);
                String msg = "<html>The file " + abbrName + " was previously downloaded.<br>"
                + "Open the existing download folder or re-run the download anyway?</html>";
                
                JCheckBox applyToAll = new JCheckBox("Apply to all");
                
                JPanel questionPanel = new JPanel(new BorderLayout());
                questionPanel.add(new JLabel(msg), BorderLayout.CENTER);
                
                if (showApplyToAll) {
                    questionPanel.add(applyToAll, BorderLayout.SOUTH);
                }
                
                String[] options = { "Open Folder", "Run Download", "Ignore" };
                chosenOptionIndex = JOptionPane.showOptionDialog(
                        ConsoleApp.getMainFrame(),
                        questionPanel,
                        "File Previously Downloaded",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
                
                if (applyToAll.isSelected()) {
                    log.info("Setting apply to all option: {}", chosenOptionIndex);
                    applyToAllChoice = chosenOptionIndex;
                }
            }

            continueWithDownload = (chosenOptionIndex == 1);
            if (chosenOptionIndex == 0) {
                if (numBrowseFileAttempts == MAX_BROWSE_FILES) {
                    log.info("Reached max number of file browses for this download context: {}", numBrowseFileAttempts);
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                            "Maximum number of folders have been opened. Further folders will not be opened for this file set.", "Open Folder", JOptionPane.WARNING_MESSAGE);
                }
                else if (numBrowseFileAttempts < MAX_BROWSE_FILES) {
                    DesktopApi.browse(targetDir);
                }
                numBrowseFileAttempts++;
            }
        }

        return continueWithDownload;
    }
    
}
