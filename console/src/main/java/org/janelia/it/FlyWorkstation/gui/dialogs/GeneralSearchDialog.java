package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.table.TableModel;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A dialog for performing general searches and saving the results to the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GeneralSearchDialog extends ModalDialog {

	protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
	/** Number of characters before annotations are truncated */
	protected static final int MAX_ANNOTATIONS_LENGTH = 50;
	
	/** How many results to load at a time */
	protected static final int PAGE_SIZE = 50;

	/** Fields on which to calculate facet counts */
    protected String[] facets = {"entity_type", "tiling_pattern_txt", "username"};

    /** Fields to use as columns */
    protected String[] columnFields = {"name", "entity_type", "username", "updated_date", "annotations", "score"};
    
    /** Labels to use on the columns */
    protected String[] columnLabels = {"Name", "Type", "Owner", "Last Updated", "Annotations", "Score"};
    
    /** Which columns are sortable */
    protected boolean[] sortable = {true, true, true, true, false, true};
    
    // UI Settings
    protected Font facetLabelFont = new Font("Sans Serif", Font.BOLD, 11);
	protected Font facetValueFont = new Font("Sans Serif", Font.PLAIN, 11);
	
    // UI Elements
    protected final JPanel inputPanel;
    protected final JLabel titleLabel;
    protected final JTextField inputField;
    protected final JLabel statusLabel;
    protected final JSplitPane splitPane;
    protected final JPanel facetsPanel;
    protected final DynamicTable resultsTable;
    protected final JTextField folderNameField;
    
    // Search state
    protected Entity searchRoot;
    protected final List<SolrResults> pages = new ArrayList<SolrResults>();
    protected final Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
    protected final Map<Long,SolrDocument> docMap = new HashMap<Long,SolrDocument>();
    protected int numLoaded = 0;
    protected String sortField;
    protected boolean ascending = true;
    
    public GeneralSearchDialog() {
    	
        setTitle("Search");

        // --------------------------------
        // Query interface at top
        // --------------------------------
        inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        titleLabel = new JLabel("Search for:");
        inputPanel.add(titleLabel, BorderLayout.NORTH);
        
        inputField = new JTextField(40);
        inputField.setToolTipText("Enter search terms...");
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					performFreshSearch(true, true, true);
				}
			}
		});
        
        statusLabel = new JLabel(" ");
        inputPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(inputPanel, BorderLayout.NORTH);
        
        // --------------------------------
        // Facets on left
        // --------------------------------x
        facetsPanel = new JPanel();
        facetsPanel.setLayout(new BoxLayout(facetsPanel, BoxLayout.PAGE_AXIS));
        JScrollPane facetScrollPane = new JScrollPane();
        facetScrollPane.setViewportView(facetsPanel);
		
        // --------------------------------
        // Results on right
        // --------------------------------
        resultsTable = new DynamicTable() {
			@Override
			public Object getValue(Object userObject, DynamicColumn column) {
				return GeneralSearchDialog.this.getValue(userObject, column);
			}

			@Override
			protected void loadMoreResults() {
				performSearch(pages.size(), false);	
			}
		};

		resultsTable.setMaxColWidth(80);
		resultsTable.setMaxColWidth(600);
		
		for(String columnLabel : columnLabels) {
			resultsTable.addColumn(columnLabel, true, false, false);	
		}
		
        // --------------------------------
		// Split pane center
        // --------------------------------
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, facetScrollPane, resultsTable);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(260);
		add(splitPane, BorderLayout.CENTER);
        
        // --------------------------------
		// Saving interface at bottom
        // --------------------------------
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());        
        
		JLabel folderNameLabel = new JLabel("Save selected objects in folder: ");
		buttonPane.add(folderNameLabel);
		
        folderNameField = new JTextField(10);
        folderNameField.setToolTipText("Enter the folder name to save the results in");
        buttonPane.add(folderNameField);
        
        JButton okButton = new JButton("Save");
        okButton.setToolTipText("Save the results");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveResults();
			}
		});
        buttonPane.add(okButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});
        buttonPane.add(cancelButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }
    
	protected void init() {
		folderNameField.setText(getNextFolderName());
		performFreshSearch(true, true, true);

		Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
		setPreferredSize(new Dimension((int)(browser.getWidth()*0.8),(int)(browser.getHeight()*0.8)));
		
    	packAndShow();
    	inputField.requestFocus();
    }
    
    public void showDialog() {
    	this.searchRoot = null;
    	titleLabel.setText("Search all data");
    	init();
    }
    
    public void showDialog(Entity entity) {
    	this.searchRoot = entity;
    	titleLabel.setText("Search within "+searchRoot.getName());
    	init();
    }
    
    protected void performFreshSearch(boolean clearFilters, boolean clearSort, boolean showLoading) {
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
    
    protected synchronized void performSearch(final int page, final boolean showLoading) {

    	final String qs = inputField.getText();    	
    	if (qs==null || "".equals(qs)) return;
    	
    	SimpleWorker worker = new SimpleWorker() {
			
    		private SolrResults results;
    		
			@Override
			protected void doStuff() throws Exception {
	        	String queryString = "-entity_type:Ontology* AND (" + qs +")";
	        	
	    		SolrQueryBuilder builder = new SolrQueryBuilder();
	    		// TODO: set this to the user's username once we've tested
	    		builder.setQueryString(queryString);
	    		builder.setUsername("*"); 
	    		if (searchRoot!=null) builder.setRootId(searchRoot.getId());
	    		SolrQuery query = builder.getQuery();
	    		query.addField("score");
	    		query.setStart(PAGE_SIZE*page);
	    		query.setRows(PAGE_SIZE);
	    		
	    		if (sortField!=null) {
	    			query.setSortField(sortField, ascending?ORDER.asc:ORDER.desc);
	    		}
	    		
	    		for(String fieldName : filters.keySet()) {
	    			Set<String> values = filters.get(fieldName);
	    			if (values==null||values.isEmpty()) continue;
	    			query.addFilterQuery(getFilterQuery(fieldName, values));
	    		}
	    		
	    		for(String facet : facets) {
	    			// Exclude the facet field from itself, to support multi-valued faceting
		    		query.addFacetField("{!ex="+facet+"}"+facet);
	    		}
	    		
	    		this.results = ModelMgr.getModelMgr().searchSolr(query);
	    		pages.add(results);
	    		numLoaded += results.getResultList().size();
	    		resultsTable.setMoreResults(results.getResponse().getResults().getNumFound()>numLoaded);

	    		Iterator<SolrDocument> i = results.getResponse().getResults().iterator();
	    		while (i.hasNext()) {
	    			SolrDocument doc = i.next();
	    			docMap.put(new Long(doc.get("id").toString()), doc);
	    		}	    		
			}

			@Override
			protected void hadSuccess() {
				populateFacets(results);
	        	populateResultView(results);
		    	Utils.setDefaultCursor(GeneralSearchDialog.this);
		    	if (showLoading) resultsTable.showTable();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
		    	Utils.setDefaultCursor(GeneralSearchDialog.this);
		    	if (showLoading) resultsTable.showNothing();
			}
    	};
    	
    	Utils.setWaitingCursor(GeneralSearchDialog.this);
    	if (showLoading) resultsTable.showLoadingIndicator();
		worker.execute();
    }
    
    protected synchronized void saveResults() {
    	
    	SimpleWorker worker = new SimpleWorker() {

    		private Entity newFolder;
    		
			@Override
			protected void doStuff() throws Exception {

				String folderName = folderNameField.getText();
				this.newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
				newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
				ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);

		        for (Object obj : resultsTable.getSelectedObjects()) {
		        	Entity entity = (Entity)obj;
					EntityData newEd = newFolder.addChildEntity(entity);
					ModelMgr.getModelMgr().saveOrUpdateEntityData(newEd);	
				}
			}
			
			@Override
			protected void hadSuccess() {
				final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
				entityOutline.refresh(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
		        		ModelMgr.getModelMgr().selectOutlineEntity("/e_"+newFolder.getId(), true);	
						return null;
					}
					
				});
		    	Utils.setDefaultCursor(GeneralSearchDialog.this);
	            setVisible(false);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
		    	Utils.setDefaultCursor(GeneralSearchDialog.this);
			}
		};

    	Utils.setWaitingCursor(this);
		worker.execute();
    }
    
    protected void populateResultView(SolrResults pageResults) {

    	if (pageResults==null) return;
    	
    	statusLabel.setText(pageResults.getResponse().getResults().getNumFound()+" results found");
    	statusLabel.setToolTipText("Query took "+pageResults.getResponse().getElapsedTime()+" milliseconds");
    	
    	if (pages.size()==1) {
    		// First page, so clear the previous results
			resultsTable.removeAllRows();
    	}	

    	for(Entity entity : pageResults.getResultList()) {
    		resultsTable.addRow(entity);
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
			for (int i=0; i<columnFields.length; i++) {
				if (columnFields[i].equals(sortField)) {
					sortKeys.add(new SortKey(i, ascending?SortOrder.ASCENDING:SortOrder.DESCENDING));
				}
			}
		}
		
		@Override
		public void toggleSortOrder(int column) {
			if (!sortable[column]) return;
			
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
			sortField = columnFields[column];
			ascending = (newOrder != SortOrder.DESCENDING);
			performFreshSearch(false, false, true);
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

    	if (pageResults==null) return;
    	
    	QueryResponse qr = pageResults.getResponse();
    	for(final FacetField ff : qr.getFacetFields()) {
    		
    		JPanel facetPanel = new JPanel();
    		facetPanel.setLayout(new BoxLayout(facetPanel, BoxLayout.PAGE_AXIS));
    		
    		JLabel facetLabel = new JLabel(getFieldLabel(ff.getName()));
    		facetLabel.setFont(facetLabelFont);
    		facetPanel.add(facetLabel);
    		
    		Set<String> selectedValues = filters.get(ff.getName());
    		List<Count> counts = ff.getValues();
    		if (counts==null) continue;
    		
    		for(final Count count : ff.getValues()) {
    			final String label = getValueLabel(ff.getName(), count.getName())+" ("+count.getCount()+")";
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
						performFreshSearch(false, true, true);
					}
				});
    			
    			checkBox.setSelected(selectedValues!=null && selectedValues.contains(count.getName()));
    			checkBox.setFont(facetValueFont);
    			facetPanel.add(checkBox);
    		}

    		facetsPanel.add(Box.createRigidArea(new Dimension(0,10)));
    		facetsPanel.add(facetPanel);
    	}
    	
    	facetsPanel.revalidate();
    	facetsPanel.repaint();
    }

    /**
     * Return the value of the specified column for the given object.
     * @param userObject
     * @param column
     * @return
     */
	protected Object getValue(Object userObject, DynamicColumn column) {
		if (userObject instanceof Entity) {
			Entity entity = (Entity)userObject;
			if ("Name".equals(column.getName())) {
				return entity.getName();
			}
			else if ("Type".equals(column.getName())) {
				return entity.getEntityType().getName();
			}
			else if ("Owner".equals(column.getName())) {
				return entity.getUser().getUserLogin();
			}
			else if ("Last Updated".equals(column.getName())) {
				return df.format(entity.getUpdatedDate());
			}
			else if ("Annotations".equals(column.getName())) {
				SolrDocument doc = docMap.get(entity.getId());
				return getCommaDelimited(doc.getFieldValues("annotations"), MAX_ANNOTATIONS_LENGTH);
			}
			else if ("Score".equals(column.getName())) {
				SolrDocument doc = docMap.get(entity.getId());
				Float score = (Float)doc.get("score");
		        DecimalFormat twoDForm = new DecimalFormat("#.##");
		        return Double.valueOf(twoDForm.format(score));
			}
		}
		return null;
	}
	
    /**
     * Looks for folders in the entity tree, and creates a new result name which does not already exist.
     * @return
     */
	protected String getNextFolderName() {
		final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
		int maxNum = 0;
		for(EntityData ed : entityOutline.getRootEntity().getEntityData()) {
			Entity topLevelFolder = ed.getChildEntity();
			if (topLevelFolder != null) {
				Pattern p = Pattern.compile("^Search Results #(\\d+)$");
				Matcher m = p.matcher(topLevelFolder.getName());
				if (m.matches()) {
					String num = m.group(1);
					if (num!=null && !"".equals(num)) {
						int n = Integer.parseInt(num);
						if (n>maxNum) {
							maxNum = n;
						}
					}
				}
			}
		}
		return "Search Results #"+(maxNum+1);
    }

    /**
     * Returns a SOLR-style field query for the given field containing the given values. Also tags the
     * field so that it can be excluded in facets on other fields. 
     * @param fieldName
     * @param values
     * @return
     */
	protected String getFilterQuery(String fieldName, Set<String> values) {
    	StringBuffer sb = new StringBuffer("{!tag="+fieldName+"}"+fieldName);
    	sb.append(":("); // Sad face :/
    	for(String value : values) {
    		sb.append("\"");
    		sb.append(value);
    		sb.append("\" ");
    	}
    	sb.append(")");
    	return sb.toString();
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
	protected String getValueLabel(String fieldName, String value) {
    	if ("tiling_pattern_txt".equals(fieldName)) {
    		return underscoreToTitleCase(value);
    	}
    	return value;
    }
    
	protected String underscoreToTitleCase(String name) {
    	String[] words = name.split("_");
    	StringBuffer buf = new StringBuffer();
    	for(String word : words) {
    		char c = Character.toUpperCase(word.charAt(0));
    		if (buf.length()>0) buf.append(' ');
    		buf.append(c);
    		buf.append(word.substring(1));
    	}
    	return buf.toString();
    }

	protected String getCommaDelimited(Collection<Object> objs, int maxLength) {
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
}