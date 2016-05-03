package org.janelia.it.workstation.gui.browser.gui.listview.table;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.gui.browser.actions.RemoveItemsFromFolderAction;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.dialogs.TableViewerConfigDialog;
import org.janelia.it.workstation.gui.browser.gui.hud.Hud;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableModel;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * A table viewer for domain objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectTableViewer extends TableViewerPanel<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectTableViewer.class);

    private static final String COLUMN_KEY_ANNOTATIONS = "annotations";

    private final DomainObjectAttribute annotationAttr = new DomainObjectAttribute(COLUMN_KEY_ANNOTATIONS,"Annotations",null,null,true,null,null);
    private final Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();
    private AnnotatedDomainObjectList domainObjectList;
    private DomainObjectSelectionModel selectionModel;

    private List<DomainObjectAttribute> attrs;
    private String sortField;
    private boolean ascending = true;

    // TODO: this is mostly copy and pasted from DomainObjectIconGridViewer, and should be refactored later
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
        public DomainObject getImageByUniqueId(Reference id) {
            return DomainMgr.getDomainMgr().getModel().getDomainObject(id);
        }

        @Override
        public String getImageLabel(DomainObject domainObject) {
            return domainObject.getName();
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
    public void setSearchProvider(SearchProvider searchProvider) {
        super.setSearchProvider(searchProvider);
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
    public void showDomainObjects(final AnnotatedDomainObjectList domainObjectList, final Callable<Void> success) {

        this.domainObjectList = domainObjectList;

        attributeMap.clear();

        SimpleWorker worker = new SimpleWorker() {

            private final Set<DomainObjectAttribute> attrSet = new HashSet<>();

            @Override
            protected void doStuff() throws Exception {

                attrSet.add(annotationAttr);

                Set<Class<? extends DomainObject>> domainClasses = new HashSet<>();
                for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
                    domainClasses.add(domainObject.getClass());
                }

                for(Class<? extends DomainObject> domainClass : domainClasses) {
                    for (DomainObjectAttribute attr : DomainUtils.getSearchAttributes(domainClass)) {
                        if (attr.isDisplay()) {
                            attrSet.add(attr);
                        }
                    }
                }

            }

            @Override
            protected void hadSuccess() {

                attrs = new ArrayList<>(attrSet);
                Collections.sort(attrs, new Comparator<DomainObjectAttribute>() {
                    @Override
                    public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                        return o1.getLabel().compareTo(o2.getLabel());
                    }
                });

                TableViewerConfiguration config = TableViewerConfiguration.loadConfig();

                getDynamicTable().clearColumns();
                for(DomainObjectAttribute attr : attrs) {
                    attributeMap.put(attr.getName(), attr);
                    boolean visible = config.isColumnVisible(attr.getName());
                    boolean sortable = !COLUMN_KEY_ANNOTATIONS.equals(attr.getName());
                    getDynamicTable().addColumn(attr.getName(), attr.getLabel(), visible, false, true, sortable);
                }

                showObjects(domainObjectList.getDomainObjects());

                // Finally, we're done, we can call the success callback
                ConcurrentUtils.invokeAndHandleExceptions(success);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }

    @Override
    public void refreshDomainObject(DomainObject domainObject) {
        // TODO: refresh the table
        throw new UnsupportedOperationException("refreshDomainObject is not yet supported");
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {

        List<Reference> ids = selectionModel.getSelectedIds();
        List<DomainObject> selected = DomainMgr.getDomainMgr().getModel().getDomainObjects(ids);
        // TODO: should this use the same result as the icon grid viewer?
        DomainObjectContextMenu popupMenu = new DomainObjectContextMenu((DomainObject)selectionModel.getParentObject(), selected, ResultDescriptor.LATEST, null);

        JTable table = getTable();
        ListSelectionModel lsm = table.getSelectionModel();
        if (lsm.getMinSelectionIndex()==lsm.getMaxSelectionIndex()) {
            final String value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()).toString();
            JMenuItem titleMenuItem = new JMenuItem(value);
            titleMenuItem.setEnabled(false);
            popupMenu.add(titleMenuItem);

            JMenuItem copyMenuItem = new JMenuItem("  Copy To Clipboard");
            copyMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Transferable t = new StringSelection(value);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                }
            });
            popupMenu.add(copyMenuItem);
        }

        popupMenu.addMenuItems();
        return popupMenu;
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
                    SessionMgr.getSessionMgr().handleException(ex);
                }
            }
        });
        menu.add(hideItem);
        return menu;
    }

    @Override
    protected void objectDoubleClicked(DomainObject object) {
        getContextualPopupMenu().runDefaultAction();
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
        IsParent parent = selectionModel.getParentObject();
        if (parent instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)parent;
            if (ClientDomainUtils.hasWriteAccess(treeNode)) {
                RemoveItemsFromFolderAction action = new RemoveItemsFromFolderAction(treeNode, getSelectedObjects());
                action.doAction();
            }
        }
    }

    @Override
    public void activate() {
        Hud.getSingletonInstance().setKeyListener(keyListener);
    }

    @Override
    public void deactivate() {
    }
    
    @Override
    public Object getValue(DomainObject object, String columnName) {
        try {
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
                return attr.getGetter().invoke(object);
            }
        }
        catch (IllegalArgumentException e) {
            // This happens if we have mixed objects and we try to get an attribute from one on another 
            log.debug("Cannot get attribute {} for {}",columnName,object.getType());
            return null;
        }
        catch(IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot get attribute {} for {}",columnName,object.getType(),e);
            return null;
        }
    }

    @Override
    protected void updateTableModel() {
        super.updateTableModel();
        getTable().setRowSorter(new DomainObjectRowSorter());
    }

    @Override
    protected void updateHud(boolean toggle) {

        Hud hud = Hud.getSingletonInstance();
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
    
    private List<DomainObject> getSelectedObjects() {
        return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
    }
    
    protected class DomainObjectRowSorter extends RowSorter<TableModel> {

        private List<SortKey> sortKeys = new ArrayList<>();

        public DomainObjectRowSorter() {
            List<DynamicColumn> columns = getDynamicTable().getDisplayedColumns();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).getName().equals(sortField)) {
                    sortKeys.add(new SortKey(i, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING));
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
            this.sortKeys = Collections.unmodifiableList(new ArrayList<>(sortKeys));
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
}
