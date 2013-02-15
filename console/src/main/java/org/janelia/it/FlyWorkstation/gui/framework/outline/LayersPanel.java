package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeModel;

import org.janelia.it.FlyWorkstation.api.entity_model.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.FlyWorkstation.api.entity_model.events.AlignmentBoardOpenEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.jacs.model.entity.Entity;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayersPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(LayersPanel.class);

    private AlignmentBoardContext alignmentBoardContext;
    private Outline outline;
    
    public LayersPanel() {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));

        outline = new Outline();
        outline.setRootVisible(false);
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outline.setColumnHidingAllowed(false);

        OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(new SampleTreeModel(), new AlignedEntityRowModel(), true, "Name");
        updateTableModel(outlineModel);
        
        addComponentListener(new ComponentListener() {
            
            @Override
            public void componentShown(ComponentEvent e) {
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
                if (outline==null) return;
                TableColumnModel columnModel = outline.getColumnModel();
                if (columnModel.getColumnCount()<2) return;
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

    public AlignmentBoardContext getAlignmentBoardContext() {
        return alignmentBoardContext;
    }
    
    public void openAlignmentBoard(RootedEntity rootedEntity) {
        this.alignmentBoardContext = new AlignmentBoardContext(rootedEntity);
        
        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                alignmentBoardContext.loadContextualizedChildren(null);
            }

            @Override
            protected void hadSuccess() {

                OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(new SampleTreeModel(), new AlignedEntityRowModel(), true, "Name");
                updateTableModel(outlineModel);

                AlignmentBoardOpenEvent event = new AlignmentBoardOpenEvent(alignmentBoardContext);
                ModelMgr.getModelMgr().postOnEventBus(event);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();   
    }
    
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
        TableColumn second = columns[1];
        
        // Swap first and second columns, to bring the checkbox to the front
        columns[0] = second;
        columns[1] = first;

        for(int i=0; i<columns.length; i++) {
            columnModel.addColumn(columns[i]);
        }

        first.setCellRenderer(new EntityCellRenderer());
        
    }
    
    @Override
    public void refresh() {
        
    }

    @Override
    public void totalRefresh() {
    }
    
    private static class EntityCellRenderer extends DefaultTableCellRenderer {
        
        protected JPanel cellPanel;
        protected JLabel titleLabel;

        public EntityCellRenderer() {

            cellPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            cellPanel.setOpaque(false);
            
            titleLabel = new JLabel(" ");
            titleLabel.setOpaque(true);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
            cellPanel.add(titleLabel);
        }
            
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean hasFocus, int row, int column) {

            JComponent cell = (JComponent)super.getTableCellRendererComponent(
                    table, value, selected, hasFocus, row, column);
            titleLabel.setForeground(cell.getForeground());
            titleLabel.setBackground(cell.getBackground());
            
//            cell.setPreferredSize(new Dimension(0, 50));
            
            if (value instanceof AlignedItem) {
                
                cellPanel.setEnabled(table.isEnabled());
                
                AlignedItem alignedItem = (AlignedItem)value;
                EntityWrapper wrapper = alignedItem.getEntity();
                Entity entity = wrapper.getInternalEntity();
                String entityTypeName = wrapper.getType();
                
                // Set the labels
                titleLabel.setText(wrapper.getName());
                titleLabel.setIcon(Icons.getIcon(entity));
                titleLabel.setToolTipText(entityTypeName);
                
//                String filepath = EntityUtils.getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
//                if (filepath == null) {
//                    throw new IllegalStateException("Entity has no filepath");
//                }
//                
//                File file = new File(PathTranslator.convertPath(filepath));
//                
//                ImageCache imageCache = SessionMgr.getBrowser().getImageCache();
//                
//                table.setRowHeight(row, 40);
//                
//                BufferedImage image = imageCache.get(file.getAbsolutePath());
//                if (image!=null) {
//                    BufferedImage thumbnail = Utils.getScaledImage(image, 50, 50);
//                    titleLabel.setIcon(new ImageIcon(thumbnail));
//                }
                
               return cellPanel;
            }
            else {
                if (value==null) {
                    log.warn("Null value in Layers Panel table");
                }
                else {
                    log.warn("Unrecognized value type in Layers Panel table: "+value.getClass().getName());
                }
                return cell;
            }
        }
    }
    
    private class SampleTreeModel implements TreeModel {
        
        private final AlignedItem root = new AlignedItem(new RootedEntity(null));
        
        public SampleTreeModel() {
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
            AlignedItem alignedItem = (AlignedItem)node;
            return alignedItem.isVisible();
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column==0;
        }

        @Override
        public void setValueFor(Object node, int column, Object value) {
            final AlignedItem alignedItem = (AlignedItem)node;
            final Boolean isVisible = (Boolean)value;
            SimpleWorker worker = new SimpleWorker() {
                
                @Override
                protected void doStuff() throws Exception {
                    alignedItem.setIsVisible(isVisible);
                }
                
                @Override
                protected void hadSuccess() {
                    AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                            alignmentBoardContext, alignedItem, ChangeType.VisibilityChange);
                    ModelMgr.getModelMgr().postOnEventBus(event);
                }
                
                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            worker.execute();
        }
    }
}
