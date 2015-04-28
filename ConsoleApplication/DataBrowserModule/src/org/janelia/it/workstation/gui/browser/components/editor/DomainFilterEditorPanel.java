package org.janelia.it.workstation.gui.browser.components.editor;

import de.javasoft.swing.JYTaskPane;
import de.javasoft.swing.SimpleDropDownButton;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.SavedSearch;
import org.janelia.it.jacs.model.domain.gui.search.filters.AttributeFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;
import org.janelia.it.jacs.model.domain.gui.search.filters.FullTextFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.SetFilter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
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
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainFilterEditorPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DomainFilterEditorPanel.class);
    
    // UI Settings
    private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    // UI Elements
    private JYTaskPane filterTaskPane;
    private JYTaskPane optionsTaskPane;
    private final PaginatedResultsPanel resultsPanel;
    
    // Search state
    private SavedSearch savedSearch;
    private String displayQueryString;
    private final int pageSize = SearchResults.PAGE_SIZE;
    protected SolrQuery query;
    protected SearchResults searchResults;

    private SimpleDropDownButton addFilterButton;

    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    public DomainFilterEditorPanel() {

        this.addFilterButton = new SimpleDropDownButton("Add Filter...");
        addFilterButton.setPopupMenu(getAddFilterMenu());

        this.filterTaskPane = new JYTaskPane();
        filterTaskPane.setTitle("Filters");

        JPanel optionPanel = new JPanel();

        this.optionsTaskPane = new JYTaskPane();
        optionsTaskPane.setTitle("Options");
        optionsTaskPane.add(optionPanel);

        JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();
        taskPaneContainer.add(filterTaskPane);
        taskPaneContainer.add(optionsTaskPane);

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
        
        JPanel searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BorderLayout());
        searchResultsPanel.add(resultsPanel, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(taskPaneContainer, BorderLayout.WEST);
        add(searchResultsPanel, BorderLayout.CENTER);

        loadSavedSearch(new SavedSearch());
    }
    
    private JPopupMenu getAddFilterMenu() {

        JPopupMenu addFilterMenu = new JPopupMenu();
        addFilterMenu.setLightWeightPopupEnabled(true);

        JMenuItem fullTextItem = new JMenuItem("Full Text Filter");
        fullTextItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Filter filter = new FullTextFilter();
                EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
                updateView();
            }
        });
        addFilterMenu.add(fullTextItem);

        JMenuItem attributeFilterItem = new JMenuItem("Attribute Filter");
        attributeFilterItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Filter filter = new AttributeFilter();
                EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
                updateView();
            }
        });
        addFilterMenu.add(attributeFilterItem);

        JMenuItem setItem = new JMenuItem("Set Filter");
        setItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                Filter filter = new SetFilter();
                EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
                updateView();
            }
        });
        addFilterMenu.add(setItem);

        return addFilterMenu;
    }

    public final void loadSavedSearch(SavedSearch savedSearch) {
        this.savedSearch = savedSearch;
        updateView();
    }

    private void updateView() {

        log.info("Updating view");

        // Update filters
        filterTaskPane.removeAll();
        if (savedSearch!=null && savedSearch.getFilters()!=null) {
            for (Filter filter : savedSearch.getFilters()) {
                filterTaskPane.add(createFilterLabel(filter));
            }
        }
        filterTaskPane.add(addFilterButton);
        optionsTaskPane.revalidate();

        // Update search
        performSearch(0, true);
    }

    private JComponent createFilterLabel(final Filter filter) {
        RoundedPanel filterLabel = new RoundedPanel() {
            @Override
            protected void showPopupMenu(MouseEvent e) {
                final RoundedPanel thisPanel = this;

                JPopupMenu popupMenu = new JPopupMenu();
                popupMenu.setLightWeightPopupEnabled(true);

                JMenuItem editItem = new JMenuItem("Filter...");
                editItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
                        updateView();
                    }
                });
                popupMenu.add(editItem);

                popupMenu.addSeparator();

                JMenuItem removeItem = new JMenuItem("Remove");
                removeItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        savedSearch.removeFilter(filter);
                        filterTaskPane.remove(thisPanel);
                        filterTaskPane.revalidate();
                    }
                });
                popupMenu.add(removeItem);

                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                e.consume();
            }

            @Override
            protected void doubleClicked(MouseEvent e) {
                EditFilterDialog.getInstanceForFilter(savedSearch, filter).showDialog();
                updateView();
            }

            @Override
            protected String getLabel() {
                return filter.getLabel();
            }
        };
        filterLabel.setBorder(PADDING_BORDER);
        return filterLabel;
    }

    public boolean isInFilterPane(Point p) {
        return filterTaskPane.getBounds().contains(p);
    }

    public void dropDomainObject(DomainObject obj) {

        Reference reference = new Reference("objectSet", obj.getId());

        SetFilter filter = new SetFilter();
        filter.setSetName(obj.getName());
        filter.setSetReference(reference);

        savedSearch.addFilter(filter);

        updateView();
    }

    /**
     * Returns a query builder for the current search parameters.
     *
     * @return
     */
    public SolrQueryBuilder getQueryBuilder() {

        SolrQueryBuilder builder = new SolrQueryBuilder();
        
        List<Filter> filters = savedSearch.getFilters();
        if (filters==null) {
            return builder;
        }

        for (String subjectKey : SessionMgr.getSubjectKeys()) {
            builder.addOwnerKey(subjectKey);
        }

        StringBuilder ss = new StringBuilder();
        for (Filter filter : filters) {
            if (filter instanceof FullTextFilter) {
                String text = ((FullTextFilter) filter).getText();
                if (ss.length()>0) {
                    ss.append(" ");
                }
                ss.append(text);
            }
        }
        builder.setSearchString(ss.toString());

        for (Filter filter : filters) {
            if (filter instanceof SetFilter) {
                SetFilter setFilter = (SetFilter) filter;
                Reference ref = setFilter.getSetReference();
                builder.setRootId(ref.getTargetId());
                break;
            }
        }

        StringBuilder aux = new StringBuilder();

        // TODO: make this user definable
        aux.append("+entity_type:sample ");
        
        for (Filter filter : filters) {
            if (filter instanceof AttributeFilter) {

                // TODO: implement AttributeFilters
//                String value1 = null;
//                String value2 = null;
//
//                SearchAttribute sa = criteria.getAttribute();
//                if (sa == null) {
//                    continue;
//                }
//
//                if (sa.getDataType().equals(SearchAttribute.DataType.DATE)) {
//                    Calendar startCal = (Calendar) criteria.getValue1();
//                    Calendar endCal = (Calendar) criteria.getValue2();
//                    value1 = startCal == null ? "*" : SolrUtils.formatDate(startCal.getTime());
//                    value2 = endCal == null ? "*" : SolrUtils.formatDate(endCal.getTime());
//                }
//                else {
//                    value1 = (String) criteria.getValue1();
//                    value2 = (String) criteria.getValue2();
//                }
//
//                if (value1 == null && value2 == null) {
//                    continue;
//                }
//
//                if (aux.length() > 0) {
//                    aux.append(" ");
//                }
//                aux.append("+");
//                aux.append(sa.getName());
//                aux.append(":");
//
//                switch (criteria.getOp()) {
//                    case CONTAINS:
//                        aux.append(value1);
//                        break;
//                    case BETWEEN:
//                        aux.append("[");
//                        aux.append(value1);
//                        aux.append(" TO ");
//                        aux.append(value2);
//                        aux.append("]");
//                        break;
//                    case NOT_NULL:
//                        aux.append("*");
//                        break;
//                }
            }
        }

        builder.setAuxString(aux.toString());

        return builder;
    }

    public SolrQueryBuilder getQueryBuilder(boolean fetchFacets) {
        SolrQueryBuilder builder = getQueryBuilder();
        
        String sortCriteria = savedSearch.getSortCriteria();
        if (!StringUtils.isEmpty(sortCriteria)) {
            builder.setSortField(sortCriteria.substring(1));
            builder.setAscending(!sortCriteria.startsWith("-"));
        }
        
        // TODO: add UI for facet filters
//        builder.getFilters().putAll(filters);
//        if (fetchFacets) {
//            builder.getFacets().addAll(Arrays.asList(facets));
//        }
        
        return builder;
    }
            
    public synchronized void performSearch(final int pageNum, final boolean showLoading) {

        log.debug("performSearch(pageNum={},showLoading={})", pageNum, showLoading);

        final SolrQueryBuilder builder = getQueryBuilder(true);
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
        
        List<Long> ids = new ArrayList<>();
        List<Reference> refs = new ArrayList<>();
        for(SolrDocument doc : solrResults.getResponse().getResults()) {
            Long id = new Long(doc.get("id").toString());
            String type = (String)doc.getFieldValue("entity_type");
            refs.add(new Reference(type, id));
            ids.add(id);
        }
        
        DomainDAO dao = DomainExplorerTopComponent.getDao();
        List<DomainObject> domainObjects = dao.getDomainObjects(SessionMgr.getSubjectKey(), refs);
        List<Annotation> annotations = dao.getAnnotations(SessionMgr.getSubjectKey(), ids);
        
        int numFound = (int)solrResults.getResponse().getResults().getNumFound();
        
        log.info("Search found {} objects",numFound);
        log.info("Page contains {} objects and {} annotations",domainObjects.size(),annotations.size());
        
        return new ResultPage(domainObjects, annotations, numFound);
    }
}
