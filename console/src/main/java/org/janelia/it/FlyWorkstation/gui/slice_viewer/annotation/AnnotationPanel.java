package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


// std lib imports

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.signal.Signal;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;




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

    public Signal centerAnnotationSignal = new Signal();
    private final Action centerAnnotationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            centerAnnotationSignal.emit();
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

        // add a little breathing space at the top of the panel
        add(Box.createRigidArea(new Dimension(0, 20)));

        // workspace information; show name, whatever attributes, list of neurons
        workspaceInfoPanel = new WorkspaceInfoPanel();
        add(workspaceInfoPanel);

        // buttons for acting on neurons (which are in the list immediately above):
        JPanel neuronButtonsPanel = new JPanel();
        neuronButtonsPanel.setLayout(new BoxLayout(neuronButtonsPanel, BoxLayout.LINE_AXIS));
        add(neuronButtonsPanel);

        JButton createNeuronButtonPlus = new JButton("+");
        neuronButtonsPanel.add(createNeuronButtonPlus);
        createNeuronAction.putValue(Action.NAME, "+");
        createNeuronAction.putValue(Action.SHORT_DESCRIPTION, "Create a new neuron");
        createNeuronButtonPlus.setAction(createNeuronAction);




        // neuron information; show name, whatever attributes, list of neurites
        add(Box.createRigidArea(new Dimension(0, 20)));
        neuronInfoPanel = new NeuronInfoPanel();
        add(neuronInfoPanel);


        // buttons for acting on annotations or neurites (which are in the list immediately above):
        JPanel neuriteButtonsPanel = new JPanel();
        neuriteButtonsPanel.setLayout(new BoxLayout(neuriteButtonsPanel, BoxLayout.LINE_AXIS));
        add(neuriteButtonsPanel);

        JButton centerAnnotationButton = new JButton("Center");
        centerAnnotationAction.putValue(Action.NAME, "Center");
        centerAnnotationAction.putValue(Action.SHORT_DESCRIPTION, "Center on current annotation [C]");
        centerAnnotationButton.setAction(centerAnnotationAction);
        String parentIconFilename = "ParentAnchor16.png";
        ImageIcon anchorIcon = Icons.getIcon(parentIconFilename);
        centerAnnotationButton.setIcon(anchorIcon);
        centerAnnotationButton.setHideActionText(true);
        neuriteButtonsPanel.add(centerAnnotationButton);


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

        /*
        // moved to button just under neuron list
        JButton createNeuronButton = new JButton("Create neuron");
        createNeuronAction.putValue(Action.NAME, "Create neuron");
        createNeuronAction.putValue(Action.SHORT_DESCRIPTION, "Create a new neuron");
        createNeuronButton.setAction(createNeuronAction);        
        add(createNeuronButton);
        */


        // the bilge...
        add(Box.createVerticalGlue());


    }



}



