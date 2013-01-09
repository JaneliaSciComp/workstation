package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeModel;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.ab.AlignedEntity;
import org.janelia.it.FlyWorkstation.gui.framework.outline.ab.AlignedItem;
import org.janelia.it.FlyWorkstation.gui.framework.outline.ab.AlignedItemFolder;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayersPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(LayersPanel.class);

    private Outline outline;
    
    private void updateTableModel(OutlineModel outlineModel) {

        outline.setModel(outlineModel);
        
        TableColumnModel columnModel = outline.getColumnModel();
        TableColumn[] columns = new TableColumn[columnModel.getColumnCount()];
        
        for(int i=0; i<columnModel.getColumnCount(); i++) {
            TableColumn tableColumn = columnModel.getColumn(i);
            columns[i] = tableColumn;
        }
        
        for(int i=0; i<columns.length; i++) {
          columnModel.removeColumn(columns[i]);
        }
        
        TableColumn first = columns[0];
        columns[0] = columns[1];
        columns[1] = first;

        for(int i=0; i<columns.length; i++) {
            columnModel.addColumn(columns[i]);
        }

    }
    
    public LayersPanel() {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));

        TreeModel treeModel = new SampleTreeModel(new ArrayList<AlignedEntity>());
        OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(treeModel, new AlignedEntityRowModel(), true, "Name");
        
        outline = new Outline();
        outline.setRootVisible(false);
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outline.setColumnHidingAllowed(false);
        updateTableModel(outlineModel);
        
        addComponentListener(new ComponentListener() {
            
            @Override
            public void componentShown(ComponentEvent e) {
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
                TableColumnModel columnModel = outline.getColumnModel();
                columnModel.getColumn(0).setPreferredWidth(30);
                columnModel.getColumn(1).setPreferredWidth(getWidth()-100);
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {
            }
            
            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
        
        
        add(new JScrollPane(outline),BorderLayout.CENTER);
    }

    public void showEntities(final List<Entity> entities) {
        
        SimpleWorker worker = new SimpleWorker() {
            
            List<AlignedEntity> alignedEntities = new ArrayList<AlignedEntity>();
            
            @Override
            protected void doStuff() throws Exception {
                
                for(Entity entity : entities) {
                    ModelMgr.getModelMgr().loadLazyEntity(entity, true);
                    alignedEntities.add(new AlignedEntity(entity));
                }
            }

            @Override
            protected void hadSuccess() {
                TreeModel treeModel = new SampleTreeModel(alignedEntities);
                OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(treeModel, new AlignedEntityRowModel(), true, "Name");
                updateTableModel(outlineModel);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
    }
    
    @Override
    public void refresh() {
        
    }

    @Override
    public void totalRefresh() {
    }
    
    private static class EntityCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            
            
            
            return this;
        }
    }
    
    private static class SampleTreeModel implements TreeModel {
        
        private AlignedItem root;
        
        public SampleTreeModel(List<AlignedEntity> entities) {
            this.root = new AlignedItemFolder("", entities);
        }

        @Override
        public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        }

        @Override
        public Object getChild(Object parent, int index) {
            AlignedItem item = (AlignedItem)parent;
            return item.getChildren().get(index);
        }

        @Override
        public int getChildCount(Object parent) {
            AlignedItem item = (AlignedItem)parent;
            return item.getChildren().size();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            AlignedItem item = (AlignedItem)parent;
            return item.getChildren().indexOf(child);
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public boolean isLeaf(Object node) {
            return getChildCount(node)==0;
        }

        @Override
        public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        }

        @Override
        public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
        }

    }
    
    private class AlignedEntityRowModel implements RowModel {

        private Map<Object,Boolean> visibility = new HashMap<Object,Boolean>();
        
        @Override
        public Class getColumnClass(int column) {
            switch (column) {
                case 0:
                    return Boolean.class;
                default:
                    assert false;
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getValueFor(Object node, int column) {
//            AlignedEntity alignedEntity = (AlignedEntity) node;
            switch (column) {
                case 0:
                    Boolean v = visibility.get(node);
                    return v==null?false:v;
                default:
                    assert false;
            }
            return null;
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column==0;
        }

        @Override
        public void setValueFor(Object node, int column, Object value) {
            if (column==0) {
                visibility.put(node, (Boolean)value);
            }
        }
    }
}
