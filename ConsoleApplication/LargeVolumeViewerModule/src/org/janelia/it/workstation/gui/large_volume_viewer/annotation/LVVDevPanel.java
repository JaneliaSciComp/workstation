package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import groovy.ui.Console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * this panel is only shown to me; I use it when I need to insert
 * pieces of code for testing, etc.
 *
 * djo, 11/14
 */
public class LVVDevPanel extends JPanel {
    // these are useful to have around when testing:
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private LargeVolumeViewerTranslator largeVolumeViewerTranslator;


    public LVVDevPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel,
                       LargeVolumeViewerTranslator largeVolumeViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.largeVolumeViewerTranslator = largeVolumeViewerTranslator;

        setupUI();
    }


    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Debug functions", JLabel.CENTER));

        JButton groovyButton = new JButton("Groovy");
        groovyButton.setAction(new AbstractAction("Groovy console") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Console console = new Console();
                console.setVariable("annMgr", annotationMgr);
                console.setVariable("annModel", annotationModel);
                console.setVariable("lvvTrans", largeVolumeViewerTranslator);
                console.run();

            }
        });
        add(groovyButton);

    }

}
