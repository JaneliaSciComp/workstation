package org.janelia.it.workstation.gui.browser.components.editor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.SetCriteria;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.jacs.shared.solr.SolrQueryBuilder;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.viewer.PaginatedResultsPanel;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.search.ResultPage;
import org.janelia.it.workstation.gui.browser.search.SearchResults;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WrapLayout;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.javasoft.swing.SimpleDropDownButton;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainFilterEditorPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DomainFilterEditorPanel.class);
    
    // UI Settings
    private static final String DEFAULT_FILTER_NAME = "Unsaved Filter";
    private static final Font FILTER_NAME_FONT = new Font("Sans Serif", Font.BOLD, 16);
    private static final Class<?> DEFAULT_SEARCH_CLASS = Sample.class;
    
    // UI Elements
    private JPanel filterPanel;
    private JLabel filterNameLabel;
    private JButton renameButton;
    private JPanel criteriaPanel;
    private final PaginatedResultsPanel resultsPanel;
    
    private SimpleDropDownButton typeCriteriaButton;
    private SimpleDropDownButton addFilterButton;
    private JComboBox inputField;    
    
    // Search state
    private Filter filter;
    
    private Class<?> searchClass;
    private List<DomainObjectAttribute> searchAttrs = new ArrayList<>();
    private List<String> facets = new ArrayList<>();
    
    private String displayQueryString;
    private final int pageSize = SearchResults.PAGE_SIZE;
    private SolrQuery query;
    
    // Results
    private SearchResults searchResults;
    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    
    // TODO: Move somewhere and factor out of DomainObjectTableViewer
    protected static final String JANELIA_MODEL_PACKAGE = "org.janelia.it.jacs.model.domain";

    private Map<String,List<FacetValue>> facetValues = new HashMap<>();
    
    
    public DomainFilterEditorPanel() {

        this.filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                
        this.filterNameLabel = new JLabel("");
        filterNameLabel.setFont(FILTER_NAME_FONT);
        filterPanel.add(filterNameLabel);
        
        this.renameButton = new JButton("Save As");
        renameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
                        "Filter Name:\n", "Save Filter", JOptionPane.PLAIN_MESSAGE, null, null, filter.getName());
                if ((newName == null) || (newName.length() <= 0)) {
                    return;
                }
                // TODO: save it
            }
        });
        filterPanel.add(renameButton);
        
        this.criteriaPanel = new JPanel(new WrapLayout(true, FlowLayout.LEFT));
        criteriaPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        
        this.typeCriteriaButton = new SimpleDropDownButton("Type: Sample");
        
        ButtonGroup typeGroup = new ButtonGroup();
        
        Reflections reflections = new Reflections(JANELIA_MODEL_PACKAGE);
        Set<Class<?>> searchClasses = reflections.getTypesAnnotatedWith(SearchType.class);
    	        
        for (final Class<?> searchClazz : searchClasses) {
            final String label = searchClazz.getAnnotation(SearchType.class).label();
            JMenuItem menuItem = new JRadioButtonMenuItem(label, searchClazz.equals(DEFAULT_SEARCH_CLASS));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setSearchClass(searchClazz);
                }
            });
            typeGroup.add(menuItem);
            typeCriteriaButton.getPopupMenu().add(menuItem);
        }
        
        this.addFilterButton = new SimpleDropDownButton("Add Filter...");

        this.inputField = new JComboBox();
        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        inputField.setEditable(true);
        inputField.setToolTipText("Enter search terms...");
        
        AbstractAction mySearchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateView();
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

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(filterPanel);
        add(new JSeparator(JSeparator.HORIZONTAL));
        add(criteriaPanel);
        add(resultsPanel);

        loadNewFilter();
    }
    
    private void loadNewFilter() {
        this.filter = new Filter();
        filter.setName(DEFAULT_FILTER_NAME);
        filter.setSearchType(DEFAULT_SEARCH_CLASS.getName());
        setSearchClass(DEFAULT_SEARCH_CLASS);
    }
    
    public void loadFilter(Filter filter) {
        this.filter = filter;
        try {
            setSearchClass(Class.forName(filter.getSearchType()));
        }
        catch (ClassNotFoundException e) {
            loadNewFilter();
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    private void setSearchClass(Class<?> searchClass) {
        this.searchClass = searchClass;
        
        SearchType searchTypeAnnot = searchClass.getAnnotation(SearchType.class);
        final String label = searchTypeAnnot.label();
        typeCriteriaButton.setText("Type: " + label);

        searchAttrs.clear();
        facets.clear();

        for (Field field : ReflectionUtils.getAllFields(searchClass)) {
            SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot != null) {
                if (searchAttributeAnnot.facet()) {
                    facets.add(searchAttributeAnnot.key());
                }
                try {
                    Method getter = ReflectionHelper.getGetter(searchClass, field.getName());
                    DomainObjectAttribute attr = new DomainObjectAttribute(searchAttributeAnnot.key(), searchAttributeAnnot.label(), searchAttributeAnnot.facet(), getter);
                    searchAttrs.add(attr);
                }
                catch (Exception e) {
                    log.warn("Error getting field "+searchClass.getName()+"."+field.getName(), e);
                }
            }
        }
        
        Collections.sort(searchAttrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });

        populateAddFilterMenu(addFilterButton.getPopupMenu());
        
        updateView();
    }

    private void populateAddFilterMenu(JPopupMenu popupMenu) {
    
        popupMenu.removeAll();

        for (DomainObjectAttribute attr : searchAttrs) {
            JMenuItem menuItem = new JMenuItem(attr.getLabel());
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // TODO: Add filter on key
                    updateView();
                }
            });
            popupMenu.add(menuItem);
        }
    }
    
    private void updateView() {
        filter.setSearchString(getInputFieldValue());
//        updateFilterView();
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
//                if (filter instanceof FullTextFilter) {
//                    // Ignore
//                }
//                else {
//                    criteriaPanel.add(createFilterLabel(filter));
//                }
            }
        }
        
        for(DomainObjectAttribute attr : searchAttrs) {
            if (attr.isFacet()) {
                // TODO: create button name with selected values, like Jira has
                SimpleDropDownButton facetButton = new SimpleDropDownButton(attr.getLabel());
                populateFacetMenu(attr, facetButton.getPopupMenu());
                criteriaPanel.add(facetButton);
                
            }
        }
        
        criteriaPanel.add(addFilterButton);
        criteriaPanel.revalidate();
    }
    
    private void populateFacetMenu(DomainObjectAttribute attr, JPopupMenu popupMenu) {

        popupMenu.removeAll();

        for (final FacetValue facetValue : facetValues.get(attr.getName())) {
            String label = facetValue.getValue()+" ("+facetValue.getCount()+")";
            JMenuItem menuItem = new JCheckBoxMenuItem(label, false);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    
                }
            });
            popupMenu.add(menuItem);
        }
    }


//    protected void populateFacets(ResultPage resultPage) {
//
//        facetsPanel.removeAll();
//
//        SolrResults pageResults = resultPage.getSolrResults();
//        if (pageResults == null) {
//            return;
//        }
//
//        QueryResponse qr = pageResults.getResponse();
//        for (final FacetField ff : qr.getFacetFields()) {
//
//            JPanel facetPanel = new JPanel();
//            facetPanel.setOpaque(false);
//            facetPanel.setLayout(new BoxLayout(facetPanel, BoxLayout.PAGE_AXIS));
//
//            JLabel facetLabel = new JLabel(getFieldLabel(ff.getName()));
//            facetLabel.setFont(groupFont);
//            facetPanel.add(facetLabel);
//
//            Set<String> selectedValues = filters.get(ff.getName());
//            List<Count> counts = ff.getValues();
//            if (counts == null) {
//                continue;
//            }
//
//            for (final Count count : ff.getValues()) {
//
//                if (count==null) {
//                    log.warn("Got null count value for facet field "+ff.getName());
//                    continue;
//                }
//                final SearchAttribute attr = searchConfig.getAttributeByName(ff.getName());
//                final String name = attr == null ? null : attr.getName();
//                final String label = searchConfig.getFormattedFieldValue(count.getName(), name) + " (" + count.getCount() + ")";
//
//                final JCheckBox checkBox = new JCheckBox(new AbstractAction(label) {
//                    public void actionPerformed(ActionEvent e) {
//                        JCheckBox cb = (JCheckBox) e.getSource();
//                        Set<String> values = filters.get(ff.getName());
//                        if (values == null) {
//                            values = new HashSet<String>();
//                            filters.put(ff.getName(), values);
//                        }
//                        if (cb.isSelected()) {
//                            values.add(count.getName());
//                        }
//                        else {
//                            values.remove(count.getName());
//                        }
//                        performSearch(false, true, true);
//                    }
//                });
//
//                checkBox.setSelected(selectedValues != null && selectedValues.contains(count.getName()));
//                checkBox.setFont(checkboxFont);
//                facetPanel.add(checkBox);
//            }
//
//            facetsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
//            facetsPanel.add(facetPanel);
//        }
//
//        facetsPanel.revalidate();
//        facetsPanel.repaint();
//    }
    
    public String getInputFieldValue() {
        return (String)inputField.getSelectedItem();
    }
//    
//    private JComponent createFilterLabel(final Filter filter) {
//        RoundedPanel filterLabel = new RoundedPanel() {
//            @Override
//            protected void showPopupMenu(MouseEvent e) {
//                final RoundedPanel thisPanel = this;
//
//                JPopupMenu popupMenu = new JPopupMenu();
//                popupMenu.setLightWeightPopupEnabled(true);
//
//                JMenuItem editItem = new JMenuItem("Filter...");
//                editItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                        EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
//                        updateView();
//                    }
//                });
//                popupMenu.add(editItem);
//
//                popupMenu.addSeparator();
//
//                JMenuItem removeItem = new JMenuItem("Remove");
//                removeItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                        savedSearch.removeFilter(filter);
//                        criteriaPanel.remove(thisPanel);
//                        criteriaPanel.revalidate();
//                    }
//                });
//                popupMenu.add(removeItem);
//
//                popupMenu.show(e.getComponent(), e.getX(), e.getY());
//                e.consume();
//            }
//
//            @Override
//            protected void doubleClicked(MouseEvent e) {
//                EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
//                updateView();
//            }
//
//            @Override
//            protected String getLabel() {
//                return filter.getLabel();
//            }
//        };
//        filterLabel.setBorder(PADDING_BORDER);
//        return filterLabel;
//    }

    public boolean isInFilterPanel(Point p) {
        return criteriaPanel.getBounds().contains(p);
    }

    public void dropDomainObject(DomainObject obj) {

        Reference reference = new Reference("objectSet", obj.getId());

        SetCriteria criteria = new SetCriteria();
        criteria.setSetName(obj.getName());
        criteria.setSetReference(reference);
        filter.addCriteria(criteria);

        updateView();
    }

    /**
     * Returns a query builder for the current search parameters.
     *
     * @return
     */
    public SolrQueryBuilder getQueryBuilder() {

        SolrQueryBuilder builder = new SolrQueryBuilder();
        
        for (String subjectKey : SessionMgr.getSubjectKeys()) {
            builder.addOwnerKey(subjectKey);
        }
        
        builder.setSearchString(filter.getSearchString());

        List<Criteria> criteriaList = filter.getCriteriaList();
        if (criteriaList!=null) {
            for (Criteria criteria : criteriaList) {
                if (criteria instanceof SetCriteria) {
                    SetCriteria setCriteria = (SetCriteria) criteria;
                    Reference ref = setCriteria.getSetReference();
                    builder.setRootId(ref.getTargetId());
                    break;
                }
            }
        }
        
        StringBuilder aux = new StringBuilder();

        // TODO: make this user definable
        SearchType searchTypeAnnot = searchClass.getAnnotation(SearchType.class);
        String searchType = searchTypeAnnot.key();    
        aux.append("+entity_type:").append(searchType).append(" ");
        
        if (criteriaList!=null) {
            for (Criteria criteria : criteriaList) {
//                if (criteria instanceof AttributeCriteria) {
//
//                    // TODO: implement AttributeFilters
//                    String value1 = null;
//                    String value2 = null;
//
//                    SearchAttribute sa = criteria.getAttribute();
//                    if (sa == null) {
//                        continue;
//                    }
//
//                    if (sa.getDataType().equals(SearchAttribute.DataType.DATE)) {
//                        Calendar startCal = (Calendar) criteria.getValue1();
//                        Calendar endCal = (Calendar) criteria.getValue2();
//                        value1 = startCal == null ? "*" : SolrUtils.formatDate(startCal.getTime());
//                        value2 = endCal == null ? "*" : SolrUtils.formatDate(endCal.getTime());
//                    }
//                    else {
//                        value1 = (String) criteria.getValue1();
//                        value2 = (String) criteria.getValue2();
//                    }
//
//                    if (value1 == null && value2 == null) {
//                        continue;
//                    }
//
//                    if (aux.length() > 0) {
//                        aux.append(" ");
//                    }
//                    aux.append("+");
//                    aux.append(sa.getName());
//                    aux.append(":");
//
//                    switch (criteria.getOp()) {
//                        case CONTAINS:
//                            aux.append(value1);
//                            break;
//                        case BETWEEN:
//                            aux.append("[");
//                            aux.append(value1);
//                            aux.append(" TO ");
//                            aux.append(value2);
//                            aux.append("]");
//                            break;
//                        case NOT_NULL:
//                            aux.append("*");
//                            break;
//                    }
//                }
            }
        }

        builder.setAuxString(aux.toString());

        return builder;
    }
            
    public synchronized void performSearch(final int pageNum, final boolean showLoading) {

        log.debug("performSearch(pageNum={},showLoading={})", pageNum, showLoading);

        final SolrQueryBuilder builder = getQueryBuilder();
        
        String sortCriteria = filter.getSort();
        if (!StringUtils.isEmpty(sortCriteria)) {
            String sortAttr = (sortCriteria.startsWith("-")||sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            builder.setSortField(sortAttr);
            builder.setAscending(!sortCriteria.startsWith("-"));
        }
        
        // TODO: add UI for facet filters
//        builder.getFilters().putAll(filters);
        builder.getFacets().addAll(facets);
        
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

//                    populateFacets(resultPage);
//                    populateResultView(resultPage);
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
            String type = (String)doc.getFieldValue("entity_type");
            refs.add(new Reference(type, id));
            ids.add(id);
        }
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        List<DomainObject> domainObjects = dao.getDomainObjects(SessionMgr.getSubjectKey(), refs);
        List<Annotation> annotations = dao.getAnnotations(SessionMgr.getSubjectKey(), ids);
        
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
}
