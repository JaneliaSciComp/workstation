package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic table viewer for a specific object type. 
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
    private SearchProvider searchProvider;

    // UI state
    private Integer selectionAnchorIndex;
    private Integer selectionCurrIndex;
    
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
            protected JPopupMenu createPopupMenu(MouseEvent e) {
                return getContextualPopupMenu();
            }
        };

        resultsTable.getTable().addKeyListener(keyListener);
        resultsTable.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                Set<Object> set = new HashSet<>(resultsTable.getSelectedObjects());
                for(T object : objectList) {
                    if (set.contains(object)) {
                        // Should be selected
                        if (!selectionModel.isObjectSelected(object)) {
                            selectionModel.select(object, false);
                        }      
                    }
                    else {
                        // Should not be selected
                        if (selectionModel.isObjectSelected(object)) {
                            selectionModel.deselect(object);
                        }      
                    }
                }
            }
        });
        
        resultsTable.setMaxColWidth(80);
        resultsTable.setMaxColWidth(600);
        resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));

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
                
            }

            @Override
            public void exportButtonPressed() {
                
            }
        };
    }
    
    protected abstract JPopupMenu getContextualPopupMenu();
    
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
                    searchProvider.userRequestedSelectAll();
                    return;
                } 
                else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // TODO: notify our hud container
//                    updateHud(true);
                    e.consume();
                    return;
                }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterKeyPressed();
                    return;
                }
                else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteKeyPressed();
                    e.consume();
                    return;
                }
            }

            revalidate();
            repaint();
        }
    };

    protected void enterKeyPressed() {}
    
    protected void deleteKeyPressed() {}

    private void beginRangeSelection(int anchorIndex) {
        selectionAnchorIndex = selectionCurrIndex = anchorIndex;
    }
    
    private void endRangeSelection() {
        selectionAnchorIndex = selectionCurrIndex = null;
    }

    protected void selectObject(T object, boolean clearAll) {
        selectObjects(Arrays.asList(object), true, clearAll);
        selectionModel.select(object, clearAll);
    }

    protected void deselectObject(T object) {
        selectObjects(Arrays.asList(object), false, false);
        selectionModel.deselect(object);
    }
    
    public void selectObjects(List<T> domainObjects, boolean select, boolean clearAll) {

        log.info("selectObjects(domainObjects.size={},select={},clearAll={})",domainObjects.size(),select,clearAll);

        if (domainObjects.isEmpty()) {
            return;
        }
        
        ListSelectionModel model = getDynamicTable().getTable().getSelectionModel();
        
//        if (clearAll) {
//            model.clearSelection();
//        }
        
        Set<T> domainObjectSet = new HashSet<>(domainObjects);
        int i = 0;
        Integer start = 0;
        for(DynamicRow row : getRows()) {
            DomainObject rowObject = (DomainObject)row.getUserObject();
            if (domainObjectSet.contains(rowObject)) {
                if (select) {
                    model.addSelectionInterval(i, i);
                    if (start==null) start = i;
                }
                else {
                    model.removeSelectionInterval(i, i);
                }
            }
            else if (clearAll) {
                model.removeSelectionInterval(i, i);
            }
            i++;
        }
        
        if (start!=null) {
            getDynamicTable().scrollToVisible(start, 0);
        }
    }

    protected abstract Object getValue(T object, String column);
    
    public void setAttributeColumns(List<DomainObjectAttribute> searchAttrs) {
        resultsTable.clearColumns();
        for(DomainObjectAttribute searchAttr : searchAttrs) {
            // TODO: control default visibility based on saved user preference
            resultsTable.addColumn(searchAttr.getName(), searchAttr.getLabel(), searchAttr.isDisplay(), false, true, searchAttr.isSortable());
        }
    }
    
    protected DynamicTable getDynamicTable() {
    	return resultsTable;
    }
    
    protected JTable getTable() {
    	return resultsTable.getTable();
    }
    
    protected List<DynamicRow> getRows() {
        return resultsTable.getRows();
    }

    protected void selectNone() {
        resultsTable.getTable().getSelectionModel().clearSelection();
    }
    
    protected void selectRange(int index1, int index2) {
        resultsTable.getTable().getSelectionModel().setSelectionInterval(index1, index2);
    }

    public T getPreviousObject() {
        if (objectList == null) {
            return null;
        }
        int i = objectList.indexOf(getLastSelectedObject());
        if (i < 1) {
            // Already at the beginning
            return null;
        }
        return objectList.get(i - 1);
    }

    public T getNextObject() {
        if (objectList == null) {
            return null;
        }
        int i = objectList.indexOf(getLastSelectedObject());
        if (i > objectList.size() - 2) {
            // Already at the end
            return null;
        }
        return objectList.get(i + 1);
    }

    public synchronized T getLastSelectedObject() {
        S uniqueId = selectionModel.getLastSelectedId();
        if (uniqueId == null) {
            return null;
        }
        return objectMap.get(uniqueId);
    }
    
    protected void showObjects(List<T> objectList) {
        
        this.objectList = objectList;
        this.objectMap = new HashMap<>();
        for(T object : objectList) {
            objectMap.put(getImageModel().getImageUniqueId(object), object);
        }
        
        resultsTable.removeAllRows();
        for (T object : objectList) {
            resultsTable.addRow(object);
        }
        updateTableModel();
        showAll();
    }
    
    protected void updateTableModel() {
        resultsTable.updateTableModel();
    }
    public void refresh() {
        refresh(false, null);
    }

    public void totalRefresh() {
        refresh(true, null);
    }

    public void refresh(final Callable<Void> successCallback) {
        refresh(false, successCallback);
    }

    public void totalRefresh(final Callable<Void> successCallback) {
        refresh(true, successCallback);
    }

    private AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public void refresh(final boolean invalidateCache, final Callable<Void> successCallback) {
        // TODO: implement
    }

    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }
    
    public SearchProvider getSearchProvider() {
        return searchProvider;
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
}
