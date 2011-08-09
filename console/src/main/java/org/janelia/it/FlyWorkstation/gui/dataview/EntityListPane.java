package org.janelia.it.FlyWorkstation.gui.dataview;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class EntityListPane extends JPanel implements ActionListener {

    private static final String DELETE_ENTITY_COMMAND = "delete_entity";
    private static final String DELETE_TREE_COMMAND = "delete_tree";

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

    private EntityType shownEntityType;
    private Entity shownEntity;

    public EntityListPane(final EntityDataPane entityParentsPane, final EntityDataPane entityChildrenPane) {

        this.entityParentsPane = entityParentsPane;
        this.entityChildrenPane = entityChildrenPane;

        staticColumns.add("Id");
        staticColumns.add("User");
        //staticColumns.add("Status");
        staticColumns.add("Creation Date");
        staticColumns.add("Updated Date");
        staticColumns.add("Name");


        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JMenuItem deleteEntityMenuItem = new JMenuItem("Delete entity");
        deleteEntityMenuItem.addActionListener(this);
        deleteEntityMenuItem.setActionCommand(DELETE_ENTITY_COMMAND);
        popupMenu.add(deleteEntityMenuItem);

        JMenuItem deleteTreeMenuItem = new JMenuItem("Delete tree");
        deleteTreeMenuItem.addActionListener(this);
        deleteTreeMenuItem.setActionCommand(DELETE_TREE_COMMAND);
        popupMenu.add(deleteTreeMenuItem);

        table = new JTable();
        table.setFillsViewportHeight(true);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JTable target = (JTable) e.getSource();
                int row = target.getSelectedRow();
                if (row >= 0 && row < entities.size()) {
                    if (e.isPopupTrigger()) {
                        // Right click
                        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                JTable target = (JTable) e.getSource();
                int row = target.getSelectedRow();
                if (row >= 0 && row < entities.size()) {
                    if (e.isPopupTrigger()) {
                        // Right click
                        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
                    }
                    else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        // Double click
                    }
                    else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                        // Single click
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

        System.out.println("Populate data panes with " + entity);

        entityChildrenPane.showEntityData(entity.getOrderedEntityData());

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
        repaint();
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
                List<Entity> entities = ModelMgr.getModelMgr().getEntitiesByType(entityType.getId());
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
    private void updateTableModel(List<Entity> entities) {

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
        for (Entity entity : entities) {
            Vector<String> rowData = new Vector<String>();
            rowData.add(entity.getId().toString());
            rowData.add((entity.getUser() == null) ? "" : entity.getUser().getUserLogin());
            //rowData.add((entity.getEntityStatus() == null) ? "" : entity.getEntityStatus().getName());
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

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        int num = table.getSelectedRows().length;

        if (DELETE_ENTITY_COMMAND.equals(command)) {

            int deleteConfirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + num + " entities?", "Delete Term", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }

            List<Entity> toDelete = new ArrayList<Entity>();
            for (int i : table.getSelectedRows()) {
                toDelete.add(entities.get(i));
            }

            // Update database
            // TODO: do this in a worker thread and show a spinner, because it may take a long time
            for (Entity entity : toDelete) {
                boolean success = ModelMgr.getModelMgr().deleteEntityById(entity.getId());
                if (!success) {
                    JOptionPane.showMessageDialog(this, "Error deleting entity with id=" + entity.getId(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            reshow();
        }
        else if (DELETE_TREE_COMMAND.equals(command)) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + num + " entities and all their descendants?", "Delete Term", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }

            List<Entity> toDelete = new ArrayList<Entity>();
            for (int i : table.getSelectedRows()) {
                toDelete.add(entities.get(i));
            }

            // Update database
            // TODO: do this in a worker thread and show a spinner, because it may take a long time
            for (Entity entity : toDelete) {
                try {
                    // TODO: allow dataviewer user to override owner?
                    ModelMgr.getModelMgr().deleteEntityTree(entity.getId());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error deleting entity with id=" + entity.getId(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            reshow();
        }
    }
}