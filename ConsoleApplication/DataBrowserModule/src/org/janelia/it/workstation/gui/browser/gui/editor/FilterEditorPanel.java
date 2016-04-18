package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.*;
import org.janelia.it.jacs.model.domain.gui.search.criteria.TreeNodeCriteria;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.solr.FacetValue;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.actions.ExportResultsAction;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.gui.dialogs.EditCriteriaDialog;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.gui.listview.table.DomainObjectTableViewer;
import org.janelia.it.workstation.gui.browser.gui.support.Debouncer;
import org.janelia.it.workstation.gui.browser.gui.support.DropDownButton;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchConfiguration;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.util.datatransfer.ExTransferable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;


/**
 * The Filter Editor is the main search GUI in the Workstation. Users can create, save, and load filters 
 * into this panel. The filter is executed every time it changes, and shows results in an embedded 
 * PaginatedResultsPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FilterEditorPanel extends JPanel implements DomainObjectSelectionEditor<Filter>, SearchProvider {

    private static final Logger log = LoggerFactory.getLogger(FilterEditorPanel.class);

    public static final int EXPORT_PAGE_SIZE = 1000;
    
    // UI Settings
    public static final String DEFAULT_FILTER_NAME = "Unsaved Filter";
    public static final Class<?> DEFAULT_SEARCH_CLASS = Sample.class;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final int MAX_VALUES_STRING_LENGTH = 20;
    
    // Utilities
    private final Debouncer debouncer = new Debouncer();
    
    // UI Elements
    private final ConfigPanel configPanel;
    private final JButton saveButton;
    private final JButton saveAsButton;
    private final PaginatedResultsPanel resultsPanel;
    private final DropDownButton typeCriteriaButton;
    private final DropDownButton addCriteriaButton;
    private final JComboBox<String> inputField;    
    
    // State
    private Filter filter;    
    private boolean dirty = false;
    private SearchConfiguration searchConfig;
    
    // Results
    private SearchResults searchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    public FilterEditorPanel() {
        
        this.saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleWorker worker = new SimpleWorker() {
                        
                    private Filter savedFilter;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        savedFilter = DomainMgr.getDomainMgr().getModel().save(filter);
                    }

                    @Override
                    protected void hadSuccess() {
                        setFilter(savedFilter);
                        saveButton.setVisible(false);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
                
            }
        });
        
        this.saveAsButton = new JButton("Save As");
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String name = filter.getName().equals(DEFAULT_FILTER_NAME)?null:filter.getName();
                String newName = null;
                while (StringUtils.isEmpty(newName)) {
                    newName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(),
                            "Filter Name:\n", "Save Filter", JOptionPane.PLAIN_MESSAGE, null, null, name);
                    log.info("newName:" + newName);
                    if (newName == null) {
                        // User chose "Cancel"
                        return;
                    }
                    if ("".equals(newName)) {
                        JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "Filter name cannot be blank");
                    }
                }

                final String finalName = newName;
                final boolean isNewFilter = filter.getId()==null;

                SimpleWorker worker = new SimpleWorker() {
                        
                    @Override
                    protected void doStuff() throws Exception {
                        Filter savedFilter = filter;
                        if (!isNewFilter) {
                            // This filter is already saved, duplicate it so that we don't overwrite the existing one
                            savedFilter = DomainUtils.cloneFilter(filter);
                        }
                        savedFilter.setName(finalName);
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        savedFilter = model.save(savedFilter);
                        model.addChild(model.getDefaultWorkspace(), savedFilter);
                        setFilter(savedFilter);
                    }

                    @Override
                    protected void hadSuccess() {
                        // Wait for events to resolve
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                // Select the filter and force it to reload
                                DomainExplorerTopComponent.getInstance().selectNodeById(filter.getId());
                            }
                        });
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.execute();
                
            }
        });
        
        this.typeCriteriaButton = new DropDownButton("Type: Sample");
        
        ButtonGroup typeGroup = new ButtonGroup();
        
        for (final Class<? extends DomainObject> searchClass : DomainUtils.getSearchClasses()) {
            final String label = searchClass.getAnnotation(SearchType.class).label();
            JMenuItem menuItem = new JRadioButtonMenuItem(label, searchClass.equals(DEFAULT_SEARCH_CLASS));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dirty = true;
                    searchConfig.setSearchClass(searchClass);
                    updateView();
                    refreshSearchResults();
                }
            });
            typeGroup.add(menuItem);
            typeCriteriaButton.getPopupMenu().add(menuItem);
        }
        
        this.addCriteriaButton = new DropDownButton("Add Criteria...");
        
        this.inputField = new JComboBox<>();
        inputField.setMinimumSize(new Dimension(100, Integer.MIN_VALUE));
        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        inputField.setEditable(true);
        inputField.setToolTipText("Enter search terms...");
        
        AbstractAction mySearchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSearchResults();
            }
        };
        
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true), "enterAction");
        getActionMap().put("enterAction", mySearchAction);
        
        this.resultsPanel = new PaginatedResultsPanel(selectionModel, this) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                return searchResults.getPage(page);
            }
        };
        resultsPanel.addMouseListener(new MouseForwarder(this, "PaginatedResultsPanel->FilterEditorPanel"));

        configPanel = new ConfigPanel(true);
        
        setLayout(new BorderLayout());
        add(configPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);

        MyDropTargetListener dtl = new MyDropTargetListener();
        DropTarget dt = new DropTarget(this, dtl);
        dt.setDefaultActions(DnDConstants.ACTION_COPY);
        dt.setActive(true);
    }
     
    private void setFilter(Filter filter) {
        this.filter = filter;
        this.searchConfig = new SearchConfiguration(filter, SearchResults.PAGE_SIZE);
    }
    
    public void loadNewFilter() {
        Filter newFilter = new Filter();
        newFilter.setName(DEFAULT_FILTER_NAME);
        newFilter.setSearchClass(DEFAULT_SEARCH_CLASS.getName());
        loadDomainObject(newFilter, true, null);
    }
    
    @Override
    public void loadDomainObject(Filter filter, final boolean isUserDriven, final Callable<Void> success) {
        
        if (filter==null) return;
        
        if (!debouncer.queue(success)) {
            log.info("Skipping load, since there is one already in progress");
            return;
        }
        
        log.debug("loadDomainObject(Filter:{})",filter.getName());
        selectionModel.setParentObject(filter);
        
        this.dirty = false;
        setFilter(filter);
        
        try {
            updateView();
            
            configPanel.removeAllTitleComponents();
            if (ClientDomainUtils.hasWriteAccess(filter) || filter.getName().equals(DEFAULT_FILTER_NAME)) {
	            configPanel.addTitleComponent(saveButton, false, true);
	            configPanel.addTitleComponent(saveAsButton, false, true);
            }
            configPanel.setExpanded(filter.getId()==null);
            
            refreshSearchResults();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public JPanel getResultsPanel() {
        return resultsPanel;
    }
    
    @Override
    public String getName() {
        if (filter==null) {
            return "Filter Editor";
        }
        return "Filter: "+StringUtils.abbreviate(filter.getName(), 15);
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
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
    
    private String getInputFieldValue() {
        return (String)inputField.getSelectedItem();
    }

    public void dropDomainObject(DomainObject obj) {
        if (obj instanceof TreeNode) {
            Reference reference = Reference.createFor(TreeNode.class, obj.getId());

            TreeNodeCriteria criteria = new TreeNodeCriteria();
            criteria.setTreeNodeName(obj.getName());
            criteria.setTreeNodeReference(reference);
            filter.addCriteria(criteria);

            dirty = true;
            refreshSearchResults();
        }
    }

    private void refreshSearchResults() {
        debouncer.queue();
        refreshSearchResults(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                debouncer.success();
                return null;
            }
        },new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                debouncer.failure();
                return null;
            }
        });
    }
    
    private void refreshSearchResults(final Callable<Void> success, final Callable<Void> failure) {
        log.trace("refresh");
        
        String inputFieldValue = getInputFieldValue();
        if (filter.getSearchString()!=null && !filter.getSearchString().equals(inputFieldValue)) {
            dirty = true;
        }
        
        filter.setSearchString(inputFieldValue);
        
        saveButton.setVisible(dirty && !filter.getName().equals(DEFAULT_FILTER_NAME));
        
        performSearch(success, failure);
    }
    
    private void updateView() {

    	SearchType searchTypeAnnot = searchConfig.getSearchClass().getAnnotation(SearchType.class);
        typeCriteriaButton.setText("Type: " + searchTypeAnnot.label());
        
        configPanel.setTitle(filter.getName());
        
        // Update filters
        configPanel.removeAllConfigComponents();
        configPanel.addConfigComponent(typeCriteriaButton);
        configPanel.addConfigComponent(inputField);
        
        for(DomainObjectAttribute attr : searchConfig.getDomainObjectAttributes()) {
            if (attr.getFacetKey()!=null) {
                log.debug("Adding facet: {}",attr.getLabel());
                StringBuilder label = new StringBuilder();
                label.append(attr.getLabel());
                List<String> values = new ArrayList<>(getSelectedFacetValues(attr.getName()));
                Collections.sort(values);
                if (!values.isEmpty()) {
                    label.append(" (");
                    label.append(StringUtils.getCommaDelimited(values, MAX_VALUES_STRING_LENGTH));
                    label.append(")");
                }
                DropDownButton facetButton = new DropDownButton(label.toString());
                populateFacetMenu(attr, facetButton.getPopupMenu());
                configPanel.addConfigComponent(facetButton);
            }
        }

        if (filter.hasCriteria()) {
            for (Criteria criteria : filter.getCriteriaList()) {
                if (criteria instanceof AttributeCriteria) {
                    DropDownButton customCriteriaButton = createCustomCriteriaButton((AttributeCriteria)criteria);
                    if (customCriteriaButton!=null) {
                    	configPanel.addConfigComponent(customCriteriaButton);
                    }
                }
            }
        }

        JPopupMenu addCriteriaPopupMenu = addCriteriaButton.getPopupMenu();
        addCriteriaPopupMenu.removeAll();

        for (final DomainObjectAttribute attr : searchConfig.getDomainObjectAttributes()) {

            boolean found = false;
            if (filter.hasCriteria()) {
                for (Criteria criteria : filter.getCriteriaList()) {
                    if (criteria instanceof AttributeCriteria) {
                        AttributeCriteria ac = (AttributeCriteria)criteria;
                        if (ac.getAttributeName().equals(attr.getName())) {
                            found = true;
                        }
                    }
                }
            }
            
            if (found) continue;
            
            JMenuItem menuItem = new JMenuItem(attr.getLabel());
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    
                    AttributeCriteria criteria;
                    Class<?> attrClass = attr.getGetter().getReturnType();
                    if (attrClass.equals(Date.class)) {
                        criteria = new DateRangeCriteria();
                    }
                    else {
                        criteria = new AttributeValueCriteria();
                    }
                    
                    criteria.setAttributeName(attr.getName());
                    EditCriteriaDialog dialog = new EditCriteriaDialog();
                    criteria = (AttributeCriteria)dialog.showForCriteria(criteria, attr.getLabel());
                    
                    if (criteria!=null) {
                        filter.addCriteria(criteria);
                        dirty = true;
                        refreshSearchResults();
                    }
                }
            });
            addCriteriaPopupMenu.add(menuItem);
        }
        
        configPanel.addConfigComponent(addCriteriaButton);
        configPanel.updateUI();
    }
    
    private DropDownButton createCustomCriteriaButton(final AttributeCriteria criteria) {
        
        String label = null;
        final DomainObjectAttribute attr = searchConfig.getDomainObjectAttribute(criteria.getAttributeName());
        
        if (criteria instanceof AttributeValueCriteria) {
            AttributeValueCriteria avc = (AttributeValueCriteria)criteria;
            label = attr.getLabel()+": "+avc.getValue();
        }
        else if (criteria instanceof DateRangeCriteria) {
            DateRangeCriteria drc = (DateRangeCriteria)criteria;
            label = attr.getLabel()+": "+DATE_FORMAT.format(drc.getStartDate())+" - "+DATE_FORMAT.format(drc.getEndDate());
        }
        else {
            return null;
        }
        
        DropDownButton facetButton = new DropDownButton(label);

        JPopupMenu popupMenu = facetButton.getPopupMenu();
        popupMenu.removeAll();
        
        final JMenuItem editMenuItem = new JMenuItem("Edit");
        editMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EditCriteriaDialog dialog = new EditCriteriaDialog();
                if (dialog.showForCriteria(criteria, attr.getLabel())!=null) {
                    dirty = true;
                    refreshSearchResults();
                }
            }
        });
        popupMenu.add(editMenuItem);
        
        final JMenuItem removeMenuItem = new JMenuItem("Remove");
        removeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeAttributeCriteria(criteria.getAttributeName());
                dirty = true;
                refreshSearchResults();
            }
        });
        popupMenu.add(removeMenuItem);
        
        return facetButton;
    }
    
    private void populateFacetMenu(final DomainObjectAttribute attr, JPopupMenu popupMenu) {

        Set<String> selectedValues = getSelectedFacetValues(attr.getName());

        if (!selectedValues.isEmpty()) {
            final JMenuItem menuItem = new JMenuItem("Clear selected");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateFacet(attr.getName(), null, false);
                    dirty = true;
                    refreshSearchResults();
                }
            });
            popupMenu.add(menuItem);
        }
        
        Collection<FacetValue> attrFacetValues = searchConfig.getFacetValues(attr.getFacetKey());
                
        if (attrFacetValues!=null) {
            for (final FacetValue facetValue : attrFacetValues) {
                boolean selected = selectedValues.contains(facetValue.getValue());
                if (facetValue.getCount()==0 && !selected) {
                    // Skip anything that is not selected, and which doesn't have results. Clicking it would be futile.
                    continue;
                }
                String label = facetValue.getValue()+" ("+facetValue.getCount()+")";
                final JMenuItem menuItem = new JCheckBoxMenuItem(label, selected);
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (menuItem.isSelected()) {
                            updateFacet(attr.getName(), facetValue.getValue(), true);
                        }
                        else {
                            updateFacet(attr.getName(), facetValue.getValue(), false);
                        }
                        dirty = true;
                        refreshSearchResults(null, null);
                    }
                });
                popupMenu.add(menuItem);
            }
        }
    }

    @Override
    public void setSortField(String sortCriteria) {
        this.filter.setSort(sortCriteria);
    }

    @Override
    public void search() {
        refreshSearchResults();
    }

    @Override
    public void export() {

        SimpleWorker worker = new SimpleWorker() {
                
            private SearchResults exportSearchResults = searchResults;
                
            @Override
            protected void doStuff() throws Exception {
                if (!searchResults.isAllLoaded()) {
                    // If anything is unloaded, we create a new search that uses a larger page size, in order to batch the export faster.
                    SearchConfiguration exportSearchConfig = new SearchConfiguration(filter, EXPORT_PAGE_SIZE);
                    exportSearchResults = exportSearchConfig.performSearch();
                }
            }

            @Override
            protected void hadSuccess() {
                DomainObjectTableViewer viewer = null;
                if (resultsPanel.getViewer() instanceof DomainObjectTableViewer) {
                    viewer = (DomainObjectTableViewer)resultsPanel.getViewer();
                }
                ExportResultsAction<DomainObject> action = new ExportResultsAction<>(exportSearchResults, viewer);
                action.doAction();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Loading...", ""));
        worker.execute();
    }
        
    public synchronized void performSearch(final Callable<Void> success, final Callable<Void> failure) {

        log.info("Performing search");
        if (searchConfig.getSearchClass()==null) return;
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                searchResults = searchConfig.performSearch();
            }

            @Override
            protected void hadSuccess() {
                try {
                    resultsPanel.showSearchResults(searchResults, true);
                    updateView();
                    ConcurrentUtils.invokeAndHandleExceptions(success);
                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                resultsPanel.showNothing();
                ConcurrentUtils.invokeAndHandleExceptions(failure);
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        resultsPanel.showLoadingIndicator();
        worker.execute();
    }
    
    private void removeAttributeCriteria(String attrName) {
        if (filter.hasCriteria()) {
            for (Iterator<Criteria> i = filter.getCriteriaList().iterator(); i.hasNext(); ) {
                Criteria criteria = i.next();
                if (criteria instanceof AttributeCriteria) {
                    AttributeCriteria ac = (AttributeCriteria) criteria;
                    if (ac.getAttributeName().equals(attrName)) {
                        // Remove facet entirely
                        i.remove();
                    }
                }
            }
        }
    }
    
    private void updateFacet(String attrName, String value, boolean addValue) {
        boolean modified = false;
        // Attempt to modify an existing facet criteria
        if (filter.hasCriteria()) {
            for (Iterator<Criteria> i = filter.getCriteriaList().iterator(); i.hasNext(); ) {
                Criteria criteria = i.next();
                if (criteria instanceof FacetCriteria) {
                    FacetCriteria fc = (FacetCriteria) criteria;
                    if (fc.getAttributeName().equals(attrName)) {
                        if (value==null) {
                            // Remove facet entirely
                            i.remove();
                        }
                        else {
                            modified = true;
                            if (addValue) {
                                fc.getValues().add(value);
                            }
                            else {
                                fc.getValues().remove(value);
                                if (fc.getValues().isEmpty()) {
                                    i.remove();
                                }
                            }
                        }
                    }
                }
            }
        }
        // Facet criteria was not found, create a new one
        if (!modified && addValue) {
            FacetCriteria fc = new FacetCriteria(); 
            fc.setAttributeName(attrName);
            fc.getValues().add(value);
            filter.addCriteria(fc);
        }
    }
    
    private Set<String> getSelectedFacetValues(String attrName) {
        if (filter==null || !filter.hasCriteria()) return new HashSet<>();
        for (Criteria criteria : filter.getCriteriaList()) {
            if (criteria instanceof FacetCriteria) {
                FacetCriteria fc = (FacetCriteria) criteria;
                if (fc.getAttributeName().equals(attrName)) {
                    return fc.getValues();
                }
            }
        }
        return new HashSet<>();
    }
    
    public class MyDropTargetListener implements DropTargetListener {

        public void dragEnter(DropTargetDragEvent dtde) {
        }

        public void dragExit(DropTargetEvent dtde) {
        }

        public void dragOver(DropTargetDragEvent dtde) {
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        public void drop(DropTargetDropEvent dtde) {
            
            Point point = dtde.getLocation();
            if (!configPanel.getBounds().contains(point)) {
                log.warn("Dropped outside of filter panel");
                dtde.rejectDrop();
                dtde.dropComplete(false);
                return;
            }
            
            if (dtde.isDataFlavorSupported(DomainObjectFlavor.SINGLE_FLAVOR)) {
                try {
                    Object transData = dtde.getTransferable().getTransferData(DomainObjectFlavor.SINGLE_FLAVOR);
                    if (transData instanceof DomainObject) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        DomainObject obj = (DomainObject)transData;
                        dropDomainObject(obj);
                        dtde.dropComplete(true);
                    }
                }
                catch (UnsupportedFlavorException e) {
                    log.warn("Flavor not supported",e);
                    dtde.rejectDrop();
                    dtde.dropComplete(true);
                }
                catch (IOException e) {
                    log.warn("Error dropping domain object",e);
                    dtde.rejectDrop();
                    dtde.dropComplete(false);
                }
            }
            else if (dtde.isDataFlavorSupported(ExTransferable.multiFlavor)) {
                // TODO: support multidrop
                log.warn("Multi flavor!");
                dtde.rejectDrop();
                dtde.dropComplete(false);
            }
            else {
                log.warn("Flavor not supported");
                dtde.rejectDrop();
                dtde.dropComplete(false);
            }
        }
    }

    @Subscribe
    public void domainObjectInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.info("total invalidation, reloading...");
            search();
        }
        else {
            for (DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject.getId().equals(filter.getId())) {
                    log.info("filter invalidated, reloading...");
                    Filter updatedFilter = DomainMgr.getDomainMgr().getModel().getDomainObject(Filter.class, filter.getId());
                    loadDomainObject(updatedFilter, false, null);
                    break;
                }
                else if (domainObject.getClass().equals(searchConfig.getSearchClass())) {
                    log.info("some objects of class "+searchConfig.getSearchClass().getSimpleName()+" were invalidated, reloading...");
                    search();
                }
            }
        }
    }
}
