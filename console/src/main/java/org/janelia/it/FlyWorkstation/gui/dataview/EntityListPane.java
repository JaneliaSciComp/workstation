package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
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
    private JComponent loadingView = new JLabel(Icons.loadingIcon);
    
    public EntityListPane(final EntityDataPane entityParentsPane, final EntityDataPane entityChildrenPane) {

        this.entityParentsPane = entityParentsPane;
        this.entityChildrenPane = entityChildrenPane;

        staticColumns.add("Id");
        staticColumns.add("User");
        //staticColumns.add("Status");
        staticColumns.add("Creation Date");
        staticColumns.add("Updated Date");
        staticColumns.add("Name");

        table = new JTable();
        table.setFillsViewportHeight(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if (e.getClickCount() == 1) {
                    JTable target = (JTable)e.getSource();
                    int row = target.getSelectedRow();
                    if (row >= 0 && row<entities.size()) {
                    	populateEntityDataPanes(entities.get(row));
                    }
                }
            }
        });
        
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        titleLabel = new JLabel("Entity");

        setLayout(new BorderLayout());
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void populateEntityDataPanes(final Entity entity) {
    	
    	System.out.println("Populate data panes with "+entity);
    	
    	entityChildrenPane.showEntityData(entity.getOrderedEntityData());

        loadTask = new SimpleWorker() {
        	
        	List<EntityData> eds;
        	
			@Override
			protected void doStuff() throws Exception {
				eds = new ArrayList(EJBFactory.getRemoteAnnotationBean().getParentEntityDatas(entity.getId()));
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
        add(loadingView,BorderLayout.CENTER);
        repaint();
    }

    /**
     * Async method for loading and displaying entities of a given type. 
     */
    public void showEntities(final EntityType entityType) {

    	if (loadTask != null && !loadTask.isDone()) {
    		System.out.println("Cancel current entity type load");
    		loadTask.cancel(true);
    	}

		System.out.println("Loading entities of type "+entityType.getName());
		
        titleLabel.setText("Entity: "+entityType.getName());
        showLoading();

        loadTask = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
        		List<Entity> entities = EJBFactory.getRemoteAnnotationBean().getEntitiesByType(entityType.getId());
        		if (isCancelled()) return;
        		tableModel = updateTableModel(entities);
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

    	if (loadTask != null && !loadTask.isDone()) {
    		System.out.println("Cancel current entity load");
    		loadTask.cancel(true);
    	}

		System.out.println("Loading entity "+entity.getName());
		showLoading();
        entityParentsPane.showEmpty();
        entityChildrenPane.showEmpty();
		 
        loadTask = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
		        titleLabel.setText("Entity: "+entity.getEntityType().getName()+" ("+entity.getName()+")");
		        List<Entity> entities = new ArrayList<Entity>();
		        entities.add(entity);
				tableModel = updateTableModel(entities);
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
    private TableModel updateTableModel(List<Entity> entities) {

        this.entities = entities;

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
        for(Entity entity : entities) {
            Vector<String> rowData = new Vector<String>();
            rowData.add(entity.getId().toString());
            rowData.add((entity.getUser() == null) ? "" : entity.getUser().getUserLogin());
            //rowData.add((entity.getEntityStatus() == null) ? "" : entity.getEntityStatus().getName());
            rowData.add((entity.getCreationDate() == null) ? "" : entity.getCreationDate().toString());
            rowData.add((entity.getUpdatedDate() == null) ? "" : entity.getUpdatedDate().toString());
            rowData.add((entity.getName() == null) ? "(unnamed)" : entity.getName().toString());
            data.add(rowData);
        }
        
        return new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int rowIndex, int mColIndex) {
                return false;
            }
        };
    }

}