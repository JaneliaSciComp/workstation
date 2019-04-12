package org.janelia.workstation.common.gui.support;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * A simple panel for adding/removing members from a list.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class MembershipListPanel<T> extends JPanel {

    private DefaultListModel<T> model;
    private JList<T> itemList;

    private boolean editable = true;

    public MembershipListPanel(final String title) {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        this.model = new DefaultListModel<>();
        this.itemList = new JList<>(model);
        itemList.setToolTipText("Right-click to remove");
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setLayoutOrientation(JList.VERTICAL);

        itemList.setVisibleRowCount(-1);
        JScrollPane scrollPane = new JScrollPane(itemList);
        add(new JLabel(title), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        itemList.addMouseListener(new MouseHandler() {
            @Override
            protected void popupTriggered(MouseEvent e) {
                if (e.isConsumed()) {
                    return;
                }
                itemList.setSelectedIndex(itemList.locationToIndex(e.getPoint()));
                showPopupMenu(e);
                e.consume();
            }

            @Override
            protected void doubleLeftClicked(MouseEvent e) {
                if (e.isConsumed()) {
                    return;
                }
                int row = itemList.locationToIndex(e.getPoint());
                itemList.setSelectedIndex(row);
                rowDoubleClicked(row);
                e.consume();
            }
        });
    }

    public JList<T> getItemList() {
        return itemList;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    protected void showPopupMenu(MouseEvent e) {
        JPopupMenu popupMenu = createPopupMenu(e);
        if (popupMenu != null) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private JPopupMenu createPopupMenu(MouseEvent e) {

        if (!editable) {
            return null;
        }

        JList<?> target = (JList<?>) e.getSource();
        if (target.getSelectedValue() == null) {
            return null;
        }

        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JMenuItem deleteItem = new JMenuItem("Remove");
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeItemFromList(getSelectedItem());
            }
        });
        popupMenu.add(deleteItem);

        return popupMenu;
    }

    protected void rowDoubleClicked(int row) {
        if (row < 0) {
            return;
        }
        if (editable) {
            T item = model.get(row);
            if (item != null) {
                removeItemFromList(item);
            }
        }
    }

    public void initItemsInList(List<T> items) {
        model.removeAllElements();
        for (T item : items) {
            model.addElement(item);
        }
    }

    public T getSelectedItem() {
        return itemList.getSelectedValue();
    }

    public void addItemToList(T object) {
        model.addElement(object);
        membershipChanged();
    }

    public void removeItemFromList(T object) {
        model.removeElement(object);
        membershipChanged();
    }

    public List<T> getItemsInList() {
        return Collections.list(model.elements());
    }

    public void membershipChanged() {
    }

}
