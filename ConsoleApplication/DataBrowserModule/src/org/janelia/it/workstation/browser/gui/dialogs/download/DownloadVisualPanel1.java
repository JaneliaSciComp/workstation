package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.browser.model.search.SolrSearchResults;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public final class DownloadVisualPanel1 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel1.class);

    private final Debouncer debouncer = new Debouncer();
    
    // GUI
    private final JLabel loadingLabel;
    private GroupedKeyValuePanel attrPanel;
    private JList<String> expandedObjectList;
    private JLabel expandedObjectCountLabel;

    // Inputs
    private List<? extends DomainObject> inputObjects;
    
    // Outputs
    private List<DownloadObject> downloadItems;

    @Override
    public String getName() {
        return "Selected Items";
    }

    /**
     * Creates new form DownloadVisualPanel1
     */
    public DownloadVisualPanel1() {
        setLayout(new BorderLayout());
        loadingLabel = new JLabel(Icons.getLoadingIcon());
        add(loadingLabel, BorderLayout.CENTER);
    }

    public void init(DownloadWizardState state) {

        this.inputObjects = state.getInputObjects();
        this.downloadItems = new ArrayList<>();
        findObjectsToExport(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                populateUI();
                populateExpandedObjectList(null);
                return null;
            }
        });
        ActivityLogHelper.logUserAction("DownloadVisualPanel1.init");
    }
    
    private void findObjectsToExport(final Callable<Void> success) {

        if (!debouncer.queue(success)) {
            log.debug("Skipping findObjectsToExport, since there is an operation already in progress");
            return;
        }

        log.info("findObjectsToExport(inputObjects={})", DomainUtils.abbr(inputObjects));

        Utils.setWaitingCursor(DownloadVisualPanel1.this);
        
        // Reset state that will be populated below
        downloadItems.clear();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for(DomainObject domainObject : inputObjects) {
                    addObjectsToExport(new ArrayList<String>(), domainObject);
                }
                log.info("Found {} objects to export",downloadItems.size());
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(DownloadVisualPanel1.this);
                debouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                Utils.setDefaultCursor(DownloadVisualPanel1.this);
                debouncer.failure();
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }

    private void addObjectsToExport(List<String> path, DomainObject domainObject) {
        try {
            // TODO: this should update some kind of label so the user knows what's going on during a long load
            log.info("addObjectsToExport({},{})", path, domainObject.getName());
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
                if (domainObject instanceof Sample) {
                    log.info("Adding Sample: " + domainObject.getName());
                    downloadItems.add(new DownloadObject(path, domainObject));
                }
                else {
                    log.info("Not just Samples. Adding " + domainObject.getName());
                    downloadItems.add(new DownloadObject(path, domainObject));
                }
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    private void populateExpandedObjectList(Callable<Void> success) {

        DefaultListModel<String> eolm = (DefaultListModel<String>)expandedObjectList.getModel();
        eolm.removeAllElements();
        for(DownloadObject downloadObject : downloadItems) {
            DomainObject domainObject = downloadObject.getDomainObject();
            log.info("Adding expanded object to list: "+domainObject.getName());
            eolm.addElement(domainObject.getName()+" ("+domainObject.getType()+")");
        }

        expandedObjectCountLabel.setText(downloadItems.size()+" items");
    }
    
    private void populateUI() {

        attrPanel = new GroupedKeyValuePanel("wrap 2, ins 10, fill", "[growprio 0]0[growprio 1, grow]", "[][growprio 200]");

        expandedObjectCountLabel = new JLabel();
        attrPanel.addItem("Item count", expandedObjectCountLabel);
        
        expandedObjectList = new JList<>(new DefaultListModel<String>());
        expandedObjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expandedObjectList.setLayoutOrientation(JList.VERTICAL);
        attrPanel.addItem("Preview items", new JScrollPane(expandedObjectList), "width 200:600:2000, grow");
        
        remove(loadingLabel);
        add(attrPanel, BorderLayout.CENTER);
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
                            countedArtifacts.add(new LSMArtifactDescriptor(objectiveSample.getObjective()));
                        }
                    }
                    SamplePipelineRun run = objectiveSample.getLatestSuccessfulRun();
                    if (run==null || run.getResults()==null) {
                        run = objectiveSample.getLatestRun();
                        if (run==null || run.getResults()==null) continue;
                    }
                    for(PipelineResult result : run.getResults()) {
                        log.trace("  Inspecting pipeline result: {}", result.getName());
                        if (result instanceof HasFileGroups && !(result instanceof LSMSummaryResult)) {
                            HasFileGroups hasGroups = (HasFileGroups)result;
                            for(String groupKey : hasGroups.getGroupKeys()) {
                                ResultDescriptor rd = ResultDescriptor.create().setObjective(objectiveSample.getObjective()).setResultName(result.getName()).setGroupName(groupKey);
                                log.trace("    Adding result artifact descriptor: {}", rd);
                                countedArtifacts.add(new ResultArtifactDescriptor(rd));
                            }
                        }
                        if (!DomainUtils.get2dTypeNames(result).isEmpty()) {
                            ResultDescriptor rd = ResultDescriptor.create().setObjective(objectiveSample.getObjective()).setResultName(result.getName());
                            log.trace("    Adding result artifact descriptor: {}", rd);
                            countedArtifacts.add(new ResultArtifactDescriptor(rd));
                        }
                    }
                }
            }
        }
        
        return countedArtifacts;
    }

    public List<DownloadObject> getDownloadObjects() {
        return downloadItems;
    }
}
