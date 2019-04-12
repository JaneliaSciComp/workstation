package org.janelia.workstation.common.gui.support;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple panel for adding/removing members from a list. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ComboMembershipListPanel<T> extends MembershipListPanel<T> {

    private static final Logger log = LoggerFactory.getLogger(ComboMembershipListPanel.class);

	private JPanel addPane;
    private DefaultComboBoxModel<T> comboBoxModel;
    private JComboBox<T> inputCombobox;
    
    public ComboMembershipListPanel(final String title) {
    	this(title, null);
    }

    public ComboMembershipListPanel(final String title, Class<? extends ListCellRenderer<T>> cellRendererClass) {
    	super(title);
        this.comboBoxModel = new DefaultComboBoxModel<>();
        this.inputCombobox = new JComboBox<>(comboBoxModel);
        inputCombobox.setEditable(false);
        inputCombobox.setToolTipText("Choose an item");
        inputCombobox.setMaximumRowCount(20);
        inputCombobox.setPreferredSize(new Dimension(150, 20));

        if (cellRendererClass!=null) {
            try {
            	getItemList().setCellRenderer(cellRendererClass.newInstance());
                inputCombobox.setRenderer(cellRendererClass.newInstance());
            }
            catch (Exception e) {
                log.error("Error setting cell renderer to new instance of " + cellRendererClass.getName(), e);
            }
        }
        
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int i = inputCombobox.getSelectedIndex();
                @SuppressWarnings("unchecked")
                T selected = (T) comboBoxModel.getSelectedItem();
                if (selected != null) {
                    addItemToList(selected);
                    if (i<comboBoxModel.getSize()) {
                        inputCombobox.setSelectedIndex(i);
                    }
                    revalidate();
                    repaint();
                }
            }
        });

        this.addPane = new JPanel();
        addPane.setLayout(new BoxLayout(addPane, BoxLayout.LINE_AXIS));
        addPane.add(inputCombobox);
        addPane.add(addButton);
        addPane.add(Box.createHorizontalGlue());
        add(addPane, BorderLayout.SOUTH);

    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        addPane.setVisible(editable);
    }

    public void initItemsInCombo(List<T> items) {
        for (T subject : items) {
            comboBoxModel.addElement(subject);
        }
    }

    @Override
    public void addItemToList(T object) {
        super.addItemToList(object);
        if (comboBoxModel!=null) comboBoxModel.removeElement(object);
    }

    @Override
    public void removeItemFromList(T object) {
        super.removeItemFromList(object);
        // TODO: resort the combo box
        if (comboBoxModel!=null) comboBoxModel.addElement(object);
    }
}
