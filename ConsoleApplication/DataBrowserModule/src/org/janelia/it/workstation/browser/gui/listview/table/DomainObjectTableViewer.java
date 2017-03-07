package org.janelia.it.workstation.browser.gui.listview.table;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.DynamicDomainObjectProxy;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.dialogs.TableViewerConfigDialog;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.listview.ListViewerType;
import org.janelia.it.workstation.browser.gui.listview.icongrid.IconGridViewerConfiguration;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table viewer for domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectTableViewer extends TableViewerPanel<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectTableViewer.class);

    private static final String COLUMN_KEY_ANNOTATIONS = "annotations";

    // Configuration
    private IconGridViewerConfiguration config;
    private final DomainObjectAttribute annotationAttr = new DomainObjectAttribute(COLUMN_KEY_ANNOTATIONS,"Annotations",null,null,true,null,null);
    private final Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();

    // These members deal with the context and entities within it
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;
    private SearchProvider searchProvider;
    private List<DomainObjectAttribute> attrs;

    // UI state
    private String sortField;
    private boolean ascending = true;

    private final ImageModel<DomainObject,Reference> imageModel = new ImageModel<DomainObject, Reference>() {

        @Override
        public Reference getImageUniqueId(DomainObject domainObject) {
            return Reference.createFor(domainObject);
        }

        @Override
        public String getImageFilepath(DomainObject domainObject) {
            return null;
        }

        @Override
        public BufferedImage getStaticIcon(DomainObject imageObject) {
            return null;
        }

        @Override
        public DomainObject getImageByUniqueId(Reference id) throws Exception {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }

        @Override
        public String getImageTitle(DomainObject domainObject) {
            String titlePattern = config.getDomainClassTitle(domainObject.getClass().getSimpleName());
            if (StringUtils.isEmpty(titlePattern)) return domainObject.getName();
            DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
            return StringUtils.replaceVariablePattern(titlePattern, proxy);
        }

        @Override
        public String getImageSubtitle(DomainObject domainObject) {
            String subtitlePattern = config.getDomainClassSubtitle(domainObject.getClass().getSimpleName());
            if (StringUtils.isEmpty(subtitlePattern)) return null;
            DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(domainObject);
            return StringUtils.replaceVariablePattern(subtitlePattern, proxy);
        }

        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            return domainObjectList.getAnnotations(domainObject.getId());
        }
    };

    public DomainObjectTableViewer() {
        setImageModel(imageModel);
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setSelectionModel(DomainObjectSelectionModel selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }

    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        super.selectObjects(domainObjects, select, clearAll, isUserDriven);
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    @Override
    public void showDomainObjects(final AnnotatedDomainObjectList domainObjectList, final Callable<Void> success) {

        try {
            this.config = IconGridViewerConfiguration.loadConfig();
        } catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }

        this.domainObjectList = domainObjectList;
        log.debug("showDomainObjects(domainObjectList={})",DomainUtils.abbr(domainObjectList.getDomainObjects()));

        attributeMap.clear();

        attrs = DomainUtils.getDisplayAttributes(domainObjectList.getDomainObjects());
        attrs.add(0, annotationAttr);

        TableViewerConfiguration config = TableViewerConfiguration.loadConfig();

        getDynamicTable().clearColumns();
        for(DomainObjectAttribute attr : attrs) {
            attributeMap.put(attr.getName(), attr);
            boolean visible = config.isColumnVisible(attr.getName());
            boolean sortable = !COLUMN_KEY_ANNOTATIONS.equals(attr.getName());
            getDynamicTable().addColumn(attr.getName(), attr.getLabel(), visible, false, true, sortable);
        }

        showObjects(domainObjectList.getDomainObjects(), success);
        setSortCriteria(searchProvider.getSortField());
    }

    @Override
    public void refreshDomainObject(DomainObject domainObject) {        
        domainObjectList.updateObject(domainObject);
        showDomainObjects(domainObjectList, null);
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {

        List<Reference> ids = selectionModel.getSelectedIds();
        try {
            List<DomainObject> selected = DomainMgr.getDomainMgr().getModel().getDomainObjects(ids);
            // TODO: should this use the same result as the icon grid viewer?
            DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject) selectionModel.getParentObject(), selected, ResultDescriptor.LATEST, null);

            JTable table = getTable();
            ListSelectionModel lsm = table.getSelectionModel();
            if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {
                String value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()).toString();
                final String label = value == null ? "null" : value;

                JMenuItem titleMenuItem = new JMenuItem(label);
                titleMenuItem.setEnabled(false);
                popupMenu.add(titleMenuItem);

                JMenuItem copyMenuItem = new JMenuItem("  Copy Value To Clipboard");
                copyMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Transferable t = new StringSelection(label);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                    }
                });
                popupMenu.add(copyMenuItem);
            }

            popupMenu.addMenuItems();

            return popupMenu;
        } catch (Exception ex) {
            ConsoleApp.handleException(ex);
            return null;
        }
    }

    protected JPopupMenu getColumnPopupMenu(final int col) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem hideItem = new JMenuItem("Hide this column");
        hideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DynamicColumn column = getDynamicTable().getVisibleColumn(col);
                log.info("Hiding column {} ({})",column.getLabel(),col);
                try {
                    TableViewerConfiguration config = TableViewerConfiguration.loadConfig();
                    config.getHiddenColumns().add(column.getName());
                    config.save();
                    column.setVisible(false);
                    updateTableModel();
                }
                catch (Exception ex) {
                    ConsoleApp.handleException(ex);
                }
            }
        });
        menu.add(hideItem);
        return menu;
    }

    @Override
    protected void objectDoubleClicked(DomainObject object) {
        try {
            getContextualPopupMenu().runDefaultAction();
        } catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }

    @Override
    protected void chooseColumnsButtonPressed() {
        TableViewerConfigDialog configDialog = new TableViewerConfigDialog(attrs);
        if (configDialog.showDialog(this)==1) {
            TableViewerConfiguration config = configDialog.getConfig();
            for(String attrName : attributeMap.keySet()) {
                boolean visible = config.isColumnVisible(attrName);
                getColumn(attrName).setVisible(visible);
            }
            updateTableModel();
        }
    }

    @Override
    protected void deleteKeyPressed() {
        // TODO: this was copy and pasted from DomainObjectIconGridViewer and should be refactored someday
        try {
            IsParent parent = selectionModel.getParentObject();
            if (parent instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) parent;
                if (ClientDomainUtils.hasWriteAccess(treeNode)) {
                    RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(treeNode, getSelectedObjects());
                    action.actionPerformed(null);
                }
            }
        } catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }

    @Override
    protected void exportButtonPressed() {
        searchProvider.export();
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }


    // TODO: implement this so things like neuron fragments can be edited in table mode
    @Override
    public void toggleEditMode(boolean editMode) {

    }

    @Override
    public void refreshEditMode() {

    }

    // TODO: implement this so things like neuron fragments can be edited in table mode
    @Override
    public void setEditSelectionModel(DomainObjectSelectionModel editSelectionModel) {

    }

    // TODO: implement this so things like neuron fragments can be edited in table mode
    @Override
    public DomainObjectSelectionModel getEditSelectionModel() {
        return null;
    }

    // TODO: implement this so things like neuron fragments can be edited in table mode
    @Override
    public void selectEditObjects(List<DomainObject> domainObjects, boolean select) {

    }

    public AnnotatedDomainObjectList getDomainObjectList() {
        return domainObjectList;
    }

    @Override
    public boolean matches(ResultPage resultPage, DomainObject domainObject, String text) {
        log.debug("Searching {} for {}",domainObject.getName(),text);

        String tupper = text.toUpperCase();

        // Exact matches on id or name always work
        if (domainObject.getId().toString().equals(text) || domainObject.getName().toUpperCase().equals(tupper)) {
            return true;
        }

        for(DynamicColumn column : getColumns()) {
            if (column.isVisible()) {
                log.trace("Searching column "+column.getLabel());
                Object value = getValue(resultPage, domainObject, column.getName());
                if (value != null) {
                    if (value.toString().toUpperCase().contains(tupper)) {
                        log.trace("Found match in column {}: {}",column.getLabel(),value);
                        return true;
                    }
                }
            }
            else {
                log.trace("Skipping invisible column {}",column.getLabel());
            }
        }
        return false;
    }

    public Object getValue(AnnotatedDomainObjectList domainObjectList, DomainObject object, String columnName) {
        if (COLUMN_KEY_ANNOTATIONS.equals(columnName)) {
            StringBuilder builder = new StringBuilder();
            for(Annotation annotation : domainObjectList.getAnnotations(object.getId())) {
                if (builder.length()>0) builder.append(", ");
                builder.append(annotation.getName());
            }
            return builder.toString();
        }
        else {
            DomainObjectAttribute attr = attributeMap.get(columnName);
            DynamicDomainObjectProxy proxy = new DynamicDomainObjectProxy(object);
            return proxy.get(attr.getLabel());
        }
    }

    @Override
    protected void updateTableModel() {
        super.updateTableModel();
        getTable().setRowSorter(new DomainObjectRowSorter());
    }

    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !Hud.isInitialized()) return;
        
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
        
        try {
            List<DomainObject> selected = getSelectedObjects();

            if (selected.size() != 1) {
                hud.hideDialog();
                return;
            }

            DomainObject domainObject = selected.get(0);
            if (toggle) {
                hud.setObjectAndToggleDialog(domainObject, null, null);
            }
            else {
                hud.setObject(domainObject, null, null, false);
            }
        } 
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }
    
    private List<DomainObject> getSelectedObjects() throws Exception {
        return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
    }

    private void setSortCriteria(String sortCriteria) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(sortCriteria)) {
            setSortColumn(null, true);
        }
        else {
            this.sortField = (sortCriteria.startsWith("-") || sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
            this.ascending = !sortCriteria.startsWith("-");
            log.info("Setting sort column: {}",sortCriteria);
            setSortColumn(sortField, ascending);
        }
    }

    protected class DomainObjectRowSorter extends RowSorter<TableModel> {

        private List<SortKey> sortKeys = new ArrayList<>();

        public DomainObjectRowSorter() {
            List<DynamicColumn> columns = getDynamicTable().getDisplayedColumns();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).getName().equals(sortField)) {
                    sortKeys.add(new SortKey(i, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING));
                    break;
                }
            }
        }

        @Override
        public void toggleSortOrder(int columnNum) {
            List<DynamicColumn> columns = getDynamicTable().getDisplayedColumns();
            DynamicColumn column = columns.get(columnNum);
            if (!column.isVisible() || !column.isSortable()) {
                return;
            }
            SortOrder newOrder = SortOrder.ASCENDING;
            if (!sortKeys.isEmpty()) {
                SortKey currentSortKey = sortKeys.get(0);
                if (currentSortKey.getColumn() == columnNum) {
                    // Reverse the sort
                    if (currentSortKey.getSortOrder() == SortOrder.ASCENDING) {
                        newOrder = SortOrder.DESCENDING;
                    }
                }
                sortKeys.clear();
            }

            sortKeys.add(new SortKey(columnNum, newOrder));
            sortField = column.getName();
            ascending = (newOrder != SortOrder.DESCENDING);
            String prefix = ascending ? "+":"-";
            getSearchProvider().setSortField(prefix + sortField);
            getSearchProvider().search();
        }

        @Override
        public void setSortKeys(List<? extends SortKey> sortKeys) {
            this.sortKeys = new ArrayList<>(sortKeys);
        }

        @Override
        public void rowsUpdated(int firstRow, int endRow, int column) {
        }

        @Override
        public void rowsUpdated(int firstRow, int endRow) {
        }

        @Override
        public void rowsInserted(int firstRow, int endRow) {
        }

        @Override
        public void rowsDeleted(int firstRow, int endRow) {
        }

        @Override
        public void modelStructureChanged() {
        }

        @Override
        public int getViewRowCount() {
            return getTable().getModel().getRowCount();
        }

        @Override
        public List<? extends SortKey> getSortKeys() {
            return sortKeys;
        }

        @Override
        public int getModelRowCount() {
            return getTable().getModel().getRowCount();
        }

        @Override
        public TableModel getModel() {
            return getDynamicTable().getTableModel();
        }

        @Override
        public int convertRowIndexToView(int index) {
            return index;
        }

        @Override
        public int convertRowIndexToModel(int index) {
            return index;
        }

        @Override
        public void allRowsChanged() {
        }
    };

    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }

    public SearchProvider getSearchProvider() {
        return searchProvider;
    }

    @Override
    public ListViewerState saveState() {
        JScrollPane scrollPane = getDynamicTable().getScrollPane();
        
        int horizontalScrollValue = scrollPane.getHorizontalScrollBar().getModel().getValue();
        log.debug("Saving horizontalScrollValue={}",horizontalScrollValue);
        
        int verticalScrollValue = scrollPane.getVerticalScrollBar().getModel().getValue();
        log.debug("Saving verticalScrollValue={}",verticalScrollValue);
        
        return new TableViewerState(horizontalScrollValue, verticalScrollValue);
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
        final JScrollPane scrollPane = getDynamicTable().getScrollPane();
        final TableViewerState tableViewerState = (TableViewerState)viewerState;
        SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                   
                   int horizontalScrollValue = tableViewerState.getHorizontalScrollValue();
                   log.debug("Restoring horizontalScrollValue={}",horizontalScrollValue);
                   scrollPane.getHorizontalScrollBar().setValue(horizontalScrollValue);
                   
                   int verticalScrollValue = tableViewerState.getVerticalScrollValue();
                   log.debug("Restoring verticalScrollValue={}",verticalScrollValue);
                   scrollPane.getVerticalScrollBar().setValue(verticalScrollValue);
               }
           }
        );
    }

    private class TableViewerState extends ListViewerState {

        private int horizontalScrollValue;
        private int verticalScrollValue;

        public TableViewerState(int horizontalScrollValue, int verticalScrollValue) {
            super(ListViewerType.TableViewer);
            this.horizontalScrollValue = horizontalScrollValue;
            this.verticalScrollValue = verticalScrollValue;
        }

        public int getHorizontalScrollValue() {
            return horizontalScrollValue;
        }

        public int getVerticalScrollValue() {
            return verticalScrollValue;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TableViewerState [horizontalScrollValue=");
            builder.append(horizontalScrollValue);
            builder.append(", verticalScrollValue=");
            builder.append(verticalScrollValue);
            builder.append("]");
            return builder.toString();
        }
    }
}
