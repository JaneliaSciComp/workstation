package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JLabel;

import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;

/**
 * this is the container panel for UI elements that are part of the
 * directed workflow; it replaces the BasicAnnotationPanel from the
 * usual manual tracing workflow
 */
public class DirectedSessionPanel extends AnnotationPanel {
    // as in BasicAnnPanel, we have a constant height unit so
    //  if we have subcomponents, we can tell each one how tall
    //  to be in multiples of the standard height
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

        // pack up your troubles in your old grid bag and smile, smile, smile...
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

        // ---------- information section
        // in basic tracing, this section is partially handled by
        //  WorkspaceInfoPanel; we'll cram it all in here and
        //  separate it out later if it makes sense

        // spacer, then some info labels:
        add(Box.createRigidArea(new Dimension(0, 10)), cVert);

        add(new JLabel("Name: " + annotationMgr.getInitialObject().getName(), JLabel.LEADING), cVert);
        add(new JLabel("Sample: " + annotationMgr.getCurrentSample().getName(), JLabel.LEADING), cVert);

        // session menu; I use the gear icon in other place, so I should keep
        //  using it; that being said, I don't like how it looks alone, without
        //  + and or - buttons near it (which we don't have here)

        // could just put in a couple buttons instead of a menu, if you
        //  think there will only be a few of them?






        // big spacer, just to separate stuff while developing
        add(Box.createRigidArea(new Dimension(0, 150)), cVert);



        // ---------- decision handling section

        add(new JLabel("decision controls here!", JLabel.CENTER), cVert);


        // this adds the button, with right name, but it's grayed out
        //  (and ctrl-N still works); tried multiple variations of
        //  instantiating button and action as class variables
        // add(new JButton(new NextDecisionAction()), cVert);




        add(Box.createRigidArea(new Dimension(0, 40)), cVert);



        add(new JLabel("Progress: xxx/yyy (zzz%)", JLabel.CENTER), cVert);



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
