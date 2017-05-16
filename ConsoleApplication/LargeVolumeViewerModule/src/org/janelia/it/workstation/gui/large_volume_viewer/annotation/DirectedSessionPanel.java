package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
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


    // actions
    AbstractAction gotoDecisionAction;
    AbstractAction nextDecisionAction;
    AbstractAction finishedAction;




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

        // ---> don't forget to hook in the controller; don't
        //  do direct calls!


        add(new JLabel("Decision controls", JLabel.CENTER), cVert);

        // next = this decision made, go to next (needs better name?)
        JButton nextDecisionButton = new JButton("Next decision");
        nextDecisionAction = new AbstractAction("Next decision") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("next decision action");
            }
        };
        nextDecisionButton.setAction(nextDecisionAction);
        add(nextDecisionButton, cVert);


        JButton gotoDecisionButton = new JButton("Go to decision");
        gotoDecisionAction = new AbstractAction("Go to decision") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("go to decision action");
            }
        };
        gotoDecisionButton.setAction(gotoDecisionAction);
        add(gotoDecisionButton, cVert);


        // this will eventually be dynamic, depending on what kind
        //  of decision we're asking for
        // first stage: correct or not
        add(new JLabel("Decision buttons (yes/no/a/b/c)", JLabel.CENTER), cVert);


        // need to rename; finished = finished with this batch (not this one)
        JButton finishedButton = new JButton("Finished");
        finishedAction = new AbstractAction("Finished") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("finished action");
            }
        };
        finishedButton.setAction(finishedAction);
        add(finishedButton, cVert);



        add(Box.createRigidArea(new Dimension(0, 40)), cVert);


        add(new JLabel("List of completed decisions", JLabel.CENTER), cVert);


        add(Box.createRigidArea(new Dimension(0, 40)), cVert);



        add(new JLabel("Progress: xxx/yyy (zzz%)", JLabel.CENTER), cVert);



        // big spacer, just to separate stuff while developing
        add(Box.createRigidArea(new Dimension(0, 150)), cVert);

        // ---------- debugging/testing section
        add(new JLabel("debugging/testing info here", JLabel.CENTER), cVert);





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
