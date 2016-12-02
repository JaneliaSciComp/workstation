package org.janelia.it.workstation.browser.gui.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.janelia.it.workstation.browser.gui.table.DynamicColumn;
import org.janelia.it.workstation.browser.gui.table.DynamicTable;

import net.miginfocom.swing.MigLayout;

/**
 * A simple panel for adding/removing members from a list.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class MembershipTablePanel<T> extends JPanel {

    private final Map<DynamicColumn, ColumnRenderer<T>> renderers = new HashMap<>();
    
    private final DynamicTable table;
    private final JPanel buttonPane;
    private final JButton addItemButton;
    
    private boolean editable = true;

    public MembershipTablePanel() {

        setLayout(new MigLayout(
                "ins 0, flowy, fillx",
                "[fill]",
                "[grow 0, growprio 0][grow 1, growprio 1, fill]"
        ));
        
        table = new DynamicTable(true, true) {
            
            @Override
            public Object getValue(Object userObject, DynamicColumn column) {
                ColumnRenderer<T> renderer = renderers.get(column);
                if (renderer==null) return userObject==null?null:userObject.toString();
                return renderer.render((T)userObject);
            }
            
            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
                JPopupMenu menu = super.createPopupMenu(e);

                if (menu != null) {
                    JTable table = getTable();
                    ListSelectionModel lsm = table.getSelectionModel();
                    if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
                        return menu;
                    }

                    final T object = (T) getRows().get(table.getSelectedRow()).getUserObject();
                    populatePopupMenu(menu, object);
                }

                return menu;
            }
        };

        addItemButton = new JButton("Add item");
        addItemButton.setEnabled(false);
        addItemButton.setToolTipText("Add an item to the table");
        addItemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                T newItem = showAddItemDialog();
                if (newItem!=null) {
                    addItemToList(newItem);
                }
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(addItemButton);
        buttonPane.add(Box.createHorizontalGlue());

        add(buttonPane, "width 10:300:3000");
        add(table, "");
    }

    public JButton getAddItemButton() {
        return addItemButton;
    }

    protected abstract T showAddItemDialog();
    
    public void populatePopupMenu(JPopupMenu menu, T object) {
    }
    
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        addItemButton.setEnabled(editable);
    }

//    protected void showPopupMenu(MouseEvent e) {
//        JPopupMenu popupMenu = createPopupMenu(e);
//        if (popupMenu != null) {
//            popupMenu.show(e.getComponent(), e.getX(), e.getY());
//        }
//    }
//
//    private JPopupMenu createPopupMenu(MouseEvent e) {
//
//        if (!editable) {
//            return null;
//        }
//
//        JList<?> target = (JList<?>) e.getSource();
//        if (target.getSelectedValue() == null) {
//            return null;
//        }
//
//        final JPopupMenu popupMenu = new JPopupMenu();
//        popupMenu.setLightWeightPopupEnabled(true);
//
//        JMenuItem deleteItem = new JMenuItem("Remove");
//        deleteItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                removeItemFromList(getSelectedItem());
//            }
//        });
//        popupMenu.add(deleteItem);
//
//        return popupMenu;
//    }

    public void initItemsInList(List<T> items) {
        table.removeAllRows();
        for (T item : items) {
            table.addRow(item);
        }
    }

    public T getSelectedItem() {
        List<Object> list = table.getSelectedObjects();
        if (list.isEmpty()) return null;
        return (T)list.get(0);
    }

    public void addItemToList(T object) {
        table.addRow(object);
        membershipChanged();
    }

    public void removeItemFromList(T object) {
        table.removeRow(object);
        membershipChanged();
    }

    public List<T> getItemsInList() {
        List<T> list = new ArrayList<>();
        for(Object object : table.getUserObjects()) {
            list.add((T)object);
        }
        return list;
    }

    public void membershipChanged() {
    }
    
    public void addColumn(String label, ColumnRenderer<T> renderer) {
        DynamicColumn column = table.addColumn(label, label, true, false, false, true);
        renderers.put(column, renderer);
    }
    
    public void updateView() {
        table.updateTableModel();
    }
    
    public interface ColumnRenderer<T> {
        Object render(T userObject);   
    }
}
