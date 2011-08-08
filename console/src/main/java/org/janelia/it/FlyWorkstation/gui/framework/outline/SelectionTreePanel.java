package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A panel containing a tree which supports adding and removing items at its top level.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SelectionTreePanel<T> extends JPanel implements ActionListener {

    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "remove";

    private JLabel countLabel;
    private DynamicTree tree;
    private JPanel treePanel;

    public SelectionTreePanel(String title) {
        super(new BorderLayout());

        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10), BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createEmptyBorder(10, 10, 0, 10)), title)));

        treePanel = new JPanel(new BorderLayout());

        add(treePanel, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBackground(new Color(0.8f, 0.8f, 0.8f));

        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));

        countLabel = new JLabel();
        buttonPane.add(countLabel);

        buttonPane.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add");
        addButton.setActionCommand(ADD_COMMAND);
        addButton.addActionListener(this);
        buttonPane.add(addButton);

        JButton removeButton = new JButton("Remove");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.addActionListener(this);
        buttonPane.add(removeButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    private void updateCount() {
        DefaultMutableTreeNode node = getDynamicTree().getRootNode();
        countLabel.setText(node.getChildCount() + " objects");
    }

    public DynamicTree getDynamicTree() {
        return tree;
    }

    /**
     * Add an item at the top level of the tree.
     *
     * @param object
     */
    public DefaultMutableTreeNode addItem(T object) {
        DefaultMutableTreeNode node = getDynamicTree().addObject(getDynamicTree().getRootNode(), object);
        updateCount();
        return node;
    }

    /**
     * Adds the given item if it is not in the list already.
     *
     * @param object
     */
    public DefaultMutableTreeNode addItemUniquely(T object) {
        if (!containsItem(object)) {
            return addItem(object);
        }
        return null;
    }

    /**
     * Return the currently selected items.
     *
     * @return
     */
    public List<T> getItems() {

        List<T> items = new ArrayList<T>();
        DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();

        for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            items.add((T) childNode.getUserObject());
        }

        return items;
    }

    /**
     * Returns true if the object is already in the item list.
     *
     * @param entity
     * @return
     */
    public boolean containsItem(T obj) {

        DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();
        for (Enumeration e = rootNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            if (childNode.getUserObject().equals(obj)) {
                return true;
            }
        }

        return false;
    }

    public void createNewTree() {
        tree = new DynamicTree("ROOT", false, false);
        tree.getTree().setRootVisible(false);
        tree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        treePanel.removeAll();
        treePanel.add(tree);
        updateCount();
    }

    /**
     * Override this method to do something when the user clicks the "Add" button.
     */
    public void addClicked() {
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (REMOVE_COMMAND.equals(command)) {
            try {
                Utils.setWaitingCursor(SelectionTreePanel.this);
                TreePath[] paths = tree.getTree().getSelectionPaths();
                if (paths == null) return;
                for (TreePath path : paths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    tree.removeNode(node);
                }
                updateCount();
                SwingUtilities.updateComponentTreeUI(this);
            }
            finally {
                Utils.setDefaultCursor(SelectionTreePanel.this);
            }
        }
        else if (ADD_COMMAND.equals(command)) {
            addClicked();
        }
    }
}