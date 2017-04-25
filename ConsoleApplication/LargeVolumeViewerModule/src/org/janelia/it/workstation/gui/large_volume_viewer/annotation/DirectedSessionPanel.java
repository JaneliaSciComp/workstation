package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;

/**
 * this is the container panel for UI elements that are part of the
 * directed workflow; it replaces the BasicAnnotationPanel from the
 * usual manual tracing workflow
 */
public class DirectedSessionPanel extends AnnotationPanel {
    public static final int SUBPANEL_STD_HEIGHT = 150;

    private AnnotationManager annotationMgr;
    private LargeVolumeViewerTranslator lvvTranslator;

    private ViewStateListener viewStateListener;

    // major UI panels


    // other UI stuff
    private static final int width = 250;


    public DirectedSessionPanel(AnnotationManager annotationMgr, LargeVolumeViewerTranslator lvvTranslator) {
        this.annotationMgr = annotationMgr;
        this.lvvTranslator = lvvTranslator;

        setupUI();



    }

    @Override
    public void setViewStateListener(ViewStateListener listener) {
        this.viewStateListener = listener;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, 0);
    }

    private void setupUI() {

        setLayout(new GridBagLayout());

        GridBagConstraints cTop = new GridBagConstraints();
        cTop.gridx = 0;
        cTop.gridy = 0;
        cTop.anchor = GridBagConstraints.PAGE_START;
        cTop.fill = GridBagConstraints.HORIZONTAL;
        cTop.insets = new Insets(10, 0, 0, 0);
        cTop.weighty = 0.0;

        // error on this line?  compare other file!
        // illegal component position

        add(new JLabel("Directed session info", JLabel.LEADING));

        // I want the rest of the components to stack vertically;
        //  components should fill or align left as appropriate
        GridBagConstraints cVert = new GridBagConstraints();
        cVert.gridx = 0;
        cVert.gridy = GridBagConstraints.RELATIVE;
        cVert.anchor = GridBagConstraints.PAGE_START;
        cVert.fill = GridBagConstraints.HORIZONTAL;
        cVert.weighty = 0.0;





        // push everything else up:
        GridBagConstraints cBottom = new GridBagConstraints();
        cBottom.gridx = 0;
        cBottom.gridy = GridBagConstraints.RELATIVE;
        cBottom.anchor = GridBagConstraints.PAGE_START;
        cBottom.fill = GridBagConstraints.BOTH;
        cBottom.weighty = 1.0;
        add(Box.createVerticalGlue(), cBottom);


    }



}
