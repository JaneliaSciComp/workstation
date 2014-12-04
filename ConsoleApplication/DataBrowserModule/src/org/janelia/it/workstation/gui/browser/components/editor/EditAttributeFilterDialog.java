package org.janelia.it.workstation.gui.browser.components.editor;

import java.awt.Dimension;

import javax.swing.*;
import org.janelia.it.jacs.model.domain.gui.search.filters.AttributeFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;


/**
 * A dialog for editing an attribute filter.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EditAttributeFilterDialog extends EditFilterDialog {

    private JComboBox inputField;
    private AttributeFilter attributeFilter;
    
    protected EditAttributeFilterDialog() {
    }
    
    @Override
    protected JPanel getFilterPanel(Filter filter) {
        
        this.attributeFilter = (AttributeFilter)filter;
//        
//        JLabel titleLabel = new JLabel("Fulltext search for ");
//        
//        this.inputField = new JComboBox();
//        inputField.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
//        inputField.setEditable(true);
//        inputField.setToolTipText("Enter search terms...");
//        if (fullTextFilter.getText()!=null) {
//            inputField.setSelectedItem(fullTextFilter.getText());
//        }
//        
        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
//        panel.add(titleLabel);
//        panel.add(inputField);
        
        return panel;
    }
    
    @Override
    protected void buttonPressedReset() {
        inputField.setSelectedItem("");
    }
    
    @Override
    protected boolean buttonPressedOK() {
        
        return true;
    }
}
