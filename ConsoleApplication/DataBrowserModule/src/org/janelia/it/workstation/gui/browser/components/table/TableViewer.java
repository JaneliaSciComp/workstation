package org.janelia.it.workstation.gui.browser.components.table;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.janelia.it.workstation.gui.browser.components.editor.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicRow;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic table viewer for a specific object type. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class TableViewer<T,S> extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(TableViewer.class);

    protected final JPanel resultsPane;
    protected final DynamicTable resultsTable;
    
    protected SelectionModel<T,S> selectionModel;
        
    public TableViewer() {
        
        setLayout(new BorderLayout());
        
        resultsTable = new DynamicTable() {
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                return TableViewer.this.getValue((T)userObject, column.getName());
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
    
    public void setSelectionModel(SelectionModel<T,S> selectionModel) {
        selectionModel.setSource(this);
        this.selectionModel = selectionModel;
    }
    
//    protected abstract JPopupMenu getContextualPopupMenu();
    
    protected abstract Object getValue(T object, String column);
    
    public void setAttributeColumns(List<DomainObjectAttribute> searchAttrs) {
        resultsTable.clearColumns();
        for(DomainObjectAttribute searchAttr : searchAttrs) {
            // TODO: control default visibility based on saved user preference
            DynamicColumn column = resultsTable.addColumn(searchAttr.getName(), searchAttr.getLabel(), true, false, true, true);
//            columnByName.put(attr.getName(), column);
        }
    }
    
    protected void showObjects(List<T> objectList) {
        
        resultsTable.removeAllRows();
        
        for (T object : objectList) {
            resultsTable.addRow(object);
        }
        
        updateTableModel();
    }
    
    protected void updateTableModel() {
        resultsTable.updateTableModel();
//        resultsTable.getTable().setRowSorter(new SearchResultsPanel.SolrRowSorter());
    }
}
