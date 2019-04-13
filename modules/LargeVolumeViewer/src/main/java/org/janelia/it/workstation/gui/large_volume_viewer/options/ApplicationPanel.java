package org.janelia.it.workstation.gui.large_volume_viewer.options;

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.janelia.workstation.integration.util.FrameworkAccess;

import net.miginfocom.swing.MigLayout;

public class ApplicationPanel extends javax.swing.JPanel {

    private static final Icon ERROR_ICON = UIManager.getIcon("OptionPane.errorIcon");
    
    public static final String PREFERENCE_LOAD_LAST_OBJECT = "LoadLastObject";
    public static final String PREFERENCE_LOAD_LAST_OBJECT_DEFAULT = "true";
    public static final String PREFERENCE_VERIFY_NEURONS = "VerifyNeurons";
    public static final String PREFERENCE_VERIFY_NEURONS_DEFAULT = "false";
    public static final String PREFERENCE_ANCHORS_IN_VIEWPORT = "AnchorsInViewport";
    public static final String PREFERENCE_ANCHORS_IN_VIEWPORT_DEFAULT = "true";
    public static final String PREFERENCE_Z_THICKNESS = "ZThickness";
    public static final String PREFERENCE_Z_THICKNESS_DEFAULT = "80";
    public static final String PREFERENCE_ANNOTATIONS_CLICK_MODE = "AnnotationClickMode";
    public static final String CLICK_MODE_SHIFT_LEFT_CLICK = "shift-left-click";
    public static final String CLICK_MODE_LEFT_CLICK = "left-click";
    public static final String PREFERENCE_ANNOTATIONS_CLICK_MODE_DEFAULT = CLICK_MODE_SHIFT_LEFT_CLICK;

    private final ApplicationOptionsPanelController controller;
    private JCheckBox loadLastCheckbox; 
    private JCheckBox verifyNeuronsCheckbox; 
    private JCheckBox anchorsInViewportCheckbox; 
    private JTextField zThicknessField;
    private JComboBox<String> clickModeCombo;
    private JLabel errorLabel;
    
    ApplicationPanel(final ApplicationOptionsPanelController controller) {
        this.controller = controller;
        initComponents();

        JPanel attrPanel = new JPanel(new MigLayout("wrap 2, ins 20", "[grow 0, growprio 0][grow 100, growprio 100]"));

        this.loadLastCheckbox = new JCheckBox();
        loadLastCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                controller.changed();
            }
        });
        JLabel titleLabel = new JLabel("Load last opened sample or workspace on startup: ");
        titleLabel.setLabelFor(loadLastCheckbox);
        attrPanel.add(titleLabel,"gap para");
        attrPanel.add(loadLastCheckbox,"gap para");

        
        this.verifyNeuronsCheckbox = new JCheckBox();
        verifyNeuronsCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                controller.changed();
            }
        });
        JLabel titleLabel4 = new JLabel("Verify neurons on workspace load: ");
        titleLabel4.setLabelFor(verifyNeuronsCheckbox);
        attrPanel.add(titleLabel4,"gap para");
        attrPanel.add(verifyNeuronsCheckbox,"gap para");
        

        this.anchorsInViewportCheckbox = new JCheckBox();
        anchorsInViewportCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                controller.changed();
            }
        });
        JLabel titleLabel2 = new JLabel("Use anchors-in-viewport optimization: ");
        titleLabel2.setLabelFor(anchorsInViewportCheckbox);
        attrPanel.add(titleLabel2,"gap para");
        attrPanel.add(anchorsInViewportCheckbox,"gap para");


        String [] modeStrings = {CLICK_MODE_LEFT_CLICK, CLICK_MODE_SHIFT_LEFT_CLICK};
        this.clickModeCombo = new JComboBox<>(modeStrings);
        clickModeCombo.addActionListener(e -> controller.changed());
        // default to the original behavior, shift-left-click
        clickModeCombo.setSelectedItem(CLICK_MODE_SHIFT_LEFT_CLICK);
        // clickModeCombo.setSelectedIndex(1);
        JLabel clickModeLabel = new JLabel("Click mode for adding annotations: ");
        clickModeLabel.setLabelFor(clickModeCombo);
        attrPanel.add(clickModeLabel, "gap para");
        attrPanel.add(clickModeCombo, "gap para");


        this.zThicknessField = new JTextField();
        zThicknessField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                controller.changed();
            }
            public void removeUpdate(DocumentEvent e) {
                controller.changed();
            }
            public void insertUpdate(DocumentEvent e) {
                controller.changed();
            }
        });
        JLabel titleLabel3 = new JLabel("Z-slice Thickness (pixels): ");
        titleLabel3.setLabelFor(zThicknessField);
        attrPanel.add(titleLabel3,"gap para");
        attrPanel.add(zThicknessField,"gap para, width 80:100:100, growx");


        this.errorLabel = new JLabel("");
        errorLabel.setIcon(ERROR_ICON);
        errorLabel.setVisible(false);
        attrPanel.add(errorLabel,"gap para, growx");
        
        
        add(attrPanel, BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    void load() {
        loadLastCheckbox.setSelected(isLoadLastObject());
        verifyNeuronsCheckbox.setSelected(isVerifyNeurons());
        anchorsInViewportCheckbox.setSelected(isAnchorsInViewport());
        clickModeCombo.setSelectedItem(getAnnotationClickMode());
        zThicknessField.setText(getZThickness()+"");
    }

    void store() {
        
        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class, 
                PREFERENCE_LOAD_LAST_OBJECT, 
                loadLastCheckbox.isSelected()+"");

        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class, 
                PREFERENCE_VERIFY_NEURONS, 
                verifyNeuronsCheckbox.isSelected()+"");
        
        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class, 
                PREFERENCE_ANCHORS_IN_VIEWPORT, 
                anchorsInViewportCheckbox.isSelected()+"");  

        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class,
                PREFERENCE_ANNOTATIONS_CLICK_MODE,
                (String) clickModeCombo.getSelectedItem());

        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class, 
                PREFERENCE_Z_THICKNESS, 
                zThicknessField.getText());  
    }

    boolean valid() {
        errorLabel.setVisible(false);
        
        if (!isZThicknessValid()) {
            errorLabel.setText("Z-slice Thickness must be a positive integer");
            errorLabel.setVisible(true);
            return false;
        }

        return true;
    }
    
    boolean isZThicknessValid() {
        try {
            int zThickness = Integer.parseInt(zThicknessField.getText());
            return zThickness>0;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    public static boolean isLoadLastObject() {
        String loadLastStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class, 
                ApplicationPanel.PREFERENCE_LOAD_LAST_OBJECT, 
                ApplicationPanel.PREFERENCE_LOAD_LAST_OBJECT_DEFAULT);
        return Boolean.parseBoolean(loadLastStr);
    }

    public static boolean isVerifyNeurons() {
        String loadLastStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class, 
                ApplicationPanel.PREFERENCE_VERIFY_NEURONS, 
                ApplicationPanel.PREFERENCE_VERIFY_NEURONS_DEFAULT);
        return Boolean.parseBoolean(loadLastStr);
    }
    
    public static boolean isAnchorsInViewport() {
        String anchorsInViewportStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class, 
                ApplicationPanel.PREFERENCE_ANCHORS_IN_VIEWPORT, 
                ApplicationPanel.PREFERENCE_ANCHORS_IN_VIEWPORT_DEFAULT);
        return Boolean.parseBoolean(anchorsInViewportStr);
    }

    public static String getAnnotationClickMode() {
        return FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class,
                ApplicationPanel.PREFERENCE_ANNOTATIONS_CLICK_MODE,
                ApplicationPanel.PREFERENCE_ANNOTATIONS_CLICK_MODE_DEFAULT);
    }

    public static int getZThickness() {
        String zThicknessStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class, 
                ApplicationPanel.PREFERENCE_Z_THICKNESS, 
                ApplicationPanel.PREFERENCE_Z_THICKNESS_DEFAULT);
        return Integer.parseInt(zThicknessStr);
    }
    
}
