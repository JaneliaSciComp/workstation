package org.janelia.workstation.controller.options;

import net.miginfocom.swing.MigLayout;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class ApplicationPanel extends JPanel {

    private static final Icon ERROR_ICON = UIManager.getIcon("OptionPane.errorIcon");

    public static final String PREFERENCE_LOAD_LAST_OBJECT = "LoadLastObject";
    public static final String PREFERENCE_LOAD_LAST_OBJECT_DEFAULT = "false";
    public static final String PREFERENCE_DISABLE_SHARED = "DisableSharedWorkspace";
    public static final String PREFERENCE_DISABLE_SHARED_DEFAULT = "false";
    public static final String PREFERENCE_LOAD_COLOR_SLIDERS = "LoadColorSliders";
    public static final String PREFERENCE_LOAD_COLOR_SLIDERS_DEFAULT = "false";
    public static final String PREFERENCE_VERIFY_NEURONS = "VerifyNeurons";
    public static final String PREFERENCE_VERIFY_NEURONS_DEFAULT = "false";
    public static final String PREFERENCE_ANCHORS_IN_VIEWPORT = "AnchorsInViewport";
    public static final String PREFERENCE_ANCHORS_IN_VIEWPORT_DEFAULT = "true";
    public static final String PREFERENCE_Z_THICKNESS = "ZThickness";
    public static final String PREFERENCE_Z_THICKNESS_DEFAULT = "80";
    public static final String PREFERENCE_ANNOTATIONS_CLICK_MODE = "AnnotationClickMode";
    public static final String CLICK_MODE_SHIFT_LEFT_CLICK = "shift-left-click";
    public static final String CLICK_MODE_LEFT_CLICK = "left-click";
    public static final String PREFERENCE_DRAG_TO_MERGE_2D = "DragToMerge2D";
    public static final String PREFERENCE_DRAG_TO_MERGE_2D_DEFAULT = "true";
    public static final String PREFERENCE_ANNOTATIONS_CLICK_MODE_DEFAULT = CLICK_MODE_SHIFT_LEFT_CLICK;

    private final ApplicationOptionsPanelController controller;
    private JCheckBox showHortaControlCenterOnStartup;
    private JCheckBox useHTTPForTileAccess;
    private JCheckBox loadLastCheckbox;
    private JCheckBox showColorSlidersOnOpen;
    private JCheckBox disableSharedWorkspace;
    private JCheckBox verifyNeuronsCheckbox;
    private JCheckBox anchorsInViewportCheckbox;
    private JTextField zThicknessField;
    private JComboBox<String> clickModeCombo;
    private JCheckBox dragToMergeCheckbox;
    private JLabel errorLabel;

    ApplicationPanel(final ApplicationOptionsPanelController controller) {
        this.controller = controller;
        initComponents();

        JPanel attrPanel = new JPanel(new MigLayout("wrap 2, ins 20", "[grow 0, growprio 0][grow 100, growprio 100]"));

        showHortaControlCenterOnStartup = new JCheckBox();
        showHortaControlCenterOnStartup.addActionListener(e -> controller.changed());
        JLabel titleLabel1 = new JLabel("Show Horta Control Center on startup");
        titleLabel1.setLabelFor(showHortaControlCenterOnStartup);
        attrPanel.add(titleLabel1,"gap para");
        attrPanel.add(showHortaControlCenterOnStartup,"gap para");

        useHTTPForTileAccess = new JCheckBox();
        useHTTPForTileAccess.addActionListener(e -> controller.changed());
        JLabel titleLabel0 = new JLabel("Use http for tile access");
        titleLabel0.setLabelFor(useHTTPForTileAccess);
        attrPanel.add(titleLabel0,"gap para");
        attrPanel.add(useHTTPForTileAccess,"gap para");

        this.loadLastCheckbox = new JCheckBox();
        loadLastCheckbox.addChangeListener(e -> controller.changed());
        JLabel titleLabel = new JLabel("Load last opened sample or workspace on startup: ");
        titleLabel.setLabelFor(loadLastCheckbox);
        attrPanel.add(titleLabel,"gap para");
        attrPanel.add(loadLastCheckbox,"gap para");

        this.showColorSlidersOnOpen = new JCheckBox();
        showColorSlidersOnOpen.addChangeListener(e -> controller.changed());
        JLabel sliderLabel = new JLabel("Open Color Sliders Panel on Horta 3D Open: ");
        sliderLabel.setLabelFor(showColorSlidersOnOpen);
        attrPanel.add(sliderLabel,"gap para");
        attrPanel.add(showColorSlidersOnOpen,"gap para");

        this.disableSharedWorkspace = new JCheckBox();
        disableSharedWorkspace.addChangeListener(e -> controller.changed());
        JLabel disableSharedLabel = new JLabel("Disable Shared Workspace for Performance: ");
        disableSharedLabel.setLabelFor(disableSharedWorkspace);
        attrPanel.add(disableSharedLabel,"gap para");
        attrPanel.add(disableSharedWorkspace,"gap para");

        this.verifyNeuronsCheckbox = new JCheckBox();
        verifyNeuronsCheckbox.addChangeListener(e -> controller.changed());
        JLabel titleLabel4 = new JLabel("Verify neurons on workspace load: ");
        titleLabel4.setLabelFor(verifyNeuronsCheckbox);
        attrPanel.add(titleLabel4,"gap para");
        attrPanel.add(verifyNeuronsCheckbox,"gap para");

        this.anchorsInViewportCheckbox = new JCheckBox();
        anchorsInViewportCheckbox.addChangeListener(e -> controller.changed());
        JLabel titleLabel2 = new JLabel("Use anchors-in-viewport optimization: ");
        titleLabel2.setLabelFor(anchorsInViewportCheckbox);
        attrPanel.add(titleLabel2,"gap para");
        attrPanel.add(anchorsInViewportCheckbox,"gap para");

        String [] modeStrings = {CLICK_MODE_LEFT_CLICK, CLICK_MODE_SHIFT_LEFT_CLICK};
        this.clickModeCombo = new JComboBox<>(modeStrings);
        clickModeCombo.addActionListener(e -> controller.changed());
        // default to the original behavior, shift-left-click
        clickModeCombo.setSelectedItem(CLICK_MODE_SHIFT_LEFT_CLICK);
        JLabel clickModeLabel = new JLabel("Click mode for adding annotations: ");
        clickModeLabel.setLabelFor(clickModeCombo);
        attrPanel.add(clickModeLabel, "gap para");
        attrPanel.add(clickModeCombo, "gap para");

        this.dragToMergeCheckbox = new JCheckBox();
        dragToMergeCheckbox.addChangeListener(e -> controller.changed());
        JLabel titlelabel4 = new JLabel("Left-drag to merge in 2D");
        titlelabel4.setLabelFor(dragToMergeCheckbox);
        attrPanel.add(titlelabel4, "gap para");
        attrPanel.add(dragToMergeCheckbox, "gap para");

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

        setLayout(new BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    void load() {
        ApplicationOptions options = ApplicationOptions.getInstance();
        showHortaControlCenterOnStartup.setSelected(options.isShowHortaOnStartup());
        useHTTPForTileAccess.setSelected(options.isUseHTTPForTileAccess());
        loadLastCheckbox.setSelected(isLoadLastObject());
        disableSharedWorkspace.setSelected(isDisableSharedWorkspace());
        showColorSlidersOnOpen.setSelected(isLoadColorSliders());
        verifyNeuronsCheckbox.setSelected(isVerifyNeurons());
        anchorsInViewportCheckbox.setSelected(isAnchorsInViewport());
        clickModeCombo.setSelectedItem(getAnnotationClickMode());
        dragToMergeCheckbox.setSelected(isDragToMerge2D());
        zThicknessField.setText(getZThickness()+"");
    }

    void store() {

        ApplicationOptions options = ApplicationOptions.getInstance();
        options.setShowHortaControlCenterOnStartup(showHortaControlCenterOnStartup.isSelected());
        options.setUseHTTPForTileAccess(useHTTPForTileAccess.isSelected());

        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class,
                PREFERENCE_LOAD_LAST_OBJECT,
                loadLastCheckbox.isSelected()+"");

        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class,
                PREFERENCE_LOAD_COLOR_SLIDERS,
                showColorSlidersOnOpen.isSelected()+"");

        FrameworkAccess.setLocalPreferenceValue(
                ApplicationPanel.class,
                PREFERENCE_DISABLE_SHARED,
                disableSharedWorkspace.isSelected()+"");

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
                PREFERENCE_DRAG_TO_MERGE_2D,
                dragToMergeCheckbox.isSelected()+"");

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

    public static boolean isLoadColorSliders() {
        String loadColorSliders = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class,
                ApplicationPanel.PREFERENCE_LOAD_COLOR_SLIDERS,
                ApplicationPanel.PREFERENCE_LOAD_COLOR_SLIDERS_DEFAULT);
        return Boolean.parseBoolean(loadColorSliders);
    }

    public static boolean isDisableSharedWorkspace() {
        String loadLastStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class,
                ApplicationPanel.PREFERENCE_DISABLE_SHARED,
                ApplicationPanel.PREFERENCE_DISABLE_SHARED_DEFAULT);
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

    public static boolean isDragToMerge2D() {
        String dragStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class,
                ApplicationPanel.PREFERENCE_DRAG_TO_MERGE_2D,
                ApplicationPanel.PREFERENCE_DRAG_TO_MERGE_2D_DEFAULT);
        return Boolean.parseBoolean(dragStr);
    }

    public static int getZThickness() {
        String zThicknessStr = FrameworkAccess.getLocalPreferenceValue(
                ApplicationPanel.class,
                ApplicationPanel.PREFERENCE_Z_THICKNESS,
                ApplicationPanel.PREFERENCE_Z_THICKNESS_DEFAULT);
        return Integer.parseInt(zThicknessStr);
    }

}
