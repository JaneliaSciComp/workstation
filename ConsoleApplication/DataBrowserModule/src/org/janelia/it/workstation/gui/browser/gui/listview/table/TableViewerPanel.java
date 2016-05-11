package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicRow;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicTable;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    // Listeners
    private final SessionModelListener sessionModelListener;
    
    public TableViewerPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);

        toolbar = createToolbar();
        toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->TableViewerPanel"));

        resultsTable = new DynamicTable() {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                return TableViewerPanel.this.getValue((T)userObject, column.getName());
            }

            @Override
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
        resultsTable.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                // Synchronize the table selection to the selection model
                Set<S> selectedIds = new HashSet<>();
                // Everything selected in the table should be selected in the model
                for(Object object : resultsTable.getSelectedObjects()) {
                    T obj = (T)object;
                    S id = imageModel.getImageUniqueId(obj);
                    selectedIds.add(id);
                    if (!selectionModel.isSelected(id)) {
                        selectionModel.select(obj, false, true);
                    }
                }
                // Clear out everything that was not selected above
                for(S selectedId : new ArrayList<>(selectionModel.getSelectedIds())) {
                    if (!selectedIds.contains(selectedId)) {
                        T object = imageModel.getImageByUniqueId(selectedId);
                        if (selectionModel.isSelected(selectedId)) {
                            selectionModel.deselect(object, true);
                        }
                    }
                }
                updateHud(false);
            }
        });
        
        resultsTable.setMaxColWidth(80);
        resultsTable.setMaxColWidth(600);
        resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));
        resultsTable.addMouseListener(new MouseForwarder(this, "DynamicTable->TableViewerPanel"));

        resultsPane = new JPanel(new BorderLayout());
        resultsPane.add(resultsTable, BorderLayout.CENTER);

        sessionModelListener = new SessionModelAdapter() {
            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if (key == "console.serverLogin") {
                    TableViewerPanel.this.clear();
                }
            }
        };
        
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
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
            if (!SessionMgr.getKeyBindings().executeBinding(shortcut)) {
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

    protected abstract Object getValue(T object, String column);

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

    public void selectObjects(List<T> objects, boolean select, boolean clearAll, boolean isUserDriven) {

        log.trace("selectObjects(objects.size={},select={},clearAll={},isUserDriven={})", objects.size(),select,clearAll,isUserDriven);

        if (objects.isEmpty()) {
            return;
        }
        
        ListSelectionModel model = getDynamicTable().getTable().getSelectionModel();
        
        Set<T> domainObjectSet = new HashSet<>(objects);
        int i = 0;
        Integer start = null;
        for(DynamicRow row : resultsTable.getRows()) {
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

        if (start!=null && isUserDriven) {
            log.debug("scrolling to start of selection at row {}",start);
            getDynamicTable().scrollCellToCenter(start, 0);
        }

        for(T object : objects) {
            if (select) {
                selectionModel.select(object, clearAll, isUserDriven);
            }
            else {
                selectionModel.deselect(object, isUserDriven);
            }
        }
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

    public synchronized void clear() {
        this.objectList = null;
        removeAll();
        revalidate();
        repaint();
    }

    public void close() {
        // TODO: this should be invoked somehow if the panel is closed
        SessionMgr.getSessionMgr().removeSessionModelListener(sessionModelListener);
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
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

    protected DynamicTable getDynamicTable() {
        return resultsTable;
    }

    protected JTable getTable() {
        return resultsTable.getTable();
    }

    protected ImageModel<T, S> getImageModel() {
        return imageModel;
    }

    protected void setImageModel(ImageModel<T, S> imageModel) {
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
}
