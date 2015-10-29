package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeValueCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.DateRangeCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.SetCriteria;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.solr.SolrUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.gui.listview.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.model.search.ResultPage;
import org.janelia.it.workstation.gui.browser.model.search.SearchResults;
import org.janelia.it.workstation.gui.dialogs.search.CriteriaOperator;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WrapLayout;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.util.datatransfer.ExTransferable;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.javasoft.swing.JYPopupMenu;
import de.javasoft.swing.SimpleDropDownButton;

/**
 * The Filter Editor is the main search GUI in the Workstation. Users can create, save, and load filters 
 * into this panel. The filter is executed every time it changes, and shows results in an embedded 
 * PaginatedResultsPanel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FilterEditorPanel extends JPanel implements DomainObjectSelectionEditor<Filter> {

    private static final Logger log = LoggerFactory.getLogger(FilterEditorPanel.class);
    
    private static final String SOLR_TYPE_FIELD = "type";
    public static final String DEFAULT_FILTER_NAME = "Unsaved Filter";
    public static final Class<?> DEFAULT_SEARCH_CLASS = Sample.class;
    
    // UI Settings
    private static final int MAX_VALUES_STRING_LENGTH = 20;
    private static final Font FILTER_NAME_FONT = new Font("Sans Serif", Font.BOLD, 16);
    private final int pageSize = SearchResults.PAGE_SIZE;
    
    // UI Elements
    private JPanel filterPanel;
    private JLabel filterNameLabel;
    private JButton saveButton;
    private JButton saveAsButton;
    private JPanel criteriaPanel;
    private final PaginatedResultsPanel resultsPanel;
    
    private SimpleDropDownButton typeCriteriaButton;
    private SimpleDropDownButton addCriteriaButton;
    private JComboBox inputField;    
    
    // TODO: Move somewhere and factor out of DomainObjectTableViewer
    protected static final String JANELIA_MODEL_PACKAGE = "org.janelia.it.jacs.model.domain";
    private final Map<String,String> searchTypeToCollection = new HashMap<>();
        
    // Search state
    private Filter filter;    
    private boolean dirty = false;
    
    // Derived from search state (Filter)
    private Class<?> searchClass;
    private List<DomainObjectAttribute> searchAttrs = new ArrayList<>();
    private List<String> facets = new ArrayList<>();
    private Map<String,List<FacetValue>> facetValues = new HashMap<>();
    
    private String displayQueryString;
    private SolrQuery query;
    
    // Results
    private SearchResults searchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    public FilterEditorPanel() {

        this.filterPanel = new JPanel(new WrapLayout(false, FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        this.filterNameLabel = new JLabel("");
        filterNameLabel.setFont(FILTER_NAME_FONT);
        filterPanel.add(filterNameLabel);
        
        this.saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleWorker worker = new SimpleWorker() {
                        
                    @Override
                    protected void doStuff() throws Exception {
                        filter = DomainMgr.getDomainMgr().getModel().save(filter);
                    }

                    @Override
                    protected void hadSuccess() {
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
        filterPanel.add(saveButton);
        
        this.saveAsButton = new JButton("Save As");
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                final String newName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
                        "Filter Name:\n", "Save Filter", JOptionPane.PLAIN_MESSAGE, null, null, filter.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }
                
                final boolean isNewFilter = filter.getId()==null;

                SimpleWorker worker = new SimpleWorker() {
                        
                    @Override
                    protected void doStuff() throws Exception {
                        
                        if (!isNewFilter) {
                            // TODO: This filter is already saved, duplicate it so that we don't overwrite the existing one
                            filter = cloneFilter(filter);
                        }
                                
                        filter.setName(newName);
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        filter = model.save(filter);
                        model.addChild(model.getDefaultWorkspace(), filter);
                    }

                    @Override
                    protected void hadSuccess() {
                        // TODO: this should be event driven, based on events generated by addChild
//                        DomainExplorerTopComponent.getInstance().refresh(new Callable<Void>() {
//                            @Override
//                            public Void call() throws Exception {
//                                DomainExplorerTopComponent.getInstance().selectNodeById(filter.getId());
////                                if (isNewFilter) {
////                                    
////                                }
////                                else {
////                                    updateFilterView();    
////                                }
//                                return null;
//                            }
//                        });
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    DomainExplorerTopComponent.getInstance().selectNodeById(filter.getId());
                                    //DomainExplorerTopComponent.getInstance().selectNodeById(filter.getId());
                                    // TODO: update the browser top component name somehow
                                }
                                catch (Exception e) {
                                    SessionMgr.getSessionMgr().handleException(e);
                                }
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
        filterPanel.add(saveAsButton);
        
        this.criteriaPanel = new JPanel(new WrapLayout(false, FlowLayout.LEFT));
        criteriaPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        
        this.typeCriteriaButton = new SimpleDropDownButton("Type: Sample");
        
        ButtonGroup typeGroup = new ButtonGroup();
        
        Reflections reflections = new Reflections(JANELIA_MODEL_PACKAGE);
        List<Class<?>> searchClasses = new ArrayList<>(reflections.getTypesAnnotatedWith(SearchType.class));
                
        for(Class<?> searchClazz : searchClasses) {
            String searchTypeKey = searchClazz.getAnnotation(SearchType.class).label();
            String collectionName = null;
            Class<?> clazz = searchClazz;
            while (clazz!=null) {
                MongoMapped mongoMapped = clazz.getAnnotation(MongoMapped.class);
                if (mongoMapped!=null) {
                    collectionName = mongoMapped.collectionName();
                    break;
                }
                clazz = clazz.getSuperclass();
            }
            if (collectionName!=null) {
                searchTypeToCollection.put(searchTypeKey, collectionName);
            }
            else {
                log.warn("Cannot find collection name for search type "+searchTypeKey);
            }
        }
        
    	Collections.sort(searchClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final String l1 = o1.getAnnotation(SearchType.class).label();
                final String l2 = o2.getAnnotation(SearchType.class).label();
                return l1.compareTo(l2);
            }
        });        
        
        for (final Class<?> searchClazz : searchClasses) {
            final String label = searchClazz.getAnnotation(SearchType.class).label();
            JMenuItem menuItem = new JRadioButtonMenuItem(label, searchClazz.equals(DEFAULT_SEARCH_CLASS));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dirty = true;
                    setSearchClass(searchClazz);
                }
            });
            typeGroup.add(menuItem);
            typeCriteriaButton.getPopupMenu().add(menuItem);
        }
        
        this.addCriteriaButton = new SimpleDropDownButton("Add Criteria...");
        JYPopupMenu popupMenu = new JYPopupMenu();
        popupMenu.setVisibleElements(10);
        addCriteriaButton.setPopupMenu(popupMenu);
        
        this.inputField = new JComboBox();
        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        inputField.setEditable(true);
        inputField.setToolTipText("Enter search terms...");
        
        AbstractAction mySearchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
        
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true), "enterAction");
        getActionMap().put("enterAction", mySearchAction);
        
        this.resultsPanel = new PaginatedResultsPanel(selectionModel) {
            @Override
            protected ResultPage getPage(SearchResults searchResults, int page) throws Exception {
                ResultPage resultPage = searchResults.getPage(page);
                if (resultPage==null) {
                    resultPage = performSearch(query, page);
                    searchResults.setPage(page, resultPage);
                }
                return resultPage;
            }
        };

        JPanel top = new JPanel(new BorderLayout());
        top.add(filterPanel, BorderLayout.NORTH);
        top.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER);
        top.add(criteriaPanel, BorderLayout.SOUTH);
        
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);

        MyDropTargetListener dtl = new MyDropTargetListener();
        DropTarget dt = new DropTarget(this, dtl);
        dt.setDefaultActions(DnDConstants.ACTION_COPY);
        dt.setActive(true);
    }
    
    public void loadNewFilter() {
        this.filter = new Filter();
        filter.setName(DEFAULT_FILTER_NAME);
        filter.setSearchType(DEFAULT_SEARCH_CLASS.getName());
        loadDomainObject(filter);
    }
    
    @Override
    public void loadDomainObject(Filter filter) {
        this.filter = filter;
        try {
            setSearchClass(Class.forName(filter.getSearchType()));
        }
        catch (ClassNotFoundException e) {
            SessionMgr.getSessionMgr().handleException(e);
            loadNewFilter();
        }
    }
    
    @Override
    public String getName() {
        return "Filter Editor";
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    @Override
    public Object getEventBusListener() {
        return resultsPanel;
    }
    
    private void setSearchClass(Class<?> searchClass) {
        this.searchClass = searchClass;
        
        SearchType searchTypeAnnot = searchClass.getAnnotation(SearchType.class);
        final String label = searchTypeAnnot.label();
        typeCriteriaButton.setText("Type: " + label);

        searchAttrs.clear();
        facets.clear();
        facets.add(SOLR_TYPE_FIELD);
        facetValues.clear();

        Set<String> attrNames = new HashSet<>();
        
        for (Field field : ReflectionUtils.getAllFields(searchClass)) {
            SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot != null) {
                if (searchAttributeAnnot.facet()) {
                    facets.add(searchAttributeAnnot.key());
                }
                try {
                    Method getter = ReflectionHelper.getGetter(searchClass, field.getName());
                    DomainObjectAttribute attr = new DomainObjectAttribute(searchAttributeAnnot.key(), searchAttributeAnnot.label(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), getter);
                    searchAttrs.add(attr);
                    attrNames.add(attr.getName());
                }
                catch (Exception e) {
                    log.warn("Error getting field "+searchClass.getName()+"."+field.getName(), e);
                }
            }
        }
        
        if (filter.hasCriteria()) {
            for(Iterator<Criteria> i=filter.getCriteriaList().iterator(); i.hasNext(); ) {
                Criteria criteria = i.next();
                if (criteria instanceof AttributeCriteria) {
                   AttributeCriteria ac = (AttributeCriteria)criteria;
                   if (!attrNames.contains(ac.getAttributeName())) {
                       i.remove();
                   }
                }
            }
        }
        
        Collections.sort(searchAttrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });

        populateAddFilterMenu(addCriteriaButton.getPopupMenu());
        refresh();
    }

    private void populateAddFilterMenu(JPopupMenu popupMenu) {
    
        popupMenu.removeAll();

        for (DomainObjectAttribute attr : searchAttrs) {
            JMenuItem menuItem = new JMenuItem(attr.getLabel());
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // TODO: Add filter on key
                    refresh();
                }
            });
            popupMenu.add(menuItem);
        }
    }

    private void refresh() {
        log.trace("refresh");
        
        String inputFieldValue = getInputFieldValue();
        
        if (filter.getSearchString()!=null && !filter.getSearchString().equals(inputFieldValue)) {
            dirty = true;
        }
        
        filter.setSearchString(inputFieldValue);
        
        saveButton.setVisible(dirty && !filter.getName().equals(DEFAULT_FILTER_NAME));
        
        performSearch(0, true);
    }
    
    private void updateFilterView() {
        
        filterNameLabel.setText(filter.getName());
        
        // Update filters
        criteriaPanel.removeAll();
        
        criteriaPanel.add(typeCriteriaButton);
        
        criteriaPanel.add(inputField);
        if (filter.getCriteriaList()!=null) {
            for (Criteria criteria : filter.getCriteriaList()) {
                if (criteria instanceof FacetCriteria) {
                    // Ignore
                }
                else {
                    // TODO: implement custom criteria
//                    criteriaPanel.add(createCustomCriteriaButton(criteria));
                }
            }
        }
        
        for(DomainObjectAttribute attr : searchAttrs) {
            if (attr.isFacet()) {
                StringBuilder label = new StringBuilder();
                label.append(attr.getLabel());
                List<String> values = new ArrayList<>(getSelectedFacetValues(attr.getName()));
                Collections.sort(values);
                if (!values.isEmpty()) {
                    label.append(" (");
                    label.append(StringUtils.getCommaDelimited(values, MAX_VALUES_STRING_LENGTH));
                    label.append(")");
                }
                SimpleDropDownButton facetButton = new SimpleDropDownButton(label.toString());
                populateFacetMenu(attr, facetButton.getPopupMenu());
                criteriaPanel.add(facetButton);
            }
        }
        
        criteriaPanel.add(addCriteriaButton);
        criteriaPanel.revalidate();
    }
    
    private void populateFacetMenu(final DomainObjectAttribute attr, JPopupMenu popupMenu) {

        popupMenu.removeAll();

        Set<String> selectedValues = getSelectedFacetValues(attr.getName());

        if (!selectedValues.isEmpty()) {
            final JMenuItem menuItem = new JMenuItem("Clear selected");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateFacet(attr.getName(), null, false);
                    dirty = true;
                    refresh();
                }
            });
            popupMenu.add(menuItem);
        }
        
        Collection<FacetValue> attrFacetValues = facetValues.get(attr.getName());
                
        if (attrFacetValues!=null) {
            for (final FacetValue facetValue : attrFacetValues) {
                String label = facetValue.getValue()+" ("+facetValue.getCount()+")";
                final JMenuItem menuItem = new JCheckBoxMenuItem(label, false);
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (menuItem.isSelected()) {
                            updateFacet(attr.getName(), facetValue.getValue(), true);
                        }
                        else {
                            updateFacet(attr.getName(), facetValue.getValue(), false);
                        }
                        dirty = true;
                        refresh();
                    }
                });
                menuItem.setSelected(selectedValues.contains(facetValue.getValue()));
                popupMenu.add(menuItem);
            }
        }
    }
    
    public String getInputFieldValue() {
        return (String)inputField.getSelectedItem();
    }
    
    public boolean isInFilterPanel(Point p) {
        return criteriaPanel.getBounds().contains(p);
    }

    public void dropDomainObject(DomainObject obj) {

        Reference reference = new Reference("objectSet", obj.getId());

        SetCriteria criteria = new SetCriteria();
        criteria.setSetName(obj.getName());
        criteria.setSetReference(reference);
        filter.addCriteria(criteria);

        dirty = true;   
        refresh();
    }

    /**
     * Returns a query builder for the current search parameters.
     *
     * @return
     */
    public SolrQueryBuilder getQueryBuilder() {

        SolrQueryBuilder builder = new SolrQueryBuilder();
        
        for (String subjectKey : SessionMgr.getSubjectKeys()) {
            log.trace("Adding query owner key: {}",subjectKey);
            builder.addOwnerKey(subjectKey);
        }
        
        if (filter.getSearchString()!=null) {
            log.info("Setting query string: {}",filter.getSearchString());
            builder.setSearchString(filter.getSearchString());
        }

        StringBuilder aux = new StringBuilder();
        StringBuilder auxAnnot = new StringBuilder();

        final Map<String, Set<String>> filters = new HashMap<>();
        SearchType searchTypeAnnot = searchClass.getAnnotation(SearchType.class);
        String searchType = searchTypeAnnot.label();
        filters.put(SOLR_TYPE_FIELD,getSingleItemMap(searchType));
        
        List<Criteria> criteriaList = filter.getCriteriaList();
        if (criteriaList!=null) {
            for (Criteria criteria : criteriaList) {
                if (criteria instanceof FacetCriteria) {
                    FacetCriteria fc = (FacetCriteria) criteria;
                    filters.put(fc.getAttributeName(), fc.getValues());
                }
                else if (criteria instanceof AttributeCriteria) {
                    AttributeCriteria ac = (AttributeCriteria) criteria;
                    CriteriaOperator operator = CriteriaOperator.CONTAINS;
                    String value1 = null;
                    String value2 = null;

                    if (criteria instanceof DateRangeCriteria) {
                        DateRangeCriteria drc = (DateRangeCriteria) criteria;
                        Date startCal = drc.getStartDate();
                        Date endCal = drc.getEndDate();
                        value1 = startCal == null ? "*" : SolrUtils.formatDate(startCal);
                        value2 = endCal == null ? "*" : SolrUtils.formatDate(endCal);
                    }
                    else if (criteria instanceof AttributeValueCriteria) {
                        AttributeValueCriteria avc = (AttributeValueCriteria) criteria;
                        value1 = avc.getValue();
                    }
                    else {
                        log.warn("Unsupported criteria type: {}",criteria.getClass().getName());
                    }
                    
                    if (value1 == null && value2 == null) {
                        continue;
                    }

                    if ("annotations".equals(ac.getAttributeName())) {
                        if (auxAnnot.length()>1) {
                            auxAnnot.append(" ");
                        }
                        switch (operator) {
                            case NOT_NULL:
                                auxAnnot.append("*");
                                break;
                            default:
                                auxAnnot.append(value1);
                                break;
                        }
                        continue;
                    }

                    if (aux.length() > 0) {
                        aux.append(" ");
                    }
                    aux.append("+");
                    aux.append(ac.getAttributeName());
                    aux.append(":");

                    switch (operator) {
                        case CONTAINS:
                            if (criteria instanceof DateRangeCriteria) {
                                aux.append("[");
                                aux.append(value1);
                                aux.append(" TO ");
                                aux.append(value1);
                                aux.append("+1DAY]");
                            }
                            else {
                                aux.append(value1);
                            }
                            break;
                        case BETWEEN:
                            aux.append("[");
                            aux.append(value1);
                            aux.append(" TO ");
                            aux.append(value2);
                            aux.append("]");
                            break;
                        case NOT_NULL:
                            aux.append("*");
                            break;
                    }

                }
                else if (criteria instanceof SetCriteria) {
                    SetCriteria sc = (SetCriteria) criteria;
                    Reference ref = sc.getSetReference();
                    log.info("Setting query root: {}",ref.getTargetId());
                    builder.setRootId(ref.getTargetId());
                }
            }
        }
        
        if (aux.length()>0) {
            log.info("Adding aux query string: {}",aux);
            builder.setAuxString(aux.toString());
        }
        
        if (auxAnnot.length()>0) {
            log.info("Adding aux annotation query string: {}",auxAnnot);
            builder.setAuxAnnotationQueryString(auxAnnot.toString());
        }
        
        log.info("Adding facets: {}",facets);
        builder.getFacets().addAll(facets);
        
        log.info("Adding facet filters: {}",filters);
        builder.getFilters().putAll(filters);

        String sortCriteria = filter.getSort();
        if (!StringUtils.isEmpty(sortCriteria)) {
            log.info("Setting sort: {}",sortCriteria);
            String sortAttr = (sortCriteria.startsWith("-")||sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            builder.setSortField(sortAttr);
            builder.setAscending(!sortCriteria.startsWith("-"));
        }
        
        return builder;
    }
            
    public synchronized void performSearch(final int pageNum, final boolean showLoading) {

        log.debug("performSearch(pageNum={},showLoading={})", pageNum, showLoading);

        final SolrQueryBuilder builder = getQueryBuilder();
        
        
        // If we don't have a query at this point, just give up
        if (!builder.hasQuery()) {
            return;
        }

        // We don't want to display all the system level query parameters, so build a simplified version of 
        // the query string for display purposes. 
        StringBuilder qs = new StringBuilder();
        if (builder.getAuxString()!=null) {
            qs.append(builder.getAuxString());
        }
        if (qs.length()>0) {
            qs.append(" ");
        }
        if (builder.getSearchString()!=null) {
            qs.append(builder.getSearchString());
        }
        this.displayQueryString = qs.toString();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                query = builder.getQuery();
                ResultPage firstPage = performSearch(query, pageNum);
                searchResults = new SearchResults(firstPage);
                log.debug("Got {} results", firstPage.getNumPageResults());
            }

            @Override
            protected void hadSuccess() {
                try {
                    resultsPanel.showSearchResults(searchResults);
                    updateFilterView();
                    if (showLoading) {
                        resultsPanel.showResultsView();
                    }
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                if (showLoading) {
                    resultsPanel.showNothing();
                }
            }
        };

        if (showLoading) {
            resultsPanel.showLoadingIndicator();
        }
        worker.execute();
    }

    public ResultPage performSearch(SolrQuery query, int page) throws Exception {
        
        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("DomainFilterEditorPanel.performSearch called in the EDT");
        }
        
        query.setStart(pageSize*page);
        query.setRows(pageSize);
        
        SolrResults solrResults = ModelMgr.getModelMgr().searchSolr(query, false);
        QueryResponse qr = solrResults.getResponse();
        
        List<Long> ids = new ArrayList<>();
        List<Reference> refs = new ArrayList<>();
        for(SolrDocument doc : qr.getResults()) {
            Long id = new Long(doc.get("id").toString());
            String type = (String)doc.getFieldValue(SOLR_TYPE_FIELD);
            String collectionName = searchTypeToCollection.get(type);
            if (collectionName!=null) {
                refs.add(new Reference(collectionName, id));
                ids.add(id);
            }
            else {
                log.warn("Unknown type has no collection mapping: "+type);
            }
        }
        
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> domainObjects = model.getDomainObjectsByReference(refs);
        List<Annotation> annotations = model.getAnnotations(ids);
        
        int numFound = (int)qr.getResults().getNumFound();
        
        log.info("Search found {} objects",numFound);
        log.info("Page contains {} objects and {} annotations",domainObjects.size(),annotations.size());
        
        facetValues.clear();
        if (qr.getFacetFields()!=null) {
            for (final FacetField ff : qr.getFacetFields()) {
                log.debug("Facet {}",ff.getName());
                List<FacetValue> favetValues = new ArrayList<>();
                if (ff.getValues()!=null) {
                    for (final FacetField.Count count : ff.getValues()) {
                        favetValues.add(new FacetValue(count.getName(),count.getCount()));
                        log.debug("  Value: {} (count={})",count.getName(),count.getCount());
                    }
                }
                facetValues.put(ff.getName(), favetValues);
            }
        }
        
        return new ResultPage(domainObjects, annotations, numFound);
    }
    
    public JPanel getResultsPanel() {
        return resultsPanel;
    }
    
    private void updateFacet(String attrName, String value, boolean add) {
        List<Criteria> criteriaList = filter.getCriteriaList();
        boolean modified = false;
        if (criteriaList != null) {
            for (Iterator<Criteria> i = criteriaList.iterator(); i.hasNext(); ) {
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
                            if (add) {
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
        if (!modified && add) {
            FacetCriteria fc = new FacetCriteria(); 
            fc.setAttributeName(attrName);
            fc.getValues().add(value);
            filter.addCriteria(fc);
        }
    }
    
    private Set<String> getSelectedFacetValues(String attrName) {
        if (filter==null || filter.getCriteriaList()==null) return new HashSet<>();
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

    private class FacetValue {
        
        private final String value;
        private final long count;

        public FacetValue(String value, long count) {
            this.value = value;
            this.count = count;
        }

        public String getValue() {
            return value;
        }

        public long getCount() {
            return count;
        }
    }

    private Set<String> getSingleItemMap(String searchType) {
        Set<String> types = new HashSet<>();
        types.add(searchType);
        return types;
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
            if (!isInFilterPanel(point)) {
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

    /**
     * There are better ways of deep cloning, but this is easier for now. 
     */
    private Filter cloneFilter(Filter filter) {
        Filter newFilter = new Filter();
        newFilter.setName(filter.getName());
        newFilter.setSearchString(filter.getSearchString());
        newFilter.setSearchType(filter.getSearchType());
        newFilter.setSort(filter.getSort());
        if (filter.hasCriteria()) {
            for(Criteria criteria : filter.getCriteriaList()) {
                newFilter.addCriteria(cloneCriteria(criteria));
            }
        }
        
        return newFilter;
    }
    
    private Criteria cloneCriteria(Criteria criteria) {
        if (criteria instanceof AttributeValueCriteria) {
            AttributeValueCriteria source = (AttributeValueCriteria)criteria;
            AttributeValueCriteria newCriteria = new AttributeValueCriteria();
            newCriteria.setAttributeName(source.getAttributeName());
            newCriteria.setValue(source.getValue());
            return newCriteria;
        }
        else if (criteria instanceof DateRangeCriteria) {
            DateRangeCriteria source = (DateRangeCriteria)criteria;
            DateRangeCriteria newCriteria = new DateRangeCriteria();
            newCriteria.setAttributeName(source.getAttributeName());
            newCriteria.setStartDate(source.getStartDate());
            newCriteria.setEndDate(source.getEndDate());
            return newCriteria;
        }
        else if (criteria instanceof FacetCriteria) {
            FacetCriteria source = (FacetCriteria)criteria;
            FacetCriteria newCriteria = new FacetCriteria();
            newCriteria.setAttributeName(source.getAttributeName());
            newCriteria.setValues(new HashSet<>(source.getValues()));
            return newCriteria;
        }
        else if (criteria instanceof SetCriteria) {
            SetCriteria source = (SetCriteria)criteria;
            SetCriteria newCriteria = new SetCriteria();
            newCriteria.setSetName(source.getSetName());
            Reference setReference = new Reference(source.getSetReference().getTargetType(), source.getSetReference().getTargetId());
            newCriteria.setSetReference(setReference);
            return newCriteria;
        }
        else {
            throw new IllegalArgumentException("Unknown criteria subtype: "+criteria.getClass().getName());
        }
    }

}
