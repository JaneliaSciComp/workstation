package org.janelia.workstation.browser.gui.listview.table;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.lifecycle.SessionEvent;
import org.janelia.workstation.core.events.selection.SelectionModel;
import org.janelia.workstation.core.keybind.KeyBindings;
import org.janelia.workstation.core.keybind.KeyboardShortcut;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.common.gui.table.DynamicColumn;
import org.janelia.workstation.common.gui.table.DynamicRow;
import org.janelia.workstation.common.gui.table.DynamicTable;
import org.janelia.workstation.core.model.AnnotatedObjectList;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A generic table viewer supporting a data model and object selection.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TableViewerPanel<T,S> extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(TableViewerPanel.class);
    
    // Main components
    private TableViewerToolbar toolbar;
    private final JPanel resultsPane;
    private final DynamicTable resultsTable;

    // These members deal with the context and entities within it
    private List<T> objectList;
    private Map<S,T> objectMap;
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
        
    public TableViewerPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        toolbar = createToolbar();
        toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->TableViewerPanel"));

        resultsTable = new DynamicTable() {
            @Override
            @SuppressWarnings("unchecked")
            public Object getValue(Object userObject, DynamicColumn column) {
                return TableViewerPanel.this.getValue(getObjectList(), (T)userObject, column.getName());
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void rowDoubleClicked(int row) {
                objectDoubleClicked((T)getRows().get(row).getUserObject());
            }

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
                return getContextualPopupMenu();
            }

            @Override
            protected JPopupMenu createColumnPopupMenu(MouseEvent e, int col) {
                return getColumnPopupMenu(col);
            }
        };

        resultsTable.getTable().addKeyListener(keyListener);
        resultsTable.getTable().getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            // Synchronize the table selection to the selection model
            List<T> selectedObjects = new ArrayList<>();
            // Everything selected in the table should be selected in the model
            for (Object object : resultsTable.getSelectedObjects()) {
                @SuppressWarnings("unchecked")
                T obj = (T) object;
                S id = imageModel.getImageUniqueId(obj);
                selectedObjects.add(obj);
            }
            selectionModel.select(selectedObjects, true, true);
            updateHud(false);
        });
        
        resultsTable.setMaxColWidth(80);
        resultsTable.setMaxColWidth(600);
        resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));
        resultsTable.addMouseListener(new MouseForwarder(this, "DynamicTable->TableViewerPanel"));

        resultsPane = new JPanel(new BorderLayout());
        resultsPane.add(resultsTable, BorderLayout.CENTER);
    }
    
    private TableViewerToolbar createToolbar() {
        return new TableViewerToolbar() {

            @Override
            protected void refresh() {
                TableViewerPanel.this.totalRefresh();
            }

            @Override
            public void chooseColumnsButtonPressed() {
                TableViewerPanel.this.chooseColumnsButtonPressed();
            }

            @Override
            public void exportButtonPressed() {
                TableViewerPanel.this.exportButtonPressed();
            }
        };
    }

    // Listen for key strokes and execute the appropriate key bindings
    // TODO: this is copy & pasted from IconGridViewerPanel, and can probably be factored out into its own reusable class
    protected KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {

            if (KeymapUtil.isModifier(e)) {
                return;
            }
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return;
            }

            KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
            if (!KeyBindings.getKeyBindings().executeBinding(shortcut)) {
                // No keybinds matched, use the default behavior
                // Ctrl-A or Meta-A to select all
                if (e.getKeyCode() == KeyEvent.VK_A && ((SystemInfo.isMac && e.isMetaDown()) || (e.isControlDown()))) {
                    selectRange(0, objectList.size()-1);
                } 
                else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    updateHud(true);
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterKeyPressed();
                }
                else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteKeyPressed();
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    updateHud(false);
                }
                else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    updateHud(false);
                }
            }
            
            revalidate();
            repaint();
        }
    };

    public abstract Object getValue(AnnotatedObjectList<T,S> annotatedDomainObjectList, T object, String column);

    protected void enterKeyPressed() {
        T selectedObject = getLastSelectedObject();
        objectDoubleClicked(selectedObject);
    }

    protected void deleteKeyPressed() {}

    protected abstract void objectDoubleClicked(T object);

    protected abstract JPopupMenu getContextualPopupMenu();

    protected abstract JPopupMenu getColumnPopupMenu(int col);

    protected abstract void chooseColumnsButtonPressed();

    protected abstract void exportButtonPressed();

    protected void updateHud(boolean toggle) {}

    public abstract AnnotatedObjectList<T,S> getObjectList();

    public void selectObjects(List<T> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {

        log.info("selectObjects(objects.size={},select={},clearAll={},isUserDriven={},notifyModel={})", objects.size(),select,clearAll,isUserDriven,notifyModel);

        if (objects.isEmpty()) {
            return;
        }

        ListSelectionModel model = getDynamicTable().getTable().getSelectionModel();

        Set<T> domainObjectSet = new HashSet<>(objects);
        int i = 0;
        Integer start = null;
        for(DynamicRow row : resultsTable.getRows()) {
            @SuppressWarnings("unchecked")
            T rowObject = (T)row.getUserObject();
            if (domainObjectSet.contains(rowObject)) {
                if (select) {
                    if (!model.isSelectedIndex(i)) {
                        model.addSelectionInterval(i, i);
                    }
                    if (start==null) start = i;
                }
                else {
                    if (model.isSelectedIndex(i)) {
                        model.removeSelectionInterval(i, i);
                    }
                }
            }
            else if (clearAll) {
                if (model.isSelectedIndex(i)) {
                    model.removeSelectionInterval(i, i);
                }
            }
            i++;
        }


        if (select) {
            selectionModel.select(objects, clearAll, isUserDriven);
        }
        else {
            selectionModel.deselect(objects, isUserDriven);
        }

        if (start!=null) {
            SwingUtilities.invokeLater(() -> scrollSelectedObjectsToCenter());
        }
    }
    
    private void scrollSelectedObjectsToCenter() {
        ListSelectionModel model = getDynamicTable().getTable().getSelectionModel();
        int start = model.getMinSelectionIndex();
        log.debug("Scrolling to start of selection at row {}",start);
        getDynamicTable().scrollCellToCenter(start, 0);   
    }

    protected void selectNone() {
        resultsTable.getTable().getSelectionModel().clearSelection();
    }
    
    protected void selectRange(int index1, int index2) {
        resultsTable.getTable().getSelectionModel().setSelectionInterval(index1, index2);
    }

    public synchronized T getLastSelectedObject() {
        S uniqueId = selectionModel.getLastSelectedId();
        if (uniqueId == null) {
            return null;
        }
        return objectMap.get(uniqueId);
    }

    public void showObjects(List<T> objectList, final Callable<Void> success) {

        log.debug("showObjects(objects.size={})", objectList.size());

        setObjects(objectList);

        resultsTable.removeAllRows();
        for (T object : objectList) {
            resultsTable.addRow(object);
        }
        updateTableModel();

        // Actually display everything
        showAll();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Finally, we're done, we can call the success callback
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }
        });
    }

    public void refresh() {
        showObjects(objectList, null);
    }

    public void totalRefresh() {
        DomainMgr.getDomainMgr().getModel().invalidateAll();
    }
    
    private void setObjects(List<T> objectList) {
        log.debug("Setting {} objects", objectList.size());
        this.objectList = objectList;
        this.objectMap = new HashMap<>();
        for(T object : objectList) {
            objectMap.put(getImageModel().getImageUniqueId(object), object);
        }
    }

    protected List<T> getObjects() {
        return objectList;
    }
    
    public synchronized void clear() {
        this.objectList = null;
        removeAll();
        revalidate();
        repaint();
    }

    public synchronized void showAll() {
        removeAll();
        add(toolbar, BorderLayout.NORTH);
        add(resultsPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    protected void updateTableModel() {
        resultsTable.updateTableModel();
    }

    public List<DynamicColumn> getColumns() {
        return resultsTable.getColumns();
    }

    public DynamicColumn getColumn(String columnName) {
        return resultsTable.getColumn(columnName);
    }

    public int getColumnIndex(String columnName) {
        return resultsTable.getTableColumnIndex(getColumn(columnName));
    }

    public void setSortColumn(String columnName, boolean ascending) {
        if (StringUtils.isEmpty(columnName)) {
            getTable().getRowSorter().setSortKeys(new ArrayList<>());
        }
        else {
            int index = getColumnIndex(columnName);
            if (index >= 0) {
                getTable().getRowSorter().setSortKeys(Collections.singletonList(
                        new RowSorter.SortKey(index, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
            } 
            else {
                log.error("Sort column does not exist: " + columnName);
            }
        }
    }

    protected DynamicTable getDynamicTable() {
        return resultsTable;
    }

    protected JTable getTable() {
        return resultsTable.getTable();
    }

    public ImageModel<T, S> getImageModel() {
        return imageModel;
    }

    public void setImageModel(ImageModel<T, S> imageModel) {
        this.imageModel = imageModel;
    }
    
    public void setSelectionModel(SelectionModel<T,S> selectionModel) {
        selectionModel.setSource(this);
        this.selectionModel = selectionModel;
    }
    
    public SelectionModel<T,S> getSelectionModel() {
        return selectionModel;
    }

    public TableViewerToolbar getToolbar() {
        return toolbar;
    }

    public void scrollObjectToCenter(T object) {
        int row = objectList.indexOf(object);
        getDynamicTable().scrollCellToCenter(row, 0);
    }
    
    @Subscribe
    public void sessionChanged(SessionEvent event) {
        TableViewerPanel.this.clear();
    }
        
}
