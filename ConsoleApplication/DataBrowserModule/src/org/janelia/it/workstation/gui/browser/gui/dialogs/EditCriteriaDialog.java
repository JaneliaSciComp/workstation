package org.janelia.it.workstation.gui.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeValueCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.DateRangeCriteria;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;

import de.javasoft.swing.DateComboBox;
import net.miginfocom.swing.MigLayout;

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
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.LINE_AXIS));
            startDatePicker = new DateComboBox();
            
            inputPanel.add(startDatePicker);
            inputPanel.add(new JLabel(" to "));
            endDatePicker = new DateComboBox();
            inputPanel.add(endDatePicker);
            
            JLabel dateLabel = new JLabel(label);
            dateLabel.setLabelFor(inputPanel);
            attrPanel.add(dateLabel,"gap para");
            attrPanel.add(inputPanel,"gap para");
            
            Date now = new Date();
            DateRangeCriteria drc = (DateRangeCriteria)criteria;
            if (drc.getStartDate()==null) {
                startDatePicker.setDate(now);    
            }
            else {
                startDatePicker.setDate(drc.getStartDate());
            }

            if (drc.getEndDate()==null) {
                endDatePicker.setDate(now);    
            }
            else {
                endDatePicker.setDate(drc.getEndDate());
            }
        }
        else if (criteria instanceof AttributeValueCriteria) {

            textField = new JTextField();
            textField.setColumns(25);

            JLabel attrLabel = new JLabel(label);
            attrLabel.setLabelFor(textField);
            attrPanel.add(attrLabel,"gap para");
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
            String text = textField.getText().trim();
            if (!text.isEmpty()) {
                AttributeValueCriteria avc = (AttributeValueCriteria)criteria;
                avc.setValue(text);
                this.save = true;
            }
        }
        
        setVisible(false);
    }
    
    public Criteria getCriteria() {
        return criteria;
    }
}
