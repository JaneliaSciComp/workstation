package org.janelia.it.FlyWorkstation.gui.dataview;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A panel for displaying entity data objects. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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
    private JComponent loadingView = new JLabel(Icons.getLoadingIcon());

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

        table.addMouseListener(new MouseHandler() {
			@Override
			protected void popupTriggered(MouseEvent e) {
				if (datas==null) return;
                table.setColumnSelectionAllowed(true);
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                table.getSelectionModel().setSelectionInterval(row, row);
                table.getColumnModel().getSelectionModel().setSelectionInterval(col, col);
				showPopupMenu(e);
			}

			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				if (datas==null) return;
                table.setColumnSelectionAllowed(false);
                doubleClick(datas.get(table.getSelectedRow()));
			}

			@Override
			protected void singleLeftClicked(MouseEvent e) {
				if (datas==null) return;
                table.setColumnSelectionAllowed(false);
                table.getColumnModel().getSelectionModel().setSelectionInterval(0, table.getColumnCount());
			}
			
			
        });

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        titleLabel = new JLabel(title);

        setLayout(new BorderLayout());
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void showPopupMenu(MouseEvent e) {

        JTable target = (JTable) e.getSource();
        final String value = target.getValueAt(target.getSelectedRow(), target.getSelectedColumn()).toString();
    	
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JMenuItem copyMenuItem = new JMenuItem("Copy To Clipboard");
        copyMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            Transferable t = new StringSelection(value);
	            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
			}
		});
        popupMenu.add(copyMenuItem);

        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
    }
    
    /**
     * Override this method to provide double click behavior.
     *
     * @param entityData
     */
    protected void doubleClick(EntityData entityData) {
    }

    public void showLoading() {
	    remove(scrollPane);
	    remove(loadingView);
	    add(loadingView, BorderLayout.CENTER);
	    updateUI();
    }

    public void showEmpty() {
        titleLabel.setText(title);
	    remove(loadingView);
        remove(scrollPane);
        updateUI();
    }

    /**
     * Async method for loading and displaying entities of a given type.
     */
    public void showEntityData(final List<EntityData> datas) {

        if (loadTask != null && !loadTask.isDone()) {
            System.out.println("Cancel current entity data load for " + title);
            loadTask.cancel(true);
        }

        showLoading();

        loadTask = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (EntityData data : datas) {
                    Entity child = data.getChildEntity();
                    if (child != null && !EntityUtils.isInitialized(child)) {
                    	System.out.println("Fetching child "+child.getId());
                        data.setChildEntity(ModelMgr.getModelMgr().getEntityById(child.getId().toString()));
                    }
                    Entity parent = data.getParentEntity();
                    if (parent != null && !EntityUtils.isInitialized(parent)) {
                    	System.out.println("Fetching parent "+parent.getId());
                        data.setParentEntity(ModelMgr.getModelMgr().getEntityById(parent.getId().toString()));
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
        for (EntityData entityData : datas) {
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
