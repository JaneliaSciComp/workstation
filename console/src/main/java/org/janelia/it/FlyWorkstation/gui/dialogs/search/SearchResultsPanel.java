package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.table.TableModel;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration.AttrGroup;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicRow;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.panels.ScrollablePanel;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A general search results panel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SearchResultsPanel extends JPanel implements SearchConfigurationListener, Refreshable {

	/** Format for displaying dates */
	protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
	/** Number of characters before cell values are truncated */
	protected static final int MAX_CELL_LENGTH = 50;
	
	/** How many results to load at a time */
	protected static final int PAGE_SIZE = 50;	

	/** Fields on which to calculate facet counts */
    protected String[] facets = {"entity_type", "tiling_pattern_txt", "username"};
    
    // UI Settings
    protected Font groupFont = new Font("Sans Serif", Font.BOLD, 11);
	protected Font checkboxFont = new Font("Sans Serif", Font.PLAIN, 11);
	
    // UI Elements
    protected final JSplitPane splitPane;
    protected final JPanel facetsPanel;
    protected final JPanel attrsPanel;
    protected final JLabel statusLabel;
    protected final DynamicTable resultsTable;
    
    // Search state
    protected SearchConfiguration searchConfig;
    protected final List<SolrResults> pages = new ArrayList<SolrResults>();
    protected final Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
    protected int numLoaded = 0;
    protected String sortField;
    protected boolean ascending = true;
    protected String fullQueryString = "";
    
    
	public SearchResultsPanel() {
		setLayout(new BorderLayout());
		
        // --------------------------------
        // Facets and Attributes on left
        // --------------------------------x
        Color color = new Color(238,238,238);
        
        facetsPanel = new ScrollablePanel();
        facetsPanel.setLayout(new BoxLayout(facetsPanel, BoxLayout.PAGE_AXIS));
        JScrollPane facetScrollPane = new JScrollPane();
        facetScrollPane.setViewportView(facetsPanel);
        facetScrollPane.getViewport().setBackground(color);
        
        attrsPanel = new ScrollablePanel();
        attrsPanel.setLayout(new BoxLayout(attrsPanel, BoxLayout.PAGE_AXIS));
        final JScrollPane attrScrollPane = new JScrollPane();
        attrScrollPane.setViewportView(attrsPanel);
        attrScrollPane.getViewport().setBackground(color);
        
        
		JTabbedPane leftTabbedPane = new JTabbedPane();
		leftTabbedPane.addTab("Result Filter", null, facetScrollPane, "Values to filter the results on");
		leftTabbedPane.addTab("Attributes", null, attrScrollPane, "Attributes to display in the result table");
		
        // --------------------------------
        // Results on right
        // --------------------------------
        resultsTable = new DynamicTable() {
			@Override
			public Object getValue(Object userObject, DynamicColumn column) {
				return SearchResultsPanel.this.getValue(userObject, column);
			}
			@Override
			protected void loadMoreResults() {
				performSearch(pages.size(), false);	
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
		
		
		JPanel resultsPane = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        resultsPane.add(statusLabel, BorderLayout.NORTH);
        resultsPane.add(resultsTable, BorderLayout.CENTER);
        
        // --------------------------------
		// Split pane center
        // --------------------------------
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, leftTabbedPane, resultsPane);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(260);
		add(splitPane, BorderLayout.CENTER);
	}

    @Override
	public void configurationChange(SearchConfigurationEvent evt) {
    	searchConfig = evt.getSearchConfig();
    	
    	Map<AttrGroup, List<SearchAttribute>> attributeGroups = searchConfig.getAttributeGroups();
    	
    	for(SearchAttribute attr : attributeGroups.get(AttrGroup.BASIC)) {
			resultsTable.addColumn(attr.getName(), attr.getLabel(), !attr.getName().equals("id"), false, true, attr.isSortable());	
    	}
    	
    	for(SearchAttribute attr : attributeGroups.get(AttrGroup.EXT)) {
    		resultsTable.addColumn(attr.getName(), attr.getLabel(), false, false, true, true);
    	}

		for(SearchAttribute attr : attributeGroups.get(AttrGroup.SAGE)) {
			resultsTable.addColumn(attr.getName(), attr.getLabel(), false, false, true, true);
		}
		
    	populateAttrs();
		revalidate();
	}
    
    public void documentSelected(EntityDocument doc) {
    }
    
    public void entitySelected(Entity entity) {
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
		pages.clear();
		numLoaded = 0;
		performSearch(0, showLoading);
		resultsTable.getScrollPane().getVerticalScrollBar().setValue(0); 
    }
    
    public synchronized void performSearch(final int page, final boolean showLoading) {
    	
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
			
    		private SolrResults results;
    		
			@Override
			protected void doStuff() throws Exception {
				results = performSearch(builder, page, PAGE_SIZE);
	    		pages.add(results);
	    		numLoaded += results.getResultList().size();
	    		resultsTable.setMoreResults(results.getResponse().getResults().getNumFound()>numLoaded);
			}

			@Override
			protected void hadSuccess() {
				populateFacets(results);
	        	populateResultView(results);
		    	if (showLoading) resultsTable.showTable();
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

    protected void populateResultView(SolrResults pageResults) {

    	if (pageResults==null) return;
    	
    	long numResults = pageResults.getResponse().getResults().getNumFound();
    	if (pageResults.getResultList().isEmpty()) numResults = 0;
    	
    	statusLabel.setText(numResults+" results found for '"+fullQueryString.trim()+"'");
    	statusLabel.setToolTipText("Query took "+pageResults.getResponse().getElapsedTime()+" milliseconds");
    	
    	if (pages.size()==1) {
    		// First page, so clear the previous results
			resultsTable.removeAllRows();
    	}	

    	for(EntityDocument entityDoc : pageResults.getEntityDocuments()) {
    		resultsTable.addRow(entityDoc);
    	}    	

    	updateTableModel();
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
		public void toggleSortOrder(int column) {
			List<DynamicColumn> columns = resultsTable.getDisplayedColumns();
			if (!columns.get(column).isVisible()) return;
			
			SortOrder newOrder = SortOrder.ASCENDING;
			if (!sortKeys.isEmpty()) {
				SortKey currentSortKey = sortKeys.get(0);
				if (currentSortKey.getColumn()==column) {
					// Reverse the sort
					if (currentSortKey.getSortOrder() == SortOrder.ASCENDING) {
						newOrder = SortOrder.DESCENDING;
					}
				}
				sortKeys.clear();
			}
			
			sortKeys.add(new SortKey(column, newOrder));
			sortField = columns.get(column).getName();
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
	
	protected void populateFacets(SolrResults pageResults) {
    	
    	facetsPanel.removeAll();

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
    			final String label = getFormattedFieldValue(ff.getName(), count.getName())+" ("+count.getCount()+")";
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

		JPanel attrGroupPanel = null;
		
		int i = 0;
		int currGroupIndex = 0;
		int groupIndex = 0;
		
		for(final DynamicColumn column : resultsTable.getColumns()) {
			AttrGroup currGroup = AttrGroup.values()[currGroupIndex];
			
			if (i==0 || groupIndex>=searchConfig.getAttributeGroups().get(currGroup).size()) {
				// Start a new group
				if (i>0) {
					currGroupIndex++;
					groupIndex = 0;
				}
				attrGroupPanel = new JPanel();
				attrGroupPanel.setOpaque(false);
				attrGroupPanel.setLayout(new BoxLayout(attrGroupPanel, BoxLayout.PAGE_AXIS));
				JLabel attrGroupLabel = new JLabel(currGroup.toString());
				attrGroupLabel.setFont(groupFont);
				attrGroupPanel.add(attrGroupLabel);
				attrsPanel.add(Box.createRigidArea(new Dimension(0,10)));
				attrsPanel.add(attrGroupPanel);
			}
			
			final JCheckBox checkBox = new JCheckBox(new AbstractAction(column.getLabel()) {
				public void actionPerformed(ActionEvent e) {
					JCheckBox cb = (JCheckBox) e.getSource();
					column.setVisible(cb.isSelected());
					performSearch(false, false, false);
				}
			});
			
			checkBox.setSelected(column.isVisible());
			checkBox.setFont(checkboxFont);
			attrGroupPanel.add(checkBox);
			i++;
			groupIndex++;
		}
		
		attrsPanel.add(Box.createRigidArea(new Dimension(0,10)));
		
		attrsPanel.revalidate();
		attrsPanel.repaint();
	}

	protected abstract SolrQueryBuilder getQueryBuilder();
	
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
     * Return the value of the specified column for the given object.
     * @param userObject
     * @param column
     * @return
     */
	public Object getValue(Object userObject, DynamicColumn column) {
		EntityDocument entityDoc = (EntityDocument)userObject;
		Entity entity = entityDoc.getEntity();
		SolrDocument doc = entityDoc.getDocument();
		String field = column.getName();
		Object value = null;
		if ("id".equals(field)) {
			value = entity.getId();
		}
		else if ("name".equals(field)) {
			value = entity.getName();
		}
		else if ("entity_type".equals(field)) {
			value = entity.getEntityType().getName();
		}
		else if ("username".equals(field)) {
			value = entity.getUser().getUserLogin();
		}
		else if ("creation_date".equals(field)) {
			value = df.format(entity.getCreationDate());
		}
		else if ("updated_date".equals(field)) {
			value = df.format(entity.getUpdatedDate());
		}
		else if ("annotations".equals(field)) {
			value = doc.getFieldValues("annotations");
		}
		else if ("score".equals(field)) {
			Float score = (Float)doc.get("score");
	        DecimalFormat twoDForm = new DecimalFormat("#.##");
	        value = Double.valueOf(twoDForm.format(score));
		}
		else {
			value = doc.getFieldValues(column.getName());
		}
		
		return getFormattedFieldValue(field, value);
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
    	return underscoreToTitleCase(fieldName);
    }

    /**
     * Returns the human-readable label for the specified value in the given field. 
     * @param fieldName
     * @param value
     * @return
     */
	protected Object getFormattedFieldValue(String fieldName, Object value) {
		if (value==null) return null;
		String formattedValue = value.toString();
		if (value instanceof Collection) {
			formattedValue = getCommaDelimited((Collection)value, MAX_CELL_LENGTH);
    	}
    	if ("tiling_pattern_txt".equals(fieldName)) {
    		formattedValue = underscoreToTitleCase(formattedValue);
    	}
    	return formattedValue;
    }
    
	protected String underscoreToTitleCase(String name) {
    	String[] words = name.split("_");
    	StringBuffer buf = new StringBuffer();
    	for(String word : words) {
    		char c = Character.toUpperCase(word.charAt(0));
    		if (buf.length()>0) buf.append(' ');
    		buf.append(c);
    		buf.append(word.substring(1).toLowerCase());
    	}
    	return buf.toString();
    }

	protected String getCommaDelimited(Collection objs, int maxLength) {
		if (objs==null) return null;
		StringBuffer buf = new StringBuffer();
		for(Object obj : objs) {
			if (buf.length()+3>=maxLength) {
				buf.append("...");
				break;
			}
			if (buf.length()>0) buf.append(", ");
			buf.append(obj.toString());
		}
		return buf.toString();
	}

	public DynamicTable getResultsTable() {
		return resultsTable;
	}	
}
