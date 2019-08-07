package org.janelia.workstation.browser.gui.editor;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.Node;
import org.janelia.workstation.browser.actions.ExportResultsAction;
import org.janelia.workstation.browser.gui.listview.PaginatedDomainResultsPanel;
import org.janelia.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectEditSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionModel;
import org.janelia.workstation.core.events.selection.ViewerContextChangeEvent;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple editor panel for viewing folders. In the future it may support drag and drop editing of folders.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TreeNodeEditorPanel extends DomainObjectEditorPanel<Node,DomainObject>
        implements SearchProvider, PreferenceSupport {

    private final static Logger log = LoggerFactory.getLogger(TreeNodeEditorPanel.class);
    
    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Elements
    private final PaginatedDomainResultsPanel resultsPanel;

    // State
    private DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    private DomainObjectEditSelectionModel editSelectionModel = new DomainObjectEditSelectionModel();
    private DomainObjectNode<Node> nodeNode;
    private Node node;
    
    // Results
    private DomainObjectSearchResults searchResults;
    private String sortCriteria;

    public TreeNodeEditorPanel() {
        
        setLayout(new BorderLayout());
        
        resultsPanel = new PaginatedDomainResultsPanel(getSelectionModel(), getEditSelectionModel(), this, this) {
            @Override
            protected ResultPage<DomainObject, Reference> getPage(SearchResults<DomainObject, Reference> searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
            @Override
            public Reference getId(DomainObject object) {
                return Reference.createFor(object);
            }
            @Override
            protected void viewerContextChanged() {
                Events.getInstance().postOnEventBus(new ViewerContextChangeEvent(this, getViewerContext()));
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->TreeNodeEditorPanel"));
        add(resultsPanel, BorderLayout.CENTER);
    }

    @Override
    public void loadDomainObjectNode(DomainObjectNode<Node> nodeNode, boolean isUserDriven, Callable<Void> success) {
        this.nodeNode = nodeNode;
        loadDomainObject(nodeNode.getDomainObject(), isUserDriven, success);
    }

    @Override
    public void loadDomainObject(final Node node, final boolean isUserDriven, final Callable<Void> success) {

        if (node==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }

        log.info("loadDomainObject({})",node.getName());
        final StopWatch w = new StopWatch();
        resultsPanel.showLoadingIndicator();

        this.node = node;
        getSelectionModel().setParentObject(node);
        
        SimpleWorker worker = new SimpleWorker() {

            private List<DomainObject> children;
            private List<Annotation> annotations;
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                children = model.getDomainObjects(node.getChildren());
                annotations = model.getAnnotations(DomainUtils.getReferences(children));
                loadPreferences();
                DomainUtils.sortDomainObjects(children, sortCriteria);
                searchResults = new DomainObjectSearchResults(children, annotations);
                log.info("Showing "+children.size()+" items");
            }

            @Override
            protected void hadSuccess() {
                resultsPanel.showSearchResults(searchResults, true, () -> {
                    debouncer.success();
                    ActivityLogHelper.logElapsed("TreeNodeEditorPanel.loadDomainObject", node, w);
                    return null;
                });
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                debouncer.failure();
                FrameworkAccess.handleException(error);
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
            if (node==null) return;
            if (event.isTotalInvalidation()) {
                log.info("Total invalidation, reloading...");
                reload();
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (node.getId().equals(domainObject.getId())) {
                        log.info("Tree node was invalidated, reloading...");
                        reload();
                        break;
                    }
                }
            }
        }  catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    private void reload() throws Exception {
        
        if (node==null) {
            // Nothing to reload
            return;
        }
        
        Node updatedNode = DomainMgr.getDomainMgr().getModel().getDomainObject(node.getClass(), node.getId());
        log.info("Got updated node: {}", updatedNode);
        if (updatedNode!=null) {
            if (nodeNode != null && !nodeNode.getDomainObject().equals(updatedNode)) {
                nodeNode.update(updatedNode);
            }
            this.node = updatedNode;
            restoreState(saveState());
        }
        else {
            // The folder no longer exists, or we no longer have access to it (perhaps running as a different user?) 
            // Either way, there's nothing to show. 
            showNothing();
        }
    }

    @Override
    public String getName() {
        if (node==null) {
            return "Folder Editor";
        }
        else {
            return "Folder: "+StringUtils.abbreviate(node.getName(), 15);
        }
    }

    @Override
    protected PaginatedDomainResultsPanel getResultsPanel() {
        return resultsPanel;
    }

    @Override
    protected Node getDomainObject() {
        return node;
    }

    @Override
    protected DomainObjectNode<Node> getDomainObjectNode() {
        return nodeNode;
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
        if (node==null) return;
        if (event.getDomainObject().getId().equals(node.getId())) {
            this.node = null;
            searchResults = null;
            showNothing();
        }
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        if (searchResults!=null && searchResults.updateIfFound(event.getDomainObject())) {
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
        if (nodeNode ==null) {
            loadDomainObject(node, false, null);
        }
        else {
            loadDomainObjectNode(nodeNode, false, null);
        }
    }

    private void loadPreferences() {
        if (node==null || node.getId()==null) return;
        try {
            sortCriteria = (String) FrameworkAccess.getRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, node.getId().toString(), null);
            log.debug("Loaded sort criteria preference: {}",sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (node==null || node.getId()==null) return;
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            FrameworkAccess.setRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, node.getId().toString(), sortCriteria);
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
        ExportResultsAction<DomainObject, Reference> action = new ExportResultsAction<>(searchResults, viewer);
        action.actionPerformed(null);
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Override
    public DomainObjectEditSelectionModel getEditSelectionModel() {
        return editSelectionModel;
    }

    @Override
    public ViewerContext<DomainObject, Reference> getViewerContext() {
        return new ViewerContext<DomainObject, Reference>() {
            @Override
            public ChildSelectionModel<DomainObject, Reference> getSelectionModel() {
                return selectionModel;
            }

            @Override
            public ChildSelectionModel<DomainObject, Reference> getEditSelectionModel() {
                return resultsPanel.isEditMode() ? editSelectionModel : null;
            }

            @Override
            public ImageModel<DomainObject, Reference> getImageModel() {
                return resultsPanel.getImageModel();
            }
        };
    }

    @Override
    public Long getCurrentContextId() {
        Object parentObject = getSelectionModel().getParentObject();
        if (parentObject instanceof HasIdentifier) {
            return ((HasIdentifier)parentObject).getId();
        }
        throw new IllegalStateException("Parent object has no identifier: "+getSelectionModel().getParentObject());
    }
}
