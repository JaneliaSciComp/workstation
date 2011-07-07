/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.AnnotateAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.NavigateToNodeAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OntologyTermAction;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.*;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.*;
import org.janelia.it.jacs.model.ontology.Enum;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;


/**
 * An ontology editor panel.
 * 
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyOutline extends JPanel implements ActionListener, KeybindChangeListener, DataAvailabilityListener {
	
	private static final String KEYBIND_PREF_CATEGORY = "Keybind";
	
    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "remove";
    private static final String SHOW_MANAGER_COMMAND = "manager";
    private static final String BIND_EDIT_COMMAND = "change_bind";
    private static final String BIND_MODE_COMMAND = "bind_mode";
    private static final String DELIMITER = "#";

    private Map<String,KeyboardShortcut> entityId2Shortcut = new HashMap<String,KeyboardShortcut>();
    
    private JPanel treesPanel;
    private DynamicTree selectedTree;
    private KeyBindFrame keyBindDialog;
    private JToggleButton keyBindButton;
    private OntologyManager ontologyManager;
    
    private final JPopupMenu popupMenu;
    private final JMenuItem assignShortcutMenuItem;
    private final JMenuItem removeNodeMenuItem;
    private final JMenu addMenuPopup;
    private final JMenu addItemPopup;

    private List<Class<? extends OntologyTermType>> nodeTypes = new ArrayList<Class<? extends OntologyTermType>>();
    private MouseListener mouseListener;


    public OntologyOutline() {
        super(new BorderLayout());

        nodeTypes.add(Category.class);
        nodeTypes.add(Tag.class);
        nodeTypes.add(Enum.class);
        nodeTypes.add(Interval.class);
        nodeTypes.add(Text.class);

        // Create the components

        JButton manageButton = new JButton("Ontology Manager");
        manageButton.setActionCommand(SHOW_MANAGER_COMMAND);
        manageButton.addActionListener(this);

        keyBindButton = new JToggleButton("Set Shortcuts");
        keyBindButton.setActionCommand(BIND_MODE_COMMAND);
        keyBindButton.addActionListener(this);

        // Create context menus

        popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        assignShortcutMenuItem = new JMenuItem("Assign shortcut...");
        assignShortcutMenuItem.addActionListener(this);
        assignShortcutMenuItem.setActionCommand(BIND_EDIT_COMMAND);

        addMenuPopup = new JMenu("Add...");
        for(Class<? extends OntologyTermType> nodeType : nodeTypes) {
			try {
				JMenuItem smi = new JMenuItem(nodeType.newInstance().getName());
				smi.addActionListener(this);
				smi.setActionCommand(ADD_COMMAND + DELIMITER + nodeType.getSimpleName());
				addMenuPopup.add(smi);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        // Prepare an alternative submenu for enumeration nodes
        addItemPopup = new JMenu("Add...");
        JMenuItem smi = new JMenuItem("Item");
        smi.addActionListener(this);
        smi.setActionCommand(ADD_COMMAND+DELIMITER+EnumItem.class.getSimpleName());
        addItemPopup.add(smi);

        removeNodeMenuItem = new JMenuItem("Remove this node");
        removeNodeMenuItem.addActionListener(this);
        removeNodeMenuItem.setActionCommand(REMOVE_COMMAND);

        mouseListener = new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                JTree tree = selectedTree.getTree();
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    tree.setSelectionRow(row);
                    if (e.isPopupTrigger()) {
                        showPopupMenu(e);
                    }
                    // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                    else if (e.getClickCount()==2 
                    		&& e.getButton()==MouseEvent.BUTTON1 
                    		&& (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        OntologyTerm currTerm = getOntologyTermFromTreeNode(selectedTree.getCurrentNode());
                        if (!(currTerm.getAction() instanceof NavigateToNodeAction))
                            currTerm.getAction().doAction();
                    }
                }
            }
            public void mousePressed(MouseEvent e) {
                JTree tree = selectedTree.getTree();
                // We have to also listen for mousePressed because OSX generates the popup trigger here
                // instead of mouseReleased like any sane OS.
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    tree.setSelectionRow(row);
                    if (e.isPopupTrigger()) {
                        showPopupMenu(e);
                    }
                }
            }
        };

        // Lay everything out

        add(new JLabel("Ontology Editor"), BorderLayout.NORTH);
        
        this.treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        JPanel panel = new JPanel(new GridBagLayout());

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(manageButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(keyBindButton, c);
        
        add(panel, BorderLayout.SOUTH);
        
        // Add ourselves as a keybind change listener
        
        ConsoleApp.getKeyBindings().addChangeListener(this);

        // Prepare the key binding dialog box

        this.keyBindDialog = new KeyBindFrame();
        keyBindDialog.pack();

        keyBindDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                // refresh the tree in case the key bindings were updated
                DefaultTreeModel treeModel = (DefaultTreeModel)selectedTree.getTree().getModel();
                treeModel.nodeChanged(selectedTree.getCurrentNode());
            }
		});

		// Prepare the ontology manager and start preloading ontologies

		this.ontologyManager = new OntologyManager(this);
		ontologyManager.pack();
		ontologyManager.getPrivateTable().addDataListener(this);
		ontologyManager.preload();

		// Show a loading spinner until the data is loaded

		treesPanel.add(new JLabel(Icons.loadingIcon));

	}
    
    
    public OntologyTerm getCurrentOntology() {
    	if (selectedTree == null) return null;
    	return getOntologyTermFromTreeNode(selectedTree.getRootNode());
    }
    
    DynamicTree initializeTree(Entity ontologyRoot) {

    	if (ontologyRoot == null) {
            treesPanel.removeAll();
            this.updateUI();
            return null;
    	}
    	
    	// Load preferences for this ontology first, so that keys can be bound during tree loading
    	
    	loadKeyBindPrefs(ontologyRoot);
    	
    	// Clear the key bindings
    	
    	ConsoleApp.getKeyBindings().clearBindings();
    	
        // Create a new tree and add all the nodes to it

        OntologyTerm rootAE = new OntologyTerm(ontologyRoot,null);
        rootAE.setAction(new NavigateToNodeAction(rootAE));

        selectedTree = new DynamicTree(new OntologyTreeCellRenderer(), rootAE);
        addNodes(selectedTree, null, rootAE);
        selectedTree.expandAll(true);

        // Replace the default key listener on the tree
        
        final JTree tree = selectedTree.getTree();
        KeyListener defaultKeyListener = tree.getKeyListeners()[0];
        tree.removeKeyListener(defaultKeyListener);
        tree.addKeyListener(keyListener);

        // Set the mouse listener which keeps track of doubleclicks on nodes, and rightclicks to show the context menu

        tree.addMouseListener(mouseListener);

        // Replace the tree in the panel

        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        this.updateUI();
        return selectedTree;
    }

    private OntologyTerm getOntologyTermFromTreeNode(DefaultMutableTreeNode targetNode) {
        return (OntologyTerm)targetNode.getUserObject();
    }

    private String getEntityNameFromTreeNode(DefaultMutableTreeNode targetNode) {
        return ((OntologyTerm)targetNode.getUserObject()).getName();
    }

    private Long getEntityIdFromTreeNode(DefaultMutableTreeNode targetNode) {
        return ((OntologyTerm)targetNode.getUserObject()).getId();
    }

    public void navigateToOntologyTerm(OntologyTerm term) {
        selectedTree.navigateToNodeWithObject(term);
    }

    /**
     * Load the key binding preferences for a given ontology. This loads preferences into a local map which is then used
     * to populate the KeyBindings object during tree load.
     * @param ontologyRoot
     */
    private void loadKeyBindPrefs(Entity ontologyRoot) {
    	
    	entityId2Shortcut.clear();

    	try {
        	User user = EJBFactory.getRemoteComputeBean().getUserByName(System.getenv("USER"));
        	Map<String,UserPreference> prefs = user.getCategoryPreferences(KEYBIND_PREF_CATEGORY+":"+ontologyRoot.getId());
        	
        	for(UserPreference pref : prefs.values()) {
        		entityId2Shortcut.put(pref.getValue(), KeyboardShortcut.fromString(pref.getName()));
        	}
    	}
    	catch (Exception e) {
    		System.out.println("Could not load user's key binding preferences");
    		e.printStackTrace();
    	}
    }
    
    /**
     * Save the key binds for the current ontology.
     */
    private void saveKeyBinds() {

    	try {
        	User user = EJBFactory.getRemoteComputeBean().getUserByName(System.getenv("USER"));
        	Long rootNodeId = getOntologyTermFromTreeNode(selectedTree.getRootNode()).getEntity().getId();

        	// Delete all keybinds first, to maintain one key per entity
        	for(String key : user.getCategoryPreferences(KEYBIND_PREF_CATEGORY+":"+rootNodeId).keySet()) {
        		user.getPreferenceMap().remove(KEYBIND_PREF_CATEGORY+":"+rootNodeId+":"+key);
        	}
        	        	
            for(Map.Entry<KeyboardShortcut, Action> entry : ConsoleApp.getKeyBindings().getBindings().entrySet()) {
                if (entry.getValue() instanceof OntologyTermAction) {
                	OntologyTermAction ota = (OntologyTermAction)entry.getValue();
                	String shortcut = entry.getKey().toString();
                	Long entityId = ota.getOntologyTerm().getEntity().getId();
                	user.setPreference(new UserPreference(shortcut, KEYBIND_PREF_CATEGORY+":"+rootNodeId, entityId.toString()));  
                }
            }

            EJBFactory.getRemoteComputeBean().genericSave(user);
    	}
    	catch (Exception e) {
    		System.out.println("Could not save user's key binding preferences");
    		e.printStackTrace();
    	}
    }
    
    /**
     * Remove all key binds referencing the given ontology.
     */
    public void removeKeyBinds(Entity root) {

    	try {
            EJBFactory.getRemoteComputeBean().removePreferenceCategory(KEYBIND_PREF_CATEGORY+":"+root.getId());
    	}
    	catch (Exception e) {
    		System.out.println("Could not delete defunct key binding preferences");
    		e.printStackTrace();
    	}
    }
    
    private void addNodes(DynamicTree tree, DefaultMutableTreeNode parentNode, OntologyTerm node) {
    	
    	// Check if there is a keyboard shortcut preference for this entity
    	KeyboardShortcut shortcut = entityId2Shortcut.get(node.getEntity().getId().toString());
    	if (shortcut != null) {
    		// Disable listener so that we don't save the prefs for every pref we load
    		ConsoleApp.getKeyBindings().removeChangeListener(this);
        	ConsoleApp.getKeyBindings().setBinding(shortcut, node.getAction());
        	ConsoleApp.getKeyBindings().addChangeListener(this);
    	}
    	
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = tree.addObject(parentNode, node);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = tree.rootNode;
        }
        Entity entity = node.getEntity();
        if (entity.getEntityData() != null) {
            for (EntityData tmpData : entity.getOrderedEntityData()) {
                Entity childEntity = tmpData.getChildEntity();
                if (childEntity != null) {
                    OntologyTerm ontologyTerm = new OntologyTerm(childEntity,node);
                    OntologyTermType type = ontologyTerm.getType();
                    if (type instanceof Category || type instanceof Enum) {
                        // These node types don't allow direct annotation actions, just navigate to them
                        ontologyTerm.setAction(new NavigateToNodeAction(ontologyTerm));
                    }
                    else {
                        // The other node types are annotatable
                        ontologyTerm.setAction(new AnnotateAction(ontologyTerm));
                    }

                    addNodes(tree, newNode, ontologyTerm);
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

		if (REMOVE_COMMAND.equals(command)) {
			int deleteConfirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this term?",
					"Delete Term", JOptionPane.YES_NO_OPTION);
			if (deleteConfirmation != 0) {
				return;
			}
			String nodeId = getEntityIdFromTreeNode(selectedTree.getCurrentNode()).toString();
            EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(System.getenv("USER"), nodeId);
            updateSelectedTreeEntity();
        }
        else if (SHOW_MANAGER_COMMAND.equals(command)) {
        	ontologyManager.showDialog();
        }
        else if (BIND_EDIT_COMMAND.equals(command)) {
            DefaultMutableTreeNode treeNode = selectedTree.getCurrentNode();
            if (treeNode != null) {
                OntologyTerm ae = getOntologyTermFromTreeNode(treeNode);
                if (ae != null)
                    keyBindDialog.showForAction(ae.getAction());
            }
        }
        else if (BIND_MODE_COMMAND.equals(command)) {
        	// Transfer focus to a node in the tree in preparation for key presses
        	selectedTree.getTree().grabFocus();
        	if (selectedTree.getCurrentNode() == null) {
        		selectedTree.setCurrentNode(selectedTree.getRootNode());
        	}
        	
        }
        else if (command.startsWith(ADD_COMMAND)) {

            if (selectedTree == null) {
                JOptionPane.showMessageDialog(this, "No ontology selected.");
            }

            DefaultMutableTreeNode treeNode = selectedTree.getCurrentNode();
            OntologyTerm actionEntity = getOntologyTermFromTreeNode(treeNode);
            OntologyTermType parentType = actionEntity.getType();

            String className = command.split(DELIMITER)[1];
            OntologyTermType childType = OntologyTermType.createTypeByName(className);
            
            // Add button clicked
            String termName = (String)JOptionPane.showInputDialog(
                    this,
                    "Ontology Term:\n",
                    "Adding to "+getEntityNameFromTreeNode(selectedTree.getCurrentNode()),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);

            if ((termName == null) || (termName.length() <= 0)) {
                return;
            }

            if (childType instanceof Interval) {

                String lowerBoundStr = (String) JOptionPane.showInputDialog(
                        this,
                        "Lower bound:\n",
                        "Adding an interval",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null);

                String upperBoundStr = (String) JOptionPane.showInputDialog(
                        this,
                        "Upper bound:\n",
                        "Adding an interval",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null);

                try {
                    ((Interval)childType).init(lowerBoundStr, upperBoundStr);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Invalid bounds");
                    return;
                }
            }

            EJBFactory.getRemoteAnnotationBean().createOntologyTerm(System.getenv("USER"), getEntityIdFromTreeNode(selectedTree.getCurrentNode()).toString(),
                    termName, childType, null);
            
            
            if (parentType instanceof Tag) {
            	// Adding a child to a Tag, so it must be coerced into a Category
            	
            	EntityData ed = actionEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
            	ed.setValue(Category.class.getSimpleName());
            	
            	try {
					EJBFactory.getRemoteComputeBean().genericSave(ed);
				} catch (DaoException ex) {
					ex.printStackTrace();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
            }
            
            updateSelectedTreeEntity();
        }
    }
    
    /**
     * @return true if the user is allowed to edit the current ontology, false otherwise.
     */
    public boolean isEditable() {
    	OntologyTerm rootTerm = getOntologyTermFromTreeNode(selectedTree.getRootNode());
    	return !rootTerm.isPublic();
    }

    private void showPopupMenu(MouseEvent e) {

        popupMenu.removeAll();
        popupMenu.add(assignShortcutMenuItem);
        
        if (isEditable()) {

            OntologyTerm curr = getOntologyTermFromTreeNode(selectedTree.getCurrentNode());
            OntologyTermType type = curr.getType();
            
            if (type instanceof Enum) {
            	popupMenu.add(addItemPopup);
            }
            else if (type.allowsChildren() || type instanceof Tag) {
            	popupMenu.add(addMenuPopup); 
            }

            // Disallow deletion of root nodes. You've gotta use the OntologyManager for that.
            if (curr.getParentTerm() != null) {
                popupMenu.add(removeNodeMenuItem);
            }
        }
        
        popupMenu.show((JComponent)e.getSource(), e.getX(), e.getY());
    }
    
    // todo This is toooooooo brute-force
    private void updateSelectedTreeEntity(){
        Entity entity = EJBFactory.getRemoteAnnotationBean().getUserEntityById(System.getenv("USER"),getEntityIdFromTreeNode(selectedTree.rootNode));
        if (null != selectedTree || entity.getName().equals(getEntityNameFromTreeNode(selectedTree.rootNode))){
            initializeTree(entity);
        }
        this.updateUI();
    }

    
    @Override
	public void dataReady(DataReadyEvent evt) {
    	AbstractEntityTable privateTable = ontologyManager.getPrivateTable();
    	if (selectedTree == null) {
	    	List<Entity> entities = privateTable.getEntityList();
	    	if (entities == null || entities.isEmpty()) return;
	    	initializeTree(entities.get(0));
    	}
    	// We got the data, no need to listen any longer
    	privateTable.removeDataListener(this);
	}


	@Override
	public void keybindChange(KeybindChangeEvent evt) {
        // Save all the key bindings
        // TODO: in the future, save just the one that changed
        saveKeyBinds();
	}

	// Listen for key strokes and execute the appropriate key bindings
    private KeyListener keyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (KeymapUtil.isModifier(e)) return;
                KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);

                if (keyBindButton.isSelected()) {

                    // Set the key bind
                    OntologyTerm actionEntity = getOntologyTermFromTreeNode(selectedTree.getCurrentNode());
                    ConsoleApp.getKeyBindings().setBinding(shortcut, actionEntity.getAction());
                    
                    // Refresh the entire tree (another key bind may have been overridden)
                    // TODO: this is very slow on large trees...

                    JTree tree = selectedTree.getTree();
                    DefaultTreeModel treeModel = (DefaultTreeModel)tree.getModel();
                    selectedTree.refreshDescendants((DefaultMutableTreeNode)treeModel.getRoot());

                    // Move to the next row

                    selectedTree.navigateToNextRow();

                }
                else {
                    ConsoleApp.getKeyBindings().executeBinding(shortcut);
                }
            }
        }
    };

}
