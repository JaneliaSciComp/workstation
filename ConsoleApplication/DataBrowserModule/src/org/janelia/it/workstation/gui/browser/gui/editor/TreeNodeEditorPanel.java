package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.gui.browser.gui.support.Debouncer;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple editor panel for viewing folders. In the future it may support drag and drop editing of folders.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeEditorPanel extends JPanel
        implements DomainObjectNodeSelectionEditor<TreeNode>, SearchProvider {

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
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    private String sortCriteria;

    public TreeNodeEditorPanel() {
        
        setLayout(new BorderLayout());
        
        resultsPanel = new PaginatedResultsPanel(selectionModel, this) {
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
    public Object getEventBusListener() {
        return resultsPanel;
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
        resultsPanel.showLoadingIndicator();

        this.treeNode = treeNode;
        selectionModel.setParentObject(treeNode);
        
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
                showResults();
                debouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }

    private void prepareResults() throws Exception {
        DomainUtils.sortDomainObjects(domainObjects, sortCriteria);
        this.searchResults = SearchResults.paginate(domainObjects, annotations);
    }

    private void showResults() {
        resultsPanel.showSearchResults(searchResults, true);
    }

    public void showNothing() {
        resultsPanel.showNothing();
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
            if (treeNodeNode==null || treeNode==null) return;
            if (event.isTotalInvalidation()) {
                log.info("total invalidation, reloading...");
                TreeNode updatedFolder = DomainMgr.getDomainMgr().getModel().getDomainObject(TreeNode.class, treeNode.getId());
                if (updatedFolder!=null) {
                    treeNodeNode.update(updatedFolder);
                    loadDomainObjectNode(treeNodeNode, false, null);
                }
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (domainObject.getId().equals(treeNode.getId())) {
                        log.info("tree node invalidated, reloading...");
                        TreeNode updatedFolder = DomainMgr.getDomainMgr().getModel().getDomainObject(TreeNode.class, treeNode.getId());
                        if (updatedFolder!=null) {
                            treeNodeNode.update(updatedFolder);
                            loadDomainObjectNode(treeNodeNode, false, null);
                        }
                        break;
                    }
                }
            }
        }  catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
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
                showResults();
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }

    @Override
    public void export() {
        DomainObjectTableViewer viewer = null;
        if (resultsPanel.getViewer() instanceof DomainObjectTableViewer) {
            viewer = (DomainObjectTableViewer)resultsPanel.getViewer();
        }
        ExportResultsAction<DomainObject> action = new ExportResultsAction<>(searchResults, viewer);
        action.doAction();
    }

    private void loadPreferences() {
        if (treeNode.getId()==null) return;
        try {
            Preference sortCriteriaPref = DomainMgr.getDomainMgr().getPreference(DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, treeNode.getId().toString());
            if (sortCriteriaPref!=null) {
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
        }
        catch (Exception e) {
            log.error("Could not save sort criteria",e);
        }
    }

    @Override
    public DomainObjectEditorState saveState() {
        DomainObjectEditorState state = new DomainObjectEditorState(
                treeNodeNode,
                resultsPanel.getCurrPage(),
                resultsPanel.getViewer().saveState(),
                selectionModel.getSelectedIds());
        return state;
    }

    @Override
    public void loadState(DomainObjectEditorState state) {
        // TODO: do a better job of restoring the state
        resultsPanel.setViewerType(state.getListViewerState().getType());
        loadDomainObjectNode((TreeNodeNode)state.getDomainObjectNode(), true, null);
    }
}
