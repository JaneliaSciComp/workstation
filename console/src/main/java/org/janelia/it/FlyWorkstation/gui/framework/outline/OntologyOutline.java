package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import sun.awt.VerticalBagLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
public class OntologyOutline extends JPanel implements ActionListener {
    private static String ADD_COMMAND       = "add";
    private static String REMOVE_COMMAND    = "remove";
    private static String ROOT_COMMAND      = "root";
    private static String SWITCH_COMMAND    = "switch";

    private JPanel treesPanel;
    private DynamicTree selectedTree;

    public OntologyOutline() {
        super(new BorderLayout());

        JButton addButton = new JButton("Add");
        addButton.setActionCommand(ADD_COMMAND);
        addButton.addActionListener(this);

        JButton removeButton = new JButton("Remove");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.addActionListener(this);

        JButton newButton = new JButton("New");
        newButton.setActionCommand(ROOT_COMMAND);
        newButton.addActionListener(this);

        JButton switchButton = new JButton("Switch");
        switchButton.setActionCommand(SWITCH_COMMAND);
        switchButton.addActionListener(this);

        // Lay everything out.
        treesPanel = new JPanel(new VerticalBagLayout());
        // Create the components.
        List<Entity> ontologyRootList = EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                EntityConstants.TYPE_ONTOLOGY_ROOT_ID);
        // For now, populate off the first tree
        if (null!=ontologyRootList && ontologyRootList.size()>=1) {
            initializeTree(ontologyRootList.get(0));
        }
        add(new JLabel("Ontology Editor"), BorderLayout.NORTH);
        add(treesPanel, BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(addButton);
        panel.add(removeButton);
        panel.add(switchButton);
        panel.add(newButton);
        add(panel, BorderLayout.SOUTH);
    }

    private DynamicTree initializeTree(Entity ontologyRoot) {
        DynamicTree newTreePanel = new DynamicTree(ontologyRoot);
        treesPanel.removeAll();
        treesPanel.add(newTreePanel);

        addNodes(newTreePanel, null, ontologyRoot);
        selectedTree = newTreePanel;
        newTreePanel.expandAll();
        this.updateUI();
        return newTreePanel;
    }

    private void addNodes(DynamicTree tree, EntityMutableTreeNode parentNode, Entity childEntity){
        EntityMutableTreeNode newNode;
        if (null!=parentNode) {
            newNode = tree.addObject(parentNode, childEntity);
        }
        else {
            // If the parent node is null, assume the parentNode userObject is the childEntity passed, and the parent is root
            newNode = tree.rootNode;
        }
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
                                "Adding to "+selectedTree.getCurrentNodeName(),
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                null,
                                null);

            if ((termName == null) || (termName.length() <= 0)) {
                return;
            }
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
            initializeTree(newOntologyRoot);
        }
        else if (SWITCH_COMMAND.equals(command)) {
            List<Entity> ontologyRootList = EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                    EntityConstants.TYPE_ONTOLOGY_ROOT_ID);
            ArrayList<String> ontologyNames = new ArrayList<String>();
            for (Entity entity : ontologyRootList) {
                ontologyNames.add(entity.getName());
            }
            String choice = (String)JOptionPane.showInputDialog(
                                this,
                                "Choose an ontology:\n",
                                "Ontology Chooser",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                ontologyNames.toArray(),
                                ontologyNames.get(0));

            if ((choice != null) && (choice.length() > 0)) {
                for (Entity ontologyEntity : ontologyRootList) {
                    if (ontologyEntity.getName().equals(choice)) {
                        initializeTree(ontologyEntity);
                        break;
                    }
                }
            }
        }
    }

    // todo This is toooooooo brute-force
    private void updateSelectedTreeEntity(){
        Entity entity= EJBFactory.getRemoteAnnotationBean().getUserEntityById(System.getenv("USER"), selectedTree.rootNode.getEntityId());
        if (null!=selectedTree || entity.getName().equals(selectedTree.rootNode.getEntityName())){
            initializeTree(entity);
        }
        this.updateUI();
    }

}
