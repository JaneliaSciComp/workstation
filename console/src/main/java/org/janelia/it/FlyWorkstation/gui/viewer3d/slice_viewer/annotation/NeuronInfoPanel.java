package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;



// workstation imports

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected neuron: name, type, etc.
 *
 * djo, 6/13
 */
public class NeuronInfoPanel extends JPanel 
{

    private JLabel neuronNameLabel;

    private JTree neuriteTree;
    private DefaultTreeModel neuriteModel;
    private DefaultMutableTreeNode neuronRootNode;


    public Slot1<TmNeuron> updateNeuronSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            updateNeuron(neuron);
        }
    };



    public NeuronInfoPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
        // neuron information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Neuron information panel"));

        neuronNameLabel = new JLabel("");
        add(neuronNameLabel);


        // neurite tree
        setupNeuriteTreeNavigator();


        // neurite information; show name, type (axon, dendrite, etc), other attributes
        //  of selected (current) neurite (not sure how this will be stored)


        updateNeuron(null);
    }

    private void setupNeuriteTreeNavigator() {
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Neurite structure"));

        // create test tree and style it; no icons, please
        neuronRootNode = new DefaultMutableTreeNode("invisible root node");
        neuriteModel = new DefaultTreeModel(neuronRootNode);
        neuriteTree = new JTree(neuriteModel);
        neuriteTree.setRootVisible(false);
        // neuriteTree = createTestTree();
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) neuriteTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);


        // listen for when the selection changes
        // neuriteTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // neuriteTree.addTreeSelectionListener(this);



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

    public void printNeuronInfo(TmNeuron neuron) {
        // try to extract some info and print it, for testing:
        if (neuron == null) {
            return;
        }

        System.out.println("loaded neuron " + neuron.getName());
        TmGeoAnnotation rootAnn = neuron.getRootAnnotation();
        if (rootAnn == null) {
            System.out.println("neuron has no root annotation");
        } else {
            double x = rootAnn.getX();
            double y = rootAnn.getY();
            double z = rootAnn.getZ();
            ArrayList<TmGeoAnnotation> children = new ArrayList<TmGeoAnnotation>(rootAnn.getChildren());
            // why not something like this?
            // List<TmGeoAnnotation> children = rootAnn.getChildren();
            int nChildren = children.size();
            System.out.println("root annotation at " + x + ", " + y + ", " + z);
            System.out.println("root annotation has " + nChildren + " children");
            if (nChildren > 0) {
                for (TmGeoAnnotation child: children) {
                    System.out.println("child at " + child.getX() + ", " + child.getY() + ", " + child.getZ());
                }
            }
        }        
    }

    public void updateNeuriteTree(TmNeuron neuron) {
        // for the short term, brute force it; recreate the tree every time; 
        //  I don't know if that's idiomatic or not

        neuronRootNode.removeAllChildren();

        // if neuron is not null, traverse it and populate nodes
        if (neuron != null) {

            TmGeoAnnotation rootAnnotation = neuron.getRootAnnotation();
            if (rootAnnotation != null) {

                // first node is the parent node of the neuron, which is the first child
                //  of the invisible root:
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(rootAnnotation);
                neuriteModel.insertNodeInto(child, neuronRootNode, neuronRootNode.getChildCount());

                // traverse tree


                neuriteModel.reload();
            }

        }


    }

    public void updateNeuron(TmNeuron neuron) {
        updateNeuronLabel(neuron);

        updateNeuriteTree(neuron);

        // testing
        printNeuronInfo(neuron);
    }

    public void updateNeuronLabel(TmNeuron neuron) {
        if (neuron == null) {
            neuronNameLabel.setText("(no neuron)");
        } else {
            neuronNameLabel.setText(neuron.getName());
        }
    }

}