package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

public class EntityListPane extends JPanel {

    private final EntityDataPane entityParentsPane;
    private final EntityDataPane entityChildrenPane;
    private final List<String> staticColumns = new ArrayList<String>();
    private final JTable table;
    private final JLabel titleLabel;
    private final JScrollPane scrollPane;
    private List<Entity> entities;
    private TableModel tableModel;
    private SimpleWorker loadTask;
    private JComponent loadingView = new JLabel(Icons.getLoadingIcon());

    private EntityType shownEntityType;
    private Entity shownEntity;

    public EntityListPane(final EntityDataPane entityParentsPane, final EntityDataPane entityChildrenPane) {

        this.entityParentsPane = entityParentsPane;
        this.entityChildrenPane = entityChildrenPane;

        staticColumns.add("Id");
        staticColumns.add("User");
        staticColumns.add("Creation Date");
        staticColumns.add("Updated Date");
        staticColumns.add("Name");

        table = new JTable();
        table.setFillsViewportHeight(true);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        table.addMouseListener(new MouseHandler() {	
			@Override
			protected void popupTriggered(MouseEvent e) {
				ListSelectionModel lsm = table.getSelectionModel();
				if (lsm.getAnchorSelectionIndex() == lsm.getLeadSelectionIndex()) {
					// User is not selecting multiple rows, so we can select the cell they right clicked on
	                table.setColumnSelectionAllowed(true);
	                int row = table.rowAtPoint(e.getPoint());
	                int col = table.columnAtPoint(e.getPoint());
	                table.getSelectionModel().setSelectionInterval(row, row);
	                table.getColumnModel().getSelectionModel().setSelectionInterval(col, col);
				}
				showPopupMenu(e);
			}
			@Override
			protected void singleLeftClicked(MouseEvent e) {
                table.setColumnSelectionAllowed(false);
                table.getColumnModel().getSelectionModel().setSelectionInterval(0, table.getColumnCount());
                int row = table.getSelectedRow();
                if (row>=0) populateEntityDataPanes(entities.get(row));
			}
        });

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        titleLabel = new JLabel("Entity");

        setLayout(new BorderLayout());
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void showPopupMenu(MouseEvent e) {

        final int num = table.getSelectedRows().length;
        JTable target = (JTable) e.getSource();
        final String value = target.getValueAt(target.getSelectedRow(), target.getSelectedColumn()).toString();
    	
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        ListSelectionModel lsm = table.getSelectionModel();
		if (lsm.getAnchorSelectionIndex() == lsm.getLeadSelectionIndex()) {
			
			// Items which are  only available when selecting a single cell
			
	        JMenuItem copyMenuItem = new JMenuItem("Copy to clipboard");
	        copyMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
		            Transferable t = new StringSelection(value);
		            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
				}
			});
	        popupMenu.add(copyMenuItem);
	        
	        JMenuItem renameMenuItem = new JMenuItem("Rename...");
	        renameMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Entity toRename = entities.get(table.getSelectedRow());
					
		            String newName = (String) JOptionPane.showInputDialog(EntityListPane.this, "Name:\n", "Rename entity", 
		            		JOptionPane.PLAIN_MESSAGE, null, null, toRename.getName());

		            if ((newName == null) || (newName.length() <= 0)) {
		                return;
		            }

		            Utils.setWaitingCursor(DataviewApp.getMainFrame());
		            
		            try {
		            	toRename.setName(newName);
		            	ModelMgr.getModelMgr().saveOrUpdateEntity(toRename);
	    	            reshow();
	    	            Utils.setDefaultCursor(DataviewApp.getMainFrame());
		            }
					catch (Exception x) {
						x.printStackTrace();
	                    error("Error renaming entity: "+x.getMessage());
					}
					
				}
			});
	        popupMenu.add(renameMenuItem);
	    }

        JMenuItem deleteTreeMenuItem = new JMenuItem("Delete tree");
        deleteTreeMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	            int deleteConfirmation = confirm("Are you sure you want to delete " + num + " entities and all their descendants?");
	            if (deleteConfirmation != 0) {
	                return;
	            }

	            final List<Entity> toDelete = new ArrayList<Entity>();
	            for (int i : table.getSelectedRows()) {
	                toDelete.add(entities.get(i));
	            }

            	boolean su = false;
	            for (Entity entity : toDelete) {
                	if (!SessionMgr.getUsername().equals(entity.getUser().getUserLogin())) {
        	            int overrideConfirmation = confirm("Override owner "+entity.getUser().getUserLogin()+" to delete "+entity.getName()+"?");
        	            if (overrideConfirmation != 0) {
        	                continue;
        	            }
        	            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.USER_NAME, entity.getUser().getUserLogin());
        	            su = true;
        	            break;
                	}
	            }
	            
	            final boolean didSu = su;
	            final String realUsername = SessionMgr.getUsername();
	            
	            Utils.setWaitingCursor(DataviewApp.getMainFrame());
	            
	            SimpleWorker loadTask = new SimpleWorker() {

	                @Override
	                protected void doStuff() throws Exception {
	    	            // Update database
	    	            for (Entity entity : toDelete) {
	    	            	System.out.println("Deleting "+entity.getId());
    	                    ModelMgr.getModelMgr().deleteEntityTree(entity.getId());
	    	            }
	                }

	                @Override
	                protected void hadSuccess() {
	                    if (didSu) {
	        	            SessionMgr.getSessionMgr().setModelProperty(SessionMgr.USER_NAME, realUsername);
	                    }
	                	Utils.setDefaultCursor(DataviewApp.getMainFrame());
	    	            reshow();
	                }

	                @Override
	                protected void hadError(Throwable error) {
	                    error.printStackTrace();
	                    error("Error deleting entity tree: "+error.getMessage());
	                }

	            };

	            loadTask.execute();
			}
		});
        popupMenu.add(deleteTreeMenuItem);
        
        JMenuItem deleteEntityMenuItem = new JMenuItem("Delete entity");
        deleteEntityMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	            int deleteConfirmation = confirm("Are you sure you want to delete " + num + 
	            		" entities? This can potentially orphan their children, if they have any.");
	            if (deleteConfirmation != 0) return;

	            final List<Entity> toDelete = new ArrayList<Entity>();
	            for (int i : table.getSelectedRows()) {
	                toDelete.add(entities.get(i));
	            }

	            Utils.setWaitingCursor(DataviewApp.getMainFrame());
	            
	            SimpleWorker loadTask = new SimpleWorker() {

	                @Override
	                protected void doStuff() throws Exception {
    		            // Update database
    		            for (Entity entity : toDelete) {
    		                boolean success = ModelMgr.getModelMgr().deleteEntityById(entity.getId());
    		                if (!success) {
    		                    error("Error deleting entity with id=" + entity.getId());
    		                }
    		            }
	                }

	                @Override
	                protected void hadSuccess() {
	                	Utils.setDefaultCursor(DataviewApp.getMainFrame());
	    	            reshow();
	                }

	                @Override
	                protected void hadError(Throwable error) {
	                    error.printStackTrace();
	                    error("Error deleting entity: "+error.getMessage());
	                }

	            };

	            loadTask.execute();
			}
		});
        popupMenu.add(deleteEntityMenuItem);
        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
    }
    
    private void populateEntityDataPanes(final Entity entity) {

        System.out.println("Populate data panes with " + entity);

        entityChildrenPane.showEntityData(entity.getOrderedEntityData());
        entityParentsPane.showLoading();
        
        loadTask = new SimpleWorker() {

            List<EntityData> eds;

            @Override
            protected void doStuff() throws Exception {
                eds = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());
            }

            @Override
            protected void hadSuccess() {
                entityParentsPane.showEntityData(eds);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }

        };

        loadTask.execute();
    }

    public void showLoading() {
        remove(scrollPane);
        add(loadingView, BorderLayout.CENTER);
	    updateUI();
    }

    public void reshow() {
        if (shownEntity != null) {
            showEntity(shownEntity);
        }
        else if (shownEntityType != null) {
            showEntities(shownEntityType);
        }
    }

    /**
     * Async method for loading and displaying entities of a given type.
     */
    public void showEntities(final EntityType entityType) {

        shownEntityType = entityType;
        shownEntity = null;

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity type load");
            loadTask.cancel(true);
        }

        System.out.println("Loading entities of type " + entityType.getName());

        titleLabel.setText("Entity: " + entityType.getName());
        showLoading();

        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                List<Entity> entities = ModelMgr.getModelMgr().getEntitiesByTypeName(entityType.getName());
                if (isCancelled()) return;
                updateTableModel(entities);
            }

            @Override
            protected void hadSuccess() {
                table.setModel(tableModel);
                Utils.autoResizeColWidth(table);
                remove(loadingView);
                add(scrollPane, BorderLayout.CENTER);
                entityParentsPane.showEmpty();
                entityChildrenPane.showEmpty();
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }

        };

        loadTask.execute();
    }

    public void showEntities(final List<Entity> entities) {

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity type load");
            loadTask.cancel(true);
        }

        titleLabel.setText("Entity Search Results");
        showLoading();

        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (isCancelled()) return;
                updateTableModel(entities);
            }

            @Override
            protected void hadSuccess() {
                table.setModel(tableModel);
                Utils.autoResizeColWidth(table);
                remove(loadingView);
                add(scrollPane, BorderLayout.CENTER);
                entityParentsPane.showEmpty();
                entityChildrenPane.showEmpty();
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }

        };

        loadTask.execute();
    }

    public void showEntity(final Entity entity) {

        shownEntityType = null;
        shownEntity = entity;

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity load");
            loadTask.cancel(true);
        }

        System.out.println("Loading entity " + entity.getName());
        showLoading();
        entityParentsPane.showEmpty();
        entityChildrenPane.showEmpty();

        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                titleLabel.setText("Entity: " + entity.getEntityType().getName() + " (" + entity.getName() + ")");
                List<Entity> entities = new ArrayList<Entity>();
                entities.add(entity);
                updateTableModel(entities);
            }

            @Override
            protected void hadSuccess() {
                table.setModel(tableModel);
                Utils.autoResizeColWidth(table);
                remove(loadingView);
                add(scrollPane, BorderLayout.CENTER);
                table.getSelectionModel().setSelectionInterval(0, 0);
                populateEntityDataPanes(entity);
            }

            @Override
            protected void hadError(Throwable error) {
                error.printStackTrace();
            }

        };

        loadTask.execute();
    }

    /**
     * Synchronous method for updating the JTable model. Should be called from the EDT.
     */
    private void updateTableModel(List<Entity> entityList) {

        this.entities = (entityList == null) ? new ArrayList<Entity>() : entityList;
        
        Collections.sort(entities, new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        // Data formatted for the JTable
        Vector<String> columnNames = new Vector<String>(staticColumns);
        Vector<Vector<String>> data = new Vector<Vector<String>>();

        // Build the data in column order
        for (Entity entity : entities) {
            Vector<String> rowData = new Vector<String>();
            rowData.add(entity.getId().toString());
            rowData.add((entity.getUser() == null) ? "" : entity.getUser().getUserLogin());
            rowData.add((entity.getCreationDate() == null) ? "" : entity.getCreationDate().toString());
            rowData.add((entity.getUpdatedDate() == null) ? "" : entity.getUpdatedDate().toString());
            rowData.add((entity.getName() == null) ? "(unnamed)" : entity.getName().toString());
            data.add(rowData);
        }

        tableModel = new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int rowIndex, int mColIndex) {
                return false;
            }
        };
    }

    private int confirm(String message) {
    	return JOptionPane.showConfirmDialog(EntityListPane.this, message, "Are you sure?", JOptionPane.YES_NO_OPTION);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(EntityListPane.this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
}