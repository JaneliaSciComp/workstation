package org.janelia.it.FlyWorkstation.gui.dataview;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

public class EntityDataPane extends JPanel {

    private final List<String> staticColumns = new ArrayList<String>();
    private final String title;
    private boolean showParent;
    private boolean showChild;
    
    private final JTable table;
    private final JLabel titleLabel;
    private final JScrollPane scrollPane;
    private List<EntityData> datas;
    private TableModel tableModel;
    private JComponent loadingView = new JLabel(Icons.loadingIcon);
    
    private SimpleWorker loadTask;
    
    
    public EntityDataPane(String title, boolean showParent, boolean showChild) {
    	
    	this.title = title;
    	this.showParent = showParent;
    	this.showChild = showChild;
    	
        staticColumns.add("Attribute Name");
        staticColumns.add("Id");
        staticColumns.add("User");
        staticColumns.add("Updated Date");
        staticColumns.add("Order");
        if (showParent) staticColumns.add("Parent");
        if (showChild) staticColumns.add("Child");
        staticColumns.add("Value");
        
        table = new JTable();
        table.setFillsViewportHeight(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if (e.getClickCount() == 2) {
                    JTable target = (JTable)e.getSource();
                    int row = target.getSelectedRow();
                    if (row >= 0 && row<datas.size()) {
                        doubleClick(datas.get(row));
                    }
                }
            }
        });

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        titleLabel = new JLabel(title);

        setLayout(new BorderLayout());
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Override this method to provide double click behavior.
     * @param entityData
     */
    protected void doubleClick(EntityData entityData) {
    }
    
	public void showLoading() {
        remove(scrollPane);
        add(loadingView,BorderLayout.CENTER);
        repaint();
    }

    public void showEmpty() {
        titleLabel.setText(title);
        remove(scrollPane);
        repaint();
    }

    /**
     * Async method for loading and displaying entities of a given type.
     */
    public void showEntityData(final List<EntityData> datas) {
		
    	if (loadTask != null && !loadTask.isDone()) {
    		System.out.println("Cancel current entity data load for "+title);
    		loadTask.cancel(true);
    	}
		
        showLoading();

        loadTask = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
            	for(EntityData data : datas) {
            		Entity child = data.getChildEntity();
            		if (child != null) {
            			data.setChildEntity(EJBFactory.getRemoteAnnotationBean().getEntityById(child.getId().toString()));	
            		}
            		Entity parent = data.getParentEntity();
            		if (parent != null) {
            			data.setParentEntity(EJBFactory.getRemoteAnnotationBean().getEntityById(parent.getId().toString()));	
            		}
            		if (isCancelled()) return;
            	}
        	    
                tableModel = updateTableModel(datas);
			}
			
			@Override
			protected void hadSuccess() {
                if (tableModel == null) {
                    System.out.println("TableModel was null");
                    return;
                }
                else {
                    table.setModel(tableModel);
                    Utils.autoResizeColWidth(table);
                }
                remove(loadingView);
                add(scrollPane, BorderLayout.CENTER);
			}
			
			@Override
			protected void hadError(Throwable error) {
				error.printStackTrace();
			}
			
		};

        loadTask.execute();
    }


    /**
     *
     *
     * Synchronous method for updating the JTable model. Should be called from the EDT.
     */
    private TableModel updateTableModel(List<EntityData> dataSet) {

        datas = dataSet;

        // Data formatted for the JTable
        Vector<String> columnNames = new Vector<String>();
        Vector<Vector<String>> data = new Vector<Vector<String>>();

        // Prepend the static columns
        columnNames.addAll(staticColumns);

        // Build the data in column order
        for(EntityData entityData : datas) {
            Vector<String> rowData = new Vector<String>();
            rowData.add((entityData.getEntityAttribute() == null) ? "" : entityData.getEntityAttribute().getName());
            rowData.add(entityData.getId().toString());
            rowData.add((entityData.getUser() == null) ? "" : entityData.getUser().getUserLogin());
            rowData.add((entityData.getUpdatedDate() == null) ? "" : entityData.getUpdatedDate().toString());
            rowData.add((entityData.getOrderIndex() == null) ? "" : entityData.getOrderIndex().toString());
            if (showParent) {
            	rowData.add((entityData.getParentEntity() == null) ? "" : (entityData.getParentEntity().getName() == null) ? "(unnamed)" : entityData.getParentEntity().getName().toString());
            }
            if (showChild) {
            	rowData.add((entityData.getChildEntity() == null) ? "" : (entityData.getChildEntity().getName() == null) ? "(unnamed)" : entityData.getChildEntity().getName().toString());
            }
            rowData.add(entityData.getValue());
            data.add(rowData);
        }

        return new DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int rowIndex, int mColIndex) {
                return false;
            }
        };
    }

}
