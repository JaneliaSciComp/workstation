package org.janelia.workstation.browser.gui.editor;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.interfaces.HasFilepath;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.GroupedFolder;
import org.janelia.model.domain.workspace.ProxyGroup;
import org.janelia.workstation.browser.actions.ExportResultsAction;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.listview.PaginatedDomainResultsPanel;
import org.janelia.workstation.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.workstation.browser.gui.support.LoadedImagePanel;
import org.janelia.workstation.browser.gui.support.SelectablePanel;
import org.janelia.workstation.browser.gui.support.SelectablePanelListPanel;
import org.janelia.workstation.common.gui.editor.DomainObjectEditor;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.common.nodes.GroupedFolderNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.selection.DomainObjectEditSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionModel;
import org.janelia.workstation.core.model.search.DomainObjectSearchResults;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.core.model.search.SearchResults;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Specialized component for executing color depth searches on the cluster and viewing their results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupedFolderEditorPanel extends JPanel implements
        DomainObjectEditor<GroupedFolder>,
        ParentNodeSelectionEditor<GroupedFolder, DomainObject, Reference>,
        SearchProvider,
        PreferenceSupport {

    private final static Logger log = LoggerFactory.getLogger(GroupedFolderEditorPanel.class);
    
    // Utilities
    private final Debouncer debouncer = new Debouncer();
    private final Debouncer groupDebouncer = new Debouncer();
    
    // UI Components
    private final JSplitPane splitPane;
    private final JPanel helpPanel;
    private final SelectablePanelListPanel groupListPanel;
    private final JScrollPane groupScrollPane;
    private final Set<LoadedImagePanel> lips = new HashSet<>();
    private final PaginatedDomainResultsPanel resultsPanel;
    
    // State
    private DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    private DomainObjectEditSelectionModel editSelectionModel = new DomainObjectEditSelectionModel();
    private GroupedFolderNode groupedFolderNode;
    private GroupedFolder groupedFolder;
    private List<ProxyGroup> groups; // cached masks
    private Map<ProxyGroup,DomainObject> groupProxyObjects = new HashMap<>(); // cached proxy objects
    private Reference selectedGroupRef = null;
    private Map<ProxyGroup,GroupPanel> groupPanelMap = new HashMap<>();

    // Results
    private DomainObjectSearchResults searchResults;
    private String sortCriteria;
    
    public GroupedFolderEditorPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        helpPanel = new JPanel();
        helpPanel.setLayout(new GridBagLayout());
        
        JPanel panel = new JPanel();    
        panel.add(new JLabel("<html>This Result Set is empty</html>"));
        helpPanel.add(panel, new GridBagConstraints());

        groupListPanel = new SelectablePanelListPanel() {

            @Override
            protected void panelSelected(SelectablePanel resultPanel, boolean isUserDriven) {
                if (resultPanel instanceof GroupPanel) {
                    ProxyGroup group = ((GroupPanel)resultPanel).getGroup();
                    loadGroup(group, isUserDriven, null);
                    Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(group), isUserDriven, true, true));
                }
            }

            @Override
            protected void updateHud(SelectablePanel resultPanel, boolean toggle) {
                if (resultPanel instanceof GroupPanel) {
                    DomainObject proxyObject = ((GroupPanel)resultPanel).getProxyObject();
                    String filepath = getFilepath(proxyObject);
                    if (filepath != null) {
                        Hud.getSingletonInstance().setFilepathAndToggleDialog(filepath, toggle, false);
                    }
                }
            }
            
            @Override
            protected void popupTriggered(MouseEvent e, SelectablePanel resultPanel) {
                if (resultPanel instanceof GroupPanel) {
                    ProxyGroup group = ((GroupPanel)resultPanel).getGroup();
                    DomainObject proxyObject = ((GroupPanel)resultPanel).getProxyObject();
                    GroupContextMenu popupMenu = new GroupContextMenu(groupedFolder, group, proxyObject);
                    popupMenu.addMenuItems();
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            
        };

        groupScrollPane = new JScrollPane();
        groupScrollPane.setBorder(BorderFactory.createEmptyBorder());
        groupScrollPane.setViewportView(groupListPanel);
        groupScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); 
        
        resultsPanel = new PaginatedDomainResultsPanel(getSelectionModel(), getEditSelectionModel(), this, this) {
            @Override
            protected ResultPage<DomainObject, Reference> getPage(SearchResults<DomainObject, Reference> searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
            @Override
            public Reference getId(DomainObject object) {
                return Reference.createFor(object);
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->TreeNodeEditorPanel"));
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, groupScrollPane, resultsPanel);
        splitPane.setDividerLocation(0.30);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        Dimension minimumSize = new Dimension(100, 0);
        groupScrollPane.setMinimumSize(minimumSize);
        resultsPanel.setMinimumSize(minimumSize);
        
        groupScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for(LoadedImagePanel image : lips) {
                    rescaleImage(image);
                    image.invalidate();
                }
            }
        });
    }
    
    private void reload() throws Exception {
        
        if (groupedFolder==null) {
            // Nothing to reload
            return;
        }
        
        try {
            GroupedFolder updatedFolder = DomainMgr.getDomainMgr().getModel().getDomainObject(groupedFolder);
            if (updatedFolder!=null) {
                if (groupedFolderNode!=null && !groupedFolderNode.getGroupedFolder().equals(updatedFolder)) {
                    groupedFolderNode.update(updatedFolder);
                }
                this.groupedFolder = updatedFolder; 
                DomainObjectEditorState<GroupedFolder, DomainObject, Reference> state = saveState();
                loadDomainObject(updatedFolder, false, () -> {
                    restoreState(state);    
                    return null;
                });
            }
            else {
                // The search no longer exists, or we no longer have access to it (perhaps running as a different user?) 
                // Either way, there's nothing to show. 
                showNothing();
            }
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
    
    @Override
    public String getName() {
        if (groupedFolder==null) {
            return "Grouped Folder";
        }
        return StringUtils.abbreviate(groupedFolder.getName(), 15);
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
    public void loadDomainObjectNode(DomainObjectNode<GroupedFolder> domainObjectNode, boolean isUserDriven, Callable<Void> success) {
        this.groupedFolderNode = (GroupedFolderNode) domainObjectNode;
        loadDomainObject(domainObjectNode.getDomainObject(), isUserDriven, success);
    }

    @Override
    public void loadDomainObject(final GroupedFolder groupedFolder, final boolean isUserDriven, final Callable<Void> success) {

        if (groupedFolder==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.info("loadDomainObject({},isUserDriven={})",groupedFolder.getName(),isUserDriven);
        final StopWatch w = new StopWatch();

        groups = null;
        groupPanelMap.clear();
        groupProxyObjects.clear();
        
        this.groupedFolder = groupedFolder;
        log.info("Loading {} groups", groupedFolder.getChildren().size());
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                groups = model.getDomainObjectsAs(ProxyGroup.class, groupedFolder.getChildren());
                
                List<Reference> proxyRefs = groups.stream().map(ProxyGroup::getProxyObject).collect(Collectors.toList());
                Map<Reference, DomainObject> proxyMap = DomainUtils.getMapByReference(model.getDomainObjects(proxyRefs));
                
                for(ProxyGroup group : groups) {
                    if (group.getProxyObject() != null) {
                        DomainObject domainObject = proxyMap.get(group.getProxyObject());
                        if (domainObject != null) {
                            groupProxyObjects.put(group, domainObject);
                        }
                    }
                }
                
                loadPreferences();
            }
            
            @Override
            protected void hadSuccess() {
                log.info("Loaded {} groups and {} proxies for {}", groups.size(), groupProxyObjects.size(), groupedFolder);
                showSearchView(isUserDriven);
                debouncer.success();
                ActivityLogHelper.logElapsed("GroupedFolderEditorPanel.loadDomainObject", groupedFolder, w);
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
        removeAll();
        updateUI();
    }
    
    private void showSearchView(boolean isUserDriven) {
        
        lips.clear();
        groupListPanel.clearPanels();
        
        JLabel titleLabel = new JLabel("");
        titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
     
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        groupListPanel.add(titlePanel);
        
        for(ProxyGroup group : groups) {
            DomainObject proxyObject = groupProxyObjects.get(group);
            GroupPanel maskPanel = new GroupPanel(group, proxyObject);
            groupListPanel.addPanel(maskPanel);
            groupPanelMap.put(group, maskPanel);
        }

        removeAll();
        
        if (groups.isEmpty()) {
            add(helpPanel, BorderLayout.CENTER);
            resultsPanel.showNothing();
        }
        else {
            ProxyGroup selectedGroup = null;
            if (selectedGroupRef != null) {
                log.debug("Checking groups for previously selected group: "+selectedGroupRef);
                for(ProxyGroup group : groups) {
                    if (group.getId().equals(selectedGroupRef.getTargetId())) {
                        selectedGroup = group;
                        break;
                    }
                }
            }
            
            add(splitPane, BorderLayout.CENTER);
            if (selectedGroup == null) {
                // Automatically select the first group
                log.debug("Selecting first group");
                groupListPanel.selectFirst(isUserDriven);
            }
            else {
                // Reselect the last selected group
                GroupPanel groupPanel = groupPanelMap.get(selectedGroup);
                log.debug("Selecting previously selected group: "+groupPanel.getGroup());
                groupListPanel.selectPanel(groupPanel, isUserDriven);
            }
            
            // Update selected group
            selectedGroupRef = Reference.createFor(((GroupPanel)groupListPanel.getSelectedPanel()).getGroup());
        }
        
        updateUI();
    }

    private void rescaleImage(LoadedImagePanel image) {
        double width = image.getParent()==null?0:image.getParent().getSize().getWidth();
        if (width<=0) {
            log.debug("Could not get width from parent, using viewport");
            width = groupScrollPane.getViewport().getSize().getWidth();
        }
        if (width<=0) {
            log.debug("Could not get width from parent or viewport");
            return;
        }
        int w = (int)Math.ceil(width) - 18;
        log.trace("Using width={}", w);
        image.scaleImage(w);
    }

    private void loadGroup(ProxyGroup group, final boolean isUserDriven, final Callable<Void> success) {

        if (group==null) return;
        
        if (!groupDebouncer.queue(success)) {
            log.info("Skipping group load, since there is one already in progress");
            return;
        }

        log.info("loadGroup({})",group);
        final StopWatch w = new StopWatch();
        resultsPanel.showLoadingIndicator();

        this.selectedGroupRef = Reference.createFor(group);
        
        SimpleWorker worker = new SimpleWorker() {

            private List<DomainObject> children;
            private List<Annotation> annotations;
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                children = model.getDomainObjects(group.getChildren());
                annotations = model.getAnnotations(DomainUtils.getReferences(children));
                DomainUtils.sortDomainObjects(children, sortCriteria);
                searchResults = new DomainObjectSearchResults(children, annotations);
            }

            @Override
            protected void hadSuccess() {
                log.info("Showing "+children.size()+" items");
                getSelectionModel().setParentObject(group);
                resultsPanel.showSearchResults(searchResults, true, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        groupDebouncer.success();
                        ActivityLogHelper.logElapsed("GroupedFolderEditorPanel.loadDomainObject", group, w);
                        return null;
                    }
                    // TODO: need debouncer failure case
                });
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                groupDebouncer.failure();
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }
    
    @Override
    public void resetState() {
        resultsPanel.reset();
    }

    @Override
    public DomainObjectEditorState<GroupedFolder, DomainObject, Reference> saveState() {
        if (groupedFolderNode==null) {
            if (groupedFolder==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new GroupedFolderEditorState(
                    groupedFolder,
                    selectedGroupRef,
                    resultsPanel.getCurrPage(),
                    resultsPanel.getViewer().saveState(),
                    getSelectionModel().getSelectedIds());
        }
        else {
            if (groupedFolderNode.getDomainObject()==null) {
                log.warn("No object is loaded, so state cannot be saved");
                return null;
            }
            return new GroupedFolderEditorState(
                    groupedFolderNode,
                    selectedGroupRef,
                    resultsPanel.getCurrPage(),
                    resultsPanel.getViewer().saveState(),
                    getSelectionModel().getSelectedIds());
        }
    }

    @Override
    public void restoreState(DomainObjectEditorState<GroupedFolder, DomainObject, Reference> state) {
        if (state==null) {
            log.warn("Cannot restore null state");
            return;
        }
        
        GroupedFolderEditorState myState = (GroupedFolderEditorState)state;
        log.info("Restoring state: {}", myState);
        if (state.getListViewerState()!=null) {
            resultsPanel.setViewerType(state.getListViewerState().getType());
        }

        // Prepare to restore the selection
        List<Reference> selected = getSelectionModel().getSelectedIds();
        selected.clear();
        selected.addAll(state.getSelectedIds());
        
        // Prepare to restore group selection
        this.selectedGroupRef = myState.getSelectedGroup();
        
        // Prepare to restore the page
        resultsPanel.setCurrPage(state.getPage());
        resultsPanel.getViewer().restoreState(state.getListViewerState());
           
        if (state.getDomainObjectNode()==null) {
            loadDomainObject(state.getDomainObject(), true, null);
        }
        else {
            loadDomainObjectNode(state.getDomainObjectNode(), true, null);
        }
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        try {
            if (groupedFolder==null) return;
            if (event.isTotalInvalidation()) {
                log.info("total invalidation, reloading...");
                reload();
            }
            else {
                for (DomainObject domainObject : event.getDomainObjects()) {
                    if (StringUtils.areEqual(domainObject.getId(), groupedFolder.getId())) {
                        log.info("Grouped folder invalidated, reloading...");
                        reload();
                        break;
                    }
                }
            }
        }  
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    @Subscribe
    public void domainObjectRemoved(DomainObjectRemoveEvent event) {
        if (groupedFolder==null) return;
        if (StringUtils.areEqual(event.getDomainObject().getId(), groupedFolder.getId())) {
            this.groupedFolder = null;
            showNothing();
        }
    }

    @Subscribe
    public void domainObjectChanged(DomainObjectChangeEvent event) {
        try {
            DomainObject domainObject = event.getDomainObject();
            if (domainObject==null) return;
            if (groupedFolder != null && domainObject.getId().equals(groupedFolder.getId())) {
                log.info("Grouped folder has changed, reloading...");
                reload();
            }
            else if (selectedGroupRef!=null && selectedGroupRef.getTargetId().equals(domainObject.getId())) {
                log.info("Selected group has changed, reloading...");
                reload();
            }
        }  
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
    
    private String getFilepath(DomainObject proxyObject) {
        // TODO: do we need an image model for the mask proxy objects?
        if (proxyObject instanceof HasFilepath) {
            return ((HasFilepath)proxyObject).getFilepath();
        }
        return null;
    }
    
    private class GroupPanel extends SelectablePanel {
        
        private final ProxyGroup group;
        private final DomainObject proxyObject;
        
        private GroupPanel(ProxyGroup group, DomainObject proxyObject) {
            
            this.group = group;
            this.proxyObject = proxyObject;
            
            int b = SelectablePanel.BORDER_WIDTH;
            setBorder(BorderFactory.createEmptyBorder(b, b, b, b));
            setLayout(new BorderLayout());
            
            JLabel label = new JLabel();
            label.setText(proxyObject==null?group.getName():proxyObject.getName());
            label.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 4));
            add(label, BorderLayout.NORTH);
            
            String filepath = getFilepath(proxyObject);
            
            add(getImagePanel(filepath), BorderLayout.CENTER);

            setFocusTraversalKeysEnabled(false);
        }

        public ProxyGroup getGroup() {
            return group;
        }
        
        public DomainObject getProxyObject() {
            return proxyObject;
        }

        private JPanel getImagePanel(String filepath) {
            LoadedImagePanel lip = new LoadedImagePanel(filepath) {
                @Override
                protected void doneLoading() {
                    rescaleImage(this);
                    revalidate();
                    repaint();
                }
            };
            rescaleImage(lip);
            lip.addMouseListener(new MouseForwarder(this, "LoadedImagePanel->PipelineResultPanel"));
            lips.add(lip);
            return lip;
        }
    }

    @Override
    public String getSortField() {
        return sortCriteria;
    }

    @Override
    public void setSortField(String sortCriteria) {
        this.sortCriteria = sortCriteria;
        savePreferences();
    }

    @Override
    public void search() {
        if (groupedFolderNode==null) {
            loadDomainObject(groupedFolder, false, null);
        }
        else {
            loadDomainObjectNode(groupedFolderNode, false, null);
        }
    }

    private void loadPreferences() {
        if (groupedFolder==null || groupedFolder.getId()==null) return;
        try {
            sortCriteria = (String) FrameworkAccess.getRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, groupedFolder.getId().toString(), null);
            log.debug("Loaded sort criteria preference: {}",sortCriteria);
        }
        catch (Exception e) {
            log.error("Could not load sort criteria",e);
        }
    }

    private void savePreferences() {
        if (StringUtils.isEmpty(sortCriteria)) return;
        try {
            FrameworkAccess.setRemotePreferenceValue(
                    DomainConstants.PREFERENCE_CATEGORY_SORT_CRITERIA, groupedFolder.toString(), sortCriteria);
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

    public DomainObjectEditSelectionModel getEditSelectionModel() {
        return editSelectionModel;
    }

    @Override
    public Long getCurrentContextId() {
        if (groupedFolder == null) return null;
        return groupedFolder.getId();
    }
}
