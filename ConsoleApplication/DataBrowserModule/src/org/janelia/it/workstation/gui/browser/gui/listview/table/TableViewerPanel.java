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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position.Bias;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.find.FindContext;
import org.janelia.it.workstation.gui.browser.gui.find.FindToolbar;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicRow;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicTable;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic table viewer for a specific object type. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TableViewerPanel<T,S> extends JPanel implements FindContext {

    private static final Logger log = LoggerFactory.getLogger(TableViewerPanel.class);
    
    // Main components
    private TableViewerToolbar toolbar;
    private final JPanel resultsPane;
    private final DynamicTable resultsTable;
    private FindToolbar findToolbar;

    // These members deal with the context and entities within it
    private List<T> objectList;
    private Map<S,T> objectMap;
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
    private SearchProvider searchProvider;
    
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
                // Synchronize the table selection to the selection model
                Set<S> selectedIds = new HashSet<>();
                // Everything selected in the table should be selected in the model
                for(Object object : resultsTable.getSelectedObjects()) {
                    T obj = (T)object;
                    selectedIds.add(imageModel.getImageUniqueId(obj));
                    selectionModel.select(obj, false);
                }
                // Clear out everything that was not selected above
                for(S selectedId : new ArrayList<>(selectionModel.getSelectedIds())) {
                    if (!selectedIds.contains(selectedId)) {
                        selectionModel.deselect(imageModel.getImageByUniqueId(selectedId));
                    }
                }
            }
        });
        
        resultsTable.setMaxColWidth(80);
        resultsTable.setMaxColWidth(600);
        resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));
        resultsTable.addMouseListener(new MouseForwarder(this, "DynamicTable->TableViewerPanel"));

        findToolbar = new FindToolbar(this);
        findToolbar.addMouseListener(new MouseForwarder(this, "FindToolbar->TableViewerPanel"));
        
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
                searchProvider.export();
            }
        };
    }

    protected abstract JPopupMenu getContextualPopupMenu();
    
    protected abstract void chooseColumnsButtonPressed();
    
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

    protected void selectObject(T object, boolean clearAll) {
        selectObjects(Arrays.asList(object), true, clearAll);
        selectionModel.select(object, clearAll);
    }

    protected void deselectObject(T object) {
        selectObjects(Arrays.asList(object), false, false);
        selectionModel.deselect(object);
    }
    
    public void selectObjects(List<T> domainObjects, boolean select, boolean clearAll) {

        log.trace("selectObjects(domainObjects.size={},select={},clearAll={})",domainObjects.size(),select,clearAll);

        if (domainObjects.isEmpty()) {
            return;
        }
        
        ListSelectionModel model = getDynamicTable().getTable().getSelectionModel();
        
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
        showObjects(objectList);
    }
    
    public void totalRefresh() {
        DomainMgr.getDomainMgr().getModel().invalidateAll();
    }

    protected Map<S, T> getObjectMap() {
        return objectMap;
    }
    
    public List<T> getObjectList() {
        return objectList;
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
        add(findToolbar, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }
    
    public void scrollObjectToCenter(T object) {
        int row = objectList.indexOf(object);
        getDynamicTable().scrollToVisible(row, 0);
    }
    
    @Override
    public void showFindUI() {
        findToolbar.open();
    }

    @Override
    public void hideFindUI() {
        findToolbar.close();
    }

    @Override
    public void findPrevMatch(String text, boolean skipStartingNode) {
        TableViewerFind<T,S> searcher = new TableViewerFind<>(this, text, getLastSelectedObject(), Bias.Backward, skipStartingNode);
        T match = searcher.find();
        if (match != null) {
            selectObject(match, true);
            scrollObjectToCenter(match);
        }
    }

    @Override
    public void findNextMatch(String text, boolean skipStartingNode) {
        TableViewerFind<T,S> searcher = new TableViewerFind<>(this, text, getLastSelectedObject(), Bias.Forward, skipStartingNode);
        T match = searcher.find();
        if (match != null) {
            selectObject(match, true);
            scrollObjectToCenter(match);
        }
    }

    @Override
    public void openMatch() {
    }
}
