package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
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
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.browser.model.search.SolrSearchResults;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

/**
 * Action which brings up the Download wizard (or the legacy download dialog, if that preference is set). 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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

    private ResultDescriptor defaultResultDescriptor;
    private List<? extends DomainObject> inputObjects;
    private List<DownloadObject> downloadItems = new ArrayList<>();
    private Map<ArtifactDescriptor,Multiset<FileType>> artifactFileCounts;

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
        
        findDownloadObjects();
    }

    private void findDownloadObjects() {

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                
                log.info("Finding items for download");
                for(DomainObject domainObject : inputObjects) {
                    downloadItems.addAll(addObjectsToExport(new ArrayList<String>(), domainObject));
                }
                
                log.info("Got {} download items", downloadItems.size());
                log.info("Collecting descriptors");
                
                Multiset<ArtifactDescriptor> artifactCounts = getArtifactCounts();
                Set<ArtifactDescriptor> elementSet = artifactCounts.elementSet();
                
                log.info("Got {} artifact descriptors", elementSet.size());
                log.info("Finding files");
                
                int i = 0;
                artifactFileCounts = new HashMap<>();
                for(ArtifactDescriptor artifactDescriptor : elementSet) {
                    artifactFileCounts.put(artifactDescriptor, getFileTypeCounts(artifactDescriptor));
                    setProgress(i++, elementSet.size());
                }
                
                log.info("Found {} objects to export",downloadItems.size());
            }

            @Override
            protected void hadSuccess() {
                showWizard();
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        ProgressMonitor monitor = new ProgressMonitor(ConsoleApp.getMainFrame(), "Finding files for download...", "", 0, 100);
        monitor.setMillisToDecideToPopup(100);
        monitor.setMillisToPopup(500);
        worker.setProgressMonitor(monitor);
        worker.execute();
    }
    
    private List<DownloadObject> addObjectsToExport(List<String> path, DomainObject domainObject) {
        List<DownloadObject> downloadItems = new ArrayList<>();
        try {
            log.debug("addObjectsToExport({},{})", path, domainObject.getName());
            if (domainObject instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) domainObject;
                if (treeNode.hasChildren()) {
                    List<Reference> childRefs = treeNode.getChildren();
                    List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(childRefs);
                    List<String> childPath = new ArrayList<>(path);
                    childPath.add(domainObject.getName());
                    for (DomainObject child : children) {
                        addObjectsToExport(childPath, child);
                    }
                }
            }
            else if (domainObject instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) domainObject;
                List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(treeNode.getChildren());
                List<String> childPath = new ArrayList<>(path);
                childPath.add(domainObject.getName());
                for (DomainObject child : children) {
                    addObjectsToExport(childPath, child);
                }
            }
            else if (domainObject instanceof Filter) {
                Filter filter = (Filter) domainObject;
                try {
                    SearchConfiguration config = new SearchConfiguration(filter, 1000);
                    SolrSearchResults searchResults = config.performSearch();
                    searchResults.loadAllResults();
                    for (ResultPage page : searchResults.getPages()) {
                        List<String> childPath = new ArrayList<>(path);
                        childPath.add(domainObject.getName());
                        for (DomainObject resultObject : page.getDomainObjects()) {
                            addObjectsToExport(childPath, resultObject);
                        }
                    }
                }
                catch (Exception e) {
                    ConsoleApp.handleException(e);
                }
            }
            else {
                downloadItems.add(new DownloadObject(path, domainObject));
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
        return downloadItems;
    }

    public Multiset<ArtifactDescriptor> getArtifactCounts() {

        Multiset<ArtifactDescriptor> countedArtifacts = LinkedHashMultiset.create();
            
        for(DownloadObject downloadObject : downloadItems) {
            DomainObject domainObject = downloadObject.getDomainObject();
            log.trace("Inspecting object: {}", domainObject);
            if (domainObject instanceof HasFiles) {
                log.trace("  Adding self descriptor");
                countedArtifacts.add(new SelfArtifactDescriptor());
            }
            if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                log.trace("  Inspecting sample: {}", sample.getName());
                
                for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                    log.trace("    Inspecting objective: {}", objectiveSample.getObjective());
                    
                    for (SampleTile tile : objectiveSample.getTiles()) {
                        log.trace("      Inspecting tile: {}", tile.getName());
                        
                        for (Reference reference : tile.getLsmReferences()) {
                            log.trace("         Adding LSM descriptor for objective: {}", objectiveSample.getObjective());
                            countedArtifacts.add(new LSMArtifactDescriptor(objectiveSample.getObjective(), tile.getAnatomicalArea()));
                        }
                    }
                    SamplePipelineRun run = objectiveSample.getLatestSuccessfulRun();
                    if (run==null || run.getResults()==null) {
                        run = objectiveSample.getLatestRun();
                    }
                    if (run!=null) {
                        for(PipelineResult result : run.getResults()) {
                            log.trace("  Inspecting pipeline result: {}", result.getName());
                            if (result instanceof SamplePostProcessingResult) {
                                // Add a descriptor for every anatomical area in the sample
                                for (SampleTile sampleTile : objectiveSample.getTiles()) {
                                    ResultArtifactDescriptor rad = new ResultArtifactDescriptor(objectiveSample.getObjective(), sampleTile.getAnatomicalArea(), result.getName(), false);
                                    log.trace("    Adding result artifact descriptor: {}", rad);
                                    countedArtifacts.add(rad);
                                }
                            }
                            else if (result instanceof HasAnatomicalArea){
                                HasAnatomicalArea aaResult = (HasAnatomicalArea)result;
                                ResultArtifactDescriptor rad = new ResultArtifactDescriptor(objectiveSample.getObjective(), aaResult.getAnatomicalArea(), result.getName(), result instanceof SampleAlignmentResult);
                                log.trace("    Adding result artifact descriptor: {}", rad);
                                countedArtifacts.add(rad);
                            }
                            else {
                                log.trace("Cannot handle result '"+result.getName()+"' of type "+result.getClass().getSimpleName());
                            }
                        }
                    }
                }
            }
        }
        
        return countedArtifacts;
    }

    private Multiset<FileType> getFileTypeCounts(ArtifactDescriptor artifactDescriptor) throws Exception {
        
        Multiset<FileType> countedTypeNames = LinkedHashMultiset.create();
        
        Set<Object> sources = new LinkedHashSet<>();
        for(DownloadObject downloadObject : downloadItems) {
            DomainObject domainObject = downloadObject.getDomainObject();
            sources.addAll(artifactDescriptor.getFileSources(domainObject));
        }
        
        boolean only2d = false;
        for (Object source : sources) {
            log.trace("Inspecting file source: {}", source.getClass().getSimpleName());
            if (source instanceof HasFileGroups) {
                Multiset<FileType> fileTypes = DomainUtils.getFileTypes((HasFileGroups)source, only2d);
                log.trace("  Source has file groups: {}",fileTypes);
                countedTypeNames.addAll(fileTypes);
            }
            if (source instanceof HasFiles) {
                Multiset<FileType> fileTypes = DomainUtils.getFileTypes((HasFiles) source, only2d);
                log.trace("  Source has files: {}",fileTypes);
                countedTypeNames.addAll(fileTypes);
            }
            if (source instanceof PipelineResult) {
                PipelineResult result = (PipelineResult)source;
                NeuronSeparation separation = result.getLatestSeparationResult();
                if (separation!=null) {
                    log.trace("  Source has separation: {}",separation);
                    Set<FileType> typeNames = new HashSet<>();
                    typeNames.add(FileType.NeuronAnnotatorLabel);
                    typeNames.add(FileType.NeuronAnnotatorSignal);
                    typeNames.add(FileType.NeuronAnnotatorReference);
                    log.trace("    Adding type names: {}",typeNames);
                    countedTypeNames.addAll(typeNames);
                }
            }
        }
        
        return countedTypeNames;
    }
    
    private void showWizard() {

        // Hide the default wizard image, which does not look good on our dark background
        UIDefaults uiDefaults = UIManager.getDefaults();
        uiDefaults.put("nb.wizard.hideimage", Boolean.TRUE); 
        
        // Setup the initial state
        DownloadWizardState state = new DownloadWizardState();
        state.setInputObjects(inputObjects);
        state.setDefaultArtifactDescriptor(defaultResultDescriptor);
        state.setDownloadObjects(downloadItems);
        state.setArtifactFileCounts(artifactFileCounts);
        
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

        // Create wizard
        DownloadWizardIterator iterator = new DownloadWizardIterator();
        WizardDescriptor wiz = new WizardDescriptor(iterator);
        iterator.initialize(wiz); 

        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
        wiz.setTitle("Download Files");
        
        // Install the state
        wiz.putProperty(DownloadWizardIterator.PROP_WIZARD_STATE, state);

        // Show the wizard
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            // Start downloading 
            DownloadWizardState endState = (DownloadWizardState) wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
            List<DownloadFileItem> downloadItems = endState.getDownloadItems();
            if (!downloadItems.isEmpty()) {
                download(downloadItems);
            }
        }
    }

    private void download(List<DownloadFileItem> downloadItems) {

        ActivityLogHelper.logUserAction("DownloadWizardAction.beginDownload");
        
        boolean started = false;
        int remaining = downloadItems.size();
        
        for(final DownloadFileItem downloadItem : downloadItems) {
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
