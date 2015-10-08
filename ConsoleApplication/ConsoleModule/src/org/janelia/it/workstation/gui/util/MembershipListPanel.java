package org.janelia.it.workstation.gui.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple panel for adding/removing members from a list. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MembershipListPanel<T> extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(MembershipListPanel.class);

    private DefaultListModel<T> model;
    private JList<T> itemList;
    private DefaultComboBoxModel<T> comboBoxModel;
    private JPanel addPane;
    private JComboBox<T> subjectCombobox;
    private boolean editable;

    public MembershipListPanel(String title, Class<? extends ListCellRenderer<T>> cellRendererClass) {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        this.model = new DefaultListModel<>();
        this.itemList = new JList<>(model);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setLayoutOrientation(JList.VERTICAL);

        try {
            itemList.setCellRenderer(cellRendererClass.newInstance());
        }
        catch (Exception e) {
            log.error("Error setting cell renderer to new instance of " + cellRendererClass.getName(), e);
        }

        itemList.setVisibleRowCount(-1);
        JScrollPane scrollPane = new JScrollPane(itemList);
        add(new JLabel(title), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        this.comboBoxModel = new DefaultComboBoxModel<>();
        this.subjectCombobox = new JComboBox<>(comboBoxModel);
        subjectCombobox.setEditable(false);
        subjectCombobox.setToolTipText("Choose a user or group");

        try {
            subjectCombobox.setRenderer(cellRendererClass.newInstance());
        }
        catch (Exception e) {
            log.error("Error setting cell renderer to new instance of " + cellRendererClass.getName(), e);
        }

        subjectCombobox.setMaximumRowCount(20);
        subjectCombobox.setPreferredSize(new Dimension(150, 20));

        JButton addButton = new JButton("Add");
        addButton.setToolTipText("Add the selected user or group");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                T selected = (T) comboBoxModel.getSelectedItem();
                if (selected != null) {
                    addItemToList(selected);
                    revalidate();
                    repaint();
                }
            }
        });

        this.addPane = new JPanel();
        addPane.setLayout(new BoxLayout(addPane, BoxLayout.LINE_AXIS));
        addPane.add(subjectCombobox);
        addPane.add(addButton);
        addPane.add(Box.createHorizontalGlue());
        add(addPane, BorderLayout.SOUTH);

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

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        addPane.setVisible(editable);
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
        
        JList target = (JList) e.getSource();
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
        T item = model.get(row);
        if (item != null) {
            removeItemFromList(item);
        }
    }

    public void init(List<T> subjects) {
        for (T subject : subjects) {
            comboBoxModel.addElement(subject);
        }
    }

    public T getSelectedItem() {
        return itemList.getSelectedValue();
    }

    public void addItemToList(T object) {
        model.addElement(object);
        comboBoxModel.removeElement(object);
    }

    public void removeItemFromList(T object) {
        model.removeElement(object);
        // TODO: resort the combo box
        comboBoxModel.addElement(object);
    }

    public List<T> getItemsInList() {
        return Collections.list(model.elements());
    }
}
