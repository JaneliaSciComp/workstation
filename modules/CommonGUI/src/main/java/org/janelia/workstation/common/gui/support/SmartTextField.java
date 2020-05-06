package org.janelia.workstation.common.gui.support;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;


import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A text field that remembers its input history based on a model property.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SmartTextField extends JComboBox<String> {

    private static final Logger log = LoggerFactory.getLogger(SmartTextField.class);

    /**
     * Number of historical terms in the drop down
     */
    private static final int MAX_HISTORY_LENGTH = 10;

    private final String modelPropertyName;

    public SmartTextField(String modelPropertyName) {
        this.modelPropertyName = modelPropertyName;
        setEditable(true);
        loadHistory();
    }

    /**
     * Get the current text string.
     * @return
     */
    public String getText() {
        String text = (String)getSelectedItem();
        if (text!=null) {
            return text.trim();
        }
        return text;
    }

    /**
     * Set the current text string
     * @param text
     */
    public void setText(String text) {

        if (text==null) {
            // Recurse with non-null parameter
            setText("");
            return;
        }

        getEditor().setItem(text);
        setSelectedItem(text);
    }

    /**
     * Override this method to provide custom history persistence. Model properties are used by default.
     * @return Current history. May be null or empty if there is no history.
     */
    private List<String> getTextHistory() {
        @SuppressWarnings("unchecked")
        List<String> history = (List<String>) FrameworkAccess.getModelProperty(modelPropertyName);
        log.trace("Returning current text history: {} ",history);
        return history;
    }

    /**
     * Override this method to provide custom history persistence.
     * Model properties are used by default.
     * @param history The history to persist. May be empty or null
     * if there is no history.
     */
    private void setTextHistory(List<String> history) {
        log.trace("Saving text history: {} ",history);
        FrameworkAccess.setModelProperty(modelPropertyName, history);
    }

    public void addCurrentTextToHistory() {

        String text = getText();
        if (StringUtils.isEmpty(text)) return;

        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) getModel();

        // Trim history
        while (model.getSize() >= MAX_HISTORY_LENGTH) {
            model.removeElementAt(model.getSize() - 1);
        }

        // Remove any current instance of the term
        int currIndex = model.getIndexOf(text);
        if (currIndex>=0) {
            model.removeElementAt(currIndex);
        }

        // Add it to the front
        model.insertElementAt(text, 0);
        setSelectedItem(text);

        List<String> history = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            history.add(model.getElementAt(i));
        }

        setTextHistory(history);
    }

    private void loadHistory() {

        String text = getText();

        List<String> history = getTextHistory();

        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) getModel();
        model.removeAllElements();

        if (history == null || history.isEmpty()) {
            return;
        }

        boolean selectedInHistory = false;

        for (String s : history) {
            if (s.equals(text)) {
                selectedInHistory = true;
            }
            model.addElement(s);
        }

        if (!StringUtils.isEmpty(text)) {
            if (!selectedInHistory) {
                model.insertElementAt(text, 0);
            }
            setSelectedItem(text);
        }
        else {
            setSelectedItem("");
        }
    }

    public void selectAll() {
        getEditor().selectAll();
    }
}
