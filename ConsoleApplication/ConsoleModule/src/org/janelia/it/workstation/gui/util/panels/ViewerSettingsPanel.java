package org.janelia.it.workstation.gui.util.panels;

import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.framework.roles.PrefEditor;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.ImagesPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ViewerSettingsPanel extends JPanel implements PrefEditor {

    private boolean settingsChanged = false;

    public static final String DISABLE_IMAGE_DRAG_PROPERTY = "SessionMgr.DisableImageDragProperty";
    public static final String ONLY_SESSION_ANNOTATIONS_PROPERTY = "SessionMgr.OnlySessionAnnotationsProperty";
    public static final String HIDE_ANNOTATED_PROPERTY = "SessionMgr.HideAnnotatedProperty";
    public static final String SHOW_ANNOTATION_TABLES_PROPERTY = "SessionMgr.ShowAnnotationTablesProperty";
    public static final String ANNOTATION_TABLES_HEIGHT_PROPERTY = "SessionMgr.AnnotationTablesHeightProperty";

    JCheckBox disableImageDrag = new JCheckBox();
    JCheckBox onlySessionAnnotations = new JCheckBox();
    JCheckBox hideAnnotatedImages = new JCheckBox();
    JCheckBox showAnnotationTables = new JCheckBox();
    JSlider annotationTableHeight = new JSlider(ImagesPanel.MIN_TABLE_HEIGHT, ImagesPanel.MAX_TABLE_HEIGHT, ImagesPanel.DEFAULT_TABLE_HEIGHT);

    SessionMgr sessionMgr = SessionMgr.getSessionMgr();

    public ViewerSettingsPanel(JFrame parentFrame) {
        try {
            jbInit();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public String getDescription() {
        return "Set image viewer preferences.";
    }

    public String getPanelGroup() {
        return PrefController.VIEWER_EDITOR;
    }

    private void jbInit() throws Exception {

        /**
         * ************* Interface Options **************
         */
        JPanel interfaceOptions = new JPanel();
        interfaceOptions.setLayout(new BoxLayout(interfaceOptions, BoxLayout.Y_AXIS));
        interfaceOptions.setBorder(new TitledBorder("Interface Options"));

        disableImageDrag.setText("Disable drag and drop in the image viewer");
        disableImageDrag.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });

        if (sessionMgr.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY) == null) {
            sessionMgr.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, Boolean.FALSE);
        }
        else {
            disableImageDrag.setSelected((Boolean) sessionMgr.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY));
        }

        interfaceOptions.add(Box.createVerticalStrut(5));
        interfaceOptions.add(disableImageDrag);
        interfaceOptions.add(Box.createVerticalStrut(5));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(Box.createVerticalStrut(10));
        this.add(interfaceOptions);

        /**
         * ************* Annotation Options **************
         */
        JPanel annotationPanel = new JPanel();
        annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.Y_AXIS));
        annotationPanel.setBorder(new TitledBorder("Annotation Options"));

        onlySessionAnnotations.setText("Only show annotations within the current annotation session");
        onlySessionAnnotations.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });
        if (sessionMgr.getModelProperty(ONLY_SESSION_ANNOTATIONS_PROPERTY) == null) {
            sessionMgr.setModelProperty(ONLY_SESSION_ANNOTATIONS_PROPERTY, Boolean.FALSE);
        }
        else {
            onlySessionAnnotations.setSelected((Boolean) sessionMgr.getModelProperty(ONLY_SESSION_ANNOTATIONS_PROPERTY));
        }

        hideAnnotatedImages.setText("Hide images that have been annotated completely according to the session's ruleset");
        hideAnnotatedImages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
            }
        });
        if (sessionMgr.getModelProperty(HIDE_ANNOTATED_PROPERTY) == null) {
            sessionMgr.setModelProperty(HIDE_ANNOTATED_PROPERTY, Boolean.FALSE);
        }
        else {
            hideAnnotatedImages.setSelected((Boolean) sessionMgr.getModelProperty(HIDE_ANNOTATED_PROPERTY));
        }

        annotationTableHeight.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        annotationTableHeight.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                settingsChanged = true;
            }
        });

        if (sessionMgr.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY) == null) {
            sessionMgr.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, ImagesPanel.DEFAULT_TABLE_HEIGHT);
        }
        else {
            annotationTableHeight.setValue((Integer) sessionMgr.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY));
        }
        annotationTableHeight.setEnabled(false);

        final JLabel tableHeightLabel = new JLabel("Annotation table height: ");

        final JPanel tableHeightPanel = new JPanel();
        tableHeightPanel.setLayout(new BoxLayout(tableHeightPanel, BoxLayout.LINE_AXIS));
        tableHeightPanel.add(Box.createHorizontalStrut(15));
        tableHeightPanel.add(tableHeightLabel);
        tableHeightPanel.add(annotationTableHeight);

        showAnnotationTables.setText("Show annotations in a table instead of a tag cloud");
        showAnnotationTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                settingsChanged = true;
                annotationTableHeight.setEnabled(showAnnotationTables.isSelected());
            }
        });
        if (sessionMgr.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY) == null) {
            sessionMgr.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, Boolean.FALSE);
        }
        else {
            showAnnotationTables.setSelected((Boolean) sessionMgr.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY));
            annotationTableHeight.setEnabled(showAnnotationTables.isSelected());
        }

        onlySessionAnnotations.setAlignmentX(Component.LEFT_ALIGNMENT);
        hideAnnotatedImages.setAlignmentX(Component.LEFT_ALIGNMENT);
        showAnnotationTables.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableHeightPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        annotationPanel.add(Box.createVerticalStrut(5));
        annotationPanel.add(onlySessionAnnotations);
        annotationPanel.add(Box.createVerticalStrut(5));
        annotationPanel.add(hideAnnotatedImages);
        annotationPanel.add(Box.createVerticalStrut(5));
        annotationPanel.add(showAnnotationTables);
        annotationPanel.add(tableHeightPanel);

        this.add(Box.createVerticalStrut(20));
        this.add(annotationPanel);
    }

    public void dispose() {
    }

    public String getName() {
        return "Viewer Settings";
    }

    public void cancelChanges() {
        settingsChanged = false;
    }

    public boolean hasChanged() {
        return settingsChanged;
    }

    public String[] applyChanges() {
        settingsChanged = false;
        if (disableImageDrag.isSelected() != (Boolean) sessionMgr.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY)) {
            sessionMgr.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, disableImageDrag.isSelected());
        }
        if (onlySessionAnnotations.isSelected() != (Boolean) sessionMgr.getModelProperty(ONLY_SESSION_ANNOTATIONS_PROPERTY)) {
            sessionMgr.setModelProperty(ONLY_SESSION_ANNOTATIONS_PROPERTY, onlySessionAnnotations.isSelected());
        }
        if (hideAnnotatedImages.isSelected() != (Boolean) sessionMgr.getModelProperty(HIDE_ANNOTATED_PROPERTY)) {
            sessionMgr.setModelProperty(HIDE_ANNOTATED_PROPERTY, hideAnnotatedImages.isSelected());
        }
        if (showAnnotationTables.isSelected() != (Boolean) sessionMgr.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY)) {
            sessionMgr.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, showAnnotationTables.isSelected());
        }
        if (annotationTableHeight.getValue() != (Integer) sessionMgr.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY)) {
            sessionMgr.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, annotationTableHeight.getValue());
        }
        return NO_DELAYED_CHANGES;
    }

}
