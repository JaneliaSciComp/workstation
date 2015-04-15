package org.janelia.it.workstation.gui.browser.components.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import org.janelia.it.jacs.model.domain.gui.search.SavedSearch;
import org.janelia.it.jacs.model.domain.gui.search.filters.AttributeFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;
import org.janelia.it.jacs.model.domain.gui.search.filters.FullTextFilter;
import org.janelia.it.jacs.model.domain.gui.search.filters.SetFilter;

import org.janelia.it.workstation.gui.dialogs.ModalDialog;

/**
 * Abstract base class for dialogs intended for editing a filter.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EditFilterDialog extends ModalDialog {

    private final JButton clearButton;
    private final JButton cancelButton;
    private final JButton okButton;
    
    protected SavedSearch savedSearch;
    protected Filter filter;
    
    protected EditFilterDialog() {
        
        this.clearButton = new JButton("Reset");
        clearButton.setToolTipText("Reset all values to defaults");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonPressedReset();
            }
        });
        
        this.cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close and cancel changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (buttonPressedCancel()) {
	            setVisible(false);
                }
            }
        });

        this.okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (buttonPressedOK()) {
	            setVisible(false);
                }
            }
        });
        
        getRootPane().setDefaultButton(okButton);
    }

    protected abstract JPanel getFilterPanel(Filter filter);
    
    public void setFilter(Filter filter) {
        this.filter = filter;
    }
    
    public void setSavedSearch(SavedSearch savedSearch) {
        this.savedSearch = savedSearch;
    }
    
    public void showDialog() {

        if (filter==null) {
            throw new IllegalStateException("No filter set. Call setFilter() first.");
        }
        
        setTitle("Filter");      
        
        add(getFilterPanel(filter), BorderLayout.CENTER);
            
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(clearButton);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
        
        // Show dialog and wait
        packAndShow();
    }
    
    protected void buttonPressedReset() {
        
    }
    
    protected boolean buttonPressedCancel() {
        return true;
    }
    
    protected boolean buttonPressedOK() {
        return true;
    }
    
    public static EditFilterDialog getInstanceForFilter(SavedSearch savedSearch, Filter filter) {
        EditFilterDialog dialog;
        if (filter instanceof FullTextFilter) {
             dialog = new EditFullTextFilterDialog();
        }
        else if (filter instanceof AttributeFilter) {
            dialog = new EditAttributeFilterDialog();
        }
        else if (filter instanceof SetFilter) {
            dialog = new EditSetFilterDialog();
        }
        else {
            throw new IllegalArgumentException("Unsupported filter type: "+filter.getClass().getName());
        }
        dialog.setSavedSearch(savedSearch);
        dialog.setFilter(filter);
        return dialog;
    }
}
