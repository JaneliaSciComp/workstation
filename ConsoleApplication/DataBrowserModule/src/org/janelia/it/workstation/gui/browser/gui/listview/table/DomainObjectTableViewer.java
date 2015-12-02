package org.janelia.it.workstation.gui.browser.gui.listview.table;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.listview.AnnotatedDomainObjectListViewer;
import org.janelia.it.workstation.gui.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.gui.browser.model.AnnotatedDomainObjectList;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table viewer for domain objects. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectTableViewer extends TableViewer<DomainObject,Reference> implements AnnotatedDomainObjectListViewer {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectTableViewer.class);

    private static final String COLUMN_KEY_ANNOTATIONS = "annotations";
    
    private final DomainObjectAttribute annotationAttr = new DomainObjectAttribute(COLUMN_KEY_ANNOTATIONS,"Annotations",null,false,true,false,null);
    private final Map<String, DomainObjectAttribute> attributeMap = new HashMap<>();
    private AnnotatedDomainObjectList domainObjectList;
    
    private DomainObjectSelectionModel selectionModel;
    private SearchProvider searchProvider;
        
    private String sortField;
    private boolean ascending = true;

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }
    
    @Override
    public void setSelectionModel(DomainObjectSelectionModel selectionModel) {
        this.selectionModel = selectionModel;
        super.setSelectionModel(selectionModel);
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    @Override
    public void showDomainObjects(AnnotatedDomainObjectList domainObjectList) {
        
        this.domainObjectList = domainObjectList;
        
        List<DomainObjectAttribute> attrs = new ArrayList<>();
        attrs.add(annotationAttr);
        
        for(DomainObject domainObject : domainObjectList.getDomainObjects()) {
            for(DomainObjectAttribute attr : ClientDomainUtils.getSearchAttributes(domainObject.getClass())) {
                if (attr.isDisplay()) {
                    attrs.add(attr);
                }
            }
            // for now we assume that we are only displaying heterogeneous 
            // lists, so we can quit after looking at the first object
            break; 
        }
        
        Collections.sort(attrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        
        for(DomainObjectAttribute attr : attrs) {
            attributeMap.put(attr.getName(), attr);
        }
        
        setAttributeColumns(attrs);
        showObjects(domainObjectList.getDomainObjects());
    }
        
    @Override
    protected Object getValue(DomainObject object, String columnName) {
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
        catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error("Error getting attribute value for column: "+columnName,e);
            return null;
        }
    }
    
    @Override
    public void selectDomainObjects(List<DomainObject> domainObjects, boolean select, boolean clearAll) {

        log.info("selectDomainObjects(domainObjects.size={},select={},clearAll={})",domainObjects.size(),select,clearAll);
            
        if (!select) {
            selectNone();
            return;
        }

        Integer start = null;
        Integer end = null;
        int i = 0;
        for(DynamicRow row : getRows()) {
            DomainObject rowObject = (DomainObject)row.getUserObject();
            if (domainObjects.indexOf(rowObject)>=0) {
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
        }
    }
    
    @Override
    public void refreshDomainObject(DomainObject domainObject) {
        // TODO: refresh the table
    }

    @Override
    public void preferenceChanged(Preference preference) {
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }
    
//    @Override
//    protected JPopupMenu getContextualPopupMenu() {
//        List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
//        List<DomainObject> domainObjects = new ArrayList<>();
//        for (String entityId : selectionIds) {
//            Long id = new Long(entityId);
//            DomainObject imageObject = domainObjectList.getDomainObject(id);
//            if (imageObject == null) {
//                log.warn("Could not locate selected object with id {}", id);
//            }
//            else {
//                domainObjects.add(imageObject);
//            }
//        }
//        JPopupMenu popupMenu = new DomainObjectContextMenu(domainObjects);
//        ((DomainObjectContextMenu) popupMenu).addMenuItems();
//        return popupMenu;
//    }
    
    @Override
    protected void updateTableModel() {
        super.updateTableModel();
        getTable().setRowSorter(new SolrRowSorter());
    }
    
    protected class SolrRowSorter extends RowSorter<TableModel> {

        private List<SortKey> sortKeys = new ArrayList<SortKey>();

        public SolrRowSorter() {
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
            searchProvider.setSortField(prefix + sortField);
            searchProvider.search();
        }

        @Override
        public void setSortKeys(List<? extends SortKey> sortKeys) {
            this.sortKeys = Collections.unmodifiableList(new ArrayList<SortKey>(sortKeys));
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
