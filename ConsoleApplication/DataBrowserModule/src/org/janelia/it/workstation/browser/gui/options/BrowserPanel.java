package org.janelia.it.workstation.browser.gui.options;

import static org.janelia.it.workstation.browser.gui.options.OptionConstants.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImagesPanel;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BrowserPanel extends javax.swing.JPanel {

    private static final Logger log = LoggerFactory.getLogger(BrowserPanel.class);

    private final BrowserOptionsPanelController controller;
    private final GroupedKeyValuePanel mainPanel;
    
    private JCheckBox downloadDialogCheckbox;
    private JCheckBox unloadImagesCheckbox;
    private JCheckBox disableImageDrag;
    private JCheckBox allowDuplicateAnnotations;
    private JCheckBox showAnnotationTables;
    private JSlider annotationTableHeight;
    
    BrowserPanel(BrowserOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        
        this.mainPanel = new GroupedKeyValuePanel();
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    void load() {

        ConsoleApp app = ConsoleApp.getConsoleApp();

        log.info("Loading browser settings...");
        mainPanel.removeAll();

        mainPanel.addSeparator("Image Browser");

        // Download Dialog

        downloadDialogCheckbox = new JCheckBox();
        downloadDialogCheckbox.setText("Use the legacy download dialog instead of the wizard");
        downloadDialogCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });
        
        if (app.getModelProperty(LEGACY_DOWNLOAD_DIALOG) == null) {
            app.setModelProperty(LEGACY_DOWNLOAD_DIALOG, Boolean.FALSE);
        }
        else {
            downloadDialogCheckbox.setSelected((Boolean) app.getModelProperty(LEGACY_DOWNLOAD_DIALOG));
        }

        mainPanel.addItem(downloadDialogCheckbox);

        // Unload Images

        unloadImagesCheckbox = new JCheckBox();
        unloadImagesCheckbox.setText("Unload images which are not visible on the screen");
        unloadImagesCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });

        if (app.getModelProperty(UNLOAD_IMAGES_PROPERTY) == null) {
            app.setModelProperty(UNLOAD_IMAGES_PROPERTY, Boolean.FALSE);
        }
        else {
            unloadImagesCheckbox.setSelected((Boolean) app.getModelProperty(UNLOAD_IMAGES_PROPERTY));
        }

        mainPanel.addItem(unloadImagesCheckbox);

        // Disable drag/drop
        
        disableImageDrag = new JCheckBox();
        disableImageDrag.setText("Disable drag and drop in the image viewer");
        disableImageDrag.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });

        if (app.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY) == null) {
            app.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, Boolean.FALSE);
        }
        else {
            disableImageDrag.setSelected((Boolean) app.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY));
        }

        mainPanel.addItem(disableImageDrag);

        // Allow duplicate annotation keys
        
        allowDuplicateAnnotations = new JCheckBox();
        allowDuplicateAnnotations.setText("Allow duplicate annotations on a single item");
        allowDuplicateAnnotations.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });
        if (app.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY) == null) {
            app.setModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY, Boolean.FALSE);
        }
        else {
            allowDuplicateAnnotations.setSelected((Boolean) app.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY));
        }

        mainPanel.addItem(allowDuplicateAnnotations);
        
        // Use Annotation Tables
        
        showAnnotationTables = new JCheckBox();
        showAnnotationTables.setText("Show annotations in a table instead of a tag cloud");
        showAnnotationTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.changed();
            }
        });
        if (app.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY) == null) {
            app.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, Boolean.FALSE);
        }
        else {
            showAnnotationTables.setSelected((Boolean) app.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY));
        }

        mainPanel.addItem(showAnnotationTables);

        // Annotation table height

        annotationTableHeight = new JSlider(ImagesPanel.MIN_TABLE_HEIGHT, ImagesPanel.MAX_TABLE_HEIGHT, ImagesPanel.DEFAULT_TABLE_HEIGHT);
        annotationTableHeight.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        annotationTableHeight.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                controller.changed();
            }
        });

        if (app.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY) == null) {
            app.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, ImagesPanel.DEFAULT_TABLE_HEIGHT);
        }
        else {
            annotationTableHeight.setValue((Integer) app.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY));
        }

        mainPanel.addItem("Annotation table height", annotationTableHeight);
    }

    void store() {

        ConsoleApp app = ConsoleApp.getConsoleApp();

        if (downloadDialogCheckbox.isSelected() != (Boolean) app.getModelProperty(LEGACY_DOWNLOAD_DIALOG)) {
            log.info("Saving legacy download dialog setting: "+downloadDialogCheckbox.isSelected());
            app.setModelProperty(LEGACY_DOWNLOAD_DIALOG, downloadDialogCheckbox.isSelected());
        }
        
        if (unloadImagesCheckbox.isSelected() != (Boolean) app.getModelProperty(UNLOAD_IMAGES_PROPERTY)) {
            log.info("Saving unload images setting: "+unloadImagesCheckbox.isSelected());
            app.setModelProperty(UNLOAD_IMAGES_PROPERTY, unloadImagesCheckbox.isSelected());
        }

        if (disableImageDrag.isSelected() != (Boolean) app.getModelProperty(DISABLE_IMAGE_DRAG_PROPERTY)) {
            log.info("Saving disable image drag: "+disableImageDrag.isSelected());
            app.setModelProperty(DISABLE_IMAGE_DRAG_PROPERTY, disableImageDrag.isSelected());
        }

        if (allowDuplicateAnnotations.isSelected() != (Boolean) app.getModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY)) {
            log.info("Saving allow annotation duplicates: "+allowDuplicateAnnotations.isSelected());
            app.setModelProperty(DUPLICATE_ANNOTATIONS_PROPERTY, allowDuplicateAnnotations.isSelected());
        }
        
        if (showAnnotationTables.isSelected() != (Boolean) app.getModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY)) {
            log.info("Saving show annotation tables: "+showAnnotationTables.isSelected());
            app.setModelProperty(SHOW_ANNOTATION_TABLES_PROPERTY, showAnnotationTables.isSelected());
        }

        if (annotationTableHeight.getValue() != (Integer) app.getModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY)) {
            log.info("Saving annotation table height: "+annotationTableHeight.getValue());
            app.setModelProperty(ANNOTATION_TABLES_HEIGHT_PROPERTY, annotationTableHeight.getValue());
        }
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
