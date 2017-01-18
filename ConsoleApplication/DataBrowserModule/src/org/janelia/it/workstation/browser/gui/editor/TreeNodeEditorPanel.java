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
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.model.search.SearchResults;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
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
    public void loadDomainObjectNode(AbstractDomainObjectNode<TreeNode> treeNodeNode, boolean isUserDriven, Callable<Void> success) {
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

            private List<DomainObject> children;
            private List<Annotation> annotations;
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                children = model.getDomainObjects(treeNode.getChildren());
                annotations = model.getAnnotations(DomainUtils.getReferences(children));
                loadPreferences();
                DomainUtils.sortDomainObjects(children, sortCriteria);
                searchResults = SearchResults.paginate(children, annotations);
                log.info("Showing "+children.size()+" items");
            }

            @Override
            protected void hadSuccess() {
                resultsPanel.showSearchResults(searchResults, true, new Callable<Void>() {
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

    public void showNothing() {
        resultsPanel.showNothing();
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
            if (treeNode==null) return;
            if (event.isTotalInvalidation()) {
                log.info("Total invalidation, reloading...");
                reload();
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (treeNode.getId().equals(domainObject.getId())) {
                        log.info("Tree node was invalidated, reloading...");
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
        if (treeNode==null) return;
        TreeNode updatedTreeNode = DomainMgr.getDomainMgr().getModel().getDomainObject(treeNode.getClass(), treeNode.getId());
        this.treeNode = updatedTreeNode;
        if (updatedTreeNode!=null && !treeNodeNode.getTreeNode().equals(updatedTreeNode)) {
            treeNodeNode.update(updatedTreeNode);
        }
        restoreState(saveState());
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
    protected AbstractDomainObjectNode<TreeNode> getDomainObjectNode() {
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

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (treeNode==null) return;
        if (event.getDomainObject().getId().equals(treeNode.getId())) {
            this.treeNode = null;
            searchResults = null;
            showNothing();
        }
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        if (searchResults.updateIfFound(event.getDomainObject())) {
            log.info("Updated search results with changed domain object: {}", event.getDomainObject());
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
        if (treeNodeNode==null) {
            loadDomainObject(treeNode, false, null);
        }
        else {
            loadDomainObjectNode(treeNodeNode, false, null);
        }
    }

    private void loadPreferences() {
        if (treeNode.getId()==null) return;
        try {
            Preference sortCriteriaPref = DomainMgr.getDomainMgr().getPreference(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, treeNode.getId().toString());
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
            DomainMgr.getDomainMgr().setPreference(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, treeNode.getId().toString(), sortCriteria);
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
