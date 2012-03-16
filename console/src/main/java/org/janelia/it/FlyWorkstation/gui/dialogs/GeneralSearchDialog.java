package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableModel;

import loci.plugins.config.SpringUtilities;
import net.sourceforge.jdatepicker.JDateComponentFactory;
import net.sourceforge.jdatepicker.impl.JDatePickerImpl;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.panels.ScrollablePanel;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;


/**
 * A dialog for performing general searches and saving the results to the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GeneralSearchDialog extends ModalDialog {

	protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	/** Default directory for exports */
	protected static final String DEFAULT_DIR = System.getProperty("user.home");
	
	/** Number of characters before cell values are truncated */
	protected static final int MAX_CELL_LENGTH = 50;
	
	/** How many results to load at a time */
	protected static final int PAGE_SIZE = 50;
	
	/** How many results to load at a time when exporting */
	protected static final int EXPORT_PAGE_SIZE = 1000;

	/** Number of historical search terms in the drop down */
	protected static final int MAX_HISTORY_LENGTH = 10;
	
	/** Fields on which to calculate facet counts */
    protected String[] facets = {"entity_type", "tiling_pattern_txt", "username"};

    /** Fields to use as columns */
    protected static final String[] columnFields = {"id", "name", "entity_type", "username", "updated_date", "annotations", "score"};
    
    /** Labels to use on the columns */
    protected static final String[] columnLabels = {"GUID", "Name", "Type", "Owner", "Last Updated", "Annotations", "Score"};
    
    /** Which columns are sortable */
    protected static final boolean[] columnSortable = {true, true, true, true, true, false, true};
    
    // UI Settings
    protected Font groupFont = new Font("Sans Serif", Font.BOLD, 11);
	protected Font checkboxFont = new Font("Sans Serif", Font.PLAIN, 11);
	
    // UI Elements
    protected final JPanel inputPanel;
    protected final JLabel titleLabel;
    protected final JLabel titleLabel2;
    protected final JComboBox inputField;
    protected final JCheckBox advancedSearchCheckbox;
    protected final JDatePickerImpl startDatePicker;
    protected final JDatePickerImpl endDatePicker;
    protected final JSplitPane splitPane;
    protected final JPanel facetsPanel;
    protected final JPanel attrsPanel;
    protected final JLabel statusLabel;
    protected final DynamicTable resultsTable;
    protected final JTextField folderNameField;
    protected final JButton exportButton;
    
    // Search state
    protected Entity searchRoot;
    protected final List<SolrResults> pages = new ArrayList<SolrResults>();
    protected final Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
    protected int numLoaded = 0;
    protected String sortField;
    protected boolean ascending = true;
    protected String searchString;
    
    public GeneralSearchDialog() {

        setTitle("Search");

        // --------------------------------
        // Query interface at top
        // --------------------------------
        
        titleLabel = new JLabel("Search for ");
        titleLabel2 = new JLabel();
        
        inputField = new JComboBox();
        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        inputField.setEditable(true);
        inputField.setToolTipText("Enter search terms...");
        
        JPanel searchBox = new JPanel();
        searchBox.setLayout(new BoxLayout(searchBox, BoxLayout.LINE_AXIS));
        searchBox.add(titleLabel);
        searchBox.add(inputField);
        searchBox.add(titleLabel2);

        final JPanel advancedSearch = new JPanel(new SpringLayout());
        advancedSearch.setBorder(BorderFactory.createCompoundBorder(
        				BorderFactory.createEmptyBorder(10,10,10,10), 
        				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
        
        advancedSearch.setVisible(false);
        
        startDatePicker = (JDatePickerImpl) JDateComponentFactory.createJDatePicker();
        startDatePicker.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        JLabel startDateLabel = new JLabel("Start Date: ");
        startDateLabel.setLabelFor(startDatePicker);
        startDatePicker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performFreshSearch(false, false, false);
			}
		});
        
        endDatePicker = (JDatePickerImpl) JDateComponentFactory.createJDatePicker();
        endDatePicker.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        JLabel endDateLabel = new JLabel("End Date: ");
        endDateLabel.setLabelFor(endDatePicker);
        endDatePicker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				performFreshSearch(false, false, false);
			}
		});
        
        advancedSearch.add(startDateLabel);
        advancedSearch.add(startDatePicker);
        advancedSearch.add(endDateLabel);
        advancedSearch.add(endDatePicker);
        SpringUtilities.makeCompactGrid(advancedSearch, advancedSearch.getComponentCount()/2, 2, 6, 6, 6, 6);
        
        JPanel advancedSearchHolder = new JPanel(new BorderLayout());
        advancedSearchHolder.add(advancedSearch, BorderLayout.WEST);
        
        advancedSearchCheckbox = new JCheckBox("Advanced search options");
        advancedSearchCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				advancedSearch.setVisible(!advancedSearch.isVisible());
			}
		});
        
        JButton infoButton = new JButton(Icons.getIcon("info.png"));
        infoButton.setBorderPainted(false);
        infoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO: make a custom help page later
	            try {
	            	Desktop.getDesktop().browse(new java.net.URI("http://lucene.apache.org/core/old_versioned_docs/versions/3_5_0/queryparsersyntax.html"));
	            }
	            catch (Exception ex) {
	            	SessionMgr.getSessionMgr().handleException(ex);
	            }
			}
		});

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(infoButton, BorderLayout.EAST);

        // Add everything to the input panel
        inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        GridBagConstraints c = new GridBagConstraints();
        
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weightx = c.weighty = 1.0;
        inputPanel.add(searchBox, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = c.weighty = 1.0;
        inputPanel.add(advancedSearchCheckbox, c);

        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.weightx = c.weighty = 1.0;
        inputPanel.add(advancedSearchHolder, c);

        c.gridx = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LAST_LINE_START;
        c.weightx = c.weighty = 1.0;
        inputPanel.add(infoPanel, c);
        
        
        add(inputPanel, BorderLayout.NORTH);
        
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
        
    	SimpleWorker attrLoadingWorker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
				List<EntityAttribute> attrs = ModelMgr.getModelMgr().getEntityAttributes();
				Collections.sort(attrs, new Comparator<EntityAttribute>() {
					@Override
					public int compare(EntityAttribute o1, EntityAttribute o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				for(EntityAttribute attr : attrs) {
					resultsTable.addColumn(SolrUtils.getFieldName(attr.getName()), attr.getName(), false, false, true, true);	
				}
			}

			@Override
			protected void hadSuccess() {
		        populateAttrs();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);		    	
			}
    	};
        
		JTabbedPane leftTabbedPane = new JTabbedPane();
		leftTabbedPane.addTab("Result Filter", null, facetScrollPane, "Values to filter the results on");
		leftTabbedPane.addTab("Attributes", null, attrScrollPane, "Attributes to display in the result table");
		
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
		resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));
		
		for(int i=0; i<columnFields.length; i++) {
			resultsTable.addColumn(columnFields[i], columnLabels[i], i>0, false, true, columnSortable[i]);	
		}
		
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
        
        // --------------------------------
		// Saving interface at bottom
        // --------------------------------
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        exportButton = new JButton("Export to File");
        exportButton.setToolTipText("Save the results");
        exportButton.setEnabled(false);
        exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exportResults();
			}
		});
        buttonPane.add(exportButton);

        buttonPane.add(Box.createHorizontalGlue());        
        
		JLabel folderNameLabel = new JLabel("Save selected objects in folder: ");
		buttonPane.add(folderNameLabel);
		
        folderNameField = new JTextField(10);
        folderNameField.setToolTipText("Enter the folder name to save the results in");
        folderNameField.setMaximumSize(new Dimension(400,20));
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
        
        JButton cancelButton = new JButton("Close");
        cancelButton.setToolTipText("Close this dialog without saving results");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});
        buttonPane.add(cancelButton);
        
        add(buttonPane, BorderLayout.SOUTH);
        
        // Load additional data for the UI
        attrLoadingWorker.execute();
    }
    
	protected void init() {

		// Add the action listener here, because if we add it in the constructor, we get some spurious events
        inputField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Execute the search if the user types a new value and presses Enter, or selects a previous search
				// from the drop down list.
				if ("comboBoxEdited".equals(e.getActionCommand()) || 
						("comboBoxChanged".equals(e.getActionCommand()) && 
								((e.getModifiers() & InputEvent.BUTTON1_MASK)!=0))) {
			        searchString = (String)inputField.getSelectedItem();
					performFreshSearch(true, true, true);
				}
			}
		});
        
		folderNameField.setText(getNextFolderName());
		performFreshSearch(true, true, true);

		Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
		setPreferredSize(new Dimension((int)(browser.getWidth()*0.8),(int)(browser.getHeight()*0.8)));

    	packAndShow();
    	inputField.requestFocus();
    }
    
    public void showDialog() {
    	this.searchRoot = null;
    	titleLabel2.setText(" in all data");
    	init();
    }
    
    public void showDialog(Entity entity) {
    	this.searchRoot = entity;
    	titleLabel2.setText(" in "+searchRoot.getName());
    	init();
    }
    
    protected void performFreshSearch(boolean clearFilters, boolean clearSort, boolean showLoading) {
    	
    	if (searchString==null) return;
    	
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
    
    
    /**
     * Execute the query. This should be run in a worker thread.
     * @param queryString
     * @param page
     * @param pageSize
     * @param fetchFacets
     * @return
     * @throws ComputeException
     */
    protected SolrResults search(SearchParameters params, int page, int pageSize, boolean fetchFacets) throws Exception {
    	
		if (SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("GeneralSearchDialog.search called in the EDT");
		
		if (params.getSearchString()==null) return null;
		
		populateHistory();
		
    	String qs = "-entity_type:Ontology* AND (" + params.getSearchString() +")";
    	
    	if (params.isUseAdvancedParameters()) {
    		qs += " +updated_date:["+params.getStartDateStr()+" TO "+params.getEndDateStr()+"]";
    	}
    	
		SolrQueryBuilder builder = new SolrQueryBuilder();
		builder.setQueryString(qs);
		builder.setUsername(params.getUsername()); 
		if (params.getSearchRoot()!=null) builder.setRootId(params.getSearchRoot().getId());
		SolrQuery query = builder.getQuery();
		query.addField("score");
		query.setStart(pageSize*page);
		query.setRows(pageSize);
		
		if (params.getSortField()!=null) {
			query.setSortField(params.getSortField(), params.isAscending()?ORDER.asc:ORDER.desc);
		}
		
		for(String fieldName : filters.keySet()) {
			Set<String> values = filters.get(fieldName);
			if (values==null||values.isEmpty()) continue;
			query.addFilterQuery(getFilterQuery(fieldName, values));
		}
		
		if (fetchFacets) {
			for(String facet : facets) {
				// Exclude the facet field from itself, to support multi-valued faceting
	    		query.addFacetField("{!ex="+facet+"}"+facet);
			}
		}
		
		return ModelMgr.getModelMgr().searchSolr(query);
    }
    
    protected synchronized void performSearch(final int page, final boolean showLoading) {
    	
    	SimpleWorker worker = new SimpleWorker() {
			
    		private SolrResults results;
    		
			@Override
			protected void doStuff() throws Exception {
				results = search(new SearchParameters(), page, PAGE_SIZE, true);
	    		pages.add(results);
	    		numLoaded += results.getResultList().size();
	    		resultsTable.setMoreResults(results.getResponse().getResults().getNumFound()>numLoaded);
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

    protected synchronized void exportResults() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File Destination");
        chooser.setFileSelectionMode(FileChooser.FILES_ONLY);
        File defaultFile = new File(DEFAULT_DIR,"WorkstationSearchResults.xls");
        
        int i = 1;
        while (defaultFile.exists() && i<10000) {
        	defaultFile = new File(DEFAULT_DIR,"WorkstationSearchResults_"+i+".xls");
        	i++;
        }
        
        chooser.setSelectedFile(defaultFile);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public String getDescription() {
				return "Tab-delimited Files (*.xls, *.txt)";
			}
			@Override
			public boolean accept(File f) {
				return !f.isDirectory();
			}
		});

        if (chooser.showDialog(GeneralSearchDialog.this, "OK") == FileChooser.CANCEL_OPTION) {
            return;
        }

        final String destFile = chooser.getSelectedFile().getAbsolutePath();
        if ((destFile == null) || destFile.equals("")) {
            return;
        }

		final SearchParameters params = new SearchParameters();
		
    	SimpleWorker worker = new SimpleWorker() {
    		
			@Override
			protected void doStuff() throws Exception {
				FileWriter writer = new FileWriter(destFile);

				StringBuffer buf = new StringBuffer();
				for(DynamicColumn column : resultsTable.getDisplayedColumns()) {
					buf.append(column.getLabel());
					buf.append("\t");
				}
				buf.append("\n");
				writer.write(buf.toString());
				
				long numProcessed = 0;
				int page = 0;
				while (true) {
					SolrResults results = search(params, page, EXPORT_PAGE_SIZE, true);
					long numFound = results.getResponse().getResults().getNumFound();
					
					for(EntityDocument entityDoc : results.getEntityDocuments()) {
						buf = new StringBuffer();
						for(DynamicColumn column : resultsTable.getDisplayedColumns()) {
							Object value = getValue(entityDoc, column);
							if (value!=null) {
								buf.append(value.toString());	
							}
							buf.append("\t");
						}
						buf.append("\n");
						writer.write(buf.toString());
						numProcessed++;
						setProgress((int)numProcessed, (int)numFound);
					}
					
					if (numProcessed>=numFound) break;
					page++;
				}
				writer.close();
			}
			
			@Override
			protected void hadSuccess() {
				int rv = JOptionPane.showConfirmDialog(GeneralSearchDialog.this, "Data was successfully exported to "+destFile+". Open file in default viewer?", 
						"Export successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if (rv==JOptionPane.YES_OPTION) {
					OpenWithDefaultAppAction openAction = new OpenWithDefaultAppAction(destFile);
					openAction.doAction();
				}
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

    	worker.setProgressMonitor(new ProgressMonitor(GeneralSearchDialog.this, "Exporting data", "", 0, 100));
		worker.execute();
    }
    
    protected void populateResultView(SolrResults pageResults) {

    	if (pageResults==null) return;
    	
    	statusLabel.setText(pageResults.getResponse().getResults().getNumFound()+" results found for '"+searchString+"'");
    	statusLabel.setToolTipText("Query took "+pageResults.getResponse().getElapsedTime()+" milliseconds");
    	
    	if (pages.size()==1) {
    		// First page, so clear the previous results
			resultsTable.removeAllRows();
    	}	

    	for(EntityDocument entityDoc : pageResults.getEntityDocuments()) {
    		resultsTable.addRow(entityDoc);
    	}    	

    	updateTableModel();
    	exportButton.setEnabled(!pageResults.getResultList().isEmpty());
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
				if (columns.get(i).equals(sortField)) {
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
						performFreshSearch(false, true, true);
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
		for(final DynamicColumn column : resultsTable.getColumns()) {
			
			if (i==0 || i==columnFields.length) {
				attrGroupPanel = new JPanel();
				attrGroupPanel.setOpaque(false);
				attrGroupPanel.setLayout(new BoxLayout(attrGroupPanel, BoxLayout.PAGE_AXIS));
				JLabel attrGroupLabel = new JLabel(i==0 ? "Basic Attributes" : "Extended Attributes");
				attrGroupLabel.setFont(groupFont);
				attrGroupPanel.add(attrGroupLabel);
				attrsPanel.add(Box.createRigidArea(new Dimension(0,10)));
				attrsPanel.add(attrGroupPanel);
			}
			
			final JCheckBox checkBox = new JCheckBox(new AbstractAction(column.getLabel()) {
				public void actionPerformed(ActionEvent e) {
					JCheckBox cb = (JCheckBox) e.getSource();
					column.setVisible(cb.isSelected());
					performFreshSearch(false, false, false);
				}
			});
			
			checkBox.setSelected(column.isVisible());
			checkBox.setFont(checkboxFont);
			attrGroupPanel.add(checkBox);
			i++;
		}
		
		attrsPanel.add(Box.createRigidArea(new Dimension(0,10)));
	}

	protected void populateHistory() {
		DefaultComboBoxModel model = (DefaultComboBoxModel)inputField.getModel();
		
		// Check if the current search term is already recorded in the history
		if (model.getSize()>0 && model.getElementAt(0).equals(searchString)) return;
		
		// Update the model
    	while (model.getSize()>=MAX_HISTORY_LENGTH) {
    		model.removeElementAt(model.getSize()-1);
    	}
    	model.insertElementAt(searchString, 0);
    }

	public void setSearchHistory(List<String> searchHistory) {
		if (searchHistory==null) return;
		DefaultComboBoxModel model = (DefaultComboBoxModel)inputField.getModel();
		model.removeAllElements();
		for(String s : searchHistory) {
			model.addElement(s);
		}
		inputField.setSelectedItem("");
	}
	
	public List<String> getSearchHistory() {
		DefaultComboBoxModel model = (DefaultComboBoxModel)inputField.getModel();
		List<String> searchHistory = new ArrayList<String>();
		for(int i=0; i<model.getSize(); i++) {
			searchHistory.add((String)model.getElementAt(i));
		}
		return searchHistory;
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
     * Return the value of the specified column for the given object.
     * @param userObject
     * @param column
     * @return
     */
	protected Object getValue(Object userObject, DynamicColumn column) {
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

    /**
     * Holder for the current search parameters. This ensures that the search parameters can be reused between
     * pagination calls.
     */
    private class SearchParameters {
    	
    	private final String searchString;
    	private final Entity searchRoot;
    	private final String username;
    	private final Map<String,Set<String>> filters = new HashMap<String,Set<String>>();
    	private final String sortField;
    	private final boolean ascending;
    	private final boolean useAdvancedParameters;
    	private final String startDateStr;
    	private final String endDateStr;
    	
    	public SearchParameters() {
    		this.searchString = GeneralSearchDialog.this.searchString;
    		this.searchRoot = GeneralSearchDialog.this.searchRoot;
    		this.sortField = GeneralSearchDialog.this.sortField;
    		this.ascending = GeneralSearchDialog.this.ascending;
    		// TODO: set this to the user's username once we've tested
    		this.username = "*";
    		filters.putAll(GeneralSearchDialog.this.filters);
    		
    		String startDateStr = "*";
    		String endDateStr = "*";
        	if (advancedSearchCheckbox.isSelected()) {
        		Calendar startCal = (Calendar)startDatePicker.getModel().getValue();
        		if (startCal!=null) {
        			Date startDate = startCal.getTime();
        			startDateStr = SolrUtils.formatDate(startDate);
        		}
        		Calendar endCal = (Calendar)endDatePicker.getModel().getValue();
        		if (endCal!=null) {
        			Date endDate = endCal.getTime();
        			endDateStr = SolrUtils.formatDate(endDate);
        		}
        	}
        	
        	this.startDateStr = startDateStr;
        	this.endDateStr = endDateStr;
        	this.useAdvancedParameters = (!"*".equals(startDateStr) || !"*".equals(endDateStr));
    	}

		public String getSearchString() {
			return searchString;
		}

		public Entity getSearchRoot() {
			return searchRoot;
		}

		public String getUsername() {
			return username;
		}

		public String getSortField() {
			return sortField;
		}
		
		public boolean isAscending() {
			return ascending;
		}

		public boolean isUseAdvancedParameters() {
			return useAdvancedParameters;
		}

		public String getStartDateStr() {
			return startDateStr;
		}

		public String getEndDateStr() {
			return endDateStr;
		}
    }
}