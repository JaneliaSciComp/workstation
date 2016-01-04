package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.AnnotatedImageButton;
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

/**
 * A generic table viewer for a specific object type. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TableViewerPanel<T,S> extends JPanel {

    // Main components
    private final JPanel resultsPane;
    private final DynamicTable resultsTable;
    
    private List<T> objectList;
    private Map<S,T> imageObjectMap;
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
                    boolean clearAll = true;
                    for (T imageObject : objectList) {
                        selectImageObject(imageObject, clearAll);
                        clearAll = false;
                    }
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
                T imageObj = null;
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    clearAll = true;
                    if (e.isShiftDown()) {
                        imageObj = getPreviousObject();
                    }
                    else {
                        imageObj = getNextObject();
                    }
                }
                else {
                    clearAll = true;
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        imageObj = getPreviousObject();
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        imageObj = getNextObject();
                    }
                }

                if (imageObj != null) {
                    S id = getImageModel().getImageUniqueId(imageObj);
                    selectImageObject(imageObj, clearAll);
//                    getDynamicTable().requestFocus();
                    // TODO: scroll to item
                    //imagesPanel.scrollObjectToCenter(imageObj);
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
                // TODO: handle multiple selection
                return super.createPopupMenu(e);
            }

            @Override
            protected void rowClicked(int row) {
                if (row < 0) {
                    return;
                }
                DynamicRow drow = getRows().get(row);
                T object = (T) drow.getUserObject();
                //objectSelected(object);
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

    protected void selectImageObject(T imageObject, boolean clearAll) {
        int last = objectList.indexOf(imageObject);
        
        if (clearAll) {
            selectRange(last, last);
        }
        else {
            int first = 0;
            int i = 0;
            for (T object : objectList) {
                if (selectionModel.isObjectSelected(object)) {
                    first = i;
                    break;
                }
                i++;
            }
            
            selectRange(first, last);
        }
        
        selectionModel.select(imageObject, clearAll);
    }

    protected void deselectImageObject(T imageObject) {
        final S id = getImageModel().getImageUniqueId(imageObject);
        // TODO: is there a better way to implement this?
        selectNone();
        selectionModel.deselect(imageObject);
    }
    
//    protected abstract JPopupMenu getContextualPopupMenu();
    
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
        return imageObjectMap.get(uniqueId);
    }
    
    protected void showObjects(List<T> objectList) {
        
        this.objectList = objectList;
        this.imageObjectMap = new HashMap<>();
        for(T imageObject : objectList) {
            imageObjectMap.put(getImageModel().getImageUniqueId(imageObject), imageObject);
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
