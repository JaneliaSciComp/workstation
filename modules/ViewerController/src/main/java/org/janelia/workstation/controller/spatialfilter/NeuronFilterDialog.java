package org.janelia.workstation.controller.spatialfilter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyDescriptor;
import java.util.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.janelia.workstation.controller.AnnotationModel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for choosing strategy for filtering neurons
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronFilterDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(NeuronFilterDialog.class);
  
    JButton okButton;
    JPanel optionsPanel;
    JCheckBox enableFiltering;
    JComboBox strategySelection;
    NeuronSpatialFilter currentFilter;

    AnnotationModel annModel;
    final Map<String,String> strategyLabels = new HashMap<>();
       
    public NeuronFilterDialog(AnnotationModel annModel) {
    	super(FrameworkAccess.getMainFrame());
        this.annModel = annModel;
        setTitle("Set Neuron Filter Strategy");

        // you could do this with a custom annotation as well, but that is more work and there
        // aren't a lot of strategies for now
        strategyLabels.put(NeuronSelectionSpatialFilter.class.getName(), "Neuron Selection Filter");
        strategyLabels.put(NeuronProximitySpatialFilter.class.getName(), "Neuron Proximity Filter");

        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 10, 0, 10);
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;


        // enable check box
        enableFiltering = new JCheckBox();
        enableFiltering.addActionListener(event -> {
            if (enableFiltering.isSelected()) {
                strategySelection.setEnabled(true);
            } else {
                strategySelection.setEnabled(false);
            }
        });
        JLabel enableFilterLabel1 = new JLabel("Neuron filtering: ");
        JLabel enableFilterLabel2 = new JLabel("enabled");
        Box filterBox = Box.createHorizontalBox();
        filterBox.add(enableFilterLabel1);
        filterBox.add(enableFiltering);
        filterBox.add(enableFilterLabel2);
        add(filterBox, constraints);


        // later objects follow the first one in the layout
        constraints.gridy = GridBagConstraints.RELATIVE;


        // strategy selection menu
        JPanel strategyPanel = new JPanel();
        strategyPanel.setLayout(new BoxLayout(strategyPanel, BoxLayout.LINE_AXIS));
        strategyPanel.add(new JLabel("Strategy: "));

        strategySelection = new JComboBox();
        strategySelection.setMaximumSize(new Dimension (300,50));
        strategySelection.setActionCommand("SetStrategy");

        ArrayList<String> sortedLabels = new ArrayList<>(strategyLabels.values());
        Collections.sort(sortedLabels);
        for (String label: sortedLabels) {
             strategySelection.addItem(label);
        }
        strategyPanel.add(strategySelection);

        add(strategyPanel, constraints);


        // options for particular selection
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel,BoxLayout.PAGE_AXIS));
        currentFilter = annModel.getFilterStrategy();
        generateFilterOptions();
        add(optionsPanel, constraints);


        // buttons at the bottom
        okButton = new JButton("Save");
        okButton.addActionListener(event -> {
                try {
                    if (update()) {
                        setVisible(false);
                    }
                }
                catch (Exception ex) {
                    FrameworkAccess.handleException(ex);
                }
            });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> setVisible(false));

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(okButton);
        buttonBox.add(cancelButton);

        // last element: add some space below, too
        constraints.insets = new Insets(5, 10, 5, 10);
        add(buttonBox, constraints);

        pack();

        // I prefer to put listeners near what they're listening to, but
        //  this one has a dependence on later-created UI object, so it's
        //  easier to put it here
        strategySelection.addActionListener(event -> {
            String label = (String)strategySelection.getSelectedItem();
            for (String key: new ArrayList<String>(strategyLabels.keySet())) {
                if (strategyLabels.get(key).equals(label)) {
                    // create a new neuron spatial filter
                    try {
                        if (currentFilter==null || !currentFilter.getLabel().equals(label))
                            currentFilter = (NeuronSpatialFilter)Class.forName(key).newInstance();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    generateFilterOptions();
                }
            }
        });
        if (annModel.isFilteringEnabled() && currentFilter!=null) {
            enableFiltering.setSelected(true);
            strategySelection.setSelectedItem(strategyLabels.get(currentFilter.getClass().getName()));
        }
        if (strategySelection.getSelectedIndex()<=0) {
            strategySelection.setSelectedIndex(0);
        }


    }

    private void generateFilterOptions() {
        optionsPanel.removeAll();

        if (currentFilter==null)
            return;
        Map<String,Object> options = currentFilter.getFilterOptions();
        Map<String, String> optionsUnits = currentFilter.getFilterOptionsUnits();
        Iterator<String> optionIter = options.keySet().iterator();
        
        while (optionIter.hasNext()) {
            String optionName = optionIter.next();
            Class param = (Class)options.get(optionName);
            JLabel entryLabel = new JLabel();
            entryLabel.setText(optionName.substring(0, 1).toUpperCase()
                    + optionName.substring(1) + ": ");
            entryLabel.setAlignmentX(LEFT_ALIGNMENT);
            JLabel entryLabelUnits = new JLabel();
            if (optionsUnits.containsKey(optionName)) {
                entryLabelUnits.setText(" " + optionsUnits.get(optionName));
                entryLabelUnits.setAlignmentX(LEFT_ALIGNMENT);
            }
            JTextField entry = new JTextField();
            entry.setMaximumSize(new Dimension (100,50));
            entry.setEnabled(true);
            entry.setAlignmentX(LEFT_ALIGNMENT);
            if (param.getSimpleName().equals("Long")) {
                entry.putClientProperty("paramClass", "Long");
            } else if (param.getSimpleName().equals("Double")) {
                entry.putClientProperty("paramClass", "Double");
            } else {
                entry.putClientProperty("paramClass", "String");
            }
            entry.putClientProperty("optionName", optionName);
            entry.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    updateFilter();
                }
                
                public void removeUpdate(DocumentEvent e) {
                    updateFilter();
                }
                
                public void insertUpdate(DocumentEvent e) {
                    updateFilter();
                }
                
                public void updateFilter() {
                    String paramName = (String)entry.getClientProperty("optionName");                  
                    Object value = null;
                    if (entry.getClientProperty("paramClass").equals("Long")) {
                        value = new Long(entry.getText());
                    } else if (entry.getClientProperty("paramClass").equals("Double")) {
                        if (entry.getText()==null || entry.getText().isEmpty()) {
                            value = new Double(0);
                        } else 
                            value = new Double(entry.getText());
                    } else if (entry.getClientProperty("paramClass").equals("String")) {
                        value = entry.getText();
                    }
                    try {
                        new PropertyDescriptor(paramName, currentFilter.getClass()).getWriteMethod().invoke(currentFilter, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                String currVal = new PropertyDescriptor(optionName, currentFilter.getClass()).getReadMethod()
                        .invoke(currentFilter).toString();
                entry.setText(currVal);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Box horizontalBox = Box.createHorizontalBox();
            horizontalBox.add(entryLabel);
            horizontalBox.add(entry);
            horizontalBox.add(entryLabelUnits);
            horizontalBox.add(Box.createHorizontalGlue());

            optionsPanel.add(horizontalBox);
        }
        
        optionsPanel.revalidate();
    }
    
    public void showDialog() {
        packAndShow();
    }
    
    private boolean update() throws Exception {
        // set the filter on/off in the model and set the strategy/options
        if (enableFiltering.isSelected()) {
            annModel.setFilterStrategy(currentFilter);
            annModel.setNeuronFiltering(true);
        } else {                
            annModel.setNeuronFiltering(false);
        }

     
        return true;
    }
}