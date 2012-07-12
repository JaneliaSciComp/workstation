package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute.DataStore;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration.AttrGroup;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicRow;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.panels.ScrollablePanel;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A general search results panel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SearchResultsPanel extends JPanel implements SearchConfigurationListener, Refreshable {

	
	/** How many results to load at a time */
	protected static final int PAGE_SIZE = 100;	

	/** Fields on which to calculate facet counts */
    protected String[] facets = {"entity_type", "tiling_pattern_txt", "username"};
    
    // UI Settings
    protected Font groupFont = new Font("Sans Serif", Font.BOLD, 11);
	protected Font checkboxFont = new Font("Sans Serif", Font.PLAIN, 11);
	
    // UI Elements
    protected final JSplitPane splitPane;
    protected final JSplitPane tableSplitPane;
    protected final JPanel facetsPanel;
    protected final JPanel attrsPanel;
    protected final JLabel statusLabel;
    protected final JLabel projectionStatusLabel;
    protected final JPanel resultsPane;
    protected final JPanel projectionPane;
    protected final DynamicTable resultsTable;
    protected final DynamicTable projectionTable;
    protected final ListSelectionListener resultsTableListener;
    protected final ListSelectionListener projectionTableListener;
 	protected final JScrollPane facetScrollPane;
 	protected final JScrollPane attrScrollPane;
 	protected final JTabbedPane leftTabbedPane;
	private final Map<DynamicColumn,JCheckBox> attrCheckboxes =  new HashMap<DynamicColumn,JCheckBox>();
	
    // Search state
 	protected final SearchParametersPanel paramsPanel;
 	protected final Map<String, DynamicColumn> columnByName = new HashMap<String, DynamicColumn>();
 	protected final Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
 	protected SearchConfiguration searchConfig;
    protected String sortField;
    protected boolean ascending = true;
    protected String fullQueryString = "";
    
    // Results
    protected SearchResults searchResults = new SearchResults();
    
	public SearchResultsPanel(SearchParametersPanel paramsPanel) {
		setLayout(new BorderLayout());
		
		this.paramsPanel = paramsPanel;
		
        // --------------------------------
        // Facets and Attributes on left
        // --------------------------------x
        Color color = new Color(238,238,238);
        
        facetsPanel = new ScrollablePanel();
        facetsPanel.setLayout(new BoxLayout(facetsPanel, BoxLayout.PAGE_AXIS));
        facetsPanel.setOpaque(false);
        facetScrollPane = new JScrollPane();
        facetScrollPane.setViewportView(facetsPanel);
        facetScrollPane.getViewport().setBackground(color);
        facetScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        attrsPanel = new ScrollablePanel();
        attrsPanel.setLayout(new BoxLayout(attrsPanel, BoxLayout.PAGE_AXIS));
        attrsPanel.setOpaque(false);
        attrScrollPane = new JScrollPane();
        attrScrollPane.setViewportView(attrsPanel);
        attrScrollPane.getViewport().setBackground(color);
        attrScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
		leftTabbedPane = new JTabbedPane();
		leftTabbedPane.addTab("Result Filter", null, facetScrollPane, "Values to filter the results on");
		leftTabbedPane.addTab("Attributes", null, attrScrollPane, "Attributes to display in the result table");
		
        // --------------------------------
        // Results on right
        // --------------------------------
        resultsTable = new DynamicTable() {
			@Override
			public Object getValue(Object userObject, DynamicColumn column) {				
				return searchConfig.getValue(userObject, column.getName());
			}
			@Override
			protected void loadMoreResults(Callable<Void> success) {
				performSearch(searchResults.getNumLoadedPages(), false, success);	
			}
        	@Override
        	protected JPopupMenu createPopupMenu(MouseEvent e) {
        		JPopupMenu menu = SearchResultsPanel.this.createPopupMenu(e);
        		if (menu != null) return menu;
        		return super.createPopupMenu(e);
        	}
        	@Override
        	protected void rowClicked(int row) {
        		if (row<0) return;
        		DynamicRow drow = getRows().get(row);
        		EntityDocument doc = (EntityDocument)drow.getUserObject();
        		documentSelected(doc);
        		if (doc.getEntity()!=null) {
        			entitySelected(doc.getEntity());
        		}
            }
		};

		resultsTable.setMaxColWidth(80);
		resultsTable.setMaxColWidth(600);
		resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));

		
        projectionTable = new DynamicTable() {
			@Override
			public Object getValue(Object userObject, DynamicColumn column) {
				return searchConfig.getValue(userObject, column.getName());
			}
			@Override
			protected void loadMoreResults(Callable<Void> success) {
				performSearch(searchResults.getNumLoadedPages(), false, success);	
			}
        	@Override
        	protected JPopupMenu createPopupMenu(MouseEvent e) {
        		return super.createPopupMenu(e);
        	}
		};

		projectionTable.setMaxColWidth(80);
		projectionTable.setMaxColWidth(600);
		projectionTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));

		final JTable resultsJTable = resultsTable.getTable();
	 	final JTable projectionJTable = projectionTable.getTable();
		
	 	resultsTableListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!projectionPane.isVisible()) return;
				projectionJTable.getSelectionModel().removeListSelectionListener(projectionTableListener);
				List<Entity> list = new ArrayList<Entity>();
				for(int row : resultsJTable.getSelectedRows()) {
	        		DynamicRow drow = resultsTable.getRows().get(row);
	        		EntityDocument doc = (EntityDocument)drow.getUserObject();
	        		Entity entity = doc.getEntity();
	            	for(ResultPage resultPage : searchResults.getPages()) {
	            		List<Entity> mappedEntities = resultPage.getMappedEntities(entity.getId());
            			list.addAll(mappedEntities);
	            	}
				}
				selectMappedEntities(list);
				projectionJTable.getSelectionModel().addListSelectionListener(projectionTableListener);
			}
		};
		
		projectionTableListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!projectionPane.isVisible()) return;
				resultsJTable.getSelectionModel().removeListSelectionListener(resultsTableListener);
				List<Entity> list = new ArrayList<Entity>();
				for(int row : projectionJTable.getSelectedRows()) {
	        		DynamicRow drow = projectionTable.getRows().get(row);
	        		Entity mappedEntity = (Entity)drow.getUserObject();
	            	for(ResultPage resultPage : searchResults.getPages()) {
	            		List<Entity> resultEntities = resultPage.getResultEntities(mappedEntity.getId());
            			list.addAll(resultEntities);
	            	}
	            	
				}
				selectResultEntities(list);
				resultsJTable.getSelectionModel().addListSelectionListener(resultsTableListener);
			}
		};

		resultsJTable.getSelectionModel().addListSelectionListener(resultsTableListener);
		projectionJTable.getSelectionModel().addListSelectionListener(projectionTableListener);

		
		resultsPane = new JPanel(new BorderLayout());
		
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        resultsPane.add(statusLabel, BorderLayout.NORTH);
        resultsPane.add(resultsTable, BorderLayout.CENTER);


        JButton hideProjectionButton = new JButton(Icons.getIcon("close_red.png"));
        hideProjectionButton.setBorderPainted(false);
        hideProjectionButton.setToolTipText("Close mapped result view");
        hideProjectionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchResults.setResultTreeMapping(null);
				projectionPane.setVisible(false);
			}
		});
        
		projectionPane = new JPanel(new BorderLayout());
        projectionStatusLabel = new JLabel(" ");
        projectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JPanel projectionTitlePane = new JPanel();
        projectionTitlePane.setLayout(new BoxLayout(projectionTitlePane, BoxLayout.LINE_AXIS));
        projectionTitlePane.add(projectionStatusLabel);
        projectionTitlePane.add(Box.createHorizontalGlue());
        projectionTitlePane.add(hideProjectionButton);
        projectionPane.add(projectionTitlePane, BorderLayout.NORTH);
        projectionPane.add(projectionTable, BorderLayout.CENTER);
        projectionPane.setVisible(false);
        
        // --------------------------------
		// Results split for projections
        // --------------------------------
        
        tableSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, resultsPane, projectionPane);
        tableSplitPane.setBorder(BorderFactory.createEmptyBorder());
        
        // --------------------------------
		// Split pane center
        // --------------------------------
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, leftTabbedPane, tableSplitPane);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(260);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
		add(splitPane, BorderLayout.CENTER);
	}
	
	public void init(SearchConfiguration searchConfig) {
		
		if (!searchConfig.isReady()) return;
		
		this.searchConfig = searchConfig;
		resultsTable.clearColumns();
		projectionTable.clearColumns();
		columnByName.clear();
		
    	Map<AttrGroup, List<SearchAttribute>> attributeGroups = searchConfig.getAttributeGroups();
    	
    	for(SearchAttribute attr : attributeGroups.get(AttrGroup.BASIC)) {
			DynamicColumn column = resultsTable.addColumn(attr.getName(), attr.getLabel(), !attr.getName().equals("id"), false, true, attr.isSortable());
			columnByName.put(attr.getName(), column);
			if (attr.getDataStore()==DataStore.ENTITY) {
				projectionTable.addColumn(attr.getName(), attr.getLabel(), !attr.getName().equals("id"), false, true, false);	
			}
    	}
    	
    	for(SearchAttribute attr : attributeGroups.get(AttrGroup.EXT)) {
    		DynamicColumn column = resultsTable.addColumn(attr.getName(), attr.getLabel(), false, false, true, attr.isSortable());
    		columnByName.put(attr.getName(), column);
    	}

		for(SearchAttribute attr : attributeGroups.get(AttrGroup.SAGE)) {
			DynamicColumn column = resultsTable.addColumn(attr.getName(), attr.getLabel(), false, false, true, attr.isSortable());
			columnByName.put(attr.getName(), column);
		}
		
    	populateAttrs();
		revalidate();
	}

    @Override
	public void configurationChange(SearchConfigurationEvent evt) {
    	init(evt.getSearchConfig());	
	}
    
    public void documentSelected(EntityDocument doc) {
    }
    
    public void entitySelected(Entity entity) {
    }
    
    public void selectResultEntities(List<Entity> entities) {
    	JTable table = resultsTable.getTable();
    	table.clearSelection();
		int firstRow = Integer.MAX_VALUE;
    	for (Entity entity : entities) {
    		Integer row = searchResults.getRowIndexForResultId(entity.getId());
    		if (row!=null) {
				table.setColumnSelectionAllowed(false);
				table.addRowSelectionInterval(row, row);
	        	if (row<firstRow) firstRow = row;
    		}
    		else {
    			System.out.println("WARNING: row index not found for "+entity.getId());
    		}
    	}
    	table.scrollRectToVisible(table.getCellRect(firstRow, 1, true));
    }

	public void selectMappedEntities(List<Entity> mappedEntities) {
		JTable table = projectionTable.getTable();
		table.clearSelection();
		int firstRow = Integer.MAX_VALUE;
    	for (Entity mappedEntity : mappedEntities) {
    		Integer row = searchResults.getRowIndexForMappedId(mappedEntity.getId());
    		if (row!=null) {
				table.setColumnSelectionAllowed(false);
				table.addRowSelectionInterval(row, row);
	        	if (row<firstRow) firstRow = row;
    		}
    		else {
    			System.out.println("WARNING: row index not found for "+mappedEntity.getId());
    		}
    	}   
    	table.scrollRectToVisible(table.getCellRect(firstRow, 1, true));
    }

    private JPopupMenu createPopupMenu(MouseEvent e) {    	
        
    	JTable target = (JTable) e.getSource();
        final String value = target.getValueAt(target.getSelectedRow(), target.getSelectedColumn()).toString();
        
        List<Entity> selectedEntities = new ArrayList<Entity>();
        for (int i : resultsTable.getTable().getSelectedRows()) {
        	EntityDocument doc = (EntityDocument)resultsTable.getRows().get(i).getUserObject();
        	selectedEntities.add(doc.getEntity());
        }
        return getPopupMenu(selectedEntities, value);
    }
    
    protected JPopupMenu getPopupMenu(List<Entity> selectedEntites, String value) {
    	return null;
    }
    
    @Override
    public void refresh() {
    	performSearch(false, false, true);
    }
    
	public void performSearch(boolean clearFilters, boolean clearSort, boolean showLoading) {
    	if (clearFilters) {
    		filters.clear();
    	}
		if (clearSort) {
			sortField = null;
			ascending = true;
		}
		clear();
		performSearch(0, showLoading);
		resultsTable.getScrollPane().getVerticalScrollBar().setValue(0); 
    }
	
	public void clear() {
		searchResults.clear();
		statusLabel.setText("");
    	statusLabel.setToolTipText("");
    	facetsPanel.removeAll();
    	resultsTable.removeAllRows();
		projectionTable.removeAllRows();
		projectionPane.setVisible(false);
	}

    public synchronized void performSearch(final int pageNum, final boolean showLoading) {
    	performSearch(pageNum, showLoading, null);
    }
    
    public synchronized void performSearch(final int pageNum, final boolean showLoading, final Callable<Void> success) {

		final SolrQueryBuilder builder = getQueryBuilder(true);		
		if (!builder.hasQuery()) return;
		
		// We don't want to display all the system level query parameters, so build a simplified version of 
		// the query string for display purposes. 
		StringBuilder qs = new StringBuilder();
		if (builder.getAuxString()!=null) {
			qs.append(builder.getAuxString());
		}
		if (qs.length()>0) qs.append(" ");
		if (builder.getSearchString()!=null) {
			qs.append(builder.getSearchString());
		}
		this.fullQueryString = qs.toString();
		
    	SimpleWorker worker = new SimpleWorker() {
			
    		private ResultPage resultPage;
    		
			@Override
			protected void doStuff() throws Exception {
				resultPage = new ResultPage(performSearch(builder, pageNum, PAGE_SIZE));
	    		searchResults.addPage(resultPage);
	    		resultsTable.setMoreResults(searchResults.hasMoreResults());
			}

			@Override
			protected void hadSuccess() {
				populateFacets(resultPage);
	        	populateResultView(resultPage);
		    	if (showLoading) resultsTable.showTable();
		    	try {
		    		if (success!=null) success.call();
		    	}
		    	catch (Exception e) {
		    		SessionMgr.getSessionMgr().handleException(e);
		    	}
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
		    	if (showLoading) resultsTable.showNothing();
			}
    	};
    	
    	if (showLoading) resultsTable.showLoadingIndicator();
		worker.execute();
    }
    
    /**
     * Actually execute the query. This method must be called from a worker thread.
     */
    public SolrResults performSearch(SolrQueryBuilder builder, int page, int pageSize) throws Exception {
    	
		if (SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("GeneralSearchDialog.search called in the EDT");
		
		SolrQuery query = builder.getQuery();
		query.setStart(pageSize*page);
		query.setRows(pageSize);
		
		return ModelMgr.getModelMgr().searchSolr(query);
    }
    
    public void projectResults(final ResultTreeMapping projection) {
    	
    	projectionTable.removeAllRows();
    	searchResults.setResultTreeMapping(projection);
    	
		SimpleWorker worker = new SimpleWorker() {
			@Override
			protected void doStuff() throws Exception {
				searchResults.projectResultPages();
			}
			@Override
			protected void hadSuccess() {
				for(ResultPage resultPage : searchResults.getPages()) {
					populateProjectionView(resultPage);
				}
			}
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
    }
    
    public void projectResultPage(final ResultPage resultPage) {
    	if (searchResults.getResultTreeMapping()==null) return;
    	
		SimpleWorker worker = new SimpleWorker() {
			@Override
			protected void doStuff() throws Exception {
				searchResults.projectResultPage(resultPage);
			}
			@Override
			protected void hadSuccess() {
				populateProjectionView(resultPage);
			}
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
    }
    
    protected void populateResultView(final ResultPage resultPage) {

    	if (resultPage==null) return;
    	SolrResults pageResults = resultPage.getSolrResults();
    	long numResults = pageResults.getResponse().getResults().getNumFound();
    	if (pageResults.getResultList().isEmpty()) numResults = 0;
    	    	
    	// Show any columns for attributes that were used in the search criteria
    	for(SearchCriteria searchCriteria : paramsPanel.getSearchCriteriaList()) {
    		SearchAttribute attr = searchCriteria.getAttribute();
    		if (attr!=null) {
	    		DynamicColumn column = columnByName.get(attr.getName());
	    		if (column!=null) {
	    			column.setVisible(true);
	    			JCheckBox checkBox = attrCheckboxes.get(column);
	    			if (checkBox!=null) {
	    				checkBox.setSelected(true);
	    			}
	    		}
    		}
    	}
    	
    	if (searchResults.getNumLoadedPages()==1) {
    		// First page, so clear the previous results
			resultsTable.removeAllRows();
    	}	

    	for(EntityDocument entityDoc : pageResults.getEntityDocuments()) {
    		resultsTable.addRow(entityDoc);
    	}
    	
    	int numLoadedResults = resultsTable.getRows().size();
    	statusLabel.setText(numResults+" results found for '"+fullQueryString.trim()+"', "+numLoadedResults+" results loaded.");
    	statusLabel.setToolTipText("Query took "+pageResults.getResponse().getElapsedTime()+" milliseconds");
    	
    	updateTableModel();
    	projectResultPage(resultPage);
    }
    
    protected void populateProjectionView(ResultPage resultPage) {

    	if (resultPage.getMappedResults()==null) {
    		System.out.println("WARNING: populateProjectionView called with null projected results");
    		return;
    	}

		projectionStatusLabel.setText("Mapped results ("+
				searchResults.getResultTreeMapping().getDescription()+")");
		
    	if (searchResults.getNumLoadedPages()==1) {
    		// First page, so clear the previous results
    		projectionTable.removeAllRows();
    	}	
    	
    	for(Entity mappedEntity : resultPage.getMappedResults())  {
			projectionTable.addRow(mappedEntity);	
    	}

    	projectionTable.setMoreResults(!projectionTable.getRows().isEmpty()&&searchResults.hasMoreResults());
    	projectionTable.updateTableModel();
    	projectionPane.setVisible(true);
    	tableSplitPane.setDividerLocation(0.5);
    }
    
    protected void updateTableModel() {
		resultsTable.updateTableModel();
		resultsTable.getTable().setRowSorter(new SolrRowSorter());
    }
    
    protected class SolrRowSorter extends RowSorter<TableModel> {
		
		private List<SortKey> sortKeys = new ArrayList<SortKey>();
		
		public SolrRowSorter() {
			List<DynamicColumn> columns = resultsTable.getDisplayedColumns();
			for (int i=0; i<columns.size(); i++) {
				if (columns.get(i).getName().equals(sortField)) {
					sortKeys.add(new SortKey(i, ascending?SortOrder.ASCENDING:SortOrder.DESCENDING));
				}
			}
		}
		
		@Override
		public void toggleSortOrder(int columnNum) {
			List<DynamicColumn> columns = resultsTable.getDisplayedColumns();
			DynamicColumn column = columns.get(columnNum);
			if (!column.isVisible() || !column.isSortable()) return;
			
			SortOrder newOrder = SortOrder.ASCENDING;
			if (!sortKeys.isEmpty()) {
				SortKey currentSortKey = sortKeys.get(0);
				if (currentSortKey.getColumn()==columnNum) {
					// Reverse the sort
					if (currentSortKey.getSortOrder() == SortOrder.ASCENDING) {
						newOrder = SortOrder.DESCENDING;
					}
				}
				sortKeys.clear();
			}
			
			sortKeys.add(new SortKey(columnNum, newOrder));
			sortField = column.getName();
			ascending = (newOrder != SortOrder.DESCENDING);
			performSearch(false, false, true);
		}
		
		@Override
		public void setSortKeys(List<? extends SortKey> sortKeys) {
            this.sortKeys = Collections.unmodifiableList(new ArrayList<SortKey>(sortKeys));
		}
		
		@Override
		public void rowsUpdated(int firstRow, int endRow, int column) {
		}
		
		@Override
		public void rowsUpdated(int firstRow, int endRow) {
		}
		
		@Override
		public void rowsInserted(int firstRow, int endRow) {
		}
		
		@Override
		public void rowsDeleted(int firstRow, int endRow) {
		}
		
		@Override
		public void modelStructureChanged() {
		}
		
		@Override
		public int getViewRowCount() {
			return resultsTable.getTable().getModel().getRowCount();
		}
		
		@Override
		public List<? extends SortKey> getSortKeys() {
			return sortKeys;
		}
		
		@Override
		public int getModelRowCount() {
			return resultsTable.getTable().getModel().getRowCount();
		}
		
		@Override
		public TableModel getModel() {
			return resultsTable.getTableModel();
		}
		
		@Override
		public int convertRowIndexToView(int index) {
			return index;
		}
		
		@Override
		public int convertRowIndexToModel(int index) {
			return index;
		}
		
		@Override
		public void allRowsChanged() {
		}
	};
	
	protected void populateFacets(ResultPage resultPage) {
    	
		facetsPanel.removeAll();
		
    	SolrResults pageResults =  resultPage.getSolrResults();
    	if (pageResults==null || pageResults.getResultList().isEmpty()) return;
    	
    	QueryResponse qr = pageResults.getResponse();
    	for(final FacetField ff : qr.getFacetFields()) {
    		
    		JPanel facetPanel = new JPanel();
    		facetPanel.setOpaque(false);
    		facetPanel.setLayout(new BoxLayout(facetPanel, BoxLayout.PAGE_AXIS));
    		
    		JLabel facetLabel = new JLabel(getFieldLabel(ff.getName()));
    		facetLabel.setFont(groupFont);
    		facetPanel.add(facetLabel);
    		
    		Set<String> selectedValues = filters.get(ff.getName());
    		List<Count> counts = ff.getValues();
    		if (counts==null) continue;
    		
    		for(final Count count : ff.getValues()) {
    			
    			final SearchAttribute attr = searchConfig.getAttributeByName(ff.getName());
    			final String label = searchConfig.getFormattedFieldValue(count.getName(), attr.getName())+" ("+count.getCount()+")";
    			
    			final JCheckBox checkBox = new JCheckBox(new AbstractAction(label) {
					public void actionPerformed(ActionEvent e) {
						JCheckBox cb = (JCheckBox) e.getSource();
						Set<String> values = filters.get(ff.getName());
						if (values==null) {
							values = new HashSet<String>();
							filters.put(ff.getName(), values);
						}
						if (cb.isSelected()) {
							values.add(count.getName());
						}
						else {
							values.remove(count.getName());
						}
						performSearch(false, true, true);
					}
				});
    			
    			checkBox.setSelected(selectedValues!=null && selectedValues.contains(count.getName()));
    			checkBox.setFont(checkboxFont);
    			facetPanel.add(checkBox);
    		}

    		facetsPanel.add(Box.createRigidArea(new Dimension(0,10)));
    		facetsPanel.add(facetPanel);
    	}
    	
    	facetsPanel.revalidate();
    	facetsPanel.repaint();
    }
	
	protected void populateAttrs() {
    	
    	attrsPanel.removeAll();
		
		for(final AttrGroup currGroup : searchConfig.getAttributeGroups().keySet()) {

			JPanel attrGroupPanel = new JPanel();
			attrGroupPanel.setOpaque(false);
			attrGroupPanel.setLayout(new BoxLayout(attrGroupPanel, BoxLayout.PAGE_AXIS));
			JLabel attrGroupLabel = new JLabel(currGroup.getLabel());
			attrGroupLabel.setFont(groupFont);
			attrGroupPanel.add(attrGroupLabel);
			attrsPanel.add(Box.createRigidArea(new Dimension(0,10)));
			attrsPanel.add(attrGroupPanel);

			for(final SearchAttribute attr : searchConfig.getAttributeGroups().get(currGroup)) {
				final DynamicColumn column = resultsTable.getColumn(attr.getName());
				final JCheckBox checkBox = new JCheckBox(new AbstractAction(attr.getLabel()) {
					public void actionPerformed(ActionEvent e) {
						JCheckBox cb = (JCheckBox) e.getSource();
						column.setVisible(cb.isSelected());
						performSearch(false, false, false);
					}
				});
				checkBox.setToolTipText(attr.getDescription());
				checkBox.setSelected(column.isVisible());
				checkBox.setFont(checkboxFont);
				attrCheckboxes.put(column, checkBox);
				attrGroupPanel.add(checkBox);
			}
		}
		
		attrsPanel.add(Box.createRigidArea(new Dimension(0,10)));
		attrsPanel.revalidate();
		attrsPanel.repaint();
	}

	protected SolrQueryBuilder getQueryBuilder() {
		return paramsPanel.getQueryBuilder();
	}
	
	public SolrQueryBuilder getQueryBuilder(boolean fetchFacets) {
		SolrQueryBuilder builder = getQueryBuilder();
		builder.setSortField(sortField);
		builder.setAscending(ascending);
		builder.getFilters().putAll(filters);		
		if (fetchFacets) {
    		builder.getFacets().addAll(Arrays.asList(facets));
		}
		return builder;
	}
	
    /**
     * Returns the human-readable label for the given field name.
     * @param fieldName
     * @return
     */
	protected String getFieldLabel(String fieldName) {
    	if ("tiling_pattern_txt".equals(fieldName)) {
    		return "Tiling Pattern";
    	}
    	else if ("entity_type".equals(fieldName)) {
    		return "Result Type";
    	}
    	else if ("username".equals(fieldName)) {
    		return "Owner";
    	}
    	return StringUtils.underscoreToTitleCase(fieldName);
    }

	public DynamicTable getResultsTable() {
		return resultsTable;
	}

	public DynamicTable getMappedResultsTable() {
		return projectionTable;
	}
	
	public SearchResults getSearchResults() {
		return searchResults;
	}	
	
	
}
