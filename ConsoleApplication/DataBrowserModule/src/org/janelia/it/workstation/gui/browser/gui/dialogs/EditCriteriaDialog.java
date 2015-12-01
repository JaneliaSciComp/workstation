package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeValueCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.DateRangeCriteria;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;

import de.javasoft.swing.DateComboBox;

/**
 * A dialog for editing different types of filter criteria. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EditCriteriaDialog extends ModalDialog {

    private final JPanel attrPanel;
    private DateComboBox startDatePicker;
    private DateComboBox endDatePicker;
    private JTextField textField;
    private Criteria criteria;
    private boolean save = false;
    
    public EditCriteriaDialog() {

        setTitle("Edit criteria");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });
        
        getRootPane().setDefaultButton(okButton);
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public Criteria showForCriteria(Criteria criteria, String label) {

        this.criteria = criteria;
        
        if (criteria instanceof DateRangeCriteria) {

            JPanel inputPanel = new JPanel();
            startDatePicker = new DateComboBox();
            inputPanel.add(startDatePicker);
            inputPanel.add(new JLabel("to"));
            endDatePicker = new DateComboBox();
            inputPanel.add(endDatePicker);
            
            attrPanel.add(new JLabel(label),"gap para");
            attrPanel.add(inputPanel,"gap para");
            
            DateRangeCriteria drc = (DateRangeCriteria)criteria;
            startDatePicker.setDate(drc.getStartDate());
            endDatePicker.setDate(drc.getEndDate());
        }
        else if (criteria instanceof AttributeValueCriteria) {

            textField = new JTextField();
            textField.setColumns(10);
            
            attrPanel.add(new JLabel(label),"gap para");
            attrPanel.add(textField,"gap para");
            
            AttributeValueCriteria avc = (AttributeValueCriteria)criteria;
            textField.setText(avc.getValue());
        }
        
        packAndShow();
        return save?criteria:null;
    }
    
    private void saveAndClose() {

        if (criteria instanceof DateRangeCriteria) {
            DateRangeCriteria drc = (DateRangeCriteria)criteria;
            drc.setStartDate(startDatePicker.getDate());
            drc.setEndDate(endDatePicker.getDate());
            this.save = true;
        }
        else if (criteria instanceof AttributeValueCriteria) {
            AttributeValueCriteria avc = (AttributeValueCriteria)criteria;
            avc.setValue(textField.getText());
            this.save = true;
        }
        
        setVisible(false);
    }
    
    public Criteria getCriteria() {
        return criteria;
    }
}
