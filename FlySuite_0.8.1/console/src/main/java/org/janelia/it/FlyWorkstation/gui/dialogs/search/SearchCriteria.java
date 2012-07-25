package org.janelia.it.FlyWorkstation.gui.dialogs.search;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.*;

import net.sourceforge.jdatepicker.JDateComponentFactory;
import net.sourceforge.jdatepicker.impl.JDatePickerImpl;

import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchAttribute.DataType;
import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * A simple search criteria specified against a SearchAttribute chosen from a list of attributes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SearchCriteria extends JPanel {
	
	private JComboBox termBox;
	private JComboBox operatorBox;
	
	private JPanel textInputPanel;
	private JLabel toTextLabel;
	private JTextField input1;
	private JTextField input2;
	
    private JPanel dateInputPanel;
    private JLabel toDateLabel;
    protected JDatePickerImpl startDatePicker;
    protected JDatePickerImpl endDatePicker;
    
	private SearchAttribute attribute;
	private CriteriaOperator op;
	
	public SearchCriteria(List<SearchAttribute> attributes, boolean enableDelete) {
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		
		if (enableDelete) {
	        JButton deleteCriteriaButton = new JButton(Icons.getIcon("delete.png"));
	        deleteCriteriaButton.setBorderPainted(false);
	        deleteCriteriaButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					removeSearchCriteria();
				}
			});
			add(deleteCriteriaButton);
		}
		else {
			add(Box.createRigidArea(new Dimension(32, 1)));
		}
		
		this.termBox = new JComboBox();
		add(termBox);
		
		this.operatorBox = new JComboBox();
		add(operatorBox);
		
		// Text Input
		this.textInputPanel = new JPanel();
		add(textInputPanel);
		
		this.input1 = new JTextField();
		input1.setColumns(10);
		textInputPanel.add(input1);

		toTextLabel = new JLabel("to");
		textInputPanel.add(toTextLabel);
		
		this.input2 = new JTextField();
		input2.setColumns(10);
		textInputPanel.add(input2);

		// Date Input 
		this.dateInputPanel = new JPanel();
		dateInputPanel.setVisible(false);
		add(dateInputPanel);
		
		this.startDatePicker = (JDatePickerImpl) JDateComponentFactory.createJDatePicker();
        dateInputPanel.add(startDatePicker);
        
		toDateLabel = new JLabel("to");
		dateInputPanel.add(toDateLabel);

		this.endDatePicker = (JDatePickerImpl) JDateComponentFactory.createJDatePicker();
		dateInputPanel.add(endDatePicker);
		
		init(attributes);
	}

	protected abstract void removeSearchCriteria();
	
	private void init(List<SearchAttribute> attributes) {

		termBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ("comboBoxChanged".equals(e.getActionCommand())) {
					Object item = termBox.getSelectedItem();
					if (item instanceof SearchAttribute) {
						setAttribute((SearchAttribute)item);
					}
				}
			}
		});
		
		operatorBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ("comboBoxChanged".equals(e.getActionCommand())) {
					setOperator((CriteriaOperator)operatorBox.getSelectedItem());
				}
			}
		});
        
		DefaultComboBoxModel termModel = (DefaultComboBoxModel)termBox.getModel();
		termModel.addElement("Select...");
		for(SearchAttribute attribute : attributes) {
			termModel.addElement(attribute);
		}
		DefaultComboBoxModel operatorModel = (DefaultComboBoxModel)operatorBox.getModel();
		for(CriteriaOperator op : CriteriaOperator.values()) {
			operatorModel.addElement(op);
		}
		operatorModel.setSelectedItem(CriteriaOperator.NOT_NULL);
	}

	private void setAttribute(SearchAttribute attribute) {
		this.attribute = attribute;
		
		switch (attribute.getDataType()) {
		case STRING:
			textInputPanel.setVisible(true);
			dateInputPanel.setVisible(false);
			break;
		case DATE:
			textInputPanel.setVisible(false);
			dateInputPanel.setVisible(true);
			break;
		}
		
		setOperator(op);
		
		revalidate();
	}
	
	private void setOperator(CriteriaOperator op) {
		
		this.op = op;
		if (op==null) return;
		
		DataType dataType = attribute==null?DataType.STRING:attribute.getDataType();
		
		switch (dataType) {
		case STRING:
			switch (op) {
			case CONTAINS: 
				input1.setVisible(true);
				toTextLabel.setVisible(false);
				input2.setVisible(false);
				break;
			case BETWEEN:
				input1.setVisible(true);
				toTextLabel.setVisible(true);
				input2.setVisible(true);
				break;
			case NOT_NULL:
				input1.setVisible(false);
				toTextLabel.setVisible(false);
				input2.setVisible(false);
				break;
			}
			break;
		case DATE:
			switch (op) {
			case CONTAINS: 
				startDatePicker.setVisible(true);
				toDateLabel.setVisible(false);
				endDatePicker.setVisible(false);
				break;
			case BETWEEN:
				startDatePicker.setVisible(true);
				toDateLabel.setVisible(true);
				endDatePicker.setVisible(true);
				break;
			case NOT_NULL:
				startDatePicker.setVisible(false);
				toDateLabel.setVisible(false);
				endDatePicker.setVisible(false);
				break;
			}
			break;
		}

		revalidate();
	}

	public SearchAttribute getAttribute() {
		return attribute;
	}

	public CriteriaOperator getOp() {
		return op;
	}

	public Object getValue1() {
		DataType dataType = attribute==null?DataType.STRING:attribute.getDataType();		
		switch (dataType) {
		case STRING: return input1.getText();
		case DATE: return startDatePicker.getModel().getValue();
		}
		throw new IllegalStateException("Unknown data type: "+dataType);
	}

	public Object getValue2() {
		DataType dataType = attribute==null?DataType.STRING:attribute.getDataType();		
		switch (dataType) {
		case STRING: return input2.getText();
		case DATE: return endDatePicker.getModel().getValue();
		}
		throw new IllegalStateException("Unknown data type: "+dataType);
	}
}