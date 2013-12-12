package org.janelia.it.FlyWorkstation.gui.slice_viewer.annotation;


import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot1;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import com.google.common.collect.HashBiMap;


/**
 * this panel shows info on the selected neuron: name, type, etc.
 *
 * djo, 6/13
 */
public class NeuriteTreePanel extends JPanel
{
    private JLabel neuronNameLabel;

    private JTree neuriteTree;
    private DefaultTreeModel neuriteModel;
    private DefaultMutableTreeNode neuronRootNode;
    private HashBiMap<String, TmGeoAnnotation> labelToAnnotationMap;

    // ----- slots
    public Slot1<TmNeuron> neuronSelectedSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            loadNeuron(neuron);
        }
    };

    // ----- signals
    public Signal1<Vec3> cameraPanToSignal = new Signal1<Vec3>();
    public Signal1<TmGeoAnnotation> annotationClickedSignal = new Signal1<TmGeoAnnotation>();


    public NeuriteTreePanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
        // neuron information; show name, whatever attributes
        // hide until we have something to show besides the name (which is
        //  visible in the list above)!

        neuronNameLabel = new JLabel("");

        // neurite tree
        setupNeuriteTreeNavigator();

        // neurite information; show name, type (axon, dendrite, etc), other attributes
        //  of selected (current) neurite (not sure how this will be stored)
        loadNeuron(null);
    }

    private void setupNeuriteTreeNavigator() {
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(new JLabel("Neurites", JLabel.CENTER));

        // create test tree and style it; no icons, please
        neuronRootNode = new DefaultMutableTreeNode("invisible root node");
        neuriteModel = new DefaultTreeModel(neuronRootNode);
        neuriteTree = new JTree(neuriteModel);
        labelToAnnotationMap = HashBiMap.create();

        neuriteTree.setRootVisible(false);
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) neuriteTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);

        // don't expand/collapse on double-click
        neuriteTree.setToggleClickCount(0);

        // listen for mouse clicks
        MouseListener treeListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                TreePath path = neuriteTree.getPathForLocation(event.getX(), event.getY());
                if (path != null) {
                    if (event.getClickCount() == 1) {
                        onAnnotationSingleClicked(path);
                    } else if (event.getClickCount() == 2) {
                        onAnnotationDoubleClicked(path);
                    }
                }
            }
        };
        neuriteTree.addMouseListener(treeListener);

        // throw it into a scrolled panel; there's going to be a *lot* of them...
        JScrollPane treePane = new JScrollPane(neuriteTree);
        // if the tree knows how much to show, it'll tell the JScrollPane:
        neuriteTree.setVisibleRowCount(10);
        add(treePane);
    }

    public void printNeuronInfo(TmNeuron neuron) {
        // print neuron info, for testing:
        if (neuron == null) {
            return;
        }

        System.out.println("loaded neuron " + neuron.getName());
        List<TmGeoAnnotation> roots = neuron.getRootAnnotations();
        if (roots.size() == 0) {
            System.out.println("neuron has no root annotation");
        } else {
            for (TmGeoAnnotation r: roots) {
                int nChildren = r.getChildren().size();
                System.out.println(String.format("root annotation at %.1f, %.1f, %.1f has %d children",
                        r.getX(), r.getY(), r.getZ(), nChildren));
                if (nChildren > 0) {
                    for (TmGeoAnnotation child: r.getChildren()) {
                        System.out.println("child at " + child.getX() + ", " + child.getY() + ", " + child.getZ());
                    }
                }
            }
        }        
    }

    public void loadNeuriteTreeTagged(TmNeuron neuron) {
        // each neurite will be a root node, then a list of other branch and end nodes

        // looks like I need to put text into tree, and keep a map to the actual
        //  geoanns; why?  because the text depends on neuron-level knowledge, which
        //  each geoann doesn't know about itself (so can't use .toString()); I
        //  could probably override the TreeNode class to do that, but I'm not
        //  going to mess with that for now
        // use Guava BiMap for this? will want both mappings for selection management purposes

        // brute force recreate for now
        neuronRootNode.removeAllChildren();
        labelToAnnotationMap.clear();

        if (neuron != null) {
            List<TmGeoAnnotation> rootList = neuron.getRootAnnotations();
            sortGeoAnnotationList(rootList);
            for (TmGeoAnnotation root: rootList) {
                // first node is the parent node of the neuron, which is the first child
                //  of the invisible root:
                String label = getTreeString(root);
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(label);
                neuriteModel.insertNodeInto(rootNode, neuronRootNode, neuronRootNode.getChildCount());
                labelToAnnotationMap.put(label, root);

                // build tree;
                populateNeuriteTreeNodeTagged(root, rootNode);
            }
        }
        neuriteModel.reload();

        // Java doesn't give you a JTree.expandAll() method!  this is the idiom:
        for (int i = 0; i < neuriteTree.getRowCount(); i++) {
            neuriteTree.expandRow(i);
        }
    }

    private void populateNeuriteTreeNodeTagged(TmGeoAnnotation parentAnnotation, DefaultMutableTreeNode rootNode) {
        // recurse through nodes; note that everything is a child of the rootNode!
        List<TmGeoAnnotation> childList = parentAnnotation.getChildren();
        sortGeoAnnotationList(childList);
        for (TmGeoAnnotation childAnnotation: childList) {
            if (!getNodeType(childAnnotation).equals("node")) {
                String label = getTreeString(childAnnotation);
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(label);
                neuriteModel.insertNodeInto(childNode, rootNode, rootNode.getChildCount());
                labelToAnnotationMap.put(label, childAnnotation);
            }
            populateNeuriteTreeNodeTagged(childAnnotation, rootNode);
        }
    }

    private TmGeoAnnotation getAnnotationAtPath(TreePath path) {
        // is this idiomatic? the double casting kind of makes me feel ill
        String label = (String) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return labelToAnnotationMap.get(label);
    }

    private String getNodeType(TmGeoAnnotation annotation) {
        if (annotation.getParent() == null) {
            return "root";
        } else {
            int nChildren = annotation.getChildren().size();
            if (nChildren == 0) {
                return "end";
            } else if (nChildren == 1) {
                return "node";
            } else {
                return "fork";
            }
        }
    }

    private String getTreeString(TmGeoAnnotation annotation) {
        return getNodeType(annotation) + ": " + annotation.toString();
    }

    public void loadNeuron(TmNeuron neuron) {
        updateNeuronLabel(neuron);

        loadNeuriteTreeTagged(neuron);

        // testing
        // printNeuronInfo(neuron);
    }

    public void updateNeuronLabel(TmNeuron neuron) {
        if (neuron == null) {
            neuronNameLabel.setText("(no neuron)");
        } else {
            neuronNameLabel.setText(neuron.getName());
        }
    }

    private void onAnnotationSingleClicked(TreePath path) {
        // select annotation at path
        annotationClickedSignal.emit(getAnnotationAtPath(path));
    }

    private void onAnnotationDoubleClicked(TreePath path) {
        // go to annotation at path
        TmGeoAnnotation annotation = getAnnotationAtPath(path);

        // emit signal
        cameraPanToSignal.emit(new Vec3(annotation.getX(), annotation.getY(), annotation.getZ()));
    }

    private void sortGeoAnnotationList(List<TmGeoAnnotation> annotationList) {
        Collections.sort(annotationList, new Comparator<TmGeoAnnotation>() {
            @Override
            public int compare(TmGeoAnnotation annotation, TmGeoAnnotation annotation2) {
                return annotation.getId().compareTo(annotation2.getId());
            }
        });
    }
}