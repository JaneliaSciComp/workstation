package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.support.DesktopApi;
import org.janelia.it.workstation.browser.gui.support.FileDownloadWorker;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.model.search.DomainObjectResultPage;
import org.janelia.it.workstation.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.browser.model.search.SolrSearchResults;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.interfaces.HasFileGroups;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.workspace.TreeNode;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
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
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 750, separatorAfter = 775),
    @ActionReference(path = "Shortcuts", name = "D-D")
})
@Messages("CTL_DownloadWizardAction=Download...")
public final class DownloadWizardAction implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(DownloadWizardAction.class);

    private ArtifactDescriptor defaultResultDescriptor;
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
    
    public DownloadWizardAction(List<? extends DomainObject> domainObjects, ArtifactDescriptor defaultResultDescriptor) {
        this.inputObjects = domainObjects;
        this.defaultResultDescriptor = defaultResultDescriptor;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("DownloadWizardAction.actionPerformed");
        findDownloadObjects();
    }

    private void findDownloadObjects() {
        
        Utils.setWaitingCursor(ConsoleApp.getMainFrame());
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                                
                log.info("Finding items for download");
                for(DomainObject domainObject : inputObjects) {
                    if (isCancelled()) return;
                    downloadItems.addAll(addObjectsToExport(new ArrayList<String>(), domainObject));
                }
                
                setProgress(1);
                
                log.info("Got {} download items", downloadItems.size());
                log.info("Collecting descriptors");
                
                List<DomainObject> domainObjects = new ArrayList<>();
                for(DownloadObject downloadObject : downloadItems) {
                    domainObjects.add(downloadObject.getDomainObject());
                }
                
                Multiset<ArtifactDescriptor> artifactCounts = DescriptorUtils.getArtifactCounts(domainObjects);
                Set<ArtifactDescriptor> elementSet = artifactCounts.elementSet();

                setProgress(2);
                
                log.info("Got {} artifact descriptors", elementSet.size());
                log.info("Finding files");
                
                int startIndex = 2;
                int progressTotal = startIndex + (downloadItems.size() * elementSet.size());
                
                artifactFileCounts = new HashMap<>();
                for(ArtifactDescriptor artifactDescriptor : elementSet) {
                    artifactFileCounts.put(artifactDescriptor, getFileTypeCounts(artifactDescriptor, this, startIndex, progressTotal));
                    if (isCancelled()) return;
                    startIndex += downloadItems.size();
                }
                
                log.info("Found {} objects to export",downloadItems.size());
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(ConsoleApp.getMainFrame());
                if (!isCancelled()) {
                    showWizard();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(ConsoleApp.getMainFrame());
                ConsoleApp.handleException(error);
            }
        };

        ProgressMonitor monitor = new ProgressMonitor(ConsoleApp.getMainFrame(), "Finding files for download...", "", 0, 100);
        monitor.setMillisToDecideToPopup(0);
        monitor.setMillisToPopup(0);
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
                        downloadItems.addAll(addObjectsToExport(childPath, child));
                    }
                }
            }
            else if (domainObject instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) domainObject;
                List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(treeNode.getChildren());
                List<String> childPath = new ArrayList<>(path);
                childPath.add(domainObject.getName());
                for (DomainObject child : children) {
                    downloadItems.addAll(addObjectsToExport(childPath, child));
                }
            }
            else if (domainObject instanceof Filter) {
                Filter filter = (Filter) domainObject;
                SearchConfiguration config = new SearchConfiguration(filter, 1000);
                SolrSearchResults searchResults = config.performSearch();
                searchResults.loadAllResults();
                for (DomainObjectResultPage page : searchResults.getPages()) {
                    List<String> childPath = new ArrayList<>(path);
                    childPath.add(domainObject.getName());
                    for (DomainObject resultObject : page.getObjects()) {
                        downloadItems.addAll(addObjectsToExport(childPath, resultObject));
                    }
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

    private Multiset<FileType> getFileTypeCounts(ArtifactDescriptor artifactDescriptor, Progress progress, int startIndex, int total) throws Exception {
        
        Multiset<FileType> countedTypeNames = LinkedHashMultiset.create();
        
        log.info("Getting file sources for {}", artifactDescriptor);
        
        int i = 0;
        Set<Object> sources = new LinkedHashSet<>();
        for(DownloadObject downloadObject : downloadItems) {
            DomainObject domainObject = downloadObject.getDomainObject();
            sources.addAll(artifactDescriptor.getFileSources(domainObject));
            progress.setProgress(startIndex+i, total);
            i++;
        }
        
        log.info("Getting file types for {}", artifactDescriptor);
        
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
            if (source instanceof NeuronFragment) {
                NeuronFragment neuron = (NeuronFragment)source;
                Sample sample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, neuron.getSample().getTargetId());
                if (sample!=null) {
                    List<NeuronSeparation> results = sample.getResultsById(NeuronSeparation.class, neuron.getSeparationId());
                    if (!results.isEmpty()) {
                        NeuronSeparation separation = results.get(0);
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
        String objective = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "objective", null);
        log.info("Setting last objective: "+objective);
        state.setObjective(objective);
        
        String area = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "area", null);
        log.info("Setting last anatomical area: "+area);
        state.setArea(area);
        
        String resultCategory = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "resultCategory", null);
        log.info("Setting last resultCategory: "+resultCategory);
        state.setResultCategory(resultCategory);
        
        String imageCategory = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "imageCategory", null);
        log.info("Setting last imageCategory: "+imageCategory);
        state.setImageCategory(imageCategory);
        
        String artifactDescriptorString = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "artifactDescriptors", null);
        try {
            log.info("Setting last artifactDescriptorString: "+artifactDescriptorString);
            state.setArtifactDescriptorString(artifactDescriptorString);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
            log.error("Error reading artifactDescriptors preference. Clearing the corrupted preference.", e);
            FrameworkImplProvider.setLocalPreferenceValue(DownloadWizardState.class, "artifactDescriptors", null);
        }
        
        String outputExtensionString = FrameworkImplProvider.getLocalPreferenceValue(DownloadWizardState.class, "outputExtensions", null);
        try {
            log.info("Setting last outputExtensionString: "+outputExtensionString);
            state.setOutputExtensionString(outputExtensionString);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
            log.error("Error reading outputExtensions preference. Clearing the corrupted preference.", e);
            FrameworkImplProvider.setLocalPreferenceValue(DownloadWizardState.class, "outputExtensions", null);
        }

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

    private int totalToCheck = 0;
    private Queue<DownloadFileItem> toCheck = new LinkedList<>();
    private Queue<DownloadFileItem> toDownload = new LinkedList<>();
    
    private void download(List<DownloadFileItem> downloadItems) {
        ActivityLogHelper.logUserAction("DownloadWizardAction.beginDownload");

        for(DownloadFileItem item : downloadItems) {
            if (item.getSourceFile()!=null) {
                toCheck.add(item);
            }
        }
        
        totalToCheck = toCheck.size();
        continueAlreadyDownloadedChecks();
    }
    
    private void continueAlreadyDownloadedChecks() {

        log.info("continueAlreadyDownloadedChecks({} items to check, {} items to download)", toCheck.size(), toDownload.size());
        
        SimpleWorker worker = new SimpleWorker() {
            
            private DownloadFileItem downloadItem;
            private String filename;
            
            @Override
            protected void doStuff() throws Exception {
                
                while (!toCheck.isEmpty()) {
                    downloadItem = toCheck.remove();
                    setProgress(totalToCheck - toCheck.size(), totalToCheck);
                    
                    filename = getAlreadyDownloadedFilename(downloadItem);
                    if (filename!=null) {
                        // This file was already download
                        if (applyToAllChoice != null) {
                            if (evaluateRedownloadOption(downloadItem, applyToAllChoice)) {
                                toDownload.add(downloadItem);
                            }
                        }
                        else {
                            // Break out and finish this worker, so that the user can be prompted
                            break;
                        }
                    }
                    else {
                        toDownload.add(downloadItem);
                    }
                    
                    if (applyToAllChoice != null) {
                        if (applyToAllChoice==1) {
                            // Just download the rest
                            toDownload.addAll(toCheck);
                            toCheck.clear();
                        }
                        else if (applyToAllChoice==2) {
                            // Just ignore the rest
                            toCheck.clear();
                        }
                    }
                }
            }
            
            @Override
            protected void hadSuccess() {

                if (filename!=null) {
                    if (askUserToRedownload(downloadItem, filename, !toCheck.isEmpty())) {
                        toDownload.add(downloadItem);
                    }
                    continueAlreadyDownloadedChecks();
                }
                else {
                    if (!toDownload.isEmpty()) {
                        FileDownloadWorker worker = new FileDownloadWorker(toDownload, COPY_FILE_LOCK);
                        worker.startDownload();
                    }
                    else {
                        JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "There are no downloads to start.", "Nothing to do", JOptionPane.PLAIN_MESSAGE);
                    }
                }
            }
            
            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
            
        };

        worker.setProgressMonitor(new ProgressMonitor(ConsoleApp.getMainFrame(), "Verifying files", "", 0, 100));
        worker.execute();
        
    }
    
    private Map<Path,List<Path>> listCache = new HashMap<>();

    private String getAlreadyDownloadedFilename(DownloadFileItem downloadItem) throws IOException {

        final Path targetDir = downloadItem.getTargetFile().getParent();
        
        List<Path> files = listCache.get(targetDir);
        if (files==null && Files.exists(targetDir)) {
            files = Files.list(targetDir).collect(Collectors.toList());
            listCache.put(targetDir, files);
        }

        if (files==null) {
            // No files found
            return null;
        }
        
        final String targetExtension = downloadItem.getTargetExtension();
        final String targetName = downloadItem.getTargetFile().getFileName().toString();
        final String basename = FileUtil.getBasename(targetName).replaceAll("#","");

        // Find the first matching file
        for(Path file : files) {
            String name = file.getFileName().toString();
            if (name.startsWith(basename) && name.endsWith(targetExtension)) {
                return name;
            }
        }
        
        return null;
    }
    
    private boolean askUserToRedownload(DownloadFileItem downloadItem, String fileName, boolean showApplyToAll) {

        Integer chosenOptionIndex = applyToAllChoice;
        if (chosenOptionIndex==null) {

            String abbrName = StringUtils.abbreviate(fileName, 40);
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

        return evaluateRedownloadOption(downloadItem, chosenOptionIndex);
    }
    
    private boolean evaluateRedownloadOption(DownloadFileItem downloadItem, int chosenOptionIndex) {

        if (chosenOptionIndex == 0) {
            if (numBrowseFileAttempts == MAX_BROWSE_FILES) {
                log.info("Reached max number of file browses for this download context: {}", numBrowseFileAttempts);
                JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                        "Maximum number of folders have been opened. Further folders will not be opened for this file set.", "Open Folder", JOptionPane.WARNING_MESSAGE);
            }
            else if (numBrowseFileAttempts < MAX_BROWSE_FILES) {
                DesktopApi.browse(downloadItem.getTargetFile().getParent().toFile());
            }
            numBrowseFileAttempts++;
        }
        
        return chosenOptionIndex == 1;
    }
    
}
