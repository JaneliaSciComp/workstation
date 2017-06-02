package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DownloadVisualPanel2 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel2.class);

    private static final String NATIVE_EXTENSION = "No conversion";

    private static final String[] FORMAT_EXTENSIONS = {
            NATIVE_EXTENSION,
            "tif", 
            "v3draw", 
            "v3dpbd", 
            "mp4", 
            "h5j",
            "lsm", 
            "lsm.bz2" 
    };
    
    // GUI
    private GroupedKeyValuePanel attrPanel;
    private JComboBox<String> formatCombo;
    private JCheckBox splitChannelCheckbox;
    
    // Inputs
    private String outputExtension;
    private boolean splitChannels;
    
    @Override
    public String getName() {
        return "File Processing";
    }
    
    /**
     * Creates new form DownloadVisualPanel4
     */
    public DownloadVisualPanel2() {
        setLayout(new BorderLayout());
    }

    public void init(DownloadWizardState state) {

        this.outputExtension = state.getOutputFormat();
        this.splitChannels = state.isSplitChannels();
        
        if (outputExtension==null) {
            outputExtension = NATIVE_EXTENSION;
        }
        
        attrPanel = new GroupedKeyValuePanel();
        
        formatCombo = new JComboBox<>();
        formatCombo.setEditable(false);
        formatCombo.setToolTipText("Choose an export format for 3d image stacks");
        attrPanel.addItem("Output image format", formatCombo);

        splitChannelCheckbox = new JCheckBox();
        splitChannelCheckbox.setSelected(splitChannels);
        attrPanel.addItem("Split channels", splitChannelCheckbox);        

        attrPanel.addItem(new JLabel("Note: this processing is only applied to 3d image stacks"), "span 2");
        
        populateExtensions();
        
        removeAll();
        add(attrPanel, BorderLayout.CENTER);
    }

    private void populateExtensions() {

        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) formatCombo.getModel();
        String currValue = (String)model.getSelectedItem();
        
        model.removeAllElements();
        for (String extension : FORMAT_EXTENSIONS) {
            model.addElement(extension);
            // Select the native extension by default
            if (extension.equals(outputExtension)) {
                model.setSelectedItem(extension);
            }
        }

        // If the user already selected something, keep it selected
        if (currValue!=null) {
            model.setSelectedItem(currValue);
        }
    }

    public boolean isSplitChannels() {
        return splitChannelCheckbox.isSelected();
    }

    public String getOutputFormat() {
        String outputExtension = (String)formatCombo.getSelectedItem();
        if (NATIVE_EXTENSION.equals(outputExtension)) {
            return null;
        }
        return outputExtension;
    }
}
