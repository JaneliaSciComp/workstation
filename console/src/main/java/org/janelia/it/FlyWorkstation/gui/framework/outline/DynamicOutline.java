package org.janelia.it.FlyWorkstation.gui.framework.outline;

import sun.awt.VerticalBagLayout;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
public class DynamicOutline extends JPanel implements ActionListener, TreeSelectionListener {
    private int newNodeSuffix = 1;
    private static String ADD_COMMAND = "add";
    private static String REMOVE_COMMAND = "remove";
    private static String ROOT_COMMAND = "root";

    private JPanel treesPanel;
    private HashMap<DefaultMutableTreeNode, DynamicTree> treeMap = new HashMap<DefaultMutableTreeNode, DynamicTree>();
    private DynamicTree selectedTree;

    public DynamicOutline() {
        super(new BorderLayout());

        // Create the components.
        DynamicTree treePanel = new DynamicTree();
//        treePanel.setPreferredSize(new Dimension(300, 150));
        treePanel.tree.addTreeSelectionListener(this);
        treeMap.put(treePanel.rootNode, treePanel);
        for (DynamicTree dynamicTree : treeMap.values()) {
            populateTrees(dynamicTree);
        }

        JButton addButton = new JButton("Add");
        addButton.setActionCommand(ADD_COMMAND);
        addButton.addActionListener(this);

        JButton removeButton = new JButton("Remove");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.addActionListener(this);

        JButton clearButton = new JButton("New");
        clearButton.setActionCommand(ROOT_COMMAND);
        clearButton.addActionListener(this);

        // Lay everything out.
        treesPanel = new JPanel(new VerticalBagLayout());
        treesPanel.add(treePanel);
        selectedTree = treePanel;
        JScrollPane treeScrollPane = new JScrollPane(treesPanel);
        treeScrollPane.createVerticalScrollBar().setVisible(true);
        add(new JLabel("Ontology Editor"), BorderLayout.NORTH);
        add(treeScrollPane, BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.add(addButton);
        panel.add(removeButton);
        panel.add(clearButton);
        add(panel, BorderLayout.SOUTH);
    }

    public void populateTrees(DynamicTree dynamicTree) {
        String p1Name = new String("Parent 1");
        String p2Name = new String("Parent 2");
        String c1Name = new String("Child 1");
        String c2Name = new String("Child 2");

        DefaultMutableTreeNode p1, p2;

        p1 = dynamicTree.addObject(null, p1Name);
        p2 = dynamicTree.addObject(null, p2Name);

        dynamicTree.addObject(p1, c1Name);
        dynamicTree.addObject(p1, c2Name);

        dynamicTree.addObject(p2, c1Name);
        dynamicTree.addObject(p2, c2Name);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (ADD_COMMAND.equals(command)) {
            // Add button clicked
            selectedTree.addObject("New Node " + newNodeSuffix++);
        }
        else if (REMOVE_COMMAND.equals(command)) {
            // Remove button clicked
            selectedTree.removeCurrentNode();
        }
        else if (ROOT_COMMAND.equals(command)) {
            // New Root button clicked.
            DynamicTree newTreePanel = new DynamicTree();
            newTreePanel.tree.addTreeSelectionListener(this);
            populateTrees(newTreePanel);
            treeMap.put(newTreePanel.rootNode, newTreePanel);
            treesPanel.add(newTreePanel);
            this.updateUI();
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeSelectionEvent.getPath().getPathComponent(0);
        selectedTree = treeMap.get(rootNode);
    }
}
