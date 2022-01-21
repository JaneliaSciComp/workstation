package org.janelia.workstation.common.gui.support;

import org.janelia.model.security.Subject;

import javax.swing.*;
import java.util.Collection;

/**
 * Combo box allowing for the selection of a single Subject (user or group) from a list of options.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubjectComboBox extends JComboBox<Subject> {

    public SubjectComboBox() {
        setEditable(false);
        setToolTipText("Choose a user or group");
        SubjectComboBoxRenderer renderer = new SubjectComboBoxRenderer();
        setRenderer(renderer);
        setMaximumRowCount(20);
    }

    /**
     * Set the collection of subjects selectable in the combo box.
     * @param subjects subjects that can be selected
     */
    public void setItems(Collection<Subject> subjects) {
        setItems(subjects, null);
    }

    /**
     * Set the collection of subject selectable in the combo box, with one of the options currently selected.
     * @param subjects subjects that can be selected
     * @param selected current selection
     */
    public void setItems(Collection<Subject> subjects, Subject selected) {
        DefaultComboBoxModel<Subject> model = (DefaultComboBoxModel<Subject>)getModel();
        model.removeAllElements();
        subjects.forEach(model::addElement);
        if (selected != null) {
            model.setSelectedItem(selected);
        }
    }

    /**
     * Get the select subject.
     * @return selected subject or null if no selection
     */
    public Subject getSelectedItem() {
        return (Subject)super.getSelectedItem();
    }

}
