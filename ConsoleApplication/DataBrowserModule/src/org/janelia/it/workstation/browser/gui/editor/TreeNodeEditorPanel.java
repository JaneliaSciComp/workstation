package org.janelia.it.workstation.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Callable;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;


/**
 * Simple editor panel for viewing folders. In the future it may support drag and drop editing of folders.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeEditorPanel extends DomainObjectEditorPanel<TreeNode> implements SearchProvider {

    private final static Logger log = LoggerFactory.getLogger(TreeNodeEditorPanel.class);
    
    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Elements
    private final PaginatedResultsPanel resultsPanel;

    // State
    private TreeNodeNode treeNodeNode;
    private TreeNode treeNode;
    private List<DomainObject> domainObjects;
    private List<Annotation> annotations;
    
    // Results
    private SearchResults searchResults;
    private String sortCriteria;

    public TreeNodeEditorPanel() {
        
        setLayout(new BorderLayout());
        
        resultsPanel = new PaginatedResultsPanel(getSelectionModel(), this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->TreeNodeEditorPanel"));
        add(resultsPanel, BorderLayout.CENTER);
    }

    @Override
    public String getName() {
        if (treeNode==null) {
            return "Folder Editor";
        }
        else {
            return "Folder: "+StringUtils.abbreviate(treeNode.getName(), 15);
        }
    }

    @Override
    protected PaginatedResultsPanel getResultsPanel() {
        return resultsPanel;
    }

    @Override
    protected TreeNode getDomainObject() {
        return treeNode;
    }

    @Override
    protected DomainObjectNode<TreeNode> getDomainObjectNode() {
        return treeNodeNode;
    }
    
    @Override
    public void activate() {
        resultsPanel.activate();
    }

    @Override
    public void deactivate() {
        resultsPanel.deactivate();
    }

    @Override
    public void loadDomainObjectNode(DomainObjectNode<TreeNode> treeNodeNode, boolean isUserDriven, Callable<Void> success) {
        this.treeNodeNode = (TreeNodeNode)treeNodeNode;
        loadDomainObject(treeNodeNode.getDomainObject(), isUserDriven, success);
    }

    @Override
    public void loadDomainObject(final TreeNode treeNode, final boolean isUserDriven, final Callable<Void> success) {

        if (treeNode==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }

        log.info("loadDomainObject(TreeNode:{})",treeNode.getName());
        final StopWatch w = new StopWatch();
        resultsPanel.showLoadingIndicator();

        this.treeNode = treeNode;
        getSelectionModel().setParentObject(treeNode);
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                domainObjects = model.getDomainObjects(treeNode.getChildren());
                annotations = model.getAnnotations(DomainUtils.getReferences(domainObjects));
                loadPreferences();
                prepareResults();
                log.info("Showing "+domainObjects.size()+" items");
            }

            @Override
            protected void hadSuccess() {
                showResults(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        debouncer.success();
                        ActivityLogHelper.logElapsed("TreeNodeEditorPanel.loadDomainObject", treeNode, w);
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }

    private void prepareResults() throws Exception {
        DomainUtils.sortDomainObjects(domainObjects, sortCriteria);
        this.searchResults = SearchResults.paginate(domainObjects, annotations);
    }

    private void showResults(Callable<Void> success) {
        resultsPanel.showSearchResults(searchResults, true, success);
    }

    public void showNothing() {
        resultsPanel.showNothing();
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
            if (treeNodeNode==null || treeNode==null) return;
            if (event.isTotalInvalidation()) {
                log.info("total invalidation, reloading...");
                reload();
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (domainObject.getId().equals(treeNode.getId())) {
                        log.info("tree node invalidated, reloading...");
                        reload();
                        break;
                    }
                }
            }
        }  catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    private void reload() throws Exception {
        if (treeNodeNode==null || treeNode==null) return;
        TreeNode updatedFolder = DomainMgr.getDomainMgr().getModel().getDomainObject(TreeNode.class, treeNode.getId());
        if (updatedFolder!=null) {
            if (!treeNodeNode.getTreeNode().equals(updatedFolder)) {
                treeNodeNode.update(updatedFolder);
            }
            loadDomainObjectNode(treeNodeNode, false, null);
        }
    }
    
    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (treeNode==null) return;
        if (event.getDomainObject().getId().equals(treeNode.getId())) {
            this.treeNode = null;
            domainObjects.clear();
            annotations.clear();
            searchResults = null;
            showNothing();
        }
    }

    @Override
    public String getSortField() {
        return sortCriteria;
    }

    @Override
    public void setSortField(final String sortCriteria) {
        this.sortCriteria = sortCriteria;
        savePreferences();
    }

    @Override
    public void search() {

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadPreferences();
                prepareResults();
                log.info("Showing "+domainObjects.size()+" items");
            }

            @Override
            protected void hadSuccess() {
                showResults(null);
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }

    private void loadPreferences() {
        if (treeNode.getId()==null) return;
        try {
            Preference sortCriteriaPref = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, treeNode.getId().toString());
            if (sortCriteriaPref!=null) {
                log.debug("Loaded sort criteria preference: {}",sortCriteriaPref.getValue());
                sortCriteria = (String)sortCriteriaPref.getValue();
            }
            else {
                sortCriteria = null;
            }
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            DomainMgr.getDomainMgr().setPreference(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, treeNode.getId().toString(), sortCriteria);
            log.debug("Saved sort criteria preference: {}",sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }

    @Override
    public void export() {
        DomainObjectTableViewer viewer = null;
        if (resultsPanel.getViewer() instanceof DomainObjectTableViewer) {
            viewer = (DomainObjectTableViewer)resultsPanel.getViewer();
        }
        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(searchResults, viewer);
        action.actionPerformed(null);
    }
}
