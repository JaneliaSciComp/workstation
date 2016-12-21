package org.janelia.it.workstation.browser.gui.support;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A simple panel for adding/removing strings from a list. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StringMemberListPanel extends MembershipListPanel<String> {

	private JPanel addPane;
    private JTextField inputTextfield;
    
    public StringMemberListPanel(final String title) {
        super(title);
        
        inputTextfield = new JTextField();
        inputTextfield.setPreferredSize(new Dimension(150, 20));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addItemToList(inputTextfield.getText());
                revalidate();
                repaint();
            }
        });
        this.addPane = new JPanel();
        addPane.setLayout(new BoxLayout(addPane, BoxLayout.LINE_AXIS));
        addPane.add(inputTextfield);
        addPane.add(addButton);
        addPane.add(Box.createHorizontalGlue());
        add(addPane, BorderLayout.SOUTH);
    }
    
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        addPane.setVisible(editable);
    }
}
