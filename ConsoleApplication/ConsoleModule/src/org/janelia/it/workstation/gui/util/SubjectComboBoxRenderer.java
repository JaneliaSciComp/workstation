package org.janelia.it.workstation.gui.util;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.model.user_data.Subject;

/**
 * A combo-box renderer for Subject selection. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubjectComboBoxRenderer extends JLabel implements ListCellRenderer<Subject> {

    public SubjectComboBoxRenderer() {
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Subject> list, Subject subject, int index, boolean isSelected, boolean cellHasFocus) {

        if (subject == null) {
            setIcon(Icons.getIcon("error.png"));
            setText("Unknown");
            return this;
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (subject.getKey() != null && subject.getKey().startsWith("group:")) {
            setIcon(Icons.getIcon("group.png"));
        }
        else {
            setIcon(Icons.getIcon("user.png"));
        }

        setText(subject.getFullName() + " (" + subject.getName() + ")");

        return this;
    }
}