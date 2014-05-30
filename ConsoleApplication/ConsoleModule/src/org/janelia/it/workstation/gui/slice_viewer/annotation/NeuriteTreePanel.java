package org.janelia.it.workstation.gui.slice_viewer.annotation;


import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot1;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import com.google.common.collect.HashBiMap;


/**
 * this panel shows info on the selected neuron: name, type, etc.
 *
 * djo, 6/13
 */
public class NeuriteTreePanel extends JPanel
{
    private JTree neuriteTree;
    private DefaultTreeModel neuriteModel;
    private DefaultMutableTreeNode neuronRootNode;
    private HashBiMap<String, TmGeoAnnotation> labelToAnnotationMap;

    private int width;
    private static final int height = 250;

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


    public NeuriteTreePanel(int width) {
        this.width = width;
        setupUI();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    private void setupUI() {
        setLayout(new GridBagLayout());
    
        // neuron information; show name, whatever attributes here,
        //  but for now, we don't have anything


        // neurite tree
        setupNeuriteTreeNavigator();

        // neurite information; show name, type (axon, dendrite, etc), other attributes
        //  of selected (current) neurite (not sure how this will be stored)
        loadNeuron(null);
    }

    private void setupNeuriteTreeNavigator() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        c.weightx = 1.0;
        c.weighty = 0.0;
        add(new JLabel("Neurites", JLabel.LEADING), c);

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

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = GridBagConstraints.RELATIVE;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.fill = GridBagConstraints.BOTH;
        c2.weighty = 1.0;
        add(treePane, c2);
    }

    public void printNeuronInfo(TmNeuron neuron) {
        // print neuron info, for testing:
        if (neuron == null) {
            return;
        }

        System.out.println(String.format("loaded neuron %s with ID %d", neuron.getName(), neuron.getId()));
        List<TmGeoAnnotation> roots = neuron.getRootAnnotations();
        if (roots.size() == 0) {
            System.out.println("neuron has no root annotation");
        } else {
            for (TmGeoAnnotation r: roots) {
                System.out.println(String.format("root annotation %d at %.1f, %.1f, %.1f",
                        r.getId(), r.getX(), r.getY(), r.getZ()));

                for (TmGeoAnnotation a: r.getSubTreeList()) {
                    TmGeoAnnotation parent = a.getParent();
                    Long parentID;
                    if (parent == null) {
                        parentID = neuron.getId();
                    } else {
                        parentID = parent.getId();
                    }
                    System.out.println(String.format("annotation %d with parent %d at %.1f, %.1f, %.1f",
                            a.getId(), parentID, a.getX(), a.getY(), a.getZ()));

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
        loadNeuriteTreeTagged(neuron);

        // testing
        // printNeuronInfo(neuron);
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