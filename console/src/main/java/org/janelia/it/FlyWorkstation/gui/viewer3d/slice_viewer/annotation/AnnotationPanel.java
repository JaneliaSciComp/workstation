package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.user_data.Subject;



/**
 * main class for slice viewer annotation GUI
 *
 * djo, 5/13
 */
public class AnnotationPanel extends JPanel implements TreeSelectionListener
{

    // things we get data from
    // not clear these belong here!  should all info be shuffled through signals and actions?
    // on the other hand, even if so, we still need them just to hook everything up
    private AnnotationManager annotationMgr;
    private AnnotationModel annotationModel;



    // UI components
    private JTree neuriteTree;

    private JLabel nodeLabel;
    
    private NeuronInfoPanel neuronInfoPanel;
    private NeuriteTreePanel neuriteTreePanel;
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


    public AnnotationPanel(AnnotationManager annotationMgr, AnnotationModel annotationModel) {
        this.annotationMgr = annotationMgr;
        this.annotationModel = annotationModel;

        setupUI();
        setupSignals();

    }

    private void setupSignals() {
        annotationModel.workspaceChangedSignal.connect(workspaceInfoPanel.updateWorkspaceSlot);
        annotationModel.neuronChangedSignal.connect(neuronInfoPanel.updateNeuronSlot);

        workspaceInfoPanel.neuronClickedSignal.connect(annotationModel.neuronSelectedSlot);
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));




        // each of these placeholders will eventually be replaced by a
        //  class that is connected to listen to the appropriate model
        // eg, a WorkplaceInfoPanel will listen to a WorkplaceModel or
        //  something like that; probably QuadViewUi will instantiate and
        //  hook up everything


        // workspace information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        workspaceInfoPanel = new WorkspaceInfoPanel();
        add(workspaceInfoPanel);


        // neuron information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        neuronInfoPanel = new NeuronInfoPanel();
        add(neuronInfoPanel);


        // neurite tree navigator
        neuriteTreePanel = new NeuriteTreePanel();
        add(neuriteTreePanel);

        // move this into the class:
        setupNeuriteTreeNavigator();



        // neurite information; show name, type (axon, dendrite, etc), other attributes
        //  of selected (current) neurite
        // add(Box.createRigidArea(new Dimension(0, 20)));
        // add(new JLabel("Neurite information"));




        // info for individual annotations; maybe not much here...not clear what any one
        //  node's going to need that's worthwhile; won't have user-defined names, or
        //  any interesting attributes, I think; at best, maybe a free-form comment?
        // could imagine wanting to bookmark nodes for later reference, but that
        //  would be a separate list

        // ...but for testing node selection, this is where we will put it:
        // nodeLabel = new JLabel("no selection");
        // add(nodeLabel);


        // at some point, we'll have our own sliceviewer menu; until then, attach those actions
        //  to buttons in plain view
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Menu proxy"));

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

        JButton testItem1Button = new JButton("Test item 1");
        testItem1Action.putValue(Action.NAME, "Test item 1");
        testItem1Action.putValue(Action.SHORT_DESCRIPTION, "Test item 1");
        testItem1Button.setAction(testItem1Action);
        add(testItem1Button);

        JButton testItem2Button = new JButton("Test item 2");
        testItem2Action.putValue(Action.NAME, "Test item 2");
        testItem2Action.putValue(Action.SHORT_DESCRIPTION, "Test item 2");
        testItem2Button.setAction(testItem2Action);
        add(testItem2Button);



        // the bilge...
        add(Box.createVerticalGlue());


    }


    private void setupNeuriteTreeNavigator() {
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Neurite structure"));

        // create test tree and style it; no icons, please
        neuriteTree = createTestTree();
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) neuriteTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);


        // listen for when the selection changes
        neuriteTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        neuriteTree.addTreeSelectionListener(this);



        // throw it into a scrolled panel; there's going to be a *lot* of them...
        JScrollPane treePane = new JScrollPane(neuriteTree);
        // if the tree knows how much to show, it'll tell the JScrollPane:
        neuriteTree.setVisibleRowCount(10);
        add(treePane);
    }

    private JTree createTestTree() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root node");
        neuriteTree = new JTree(root);


        // add some nodes
        root.add(new DefaultMutableTreeNode("link 1"));
        root.add(new DefaultMutableTreeNode("link 2"));

        DefaultMutableTreeNode branch1 = new DefaultMutableTreeNode("branch1");
        root.add(branch1);

        branch1.add(new DefaultMutableTreeNode("link 4"));        

        root.add(new DefaultMutableTreeNode("link 3"));        

        DefaultMutableTreeNode branch2 = new DefaultMutableTreeNode("branch2");
        root.add(branch2);


        return neuriteTree;

    }

    public void valueChanged(TreeSelectionEvent e) {
        // (from tutorial example)
        //Returns the last path element of the selection.
        //This method is useful only when the selection model allows a single selection.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) neuriteTree.getLastSelectedPathComponent();

        if (node == null) {
            //Nothing is selected.     
            nodeLabel.setText("no selection");
            return;
        }

        if (node.isLeaf()) {
            nodeLabel.setText(node.toString());
        } else {
            nodeLabel.setText(node.toString());
        }
    }

}



