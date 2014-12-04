package org.janelia.it.workstation.gui.browser.components.editor;

import java.awt.Dimension;

import javax.swing.*;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;

import org.janelia.it.jacs.model.domain.gui.search.filters.FullTextFilter;

/**
 * A dialog for editing a full text filter.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EditFullTextFilterDialog extends EditFilterDialog {

    private JComboBox inputField;
    private FullTextFilter fullTextFilter;
    
    protected EditFullTextFilterDialog() {
    }
    
    @Override
    protected JPanel getFilterPanel(Filter filter) {
        
        this.fullTextFilter = (FullTextFilter)filter;
        
        JLabel titleLabel = new JLabel("Full-text search for ");
        
        this.inputField = new JComboBox();
        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        inputField.setEditable(true);
        inputField.setToolTipText("Enter search terms...");
        if (fullTextFilter.getText()!=null) {
            inputField.setSelectedItem(fullTextFilter.getText());
        }
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(titleLabel);
        panel.add(inputField);
        
        return panel;
    }
    
    @Override
    protected void buttonPressedReset() {
        inputField.setSelectedItem("");
    }
    
    @Override
    protected boolean buttonPressedOK() {
        fullTextFilter.setText(inputField.getSelectedItem().toString());
        savedSearch.addFilter(filter);
        return true;
    }
}
