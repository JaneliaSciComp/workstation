package org.janelia.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.util.Progress;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.model.search.DomainObjectResultPage;
import org.janelia.workstation.core.model.search.SearchConfiguration;
import org.janelia.workstation.core.model.search.SolrSearchResults;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.core.options.DownloadOptions;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.browser.gui.support.FileDownloadWorker;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.interfaces.HasFileGroups;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.workspace.Node;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

/**
 * Action which brings up the Download wizard. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class DownloadWizardAction implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(DownloadWizardAction.class);

    private static final int MAX_CONCURRENT_DOWNLOADS = DownloadOptions.getInstance().getNumConcurrentDownloads();
    
    private ArtifactDescriptor defaultResultDescriptor;
    private Collection<? extends DomainObject> inputObjects;
    private List<DownloadObject> downloadItems = new ArrayList<>();
    private Map<ArtifactDescriptor,Multiset<FileType>> artifactFileCounts;
    private static final Semaphore COPY_SEMAPHORE = new Semaphore(MAX_CONCURRENT_DOWNLOADS);
    private static final int MAX_BROWSE_FILES = 10;
    private Integer applyToAllChoice;
    private int numBrowseFileAttempts = 0;
    
    public DownloadWizardAction() throws Exception {
        List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
        this.inputObjects = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectedIds);
    }
    
    public DownloadWizardAction(Collection<? extends DomainObject> domainObjects, ArtifactDescriptor defaultResultDescriptor) {
        this.inputObjects = domainObjects;
        this.defaultResultDescriptor = defaultResultDescriptor;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("DownloadWizardAction.actionPerformed");
        findDownloadObjects();
    }

    private void findDownloadObjects() {
        
        UIUtils.setWaitingCursor(FrameworkAccess.getMainFrame());
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                                
                log.info("Precaching LSMs");
                List<Reference> lsmRefs = new ArrayList<>();
                for(DomainObject domainObject : inputObjects) {
                    if (isCancelled()) return;
                    if (domainObject instanceof Sample) {
                        Sample sample = (Sample)domainObject;
                        lsmRefs.addAll(sample.getLsmReferences());
                    }
                }
                if (!lsmRefs.isEmpty()) {
                    DomainMgr.getDomainMgr().getModel().getDomainObjectsAs(LSMImage.class, lsmRefs);
                }

                setProgress(1);
                
                log.info("Finding items for download");
                for(DomainObject domainObject : inputObjects) {
                    if (isCancelled()) return;
                    downloadItems.addAll(addObjectsToExport(new ArrayList<String>(), domainObject));
                }
                
                setProgress(2);
                
                log.info("Got {} download items", downloadItems.size());
                log.info("Collecting descriptors");
                
                List<DomainObject> domainObjects = new ArrayList<>();
                for(DownloadObject downloadObject : downloadItems) {
                    domainObjects.add(downloadObject.getDomainObject());
                }
                
                Multiset<ArtifactDescriptor> artifactCounts = DescriptorUtils.getArtifactCounts(domainObjects);
                Set<ArtifactDescriptor> elementSet = artifactCounts.elementSet();

                setProgress(3);
                
                log.info("Got {} artifact descriptors", elementSet.size());
                log.info("Finding files");
                
                int startIndex = 3;
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
                UIUtils.setDefaultCursor(FrameworkAccess.getMainFrame());
                if (!isCancelled()) {
                    showWizard();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                UIUtils.setDefaultCursor(FrameworkAccess.getMainFrame());
                FrameworkAccess.handleException(error);
            }
        };

        ProgressMonitor monitor = new ProgressMonitor(FrameworkAccess.getMainFrame(), "Finding files for download...", "", 0, 100);
        monitor.setMillisToDecideToPopup(0);
        monitor.setMillisToPopup(0);
        worker.setProgressMonitor(monitor);
        worker.execute();
    }
    
    private List<DownloadObject> addObjectsToExport(List<String> path, DomainObject domainObject) {
        List<DownloadObject> downloadItems = new ArrayList<>();
        try {
            log.debug("addObjectsToExport({},{})", path, domainObject.getName());
            if (domainObject instanceof Node) {
                Node treeNode = (Node) domainObject;
                if (treeNode.hasChildren()) {
                    // TODO: move this to a common utility class for dealing with tree nodes
                    // Dedup objects
                    Set<Reference> childRefs = new LinkedHashSet<>(treeNode.getChildren());
                    List<DomainObject> children = DomainMgr.getDomainMgr().getModel().getDomainObjects(new ArrayList<>(childRefs));
                    List<String> childPath = new ArrayList<>(path);
                    childPath.add(domainObject.getName());
                    for (DomainObject child : children) {
                        downloadItems.addAll(addObjectsToExport(childPath, child));
                    }
                }
            }
            else if (domainObject instanceof Filter) {
                Filter filter = (Filter) domainObject;
                SearchConfiguration config = new SearchConfiguration(filter, 1000);
                SolrSearchResults searchResults = config.performSearch();
                
                // Load all results
                for(int i=0; i<searchResults.getNumTotalPages(); i++) {
                    searchResults.getPage(i);
                }
                
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
            FrameworkAccess.handleException(e);
        }
        return downloadItems;
    }

    private Multiset<FileType> getFileTypeCounts(ArtifactDescriptor artifactDescriptor, Progress progress, int startIndex, int total) throws Exception {
        
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

        Multiset<FileType> countedTypeNames = LinkedHashMultiset.create();
        boolean only2d = false;
        for (Object source : sources) {
            log.trace("Inspecting file source: {}", source.getClass().getSimpleName());
            if (source instanceof HasFileGroups) {
                Multiset<FileType> fileTypes = DomainUtils.getFileTypes((HasFileGroups)source, only2d);
                log.trace("  Source has file groups: {}",fileTypes);
                addAll(countedTypeNames, fileTypes);
            }
            if (source instanceof HasFiles) {
                Multiset<FileType> fileTypes = DomainUtils.getFileTypes((HasFiles) source, only2d);
                log.trace("  Source has files: {}",fileTypes);
                addAll(countedTypeNames, fileTypes);
            }
            if (source instanceof PipelineResult) {
                PipelineResult result = (PipelineResult)source;
                NeuronSeparation separation = result.getLatestSeparationResult();
                if (separation!=null) {
                    log.trace("  Source has separation: {}",separation);
                    Set<FileType> typeNames = new HashSet<>();
                    typeNames.add(FileType.NeuronAnnotatorLabel);
                    Sample sample = result.getParentRun().getParent().getParent();
                    if (!DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getSeparationCompressionType())) {
                        typeNames.add(FileType.NeuronAnnotatorSignal);
                        typeNames.add(FileType.NeuronAnnotatorReference);
                    }
                    log.trace("    Adding type names: {}",typeNames);
                    addAll(countedTypeNames, typeNames);
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
                        if (!DomainConstants.VALUE_COMPRESSION_VISUALLY_LOSSLESS.equals(sample.getSeparationCompressionType())) {
                            typeNames.add(FileType.NeuronAnnotatorSignal);
                            typeNames.add(FileType.NeuronAnnotatorReference);
                        }
                        log.trace("    Adding type names: {}",typeNames);
                        addAll(countedTypeNames, typeNames);
                    }
                }
            }
        }
        
        
        return countedTypeNames;
    }
    
    private void addAll(Multiset<FileType> countedTypeNames, Collection<FileType> typeNames) {
        for(FileType fileType : typeNames) {

            // Replace individual color depth MIPs with the aggregate file type
            // This allows users to download all the MIPs with signal and ignore the reference
            if (fileType==FileType.ColorDepthMip1 
                    || fileType==FileType.ColorDepthMip2 
                    || fileType==FileType.ColorDepthMip3 
                    || fileType==FileType.ColorDepthMip4) {
                countedTypeNames.add(FileType.ColorDepthMips);
            }
            else {
                countedTypeNames.add(fileType);
            }
        }
    }
    
    private void showWizard() {

        // Hide the default wizard image, which does not look good on our dark background
        UIDefaults uiDefaults = UIManager.getDefaults();
        uiDefaults.put("nb.wizard.hideimage", Boolean.TRUE); 
        
        // Setup the initial state
        DownloadWizardState state = new DownloadWizardState();
        state.setInputObjects(new ArrayList<>(inputObjects));
        state.setDefaultArtifactDescriptor(defaultResultDescriptor);
        state.setDownloadObjects(downloadItems);
        state.setArtifactFileCounts(artifactFileCounts);
        
        // Restore previous state from user's last usage
        String objective = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "objective", null);
        log.info("Setting last objective: "+objective);
        state.setObjective(objective);
        
        String area = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "area", null);
        log.info("Setting last anatomical area: "+area);
        state.setArea(area);
        
        String resultCategory = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "resultCategory", null);
        log.info("Setting last resultCategory: "+resultCategory);
        state.setResultCategory(resultCategory);
        
        String imageCategory = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "imageCategory", null);
        log.info("Setting last imageCategory: "+imageCategory);
        state.setImageCategory(imageCategory);
        
        String artifactDescriptorString = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "artifactDescriptors", null);
        try {
            log.info("Setting last artifactDescriptorString: "+artifactDescriptorString);
            state.setArtifactDescriptorString(artifactDescriptorString);
        }
        catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly(e);
            log.error("Error reading artifactDescriptors preference. Clearing the corrupted preference.", e);
            FrameworkAccess.setLocalPreferenceValue(DownloadWizardState.class, "artifactDescriptors", null);
        }
        
        String outputExtensionString = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "outputExtensions", null);
        try {
            log.info("Setting last outputExtensionString: "+outputExtensionString);
            state.setOutputExtensionString(outputExtensionString);
        }
        catch (Exception e) {
            FrameworkAccess.handleExceptionQuietly(e);
            log.error("Error reading outputExtensions preference. Clearing the corrupted preference.", e);
            FrameworkAccess.setLocalPreferenceValue(DownloadWizardState.class, "outputExtensions", null);
        }

        boolean splitChannels = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "splitChannels", state.isSplitChannels());
        log.info("Setting last splitChannels: "+splitChannels);
        state.setSplitChannels(splitChannels);

        boolean flattenStructure = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "flattenStructure", state.isFlattenStructure());
        log.info("Setting last flattenStructure: "+flattenStructure);
        state.setFlattenStructure(flattenStructure);

        String filenamePattern = FrameworkAccess.getLocalPreferenceValue(DownloadWizardState.class, "filenamePattern", null);
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
    private List<DownloadFileItem> toDownload = new LinkedList<>();
    
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
                
                // Check all files to see if they have already been downloaded
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
                     
                        log.info("Will download {} items with a max of {} workers", toDownload.size(), MAX_CONCURRENT_DOWNLOADS);
                        
                        // Start a worker for each concurrent download
                        int sublistSize = (int)Math.ceil((double)toDownload.size() / (double)MAX_CONCURRENT_DOWNLOADS);
                        for(List<DownloadFileItem> sublist : Lists.partition(toDownload, sublistSize)) {
                            FileDownloadWorker worker = new FileDownloadWorker(sublist, COPY_SEMAPHORE);
                            worker.startDownload();
                        }
                        
                    }
                    else {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "There are no downloads to start.", "Nothing to do", JOptionPane.PLAIN_MESSAGE);
                    }
                }
            }
            
            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
            
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Verifying files", "", 0, 100));
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
        final String basename = Utils.getBasename(targetName).replaceAll("#","");

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
                    FrameworkAccess.getMainFrame(),
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
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
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
