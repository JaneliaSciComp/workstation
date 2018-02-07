package org.janelia.it.workstation.browser.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;

/**
 * A button which allows the user to select multiple values from a drop-down list. 
 * 
 * When using this abstract class, you need to provide an implementation which manages the state. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SelectionButton<T> extends DropDownButton {
    
    private static final int MAX_VALUES_STRING_LENGTH = 20;
    
    private String label;

    public SelectionButton(String label) {
        this.label = label;
    }
        
    public void update() {
        updateText();
        populateFacetMenu();
    }

    private void updateText() {

        StringBuilder text = new StringBuilder();
        text.append(label);
        List<String> valueLabels = new ArrayList<>(getSelectedValues().stream().map(value -> getName(value)).collect(Collectors.toList()));
        if (!valueLabels.isEmpty()) {
            Collections.sort(valueLabels);
                text.append(" (");
                text.append(StringUtils.getCommaDelimited(valueLabels, MAX_VALUES_STRING_LENGTH));
                text.append(")");
        }
        
        setText(text.toString());
    }
    
    private void populateFacetMenu() {
        
        removeAll();

        Collection<T> values = getValues();
        if (values!=null) {
            
            Set<T> selectedValueNames = new HashSet<>(getSelectedValues());
    
            if (!selectedValueNames.isEmpty()) {
                final JMenuItem menuItem = new JMenuItem("Clear selected");
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        clearSelected();
                        update();
                    }
                });
                addMenuItem(menuItem);
            }

            if (selectedValueNames.size() != values.size()) {
                final JMenuItem selectAllItem = new JMenuItem("Select All");
                selectAllItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        selectAll();
                        update();
                    }
                });
                addMenuItem(selectAllItem);
            }
        
            for (final T value : values) {
                boolean selected = selectedValueNames.contains(value);
                if (isHidden(value) && !selected) {
                    // Skip anything that is not selected, and which doesn't have results. Clicking it would be futile.
                    continue;
                }
                String label = getLabel(value);
                final JMenuItem menuItem = new JCheckBoxMenuItem(label, selected);
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (menuItem.isSelected()) {
                            updateSelection(value, true);
                        }
                        else {
                            updateSelection(value, false);
                        }
                        update();
                    }
                });
                addMenuItem(menuItem);
            }
        }
    }

    /**
     * Set the current value(s) programmatically.
     * @param value
     */
    public void setSelectedValue(T value, boolean selected) {
        updateSelection(value, selected);
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
    public abstract Collection<T> getSelectedValues();

    /**
     * Return the given value's name. Returns the value's toString by default.
     * @param value
     * @return
     */
    public String getName(T value) {
        return value==null ? null : value.toString();
    }
    
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
    protected boolean isHidden(T value) {
        return false;
    }
    
    /**
     * Clear all selections. Called when the user clicks the "Clear selected" menu option.
     */
    protected abstract void clearSelected();
    
    /**
     * Select all values. Called when the user clicks the "Select all" menu option.
     */
    protected abstract void selectAll();
    
    /**
     * Set the selection for a given value. Called when the user clicks on a menu option.
     * @param value
     * @param selected
     */
    protected abstract void updateSelection(T value, boolean selected);
    
}
