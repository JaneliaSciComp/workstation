package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.ActivatableView;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityWrapperTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Refreshable;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.FlyWorkstation.gui.util.ColorSwatch;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
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
public class LayersPanel extends JPanel implements Refreshable, ActivatableView {

    private static final Logger log = LoggerFactory.getLogger(LayersPanel.class);

    public static final String ALIGNMENT_BOARDS_FOLDER = "Alignment Boards";
    private static final int COLUMN_WIDTH_VISIBILITY = 25;
    private static final int COLUMN_WIDTH_COLOR = 32;
    private static final int COLUMN_WIDTH_TREE_NEGATIVE = 80;
    private static final int COLOR_SWATCH_SIZE = 12;
    
    private final JPanel treesPanel;
    private Outline outline;
    
    private AlignmentBoardContext alignmentBoardContext;
    private SimpleWorker worker;
    
    public LayersPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
        showNothing();
    }

    @Override
    public void activate() {
        log.info("Activating");
        ModelMgr.getModelMgr().registerOnEventBus(this);
        refresh();
    }

    @Override
    public void deactivate() {
        log.info("Deactivating");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }
    
    public void showNothing() {
        log.debug("Show nothing");
        treesPanel.removeAll();
        revalidate();
        repaint();
    }
    
    public void showLoadingIndicator() {
        log.debug("Show loading indicator");
        treesPanel.removeAll();
        treesPanel.add(new JLabel(Icons.getLoadingIcon()));
        revalidate();
        repaint();
    }

    public void showOutline() {
        log.debug("Show outline");
        treesPanel.removeAll();
        treesPanel.add(new JScrollPane(outline));
        revalidate();
        repaint();
    }
    
    private void init() {

        outline = new Outline();
        outline.setRootVisible(false);
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outline.setColumnHidingAllowed(false);
        outline.setTableHeader(null);
        outline.setRootVisible(false);
        outline.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        outline.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                TreePath path = outline.getClosestPathForLocation(e.getX(), e.getY());
                if (e.isPopupTrigger()) {
                    int rowIndex = outline.convertRowIndexToView(outline.getLayoutCache().getRowForPath(path));
                    outline.getSelectionModel().setSelectionInterval(rowIndex,rowIndex);
                    showPopupMenu(e);
                    return;
                }
                if (path!=null) {
                    // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        nodeDoubleClicked(e);
                    }
                    else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                        nodeClicked(e);
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                // We have to also listen for mousePressed because OSX generates the popup trigger here
                // instead of mouseReleased like any sane OS.
                TreePath path = outline.getClosestPathForLocation(e.getX(), e.getY());

                if (e.isPopupTrigger()) {
                    int rowIndex = outline.convertRowIndexToView(outline.getLayoutCache().getRowForPath(path));
                    outline.getSelectionModel().setSelectionInterval(rowIndex,rowIndex);
                    showPopupMenu(e);
                    return;
                }
                if (path!=null) {
                    nodePressed(e);
                }
            }
        });
        
        setTransferHandler(new EntityWrapperTransferHandler() {
            @Override
            public JComponent getDropTargetComponent() {
                return LayersPanel.this;
            }
        });
       
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateColumnsWidths();
            }
        });
    }

    /**
     * Override this method to show a popup menu when the user right clicks a node.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {
        showPopupMenuImpl(e);
    }

    /**
     * Override this method to do something when the user left clicks a node.
     *
     * @param e
     */
    protected void nodeClicked(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user presses down on a node.
     *
     * @param e
     */
    protected void nodePressed(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user double clicks a node.
     *
     * @param e
     */
    protected void nodeDoubleClicked(MouseEvent e) {
    }

    /**
     * Override this method to show a popup menu when the user right clicks a
     * node in the tree.
     * 
     * @param e
     */
    protected void showPopupMenuImpl(final MouseEvent e) {

        Object object = outline.getOutlineModel().getValueAt(outline.getSelectedRow(), 0); 
        if (object==null) return;
        if (!(object instanceof AlignedItem)) return;
        
        AlignedItem alignedItem = (AlignedItem)object;
        
        // Create context menu
        final LayerContextMenu popupMenu = new LayerContextMenu(alignmentBoardContext, alignedItem);
        popupMenu.addMenuItems();
        popupMenu.show(outline, e.getX(), e.getY());
    }
    
    public Outline getOutline() {
        return outline;
    }

    public AlignmentBoardContext getAlignmentBoardContext() {
        return alignmentBoardContext;
    }

    public void openAlignmentBoard(long alignmentBoardId) {
        
        log.debug("openAlignmentBoard: {}",alignmentBoardId);
        loadAlignmentBoard(alignmentBoardId, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AlignmentBoardOpenEvent event = new AlignmentBoardOpenEvent(alignmentBoardContext);
                ModelMgr.getModelMgr().postOnEventBus(event);
                return null;
            }
        });
    }

    private AtomicBoolean loadInProgress = new AtomicBoolean(false);
    
    public void loadAlignmentBoard(final long alignmentBoardId, final Callable<Void> success) {
        
        showLoadingIndicator();
        
        if (loadInProgress.getAndSet(true)) {
            log.debug("Refresh in progress, killing it");
            if (worker!=null) {
                worker.disregard();
            }
        }
        
        if (outline==null) init();
        
        this.worker = new SimpleWorker() {
            
            AlignmentBoardContext abContext;
            
            @Override
            protected void doStuff() throws Exception {
                log.debug("load alignment board with id: {}",alignmentBoardId);
                Entity commonRoot = ModelMgr.getModelMgr().getCommonRootEntityByName(ALIGNMENT_BOARDS_FOLDER);
                ModelMgr.getModelMgr().loadLazyEntity(commonRoot, false);
                RootedEntity commonRootedEntity = new RootedEntity(commonRoot);
                RootedEntity abRootedEntity = commonRootedEntity.getChildById(alignmentBoardId);
                this.abContext = new AlignmentBoardContext(abRootedEntity);
                log.debug("loading ancestors for alignment board: {}",abContext);
                loadAncestors(abContext);
            }
            
            private void loadAncestors(EntityWrapper wrapper) throws Exception {
                log.trace("loadAncestors: {}",wrapper);
                wrapper.loadContextualizedChildren(abContext.getAlignmentContext());
                for(EntityWrapper childWrapper : wrapper.getChildren()) {
                    loadAncestors(childWrapper);
                    if (childWrapper instanceof AlignedItem) {
                        AlignedItem alignedItem = (AlignedItem)childWrapper;
                        loadAncestors(alignedItem.getItemWrapper());
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                alignmentBoardContext = abContext;
                
                log.debug("loadAlignmentBoard was a success, updating the outline now");
                
                OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(new SampleTreeModel(), new AlignedEntityRowModel(), true, "Name");
                updateTableModel(outlineModel);
                
                loadInProgress.set(false);
                showOutline();
                
                if (success!=null) {
                    try {
                        success.call();    
                    }
                    catch (Exception e) {
                        hadError(e);
                    }
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                loadInProgress.set(false);
                showNothing();
            }
        };
        
        // Wait for any EDT operations to finish first
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                worker.execute();   
            }
        });   
    }

    /**
     * Add a new aligned entity to the board. This method must be called from a worker thread.
     * 
     * @param wrapper
     * @throws Exception
     */
    public void addNewAlignedEntity(EntityWrapper wrapper) throws Exception {

        final Collection<AlignmentBoardEvent> events = new ArrayList<AlignmentBoardEvent>();
        
        if (wrapper instanceof Sample) {
            Sample sample = (Sample)wrapper;
            
            AlignedItem sampleAlignedItem = alignmentBoardContext.getAlignedItemWithEntityId(sample.getId());

            if (sampleAlignedItem==null) {
                if (sample.getChildren()==null) {
                    sample.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
                }
                
                sampleAlignedItem = ModelMgr.getModelMgr().addAlignedItem(alignmentBoardContext, sample);
                sampleAlignedItem.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
                
                for(Neuron neuron : sample.getNeuronSet()) {
                    AlignedItem neuronItem = ModelMgr.getModelMgr().addAlignedItem(sampleAlignedItem, neuron);
                    neuronItem.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
                    neuronItem.setIsVisible(true);
                }

                events.add(new AlignmentBoardItemChangeEvent(alignmentBoardContext, sampleAlignedItem, ChangeType.Added));
            }
            else {
                events.add(new AlignmentBoardItemChangeEvent(alignmentBoardContext, sampleAlignedItem, ChangeType.VisibilityChange));
            }
            
            sampleAlignedItem.setIsVisible(true);
        }
        else if (wrapper instanceof Neuron) {

            Neuron neuron = (Neuron)wrapper;
            
            Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(neuron.getInternalEntity(), EntityConstants.TYPE_SAMPLE);
            
            if (sampleEntity==null) {
                throw new IllegalStateException("Cannot find Sample for Neuron "+neuron.getId());
            }
            
            AlignedItem sampleAlignedItem = alignmentBoardContext.getAlignedItemWithEntityId(sampleEntity.getId());
            Sample sample = null;
            
            if (sampleAlignedItem==null) {
                sample = new Sample(new RootedEntity(sampleEntity));            
                if (sample.getChildren()==null) {
                    sample.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
                }
                
                sampleAlignedItem = ModelMgr.getModelMgr().addAlignedItem(alignmentBoardContext, sample);
                sampleAlignedItem.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
            }
            else {
                sample = (Sample)sampleAlignedItem.getItemWrapper();
            }
            
            sampleAlignedItem.setIsVisible(true);
            
            AlignedItem neuronItem = sampleAlignedItem.getAlignedItemWithEntityId(neuron.getId());
            
            if (neuronItem == null) {
                neuronItem = ModelMgr.getModelMgr().addAlignedItem(sampleAlignedItem, neuron);
                neuronItem.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
                events.add(new AlignmentBoardItemChangeEvent(alignmentBoardContext, neuronItem, ChangeType.Added));
            }
            else {
                events.add(new AlignmentBoardItemChangeEvent(alignmentBoardContext, neuronItem, ChangeType.VisibilityChange));    
            }
            
            neuronItem.setIsVisible(true);
        }
        else {
            throw new IllegalStateException("Cannot add entity of type "+wrapper.getType()+" to the alignment board.");
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(AlignmentBoardEvent event : events) {
                    ModelMgr.getModelMgr().postOnEventBus(event);
                }
            }
        });
    }

    /**
     * Removes an aligned entity from the board. This method must be called from a worker thread.
     * 
     * @param wrapper
     * @throws Exception
     */
    public void removeAlignedEntity(final AlignedItem alignedItem) throws Exception {

        RootedEntity rootedEntity = alignedItem.getInternalRootedEntity();
        ModelMgr.getModelMgr().deleteEntityTree(rootedEntity.getEntityId());
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.info("SENDING REMOVED EVENT");
                AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                        alignmentBoardContext, alignedItem, ChangeType.Removed);
                ModelMgr.getModelMgr().postOnEventBus(event);
//                refresh();
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
        
        TableColumn treeColumn = columns[0];
        TableColumn visColumn = columns[1];
        TableColumn colorColumn = columns[2];
        
        // Swap the columns to put the tree last
        columns[0] = visColumn;
        columns[1] = colorColumn;
        columns[2] = treeColumn;

        for(int i=0; i<columns.length; i++) {
            columnModel.addColumn(columns[i]);
        }

        treeColumn.setCellRenderer(new OutlineTreeCellRenderer());
        colorColumn.setCellRenderer(new OutlineColorCellRenderer());
        
        updateColumnsWidths();
    }
    
    private void updateColumnsWidths() {
        if (outline==null) return;
        TableColumnModel columnModel = outline.getColumnModel();
        if (columnModel.getColumnCount()<3) return;
        columnModel.getColumn(0).setPreferredWidth(COLUMN_WIDTH_VISIBILITY);
        columnModel.getColumn(1).setPreferredWidth(COLUMN_WIDTH_COLOR);
        columnModel.getColumn(2).setPreferredWidth(getWidth()-COLUMN_WIDTH_TREE_NEGATIVE);
    }
    
    @Subscribe 
    public void entityInvalidated(EntityInvalidationEvent event) {
        log.debug("Some entities were invalidated so we're refreshing the tree");
        refresh();
    }
    
    @Override
    public void refresh() {
        log.debug("refresh");
        refresh(false, null);
    }

    @Override
    public void totalRefresh() {
        log.debug("totalRefresh");
        refresh(true, null);
    }
    
    private void refresh(final boolean invalidateCache, final Callable<Void> success) {
        if (alignmentBoardContext!=null) {
            loadAlignmentBoard(alignmentBoardContext.getId(), success);
        }
    }
    
    private class OutlineTreeCellRenderer extends DefaultOutlineCellRenderer {
        
        public OutlineTreeCellRenderer() {
        }
            
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean hasFocus, int row, int column) {

            if (value==null) return null;
            
            JComponent cell = (JComponent)super.getTableCellRendererComponent(
                    table, value, selected, hasFocus, row, column);
            
            JLabel label = (JLabel)cell;
            if (label==null) return null;
            
            if (value instanceof AlignedItem) {
                AlignedItem alignedItem = (AlignedItem)value;
                EntityWrapper wrapper = alignedItem.getItemWrapper();
                
                if (wrapper==null) {
                    label.setText(alignmentBoardContext.getName());
                    label.setIcon(Icons.getIcon(alignmentBoardContext.getInternalEntity()));
                }
                else {
                    Entity entity = wrapper.getInternalEntity();
                    label.setText(alignedItem.getName());
                    label.setIcon(Icons.getIcon(entity));
                }
            }
            else {
                log.warn("Unrecognized value type in LayersPanel tree column: "+value.getClass().getName());
            }
            
            return label;
        }
    }

    private class OutlineColorCellRenderer extends DefaultOutlineCellRenderer {
        
        public OutlineColorCellRenderer() {
        }
            
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean hasFocus, int row, int column) {
            
            JComponent cell = (JComponent)super.getTableCellRendererComponent(
                    table, value, selected, hasFocus, row, column);
            
            if (value==null) return cell;
            
            JLabel label = (JLabel)cell;
            if (label==null) return null;
            
            if (value instanceof Color) {
                ColorSwatch swatch = new ColorSwatch(COLOR_SWATCH_SIZE, (Color)value, Color.white);
                label.setIcon(swatch);
                label.setText("");
            }
            else {
                log.warn("Unrecognized value type in LayersPanel color column: "+value.getClass().getName());
            }
            
            return label;
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
            case 1:
                return Color.class;
            default:
                assert false;
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getValueFor(Object node, int column) {
            AlignedItem alignedItem = (AlignedItem)node;
            switch (column) {
            case 0:
                if (alignedItem==alignmentBoardContext) return Boolean.TRUE;
                return alignedItem.isVisible();            
            case 1:
                if (alignedItem==alignmentBoardContext) return null;
                return alignedItem.getColor();
            default:
                assert false;
            }
            return null;
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
