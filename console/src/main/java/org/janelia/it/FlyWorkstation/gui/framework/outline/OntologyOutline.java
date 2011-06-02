package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
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
public class OntologyOutline extends JPanel implements ActionListener, TreeSelectionListener {
    private static String ADD_COMMAND = "add";
    private static String REMOVE_COMMAND = "remove";
    private static String ROOT_COMMAND = "root";

    private JPanel treesPanel;
    private HashMap<DefaultMutableTreeNode, DynamicTree> treeMap = new HashMap<DefaultMutableTreeNode, DynamicTree>();
    private DynamicTree selectedTree;

    public OntologyOutline() {
        super(new BorderLayout());

        // Create the components.
        DynamicTree treePanel = new DynamicTree(null);
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
            String termName = (String)JOptionPane.showInputDialog(
                                this,
                                "Ontology Term:\n",
                                "New Ontology Term",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                null,
                                null);

            if ((termName == null) || (termName.length() <= 0)) {
                JOptionPane.showMessageDialog(this, "Require a valid term", "Ontology Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectedTree.addObject(termName);
            EJBFactory.getRemoteAnnotationBean().createOntologyTerm(System.getenv("USER"), selectedTree.rootNode.toString(),
                    termName);
        }
        else if (REMOVE_COMMAND.equals(command)) {
            // Remove button clicked
            int deleteConfirmation = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this term?",
                "Delete Term",
                JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation!=0) {
                return;
            }
            selectedTree.removeCurrentNode();
            EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(System.getenv("USER"), selectedTree.getCurrentNodeName());
        }
        else if (ROOT_COMMAND.equals(command)) {
            // New Root button clicked.
            String rootName = (String)JOptionPane.showInputDialog(
                                this,
                                "Ontology Root Name:\n",
                                "New Ontology",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                null,
                                null);

            if ((rootName == null) || (rootName.length() <= 0)) {
                JOptionPane.showMessageDialog(this, "Require a valid name", "Ontology Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            DynamicTree newTreePanel = new DynamicTree(rootName);
            newTreePanel.tree.addTreeSelectionListener(this);
//            populateTrees(newTreePanel);
            treeMap.put(newTreePanel.rootNode, newTreePanel);
            treesPanel.add(newTreePanel);
            this.updateUI();
            EJBFactory.getRemoteAnnotationBean().createOntologyRoot(System.getenv("USER"), rootName);
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeSelectionEvent.getPath().getPathComponent(0);
        selectedTree = treeMap.get(rootNode);
    }
}
