package org.janelia.workstation.browser.gui.listview.table;


import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainObjectAttribute;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.DynamicDomainObjectProxy;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.workspace.Node;
import org.janelia.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.workstation.browser.actions.RemoveItemsActionListener;
import org.janelia.workstation.browser.gui.dialogs.TableViewerConfigDialog;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.common.gui.listview.ListViewer;
import org.janelia.workstation.common.gui.listview.ListViewerActionListener;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.PreferenceSupport;
import org.janelia.workstation.common.gui.support.SearchProvider;
import org.janelia.workstation.common.gui.table.DynamicColumn;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.AnnotatedObjectList;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.search.ResultPage;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A table viewer for domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectTableViewer extends TableViewerPanel<DomainObject,Reference> implements ListViewer<DomainObject, Reference> {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectTableViewer.class);

    private static final String COLUMN_KEY_ANNOTATIONS = "annotations";

    // Configuration
    private TableViewerConfiguration config;
    private final DomainObjectAttribute annotationAttr = new DomainObjectAttribute(COLUMN_KEY_ANNOTATIONS,"Annotations",null,null,true,null,null);
    private final Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();

    // State
    private PreferenceSupport preferenceSupport;
    private AnnotatedObjectList<DomainObject,Reference> domainObjectList;
    private ChildSelectionModel<DomainObject, Reference> selectionModel;
    private SearchProvider searchProvider;
    private List<DomainObjectAttribute> attrs;

    // UI state
    private String sortField;
    private boolean ascending = true;

    private final DomainObjectImageModel imageModel = new DomainObjectImageModel() {

        @Override
        public ArtifactDescriptor getArtifactDescriptor() {
            return null;
        }

        @Override
        public String getImageTypeName() {
            return null;
        }

        @Override
        protected String getTitlePattern(Class<? extends DomainObject> clazz) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        protected String getSubtitlePattern(Class<? extends DomainObject> clazz) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            if (domainObjectList==null) return Collections.emptyList();
            return domainObjectList.getAnnotations(Reference.createFor(domainObject));
        }

        @Override
        public List<Decorator> getDecorators(DomainObject imageObject) {
            return SampleUIUtils.getDecorators(imageObject);
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
    public void setActionListener(ListViewerActionListener listener) {
        // Ignored, because this viewer does not hide objects
    }

    @Override
    public void setSelectionModel(ChildSelectionModel<DomainObject, Reference> selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }

    @Override
    public ChildSelectionModel<DomainObject, Reference> getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void setPreferenceSupport(PreferenceSupport preferenceSupport) {
        this.preferenceSupport = preferenceSupport;
    }

    @Override
    public PreferenceSupport getPreferenceSupport() {
        return preferenceSupport;
    }
    
    @Override
    public int getNumItemsHidden() {
        if (domainObjectList==null || getObjects()==null) return 0;
        int totalItems = this.domainObjectList.getObjects().size();
        int totalVisibleItems = getObjects().size();
        if (totalVisibleItems > totalItems) {
            log.warn("Visible item count greater than total item count");
            return 0;
        }
        return totalItems-totalVisibleItems;
    }
        
    @Override
    public void select(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        super.selectObjects(domainObjects, select, clearAll, isUserDriven, notifyModel);
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    @Override
    public void show(final AnnotatedObjectList<DomainObject,Reference> domainObjectList, final Callable<Void> success) {

        try {
            this.config = TableViewerConfiguration.loadConfig();
        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }

        this.domainObjectList = domainObjectList;
        
        log.debug("showDomainObjects(domainObjectList={})",DomainUtils.abbr(domainObjectList.getObjects()));

        attributeMap.clear();

        attrs = DomainUtils.getDisplayAttributes(domainObjectList.getObjects());
        attrs.add(0, annotationAttr);

        getDynamicTable().clearColumns();
        for(DomainObjectAttribute attr : attrs) {
            attributeMap.put(attr.getName(), attr);
            boolean visible = config.isColumnVisible(attr.getName());
            boolean sortable = !COLUMN_KEY_ANNOTATIONS.equals(attr.getName());
            getDynamicTable().addColumn(attr.getName(), attr.getLabel(), visible, false, true, sortable);
        }

        showObjects(domainObjectList.getObjects(), success);
        setSortCriteria(searchProvider.getSortField());
    }

    @Override
    public void refresh(DomainObject domainObject) {
        if (domainObjectList==null) {
            log.warn("Refreshing object {} when object list is null", domainObject);
            return;
        }
        domainObjectList.updateObject(domainObject);
        show(domainObjectList, null);
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {

        List<Reference> ids = selectionModel.getSelectedIds();
        try {
            List<DomainObject> selected = DomainMgr.getDomainMgr().getModel().getDomainObjects(ids);
            // TODO: should this use the same result as the icon grid viewer?
            DomainObjectContextMenu popupMenu = new DomainObjectContextMenu(
                    selectionModel,
                    null,
                    imageModel);

            JTable table = getTable();
            ListSelectionModel lsm = table.getSelectionModel();
            if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {
                
                int selectedRow = table.getSelectedRow();
                int selectedCol = table.getSelectedColumn();
                
                if (selectedRow>=0 && selectedCol>=0) {
                    String value = table.getValueAt(selectedRow, selectedCol).toString();
                    final String label = StringUtils.isEmpty(value) ? "Empty value" : value;

                    JMenuItem titleMenuItem = new JMenuItem(label);
                    titleMenuItem.setEnabled(false);
                    popupMenu.add(titleMenuItem);

                    JMenuItem copyMenuItem = new JMenuItem("Copy Value To Clipboard");
                    copyMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Transferable t = new StringSelection(label);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                        }
                    });
                    popupMenu.add(copyMenuItem);
                    
                    popupMenu.addSeparator();
                }
            }

            popupMenu.addMenuItems();

            return popupMenu;
        } 
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
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
                    config.getHiddenColumns().add(column.getName());
                    config.save();
                    column.setVisible(false);
                    updateTableModel();
                }
                catch (Exception ex) {
                    FrameworkAccess.handleException(ex);
                }
            }
        });
        menu.add(hideItem);
        return menu;
    }

    @Override
    protected void objectDoubleClicked(DomainObject object) {
        try {
            DomainObjectContextMenu popupMenu = getContextualPopupMenu();
            if (popupMenu!=null) popupMenu.runDefaultAction();
        } 
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

    @Override
    protected void chooseColumnsButtonPressed() {
        TableViewerConfigDialog configDialog = new TableViewerConfigDialog(attrs, config);
        if (configDialog.showDialog(this)==1) {
            for(String attrName : attributeMap.keySet()) {
                boolean visible = config.isColumnVisible(attrName);
                getColumn(attrName).setVisible(visible);
            }
            updateTableModel();
            updateUI();
        }
    }

    @Override
    protected void deleteKeyPressed() {
        // TODO: this was copy and pasted from DomainObjectIconGridViewer and should be refactored someday
        try {
            Object parent = selectionModel.getParentObject();
            if (parent instanceof Node) {
                Node node = (Node) parent;
                if (ClientDomainUtils.hasWriteAccess(node)) {
                    RemoveItemsActionListener action = new RemoveItemsActionListener(node, getSelectedObjects());
                    action.actionPerformed(null);
                }
            }
        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
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

    @Override
    public boolean isEditMode() {
        return false;
    }

    @Override
    public void toggleEditMode(boolean editMode) {

    }

    @Override
    public void refreshEditMode() {

    }

    @Override
    public void setEditSelectionModel(ChildSelectionModel<DomainObject, Reference> editSelectionModel) {

    }

    @Override
    public ChildSelectionModel<DomainObject, Reference> getEditSelectionModel() {
        return null;
    }

    @Override
    public void selectEditObjects(List<DomainObject> domainObjects, boolean select) {

    }

    @Override
    public AnnotatedObjectList<DomainObject, Reference> getObjectList() {
        return domainObjectList;
    }

    @Override
    public boolean matches(ResultPage<DomainObject, Reference> resultPage, DomainObject domainObject, String text) {
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

    @Override
    public Object getValue(AnnotatedObjectList<DomainObject, Reference> domainObjectList, DomainObject object, String columnName) {
        if (domainObjectList==null) {
            log.warn("Requested value for object {} when object list is null", object);
            return null;
        }
        if (COLUMN_KEY_ANNOTATIONS.equals(columnName)) {
            StringBuilder builder = new StringBuilder();
            for(Annotation annotation : domainObjectList.getAnnotations(Reference.createFor(object))) {
                if (builder.length()>0) builder.append(", ");
                builder.append(annotation.getName());
            }
            return builder.toString();
        }
        else {
            DomainObjectAttribute attr = attributeMap.get(columnName);
            if (attr==null) {
                throw new IllegalStateException("No attribute found for column: "+columnName);
            }
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
            hud.setObjectAndToggleDialog(domainObject, null, null, toggle, true);
        } 
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
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

    @Override
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
        if (viewerState instanceof TableViewerState) {
            final JScrollPane scrollPane = getDynamicTable().getScrollPane();
            final TableViewerState tableViewerState = (TableViewerState)viewerState;
            SwingUtilities.invokeLater(() -> {

                int horizontalScrollValue = tableViewerState.getHorizontalScrollValue();
                log.debug("Restoring horizontalScrollValue={}",horizontalScrollValue);
                scrollPane.getHorizontalScrollBar().setValue(horizontalScrollValue);

                int verticalScrollValue = tableViewerState.getVerticalScrollValue();
                log.debug("Restoring verticalScrollValue={}",verticalScrollValue);
                scrollPane.getVerticalScrollBar().setValue(verticalScrollValue);
            }
            );
        }
        else {
            log.warn("Cannot restore viewer state of type {}", viewerState.getClass());
        }
    }

}
