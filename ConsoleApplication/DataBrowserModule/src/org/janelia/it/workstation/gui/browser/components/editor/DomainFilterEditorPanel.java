package org.janelia.it.workstation.gui.browser.components.editor;

import de.javasoft.swing.JYTaskPane;
import de.javasoft.swing.SimpleDropDownButton;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.SavedSearch;
import org.janelia.it.jacs.model.domain.gui.search.filters.AttributeFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;
import org.janelia.it.jacs.model.domain.gui.search.filters.FullTextFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.SetFilter;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.icongrid.node.DomainObjectIconGridViewer;
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

    /**
     * How many results to load at a time
     */
    private static final int PAGE_SIZE = 100;
    
    // UI Settings
    private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    // UI Elements
    private JYTaskPane filterTaskPane;
    private JYTaskPane optionsTaskPane;
    private final DomainObjectIconGridViewer iconGridViewer;
    
    // Search state
    private SavedSearch savedSearch;
    private String fullQueryString;
    
    // Results
    protected SearchResults searchResults = new SearchResults();
    

    private enum ViewerType {

        IconViewer("Icon View"),
        TableViewer("Table View"),
        HybridViewer("Hybrid View");
        private String name;

        ViewerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    };

    private ViewerType viewerType;
    private SimpleDropDownButton viewTypeButton;
    private SimpleDropDownButton addFilterButton;

    private void setViewerType(ViewerType viewerType) {
        this.viewerType = viewerType;
        this.viewTypeButton.setText(viewerType.getName());
        // TODO : switch viewer
    }

    public DomainFilterEditorPanel() {

        this.viewTypeButton = new SimpleDropDownButton("Choose Viewer...");
        viewTypeButton.setPopupMenu(getViewerPopupMenu());

        this.addFilterButton = new SimpleDropDownButton("Add Filter...");
        addFilterButton.setPopupMenu(getAddFilterMenu());

        this.filterTaskPane = new JYTaskPane();
        filterTaskPane.setTitle("Filters");

        JPanel optionPanel = new JPanel();
        optionPanel.add(viewTypeButton);

        this.optionsTaskPane = new JYTaskPane();
        optionsTaskPane.setTitle("Options");
        optionsTaskPane.add(optionPanel);

        JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();
        taskPaneContainer.add(filterTaskPane);
        taskPaneContainer.add(optionsTaskPane);

        this.iconGridViewer = new DomainObjectIconGridViewer();

        JPanel searchResults = new JPanel();
        searchResults.setLayout(new BorderLayout());
        searchResults.add(iconGridViewer, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(taskPaneContainer, BorderLayout.WEST);
        add(searchResults, BorderLayout.CENTER);

        setViewerType(ViewerType.IconViewer);

        loadSavedSearch(new SavedSearch());
    }

    private JPopupMenu getViewerPopupMenu() {

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JMenuItem iconViewItem = new JMenuItem("Icon View");
        iconViewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setViewerType(ViewerType.IconViewer);
            }
        });
        popupMenu.add(iconViewItem);

        JMenuItem tableViewItem = new JMenuItem("Table View");
        tableViewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setViewerType(ViewerType.TableViewer);
            }
        });
        popupMenu.add(tableViewItem);

        JMenuItem hybridViewIcon = new JMenuItem("Hybrid View");
        hybridViewIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setViewerType(ViewerType.HybridViewer);
            }
        });
        popupMenu.add(hybridViewIcon);

        return popupMenu;
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
        if (savedSearch.getFilters()!=null) {
            for (Filter filter : savedSearch.getFilters()) {
                filterTaskPane.add(createFilterLabel(filter));
            }
        }
        filterTaskPane.add(addFilterButton);
        optionsTaskPane.revalidate();

        // Update search
//        executeSearch();
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
        if (StringUtils.isEmpty(sortCriteria)) {
            builder.setSortField(sortCriteria.substring(1));
            builder.setAscending(!sortCriteria.startsWith("-"));
        }
        
//        builder.getFilters().putAll(filters);
//        if (fetchFacets) {
//            builder.getFacets().addAll(Arrays.asList(facets));
//        }
        
        return builder;
    }
    
    public synchronized void performSearch(final int pageNum, final boolean showLoading) {
        performSearch(pageNum, showLoading, null);
    }
    
    public synchronized void performSearch(final int pageNum, final boolean showLoading, final Callable<Void> success) {

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
        this.fullQueryString = qs.toString();

        SimpleWorker worker = new SimpleWorker() {

            private ResultPage resultPage;

            @Override
            protected void doStuff() throws Exception {
                resultPage = new ResultPage(performSearch(builder, pageNum, PAGE_SIZE));
                log.debug("Adding result page ({} results)", resultPage.getNumItems());
                searchResults.addPage(resultPage);
//                resultsTable.setMoreResults(searchResults.hasMoreResults());
            }

            @Override
            protected void hadSuccess() {
                try {
                    iconGridViewer.showSearchResults(searchResults);
                    
//                    populateFacets(resultPage);
//                    populateResultView(resultPage);
//                    if (showLoading) {
//                        resultsTable.showTable();
//                    }
//                    ConcurrentUtils.invoke(success);
                    
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                if (showLoading) {
//                    resultsTable.showNothing();
                }
            }
        };

        if (showLoading) {
//            resultsTable.showLoadingIndicator();
        }
        worker.execute();
    }

    public SolrResults performSearch(SolrQueryBuilder builder, int page, int pageSize) throws Exception {
        
        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("GeneralSearchDialog.search called in the EDT");
        }
        
        SolrQuery query = builder.getQuery();
        query.setStart(pageSize*page);
        query.setRows(pageSize);
        
        return ModelMgr.getModelMgr().searchSolr(query, false);
    }
}
