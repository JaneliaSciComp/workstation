package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;

import org.janelia.it.workstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.FileStats;
import org.janelia.it.workstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardCloseEvent;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardOpenEvent;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.workstation.model.viewer.AlignedItem.InclusionStatus;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.janelia.it.workstation.gui.util.ColorSwatch;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;

/**
 * The Layers Panel acts as a controller for the Alignment Board. It opens an Alignment Board Context and generates
 events that the Alignment Board can listen to in order to know when the user adds multiSelectionItems to the alignment board,
 toggles their visibility, or sets other attributes such as color.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LayersPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(LayersPanel.class);

    public static final String HARDCODED_BS_OBJECTIVE = "63x";
    public static final String ALIGNMENT_BOARDS_FOLDER = "Alignment Boards";
    private static final int COLUMN_WIDTH_VISIBILITY = 25;
    private static final int COLUMN_WIDTH_COLOR = 32;
    private static final int COLUMN_WIDTH_TREE_NEGATIVE = 80;
    private static final int COLOR_SWATCH_SIZE = 12;
    private static final int COLOR_SWATCH_COLNUM = 1;
    private static final int VIZCHECK_COLNUM = 0;

    private final JPanel treesPanel;
    private Outline outline;
    private SampleTreeModel sampleTreeModel;
    private FileStats fileStats;
    
    private AlignmentBoardContext alignmentBoardContext;
    private SimpleWorker worker;
    
    public LayersPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
        showNothing();
    }

    public void activate() {
        log.info("Activating");
        ModelMgr.getModelMgr().registerOnEventBus(this);
        refresh();
    }

    public void deactivate() {
        log.info("Deactivating");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }
    
    /** Show an empty panel.  Marked final because called by c'tor. */
    public final void showNothing() {
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
    
    public Outline getOutline() {
        return outline;
    }

    public AlignmentBoardContext getAlignmentBoardContext() {
        return alignmentBoardContext;
    }
    
    public void openAlignmentBoard(long alignmentBoardId) {
        log.info("openAlignmentBoard: {}",alignmentBoardId);
        loadAlignmentBoard(alignmentBoardId, null, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AlignmentBoardOpenEvent event = new AlignmentBoardOpenEvent(alignmentBoardContext);
                log.info("Posting AB-Open");                
                ModelMgr.getModelMgr().postOnEventBus(event);
                return null;
            }
        });
    }

    public void closeAlignmentBoard() {
        log.debug("closeAlignmentBoard");
        this.alignmentBoardContext = null;
        showNothing();
    }
    
    private void init() {

        outline = new Outline();
        outline.setRootVisible(false);
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outline.setColumnHidingAllowed(false);
        outline.setTableHeader(null);
        outline.setRootVisible(false);
        outline.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        outline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                TreePath path = outline.getClosestPathForLocation(e.getX(), e.getY());
                AlignmentBoardItem ai = getItemAtEventPos( e );
                if (e.isPopupTrigger()) {
                    handlePopup(e, ai);
                    return;
                }
                else {
                    if ( selectColorIfCorrectColumn(ai) ) {
                        return;
                    }
                }
                if (path!=null) {
                    dispatchForClickType(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // We have to also listen for mousePressed because OSX generates the popup trigger here
                // instead of mouseReleased like any sane OS.
                TreePath path = outline.getClosestPathForLocation(e.getX(), e.getY());
                AlignmentBoardItem ai = getItemForPath(path);

                if (e.isPopupTrigger()) {
                    handlePopup(e, ai);
                }
                else {
                    if (path!=null) {
                        nodePressed(e);
                    }
                    if ( ai != null ) {
                        selectColorIfCorrectColumn(ai);
                    }
                }
            }

            /** Will 'act'/return true if the click was the color col. */
            private boolean selectColorIfCorrectColumn(AlignmentBoardItem ai) {
                // Need see if this is appropriate column.
                // outline.getSelectedColumn()  does not work: clicking and selecting are different things.
                int colIndex = outline.getSelectedColumn();
                if ( colIndex == COLOR_SWATCH_COLNUM ) {
                    chooseColor( ai );
                    return true;
                }
                return false;
            }

            private void handlePopup(MouseEvent e, AlignmentBoardItem ai) {
                showPopupMenu(e, ai);
            }

            private void dispatchForClickType(MouseEvent e) {
                // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                    nodeDoubleClicked(e);
                } else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    nodeClicked(e);
                }
            }

        });
        
        setTransferHandler(new AlignmentBoardDomainObjectTransferHandler(null, null));
        /*
        {
            @Override
            public JComponent getDropTargetComponent() {
                return LayersPanel.this;
            }
        });
        */
       
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateColumnsWidths();
            }
        });
    }
    
    private AlignmentBoardItem getItemAtEventPos( MouseEvent e ) {
        TreePath path = outline.getClosestPathForLocation(e.getX(), e.getY());
        AlignmentBoardItem ai = getItemForPath(path);
        return ai;
    }

    private AlignmentBoardItem getItemForPath(TreePath path) {
        int rowIndex = outline.convertRowIndexToView(outline.getLayoutCache().getRowForPath(path));
        AlignmentBoardItem ai = getItemFromRow(rowIndex);
        return ai;
    }

    /**
     * Show a popup menu when the user right clicks a node, and has item.
     *
     * @param e
     */
    private void showPopupMenu(MouseEvent e, AlignmentBoardItem abi) {
        showPopupMenuImpl(e, abi);
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
     * Show a popup menu when the user right clicks a
     * node in the tree.
     * 
     * @param e
     */
    private void showPopupMenuImpl(final MouseEvent e, AlignmentBoardItem abi) {
        if ( abi == null ) {
            return;
        }

        // Create context menu
        // Need to find aligned multiSelectionItems for the current (multi) selection.
        int[] rows = outline.getSelectedRows();
        List<AlignmentBoardItem> multiSelectionItems = new ArrayList<>();
        for ( int row: rows ) {
            AlignmentBoardItem nextItem = getItemFromRow( row );
            if ( nextItem != null ) {
                multiSelectionItems.add( nextItem );
            }
        }
        
        final LayerContextMenu popupMenu = new LayerContextMenu(alignmentBoardContext, abi, multiSelectionItems);
        popupMenu.addMenuItems();
        popupMenu.show(outline, e.getX(), e.getY());
    }
    
    private final AtomicBoolean loadInProgress = new AtomicBoolean(false);

    private AlignmentBoardItem getItemFromRow(int row) {
        Object object = outline.getOutlineModel().getValueAt(row, 0);
        AlignmentBoardItem rtnVal = null;
        if (object != null  &&  object instanceof AlignmentBoardItem) {
            rtnVal = (AlignmentBoardItem)object;
        }

        return rtnVal;
    }

    private void loadAlignmentBoard(final long alignmentBoardId, final OutlineExpansionState expansionState, final Callable<Void> success) {
        
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
                log.trace("load alignment board with id: {}",alignmentBoardId);
// TODO : must implement prior to any board able to open.                
//                Entity commonRoot = ModelMgr.getModelMgr().getOwnedCommonRootByName(ALIGNMENT_BOARDS_FOLDER);
//                ModelMgr.getModelMgr().loadLazyEntity(commonRoot, false);
//                RootedEntity commonRootedEntity = new RootedEntity(commonRoot);
//                RootedEntity abRootedEntity = commonRootedEntity.getChildById(alignmentBoardId);
//                if (abRootedEntity==null) {
//                    throw new IllegalStateException("Alignment board does not exist");
//                }
                AlignmentBoard aboard = (AlignmentBoard)DomainMgr.getDomainMgr().getModel().getDomainObject(AlignmentBoard.class.getSimpleName(), alignmentBoardId);
                AlignmentContext alignmentContext = new AlignmentContext();
                alignmentContext.setAlignmentSpace( aboard.getAlignmentSpace() );
                alignmentContext.setImageSize( aboard.getImageSize() );
                alignmentContext.setOpticalResolution( aboard.getOpticalResolution() );
                this.abContext = new AlignmentBoardContext( aboard, alignmentContext );
                log.debug("Loading ancestors for alignment board: {}", abContext);
                //See Below: loadAncestors(abContext);
                loadCompartmentSet(abContext);
            }

//TODO: not convinced this is necessary in DomainObject paradigm.            
//            private void loadAncestors(EntityWrapper wrapper) throws Exception {
//                log.trace("loadAncestors: {}",wrapper);
//                if ( wrapper == null || abContext == null ) {
//                    log.error("Null wrapper {} or abContext {}.", wrapper, abContext);
//                    return;
//                }
//                wrapper.loadContextualizedChildren(abContext.getAlignmentContext());
//                for(EntityWrapper childWrapper : wrapper.getChildren()) {
//                    loadAncestors(childWrapper);
//                    if (childWrapper instanceof AlignedItem) {
//                        AlignedItem alignedItem = (AlignedItem)childWrapper;
//                        loadAncestors(alignedItem.getItemWrapper());
//                    }
//                }
//            }

            private void loadCompartmentSet(AlignmentBoardContext context) throws Exception {
                log.trace("loadCompartmentSet: {}", context);
                // Add the compartment set, lazily, if it is not already under this board.
                boolean hasCompartmentSet = false;
                for ( AlignmentBoardItem child: context.getAlignmentBoardItems() ) {
                    DomainObject dObj = RenderUtils.getObjectForItem(child);
                    if ( dObj.getName().startsWith("Compartment Set") ) {
                        log.trace("Context has a compartment set called {}.", dObj.getName());
                        hasCompartmentSet = true;
                    }
                }

                if ( ! hasCompartmentSet ) {
                    AlignmentContext targetSpace = abContext.getAlignmentContext();
                    List<DomainObject> compartmentSets = DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(CompartmentSet.class.getSimpleName());
                    if ( compartmentSets != null  &&  compartmentSets.size() > 0 ) {

                        for(DomainObject compartmentSetDO : compartmentSets) {
                            AlignmentContext compartmentSetSpace = new AlignmentContext();
                            CompartmentSet compartmentSet = (CompartmentSet)compartmentSetDO;
                            compartmentSetSpace.setAlignmentSpace(compartmentSet.getAlignmentSpace());

                            if (targetSpace.equals(compartmentSetSpace)) {
                                if (!compartmentSet.getCompartments().isEmpty()) {
                                    abContext.addDomainObject(compartmentSet, HARDCODED_BS_OBJECTIVE);
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                alignmentBoardContext = abContext;
                
                log.debug("loadAlignmentBoard was a success, updating the outline now");

                sampleTreeModel = new SampleTreeModel(alignmentBoardContext);
                
                recreateModel();
                
                if (expansionState!=null) expansionState.restoreExpansionState(true);
                
                loadInProgress.set(false);
                showOutline();
                
                ConcurrentUtils.invokeAndHandleExceptions(success);
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

    private void updateTableModel(OutlineModel outlineModel) {

        if (outline==null) return;
        outline.setModel(outlineModel);
        
        TableColumnModel columnModel = outline.getColumnModel();
        TableColumn[] columns = new TableColumn[columnModel.getColumnCount()];
        
        for(int i=0; i<columnModel.getColumnCount(); i++) {
            TableColumn tableColumn = columnModel.getColumn(i);
            columns[i] = tableColumn;
        }
        
        for (int i=0; i<columns.length; i++) {
            columnModel.removeColumn(columns[i]);
        }
        
        TableColumn treeColumn = columns[0];
        TableColumn visColumn = columns[1];
        TableColumn colorColumn = columns[2];
        
        // Swap the columns to put the tree last
        columns[VIZCHECK_COLNUM] = visColumn;
        columns[COLOR_SWATCH_COLNUM] = colorColumn;
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
        columnModel.getColumn(VIZCHECK_COLNUM).setPreferredWidth(COLUMN_WIDTH_VISIBILITY);
        columnModel.getColumn(COLOR_SWATCH_COLNUM).setPreferredWidth(COLUMN_WIDTH_COLOR);
        columnModel.getColumn(2).setPreferredWidth(getWidth()-COLUMN_WIDTH_TREE_NEGATIVE);
    }

    @Subscribe 
    public void entityInvalidated(EntityInvalidationEvent event) {
//TODO: re-examine this.        
//        if (event.isTotalInvalidation()) {
//            log.debug("Total invalidation, so we're refreshing the tree");
//            refresh();
//            return;
//        }
//
//        log.debug("Some entities were invalidated, let's check if we care...");
//        if (alignmentBoardContext==null) return;
//
//        final OutlineExpansionState expansionState = new OutlineExpansionState(outline);
//        expansionState.storeExpansionState();
//        
//        final Collection<AlignmentBoardItem> invalidItems = new HashSet<>();
//        
//        Collection<AlignmentBoardItem> invalidated = event.getInvalidatedEntities();
//        
//        for(AlignmentBoardItem alignmentBoardItem : invalidated) {
//            AlignmentBoardItem invalidItem = findAlignedItemByEntityId(alignmentBoardContext, alignmentBoardItem.getTarget().getTargetId());
//            if (invalidItem!=null) {
//                invalidItems.add(invalidItem);
//            }
//        }
//
//        log.debug("Found {} aligned items with invalidated entities",invalidItems.size());
//        
//        if (invalidItems.isEmpty()) return;
//        
//        for(AlignmentBoardItem invalidItem : invalidItems) {
//            try {
//                log.debug("Updating invalidated entity {} on aligned item",invalidItem.getId());
//                invalidItem.updateEntity(ModelMgr.getModelMgr().getEntityById(invalidItem.getId()));
//                invalidItem.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());
//            }
//            catch (Exception e) {
//                log.error("Error updating entity {} on aligned item",invalidItem.getId());
//            }
//        }
//        
//        recreateModel();
//        
//        expansionState.restoreExpansionState(true);
    }

    @Subscribe 
    public void alignmentBoardClosed(AlignmentBoardCloseEvent event) {
        this.alignmentBoardContext = null;
        this.outline = null;
        this.sampleTreeModel = null;
        showNothing();
    }
    
    @Subscribe 
    public void itemChanged(AlignmentBoardItemChangeEvent event) {

        if (sampleTreeModel==null || alignmentBoardContext==null) return;

        ChangeType change = event.getChangeType();
        

        final OutlineExpansionState expansionState = new OutlineExpansionState(outline);
        expansionState.storeExpansionState();

        if (event.getItem()!=null) {
            log.debug("Aligned item changed {} with change type {}", event.getItem(), change);
        }
        else {
            log.debug("All aligned items changed with change type {}", change);
        }
        
// TODO: need to look at whether parent-add is needed now.        
//        if (change==ChangeType.Added) {
//            try {
//                // Recreate the contextualized wrappers
//                EntityWrapper parent = event.getAlignedItem().getParent();
//                if (parent==null) {
//                    log.error("Aligned item has null parent: "+event.getAlignedItem().getName());
//                }
//                else {
//                    parent.loadContextualizedChildren(alignmentBoardContext.getAlignmentContext());    
//                }
//            }
//            catch (Exception e) {
//                SessionMgr.getSessionMgr().handleException(e);
//            }
//        }
//        else {
//            // No need to do anything for other changes, they've all been handled through other means
//        }

        // Generating model events is hard (we don't know the UI indexes of what was deleted, for example),
        // so we just recreate the model here.
        recreateModel();
        
        expansionState.restoreExpansionState(true);
    }
    
    private void recreateModel() {
        if (sampleTreeModel!=null) {
            log.debug("Recreating outline model and updating UI");
            OutlineModel outlineModel = DefaultOutlineModel.createOutlineModel(sampleTreeModel, new AlignedEntityRowModel(), true, "Name");
            updateTableModel(outlineModel);
            getOutline().updateUI();
        }
    }
    
    @Override
    public void refresh() {
        log.debug("refresh");
        refresh(true, false, null);
    }

    @Override
    public void totalRefresh() {
        log.debug("totalRefresh");
        refresh(true, true, null);
    }
    
    private void refresh(final boolean restoreState, final boolean invalidateCache, final Callable<Void> success) {
        // TODO: respect cache invalidation parameter
        if (alignmentBoardContext==null) {
            log.debug("No alignment board context ready for layers panel.");
            return;
        }
        if (restoreState) {
            final OutlineExpansionState expansionState = new OutlineExpansionState(outline);
            expansionState.storeExpansionState();
            loadAlignmentBoard(alignmentBoardContext.getAlignmentBoard().getId(), expansionState, success);
        }
        else {
            loadAlignmentBoard(alignmentBoardContext.getAlignmentBoard().getId(), null, success);
        }
        
    }    
    
    public void chooseColor(final AlignmentBoardItem alignedItem) {

        Color currColor = RenderUtils.getColorFromRGBStr(alignedItem.getColor());
        final DomainObject alignedItemTarget = DomainMgr.getDomainMgr().getModel().getDomainObject(alignedItem.getTarget());
        final Color newColor = JColorChooser.showDialog(SessionMgr.getMainFrame(), "Choose color", currColor);
        if (newColor==null) return;
        
        SimpleWorker localWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                alignedItem.setColor(RenderUtils.getRGBStrFromColor(newColor));
            }
            
            @Override
            protected void hadSuccess() {
                AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                        alignmentBoardContext, alignedItem, ChangeType.ColorChange);
                ModelMgr.getModelMgr().postOnEventBus(event);
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        localWorker.execute();
    }

    public void setFileStats(FileStats fileStats) {
        this.fileStats = fileStats;
    }
    
    private boolean isPassthroughRendering(AlignmentBoardItem item) {
        return RenderMappingI.PASSTHROUGH_RENDER_ATTRIBUTE.equals(item.getRenderMethod());
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
            
            if (value instanceof AlignmentBoardItem) {
                AlignmentBoardItem alignmentBoardItem = (AlignmentBoardItem)value;
                DomainObject alignedItemTarget = DomainMgr.getDomainMgr().getModel().getDomainObject(alignmentBoardItem.getTarget());
                if (alignedItemTarget == null) {
                    log.info("Null target for {} of type {}." + alignmentBoardItem.getTarget().getTargetId(), alignmentBoardItem.getTarget().getTargetClassName() );
                }
                if (alignmentBoardItem == null  ||  alignedItemTarget == null) {
                    label.setText("Item is null");
                    label.setIcon(null);
                }
                else {
                    label.setText(alignedItemTarget.getName());
//TODO workout where to get icon                    label.setIcon(Icons.getIcon(alignedItem));
                }
            }
            else if (value instanceof String) {

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

            JLabel label = (JLabel)cell;
            if (label==null) return null;

            if ( value instanceof AlignmentBoardItem ) {
                final AlignmentBoardItem alignmentBoardItem = (AlignmentBoardItem)value;
                if (isPassthroughRendering(alignmentBoardItem)) {
                    ColorSwatch swatch = new ColorSwatch(COLOR_SWATCH_SIZE, Color.pink, Color.black);
                    swatch.setMultiColor();
                    label.setIcon(swatch);
                    label.setText("");
                    label.setToolTipText( "Raw rendering" );
                }
                else if (alignmentBoardItem.getColor()!=null) {
                    ColorSwatch swatch = new ColorSwatch(COLOR_SWATCH_SIZE, getGammaCorrectedSwatchColor(RenderUtils.getColorFromRGBStr(alignmentBoardItem.getColor())), Color.white);
                    label.setIcon(swatch);
                    label.setText("");
                    label.setToolTipText( "Chosen (mono) color rendering" );
                }
                else {
                    if ( fileStats != null  &&  alignmentBoardItem != null  &&  ( !alignmentBoardItem.getTarget().getTargetClassName().toLowerCase().contains("compartment") ) ) {
                        final Long itemId = alignmentBoardItem.getTarget().getTargetId();
                        double[] colorRGB = fileStats.getChannelAverages(itemId);
                        if ( colorRGB == null ) {
                            // Happens with reference channels.
                            colorRGB = fileStats.getChannelAverages( itemId );
                        }
                        if ( colorRGB != null ) {
                            Color color = getGammaCorrectedSwatchColor(colorRGB);
                            ColorSwatch swatch = new ColorSwatch(COLOR_SWATCH_SIZE, color, Color.white);
                            label.setIcon( swatch );
                        }
                    }
                    label.setText("");
                    label.setToolTipText( "Default rendering" );
                }
            }

            return label;
        }

        private Color getGammaCorrectedSwatchColor(Color color) {
            double[] colorRGB = new double[ 3 ];
            if ( color != null ) {
                colorRGB[ 0] = color.getRed() / 256.0;
                colorRGB[ 1] = color.getGreen() / 256.0;
                colorRGB[ 2] = color.getBlue() / 256.0;
            }
            return getGammaCorrectedSwatchColor( colorRGB );
        }

        private Color getGammaCorrectedSwatchColor(double[] colorRGB) {
            return new Color(
                    (int) (256.0 * Math.pow(colorRGB[ 0], VolumeModel.STANDARDIZED_GAMMA_MULTIPLIER)),
                    (int) (256.0 * Math.pow(colorRGB[ 1], VolumeModel.STANDARDIZED_GAMMA_MULTIPLIER)),
                    (int) (256.0 * Math.pow(colorRGB[ 2], VolumeModel.STANDARDIZED_GAMMA_MULTIPLIER))
            );
        }
        
    }
    
    private class AlignedEntityRowModel implements RowModel {

        @Override
        public Class getColumnClass(int column) {
            switch (column) {
            case VIZCHECK_COLNUM:
                return Boolean.class;
            case COLOR_SWATCH_COLNUM:
                return AlignmentBoardItem.class;
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
            if (node instanceof AlignmentBoardItem) {
                AlignmentBoardItem alignedItem = (AlignmentBoardItem) node;
                switch (column) {
                case VIZCHECK_COLNUM:
                    // Q: alignment board among table cells???  if (alignedItem == alignmentBoardContext) return Boolean.TRUE;
                    return alignedItem.isVisible();
                case COLOR_SWATCH_COLNUM:
                    // Q: alignment board among table cells???  if (alignedItem == alignmentBoardContext) return null;
                    return alignedItem;
                default:
                    assert false;
                }
            }
            else if (node instanceof String) {
                switch (column) {
                case VIZCHECK_COLNUM:
                    return Boolean.FALSE;
                case COLOR_SWATCH_COLNUM:
                    return null;
                default:
                    assert false;
                }
            }
            return null;
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            if (node instanceof AlignmentBoardItem) {
                final AlignmentBoardItem alignmentBoardItem = (AlignmentBoardItem) node;
                return column == VIZCHECK_COLNUM; // LATER: will a-board be among rows/cols??   && (alignedItem != alignmentBoardContext);
            }
            else {
                return false;
            }
        }

        /**
         * The value "set" here is the checbox selected state.
         * 
         * @param node model for this checkbox.
         * @param column column number for the checkbox.
         * @param value T or F here.
         */
        @Override
        public void setValueFor(Object node, int column, Object value) {
            if (!(node instanceof AlignmentBoardItem)) {
                return;
            }
            final AlignmentBoardItem alignmentBoardItem = (AlignmentBoardItem)node;
            //  Is this possible???  If so, would NOW be the AlignmentBoard,
            //  and I would need to ensure all the types of things in table
            //  are DomainObject, instead of AlignmentBoardItem.
            final Boolean isVisible = (Boolean)value;
            SimpleWorker worker = new SimpleWorker() {
                
                private AlignmentBoardItem parent;
                private DomainObject parentObject;
                private DomainObject target = DomainMgr.getDomainMgr().getModel().getDomainObject(alignmentBoardItem.getTarget());
                
                @Override
                protected void doStuff() throws Exception {
                    alignmentBoardItem.setVisible(isVisible);

                    Collection<DomainObject> affectedEntities = new ArrayList<>();

                    for(AlignmentBoardItem child : alignmentBoardItem.getChildren()) {
                        DomainObject innerChild = DomainMgr.getDomainMgr().getModel().getDomainObject(child.getTarget());
                        affectedEntities.add( innerChild );
                    }

                    // HOW to get the parent?
                    AlignmentBoardItem parentWrapper = null; //alignedItem.getParent();
                    parentObject = RenderUtils.getObjectForItem(alignmentBoardItem);
                    if (parentWrapper!=null) {                        
                        if (parentWrapper instanceof AlignmentBoardItem) {
                            parent = (AlignmentBoardItem)parentWrapper;
                            if ( ! isVisible ) {
                                // Check children of this parent: any of them
                                // on?
                                boolean childVisible = false;
                                for (AlignmentBoardItem child : parent.getChildren()) {
                                    if (child.isVisible()) {
                                        childVisible = true;
                                        break;
                                    }
                                }
                                if ( ! childVisible ) {
                                    affectedEntities.add(parentObject);
                                }
                            }
                            else if ( ! parent.isVisible() ) {
                                // May have to read uncached visibility flag.
                                // But could non-incur whole writeback cost.
                                affectedEntities.add(parentObject);
                            }
                        }
                    }

                    if ( affectedEntities.size() > 0 ) {
// TODO How to save the values?                        
//                        ModelMgr.getModelMgr().setOrUpdateValues( 
//                                affectedEntities, 
//                                EntityConstants.ATTRIBUTE_VISIBILITY,
//                                Boolean.toString(isVisible) 
//                        );
                    }
                }
                
                @Override
                protected void hadSuccess() {
                    outline.revalidate();
                    outline.repaint();
                    AlignmentBoardItemChangeEvent event;
                    if (parent!=null && isVisible) {
                         event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, parent, ChangeType.VisibilityChange);
                    }
                    else {
                        event = new AlignmentBoardItemChangeEvent(
                                alignmentBoardContext, alignmentBoardItem, ChangeType.VisibilityChange);
                    }
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
