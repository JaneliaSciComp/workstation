package org.janelia.it.workstation.gui.browser.components.editor;


import javax.swing.*;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;

import org.janelia.it.jacs.model.domain.gui.search.filters.SetFilter;

/**
 * A dialog for editing a set filter.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EditSetFilterDialog extends EditFilterDialog {

    private JComboBox inputField;
    private SetFilter setFilter;
    
    protected EditSetFilterDialog() {
    }
    
    @Override
    protected JPanel getFilterPanel(Filter filter) {
        
        this.setFilter = (SetFilter)filter;
        
        JLabel titleLabel = new JLabel("In set: "+setFilter.getSetName());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(titleLabel);
        
        // TODO: load and display set members
        
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
