package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeModel;

import org.janelia.it.FlyWorkstation.api.entity_model.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.FlyWorkstation.api.entity_model.events.AlignmentBoardOpenEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.jacs.model.entity.Entity;
import org.netbeans.swing.outline.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * The Layers Panel acts as a controller for the Alignment Board. It opens an Alignment Board Context and generates
 * events that the Alignment Board can listen to in order to know when the user adds items to the alignment board,
 * toggles their visibility, or sets other attributes such as color.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LayersPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(LayersPanel.class);

    private AlignmentBoardContext alignmentBoardContext;
    private Outline outline;
    
    public LayersPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
    }
    
    private void init() {

        outline = new Outline();
        outline.setRootVisible(false);
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outline.setColumnHidingAllowed(false);
        outline.setRootVisible(true);
        
        setTransferHandler(new EntityWrapperTransferHandler() {
            @Override
            public JComponent getDropTargetComponent() {
                return LayersPanel.this;
            }
        });
       
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
        
        refresh();
        revalidate();
        repaint();
    }
    
    public Outline getOutline() {
        return outline;
    }

    public AlignmentBoardContext getAlignmentBoardContext() {
        return alignmentBoardContext;
    }

    public void openAlignmentBoard(RootedEntity rootedEntity) {
        loadAlignmentBoard(rootedEntity, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AlignmentBoardOpenEvent event = new AlignmentBoardOpenEvent(alignmentBoardContext);
                ModelMgr.getModelMgr().postOnEventBus(event);
                return null;
            }
        });
    }

    private AtomicBoolean loadInProgress = new AtomicBoolean(false);
    
    public void loadAlignmentBoard(final RootedEntity rootedEntity, final Callable<Void> success) {
        
        if (loadInProgress.getAndSet(true)) {
            log.debug("Skipping refresh, since there is one already in progress");
            return;
        }
        
        if (outline==null) init();
        
        SimpleWorker worker = new SimpleWorker() {
            
            AlignmentBoardContext abContext;
            
            @Override
            protected void doStuff() throws Exception {
                // Ensure we have the latest entity
                rootedEntity.setEntity(ModelMgr.getModelMgr().getEntityTree(rootedEntity.getEntityId()));
                // Load all the ancestors
                this.abContext = new AlignmentBoardContext(rootedEntity);
                loadAncestors(abContext);
            }
            
            private void loadAncestors(EntityWrapper wrapper) throws Exception {
                wrapper.loadContextualizedChildren(abContext.getAlignmentContext());
                for(EntityWrapper childWrapper : wrapper.getChildren()) {
                    loadAncestors(childWrapper);
                }
            }

            @Override
            protected void hadSuccess() {
                alignmentBoardContext = abContext;
                
                OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(new SampleTreeModel(), new AlignedEntityRowModel(), true, "Name");
                updateTableModel(outlineModel);

                revalidate();
                repaint();
                
                loadInProgress.set(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                loadInProgress.set(false);
            }
        };
        
        worker.execute();   
    }

    /**
     * Add a new aligned entity to the board. This method must be called from a worker thread.
     * 
     * @param wrapper
     * @throws Exception
     */
    public void addNewAlignedEntity(EntityWrapper wrapper) throws Exception {
        
        if (wrapper instanceof Sample) {
            Sample sample = (Sample)wrapper;
            
            if (sample.getChildren()==null) {
                sample.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
            }
            
            AlignedItem sampleItem = ModelMgr.getModelMgr().addAlignedItem(alignmentBoardContext, sample);
            sampleItem.setIsVisible(true);
            
            for(Neuron neuron : sample.getNeuronSet()) {
                AlignedItem neuronItem = ModelMgr.getModelMgr().addAlignedItem(sampleItem, neuron);
                neuronItem.setIsVisible(true);
            }
        }
        else if (wrapper instanceof Neuron) {
            // TODO: same as above but highlight the neuron
        }
        else {
            throw new IllegalStateException("Cannot add entity of type "+wrapper.getType()+" to the alignment board.");
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }
    
    private void updateTableModel(OutlineModel outlineModel) {

        if (outline==null) return;
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

        first.setCellRenderer(new OutlineEntityCellRenderer());        
    }

    @Subscribe 
    public void entityInvalidated(EntityInvalidationEvent event) {
        log.debug("Some entities were invalidated so we're refreshing the tree");
        refresh();
    }
    
    @Override
    public void refresh() {
        refresh(false, null);
    }

    @Override
    public void totalRefresh() {
        refresh(true, null);
    }

    public void refresh(final Callable<Void> success) {
        refresh(false, success);
    }
    
    public void totalRefresh(final Callable<Void> success) {
        refresh(true, success);
    }
    
    public void refresh(final boolean invalidateCache, final Callable<Void> success) {
        if (alignmentBoardContext!=null) {
            loadAlignmentBoard(alignmentBoardContext.getInternalRootedEntity(), success);
        }
    }
    
    private class OutlineEntityCellRenderer extends DefaultOutlineCellRenderer {
        
        public OutlineEntityCellRenderer() {
        }
            
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean hasFocus, int row, int column) {

            JComponent cell = (JComponent)super.getTableCellRendererComponent(
                    table, value, selected, hasFocus, row, column);
            
            JLabel label = (JLabel)cell;
            
            if (value instanceof AlignedItem) {
                
                AlignedItem alignedItem = (AlignedItem)value;
                EntityWrapper wrapper = alignedItem.getEntity();
                if (wrapper==null) {
                    label.setText(alignmentBoardContext.getName());
                    label.setIcon(Icons.getIcon(alignmentBoardContext.getInternalEntity()));
                    label.setToolTipText(alignmentBoardContext.getType());
                    return cell;
                }
                
                Entity entity = wrapper.getInternalEntity();
                String entityTypeName = wrapper.getType();
                
                // Set the labels
                label.setText(wrapper.getName());
                label.setIcon(Icons.getIcon(entity));
                label.setToolTipText(entityTypeName);
                
                return cell;
            }
            else {
                if (value==null) {
                    log.warn("Null value in Layers Panel table at row={}, col={}",row,column);
                }
                else {
                    log.warn("Unrecognized value type in Layers Panel table: "+value.getClass().getName());
                }
                return cell;
            }
        }
    }
    
    private class SampleTreeModel implements TreeModel {
        
        public SampleTreeModel() {
        }

        @Override
        public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        }

        @Override
        public Object getChild(Object parent, int index) {
            AlignedItem item = (AlignedItem)parent;
            if (item==null) return null;
            Object child = item.getChildren()==null?0:item.getChildren().get(index);
            return child;
        }

        @Override
        public int getChildCount(Object parent) {
            AlignedItem item = (AlignedItem)parent;
            if (item==null) return 0;
            int count = item.getChildren()==null?0:item.getChildren().size();
            return count;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            AlignedItem item = (AlignedItem)parent;
            if (item==null) return 0;
            int index = item.getChildren()==null?0:item.getChildren().indexOf(child);
            return index;
        }

        @Override
        public Object getRoot() {
            return alignmentBoardContext;
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
            if (alignedItem==alignmentBoardContext) return Boolean.TRUE;
            return alignedItem.isVisible();
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            final AlignedItem alignedItem = (AlignedItem)node;
            return column==0 && (alignedItem!=alignmentBoardContext);
        }

        @Override
        public void setValueFor(Object node, int column, Object value) {
            final AlignedItem alignedItem = (AlignedItem)node;
            if (alignedItem==alignmentBoardContext) return;
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
