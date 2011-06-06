package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import sun.awt.VerticalBagLayout;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
public class OntologyOutline extends JPanel implements ActionListener, TreeSelectionListener {
    private static String ADD_COMMAND       = "add";
    private static String REMOVE_COMMAND    = "remove";
    private static String ROOT_COMMAND      = "root";

    private JPanel treesPanel;
    private HashMap<Long, DynamicTree> treeMap = new HashMap<Long, DynamicTree>();
    private DynamicTree selectedTree;

    public OntologyOutline() {
        super(new BorderLayout());

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
        // Create the components.
        List<Entity> ontologyRootList = EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                EntityConstants.TYPE_ONTOLOGY_ROOT_ID);
        for (Entity entity : ontologyRootList) {
            DynamicTree treePanel = createTree(entity);
//            populateTree(treePanel, entity);
        }
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

    private DynamicTree createTree(Entity ontologyRoot) {
        DynamicTree treePanel = new DynamicTree(ontologyRoot);
        treePanel.tree.addTreeSelectionListener(this);
        treesPanel.add(treePanel);
        treeMap.put(ontologyRoot.getId(), treePanel);
        this.updateUI();
        return treePanel;
    }

    public DynamicTree populateTree(DynamicTree treePanel, Entity ontologyRoot) {
        addNodes(treePanel, null, ontologyRoot);
        return treePanel;
    }

    private void addNodes(DynamicTree tree, EntityMutableTreeNode parentNode, Entity childEntity){
        EntityMutableTreeNode newNode = tree.addObject(parentNode, childEntity);
        for (EntityData tmpData : childEntity.getEntityData()) {
            addNodes(tree, newNode, tmpData.getChildEntity());
        }
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
//            selectedTree.addObject(termName);
            EJBFactory.getRemoteAnnotationBean().createOntologyTerm(System.getenv("USER"), selectedTree.getCurrentNodeId(),
                    termName);
            updateSelectedTreeEntity();
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
            EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(System.getenv("USER"), selectedTree.getCurrentNodeId());
            updateSelectedTreeEntity();
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

            Entity newOntologyRoot = EJBFactory.getRemoteAnnotationBean().createOntologyRoot(System.getenv("USER"), rootName);
            createTree(newOntologyRoot);
        }
    }

    // todo This is toooooooo brute-force
    private void updateSelectedTreeEntity(){
        Entity entity= EJBFactory.getRemoteAnnotationBean().getUserEntityById(System.getenv("USER"), ((Entity) selectedTree.rootNode.getUserObject()).getId());
        if (null!=selectedTree || entity.getName().equals(((Entity)selectedTree.rootNode.getUserObject()).getName())){
            selectedTree.removeAll();
            addNodes(selectedTree, null, entity);
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
        EntityMutableTreeNode rootNode = (EntityMutableTreeNode) treeSelectionEvent.getPath().getPathComponent(0);
        selectedTree = treeMap.get(rootNode.getEntityId());
        for (DynamicTree dynamicTree : treeMap.values()) {
            if (selectedTree!=dynamicTree) {
                dynamicTree.tree.getSelectionModel().clearSelection();
            }
        }
    }
}
