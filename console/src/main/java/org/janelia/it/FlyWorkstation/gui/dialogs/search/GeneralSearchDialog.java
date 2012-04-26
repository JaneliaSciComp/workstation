package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OpenWithDefaultAppAction;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicRow;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.compute.api.support.EntityDocument;
import org.janelia.it.jacs.compute.api.support.SolrQueryBuilder;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.file_chooser.FileChooser;


/**
 * A dialog for performing general searches. Allows for saving the results to the entity model,
 * or exporting them to a tab-delimited file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GeneralSearchDialog extends ModalDialog {

	/** Default directory for exports */
	protected static final String DEFAULT_EXPORT_DIR = System.getProperty("user.home");

	/** How many results to load at a time when exporting */
	protected static final int EXPORT_PAGE_SIZE = 1000;

	protected final SearchConfiguration searchConfig;
	
    // UI Elements
    protected final SearchParametersPanel paramsPanel;
    protected final SearchResultsPanel resultsPanel;
    protected final JTextField folderNameField;
    protected final JButton exportButton;
	
    
    public GeneralSearchDialog() {
    	setLayout(new BorderLayout());
        setTitle("Search");
        
        paramsPanel = new SearchParametersPanel() {
        	@Override
        	public void performSearch(boolean clear) {
        		super.performSearch(clear);
        		resultsPanel.performSearch(clear, clear, true);
        	}
        };
        
        add(paramsPanel, BorderLayout.NORTH);
        
        resultsPanel = new SearchResultsPanel() {
        	@Override
        	protected void populateResultView(ResultPage resultPage) {
        		super.populateResultView(resultPage);
            	exportButton.setEnabled(!resultPage.getSolrResults().getResultList().isEmpty());
        	}
        	@Override
        	protected SolrQueryBuilder getQueryBuilder() {
        		return paramsPanel.getQueryBuilder();
        	}
        	@Override
        	protected JPopupMenu getPopupMenu(List<Entity> selectedEntites, String label) {
        		return GeneralSearchDialog.this.getPopupMenu(selectedEntites, label);
        	}
    		
        };
        add(resultsPanel, BorderLayout.CENTER);
        
        searchConfig = new SearchConfiguration();
        searchConfig.addConfigurationChangeListener(paramsPanel);
        searchConfig.addConfigurationChangeListener(resultsPanel);
        searchConfig.load();
        
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
    }
    
	protected void init() {
		
		folderNameField.setText(getNextFolderName());

		Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
		setPreferredSize(new Dimension((int)(browser.getWidth()*0.8),(int)(browser.getHeight()*0.8)));

    	paramsPanel.getInputField().requestFocus();
		paramsPanel.init();
		
    	resultsPanel.performSearch(false, false, true);

    	getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true),"searchAction");
    	getRootPane().getActionMap().put("searchAction",new AbstractAction("searchAction") {
			@Override
			public void actionPerformed(ActionEvent e) {
				paramsPanel.performSearch(true);
			}
		});
		
    	packAndShow();
    }
    
    public void showDialog() {
    	paramsPanel.setSearchRoot(null);
    	init();
    }
    
    public void showDialog(Entity entity) {
    	paramsPanel.setSearchRoot(entity);
    	init();
    }

	protected SearchResultContextMenu getPopupMenu(List<Entity> selectedEntities, String label) {
		return new SearchResultContextMenu(resultsPanel, selectedEntities, label);
	}
	
    public void setSearchHistory(List<String> searchHistory) {
    	paramsPanel.setSearchHistory(searchHistory);
    }
    
    public List<String> getSearchHistory() {
    	return paramsPanel.getSearchHistory();
    }
	
    protected synchronized void saveResults() {
    	
    	SimpleWorker worker = new SimpleWorker() {

    		private Entity newFolder;
    		
			@Override
			protected void doStuff() throws Exception {

				String folderName = folderNameField.getText();
				this.newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
				newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
				newFolder = ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);

				List<Long> childIds = new ArrayList<Long>();
				SearchResults searchResults = resultsPanel.getSearchResults();
				DynamicTable table = searchResults.getResultTreeMapping()==null?resultsPanel.getResultsTable():resultsPanel.getMappedResultsTable();
				
				for(DynamicRow row : table.getSelectedRows()) {
					Object o = row.getUserObject();
					Entity entity = null;
					if (o instanceof Entity) {
						entity = (Entity)o;
					}
					else if (o instanceof EntityDocument) {
						entity = ((EntityDocument)o).getEntity();
					}
					childIds.add(entity.getId());
				}
				
				ModelMgr.getModelMgr().addChildren(newFolder.getId(), childIds, EntityConstants.ATTRIBUTE_ENTITY);
			}
			
			@Override
			protected void hadSuccess() {
				final EntityOutline entityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
				entityOutline.refresh(true, new Callable<Void>() {
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
	
    protected synchronized void exportResults() {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File Destination");
        chooser.setFileSelectionMode(FileChooser.FILES_ONLY);
        File defaultFile = new File(DEFAULT_EXPORT_DIR,"WorkstationSearchResults.xls");
        
        int i = 1;
        while (defaultFile.exists() && i<10000) {
        	defaultFile = new File(DEFAULT_EXPORT_DIR,"WorkstationSearchResults_"+i+".xls");
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

        final SolrQueryBuilder builder = resultsPanel.getQueryBuilder(false);
        
    	SimpleWorker worker = new SimpleWorker() {
    		
			@Override
			protected void doStuff() throws Exception {
				FileWriter writer = new FileWriter(destFile);

				StringBuffer buf = new StringBuffer();
				for(DynamicColumn column : resultsPanel.getResultsTable().getDisplayedColumns()) {
					buf.append(column.getLabel());
					buf.append("\t");
				}
				buf.append("\n");
				writer.write(buf.toString());
				
				long numProcessed = 0;
				int page = 0;
				while (true) {
					SolrResults results = resultsPanel.performSearch(builder, page, EXPORT_PAGE_SIZE);
					long numFound = results.getResponse().getResults().getNumFound();
					
					for(EntityDocument entityDoc : results.getEntityDocuments()) {
						buf = new StringBuffer();
						for(DynamicColumn column : resultsPanel.getResultsTable().getDisplayedColumns()) {
							Object value = resultsPanel.getValue(entityDoc, column);
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
}