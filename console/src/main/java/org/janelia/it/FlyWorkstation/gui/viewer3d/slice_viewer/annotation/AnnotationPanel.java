package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.user_data.Subject;



/**
 * main class for slice viewer annotation GUI
 *
 * djo, 5/13
 */
public class AnnotationPanel extends JPanel
{

    // things we get data from
    // not clear these belong here!  should all info be shuffled through signals and actions?
    // on the other hand, even if so, we still need them just to hook everything up
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;
    private SliceViewerTranslator sliceViewerTranslator;


    // UI components
    private NeuronInfoPanel neuronInfoPanel;
    private WorkspaceInfoPanel workspaceInfoPanel;

    // actions
    private final Action createNeuronAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            annotationMgr.createNeuron();
            }
        };

    private final Action createWorkspaceAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            annotationMgr.createWorkspace();
            }
        };

    private final Action testItem1Action = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // do test thing 1
            // test: have we retrieved the initial entity properly?  yes!
            System.out.println("initialEntity = " + annotationMgr.getInitialEntity());
            System.out.println("initialEntity ID = " + annotationMgr.getInitialEntity().getId());

            // can we get user?  yes, this works:
            SessionMgr sessionMgr = SessionMgr.getSessionMgr();
            Subject subject = sessionMgr.getSubject();
            System.out.println("logged in subject name = " + subject.getName());
            System.out.println("logged in subject key = " + subject.getKey());
            }
        };

    private final Action testItem2Action = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // do test thing 2
            System.out.println("test item 2");
            }
        };


    public AnnotationPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel, 
        SliceViewerTranslator sliceViewerTranslator) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;
        this.sliceViewerTranslator = sliceViewerTranslator;

        setupUI();
        setupSignals();

    }

    @Override
    public Dimension getPreferredSize() {
        // since we create components without data, they tend to start too narrow
        //  for what they will eventually need, and that causes the split pane not
        //  to size right; so, give it a hint   
        return new Dimension(200, 0);
    }

    private void setupSignals() {
        // outgoing from the model:
        annotationModel.neuronSelectedSignal.connect(neuronInfoPanel.neuronSelectedSlot);
        annotationModel.neuronSelectedSignal.connect(workspaceInfoPanel.neuronSelectedSlot);

        annotationModel.workspaceLoadedSignal.connect(workspaceInfoPanel.workspaceLoadedSlot);

        // us to model:
        workspaceInfoPanel.neuronClickedSignal.connect(annotationModel.neuronClickedSlot);

        // us to graphics UI
        neuronInfoPanel.cameraPanToSignal.connect(sliceViewerTranslator.cameraPanToSlot);
        workspaceInfoPanel.cameraPanToSignal.connect(sliceViewerTranslator.cameraPanToSlot);

    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));


        // each of these placeholders will eventually be replaced by a
        //  class that is connected to listen to the appropriate model
        // eg, a WorkplaceInfoPanel will listen to a WorkplaceModel or
        //  something like that; probably QuadViewUi will instantiate and
        //  hook up everything

        // add a little breathing space at the top of the panel
        add(Box.createRigidArea(new Dimension(0, 20)));

        // workspace information; show name, whatever attributes
        workspaceInfoPanel = new WorkspaceInfoPanel();
        add(workspaceInfoPanel);


        // neuron information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        neuronInfoPanel = new NeuronInfoPanel();
        add(neuronInfoPanel);





        // at some point, we'll have our own sliceviewer menu; until then, attach those actions
        //  to buttons in plain view
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Commands"));
        // add(new JLabel("Menu proxy"));

        JButton createWorkspaceButton = new JButton("Create workspace");
        createWorkspaceAction.putValue(Action.NAME, "Create workspace");
        createWorkspaceAction.putValue(Action.SHORT_DESCRIPTION, "Create a new workspace");
        createWorkspaceButton.setAction(createWorkspaceAction);        
        add(createWorkspaceButton);

        JButton createNeuronButton = new JButton("Create neuron");
        createNeuronAction.putValue(Action.NAME, "Create neuron");
        createNeuronAction.putValue(Action.SHORT_DESCRIPTION, "Create a new neuron");
        createNeuronButton.setAction(createNeuronAction);        
        add(createNeuronButton);


        // NOTE: don't add these buttons for release!
        JButton testItem1Button = new JButton("Test item 1");
        testItem1Action.putValue(Action.NAME, "Test item 1");
        testItem1Action.putValue(Action.SHORT_DESCRIPTION, "Test item 1");
        testItem1Button.setAction(testItem1Action);
        // add(testItem1Button);

        JButton testItem2Button = new JButton("Test item 2");
        testItem2Action.putValue(Action.NAME, "Test item 2");
        testItem2Action.putValue(Action.SHORT_DESCRIPTION, "Test item 2");
        testItem2Button.setAction(testItem2Action);
        // add(testItem2Button);



        // the bilge...
        add(Box.createVerticalGlue());


    }



}



