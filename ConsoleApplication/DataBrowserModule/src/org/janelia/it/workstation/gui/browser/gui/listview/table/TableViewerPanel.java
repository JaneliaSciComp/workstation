package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
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
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
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
    private final JPanel resultsPane;
    private final DynamicTable resultsTable;
    
    private List<T> objectList;
    private Map<S,T> objectMap;
    private ImageModel<T,S> imageModel;
    private SelectionModel<T,S> selectionModel;
    private SearchProvider searchProvider;

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

                // Space on a single entity triggers a preview 
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // TODO: notify our hud container
//                    updateHud(true);
                    e.consume();
                    return;
                }

                // Enter with a single entity selected triggers an outline
                // navigation
//                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//                    List<S> selectedIds = selectionModel.getSelectedIds();
//                    if (selectedIds.size() != 1) {
//                        return;
//                    }
//                    S selectedId = selectedIds.get(0);
//                    T selectedObject = getImageByUniqueId(selectedId);
//                    selectionModel.select(selectedObject, true);
//                    return;
//                }

                // Delete triggers deletion
//                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
//                    List<AnnotatedImageButton<T,S>> selected = imagesPanel.getSelectedButtons();
//                    List<T> toDelete = new ArrayList<>();
//                    for(AnnotatedImageButton<T,S> button : selected) {
//                        T imageObject = button.getImageObject();
//                        toDelete.add(imageObject);
//                    }
//                    
//                    if (selected.isEmpty()) {
//                        return;
//                    }
//                    // TODO: implement DomainObject deletion
////                    final Action action = new RemoveEntityAction(toDelete, true, false);
////                    action.doAction();
//                    e.consume();
//                    return;
//                }

                // Tab and arrow navigation to page through the images
                boolean clearAll = false;
                T object = null;
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    clearAll = true;
                    if (e.isShiftDown()) {
                        object = getPreviousObject();
                    }
                    else {
                        object = getNextObject();
                    }
                }
                else {
                    clearAll = true;
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        object = getPreviousObject();
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        object = getNextObject();
                    }
                }

                if (object != null) {
                    selectObjects(Arrays.asList(object), true, clearAll);
                }
            }

            revalidate();
            repaint();
        }
    };
    
    public TableViewerPanel() {

        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        setFocusable(true);
        
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
                boolean clearAll = true;
                for(Object object : resultsTable.getSelectedObjects()) {
                    selectionModel.select((T)object, clearAll);
                    clearAll = false;
                }
            }
        });
        
        resultsTable.setMaxColWidth(80);
        resultsTable.setMaxColWidth(600);
        resultsTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 5));

        resultsPane = new JPanel(new BorderLayout());
        resultsPane.add(resultsTable, BorderLayout.CENTER);
        
        add(resultsPane, BorderLayout.CENTER);
        
    }
    
    protected abstract JPopupMenu getContextualPopupMenu();

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

    public void selectObjects(List<T> domainObjects, boolean select, boolean clearAll) {

        log.info("selectObjects(domainObjects.size={},select={},clearAll={})",domainObjects.size(),select,clearAll);

        if (domainObjects.isEmpty()) {
            return;
        }
        
        if (!select) {
            // TODO: this needs better logic for targeted deselection 
            selectNone();
            return;
        }

        // The table API only allows for contiguous range selection, so given a list of objects,
        // the best we can do is find the first and select everything after it until we find an object
        // that should not be selected. 
        Set<T> domainObjectSet = new HashSet<>(domainObjects);
        Integer start = null;
        Integer end = null;
        int i = 0;
        for(DynamicRow row : getRows()) {
            DomainObject rowObject = (DomainObject)row.getUserObject();
            if (domainObjectSet.contains(rowObject)) {
                if (start==null) {
                    start = i;
                }
            }
            else {
                if (start!=null) {
                    end = i-1;
                    break;
                }
            }
            i++;
        }
        
        if (start!=null) {
            if (end==null) end = i-1;
            log.info("Selecting range: {} - {}",start,end);
            selectRange(start, end);
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
    }
    
    protected void updateTableModel() {
        resultsTable.updateTableModel();
    }
}
