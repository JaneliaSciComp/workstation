package org.janelia.it.workstation.browser.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;

/**
 * A button which allows the user to select a single value from a drop-down list. 
 * 
 * When using this abstract class, you need to provide an implementation which manages the state. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SingleSelectionButton<T> extends DropDownButton {
    
    private static final int MAX_VALUES_STRING_LENGTH = 20;
    
    private String label;

    public SingleSelectionButton(String label) {
        this.label = label;
    }
        
    public void update() {
        updateText();
        populateFacetMenu();
    }

    private void updateText() {

        StringBuilder text = new StringBuilder();
        text.append(label);
        T selectedValue = getSelectedValue();
        if (selectedValue != null) {
            String selectedValueLabel = getLabel(getSelectedValue());
            text.append(": ");
            text.append(StringUtils.abbreviate(selectedValueLabel, MAX_VALUES_STRING_LENGTH));
        }
        
        setText(text.toString());
    }
    
    private void populateFacetMenu() {
        
        removeAll();

        T selectedValue = getSelectedValue();
        Collection<T> values = getValues();
        if (values!=null) {
            
            for (final T value : values) {
                boolean selected = selectedValue != null && selectedValue.equals(value);
                if (isHidden(value) && !selected) {
                    // Skip anything that is not selected, and which doesn't have results. Clicking it would be futile.
                    continue;
                }
                String label = getLabel(value);
                final JMenuItem menuItem = new JRadioButtonMenuItem(label, selected) ;
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (menuItem.isSelected()) {
                            updateSelection(value);
                        }
                        update();
                    }
                });
                addMenuItem(menuItem);
            }
        }
    }
    
    /**
     * Set the current value programmatically.
     * @param value
     */
    public void setSelectedValue(T value) {
        updateSelection(value);
        update();
    }
   
    /**
     * Returns the possible values.
     * @return
     */
    public abstract Collection<T> getValues();

    /**
     * Returns the list of value names which are currently selected.
     * @return
     */
    public abstract T getSelectedValue();
    
    /**
     * Return the given value's label. Returns the value's toString by default.
     * @param value
     * @return
     */
    public String getLabel(T value) {
        return value==null ? null : value.toString();
    }
    
    /**
     * Should the given value be hidden in the list if it isn't already selected?
     * Returns false by default.
     * @param value
     * @return
     */
    public boolean isHidden(T value) {
        return false;
    }
        
    /**
     * Set the selection for a given value. Called when the user clicks on a menu option.
     * @param value
     * @param selected
     */
    protected abstract void updateSelection(T value);
    
}
