/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.AnnotateAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.NavigateToNodeAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OntologyElementAction;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyBindFrame;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.ontology.types.Enum;


/**
 * An ontology editor panel.
 * 
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyOutline extends JPanel implements ActionListener, DataAvailabilityListener {
	
	
    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "remove";
    private static final String SHOW_MANAGER_COMMAND = "manager";
    private static final String BIND_EDIT_COMMAND = "change_bind";
    private static final String BIND_MODE_COMMAND = "bind_mode";
    private static final String DELIMITER = "#";

    private final List<Class<? extends OntologyElementType>> nodeTypes = new ArrayList<Class<? extends OntologyElementType>>();
    
    private final KeyListener keyListener;
    private final JPanel treesPanel;
    private final KeyBindFrame keyBindDialog;
    private final JToggleButton keyBindButton;
    private final JButton manageButton;
    private final OntologyManager ontologyManager;
    private final JPopupMenu popupMenu;
    private final JMenuItem assignShortcutMenuItem;
    private final JMenuItem removeNodeMenuItem;
    private final JMenu addMenuPopup;
    private final JMenu addItemPopup;
    
    private final Map<Long, Action> ontologyActionMap = new HashMap<Long, Action>();
    private DynamicTree selectedTree;
    private OntologyRoot root;
    
    
    public OntologyOutline() {
        super(new BorderLayout());

        nodeTypes.add(Category.class);
        nodeTypes.add(Tag.class);
        nodeTypes.add(Enum.class);
        nodeTypes.add(Interval.class);
        nodeTypes.add(Text.class);

        // Create the components

        treesPanel = new JPanel(new BorderLayout());
        
        manageButton = new JButton("Ontology Manager");
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
        for(Class<? extends OntologyElementType> nodeType : nodeTypes) {
			try {
				JMenuItem smi = new JMenuItem(nodeType.newInstance().getName());
				smi.addActionListener(this);
				smi.setActionCommand(ADD_COMMAND + DELIMITER + nodeType.getSimpleName());
				addMenuPopup.add(smi);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        // Alternative "Add" menu for enumeration nodes
        addItemPopup = new JMenu("Add...");
        JMenuItem smi = new JMenuItem("Item");
        smi.addActionListener(this);
        smi.setActionCommand(ADD_COMMAND + DELIMITER + EnumItem.class.getSimpleName());
        addItemPopup.add(smi);

        removeNodeMenuItem = new JMenuItem("Remove this node");
        removeNodeMenuItem.addActionListener(this);
        removeNodeMenuItem.setActionCommand(REMOVE_COMMAND);

        // Create input listeners which will be added to the DynamicTree later
        
        keyListener = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (KeymapUtil.isModifier(e)) return;
                    KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);

                    if (keyBindButton.isSelected()) {

                        // Set the key bind
                        Action action = selectedTree.getActionForNode(selectedTree.getCurrentNode());
                        ConsoleApp.getKeyBindings().setBinding(shortcut, action);
                        
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
        

        // Lay everything out

        add(new JLabel("Ontology Editor"), BorderLayout.NORTH);
        
        add(treesPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(manageButton);
        buttonPanel.add(keyBindButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Prepare the key binding dialog box

        this.keyBindDialog = new KeyBindFrame(this);
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
    
    
    public OntologyRoot getCurrentOntology() {
    	return root;
    }
    
    public void initializeTree(final Long rootId) {
    	
        treesPanel.removeAll();

        if (rootId == null) return;
    	
		// Show a loading spinner until the data is loaded
		treesPanel.add(new JLabel(Icons.loadingIcon));
        this.updateUI();
		
		SimpleWorker loadingWorker = new SimpleWorker() {

			private Entity rootEntity;
        	
            protected void doStuff() throws Exception {
            	rootEntity = EJBFactory.getRemoteAnnotationBean().getOntologyTree(System.getenv("USER"), rootId);
            	
            }

			protected void hadSuccess() {

				try {
		            // Create a new tree and add all the nodes to it
		    		
		    		root = new OntologyRoot(rootEntity);
		    		selectedTree = createNewTree(root);
		            
		    		ontologyActionMap.clear();
		    		addNodes(null, root);

		            // Load key bind preferences and bind keys to actions 
		            
		            ConsoleApp.getKeyBindings().loadOntologyKeybinds(root, ontologyActionMap);
		            
		            // Replace the tree in the panel

		            treesPanel.removeAll();
		            treesPanel.add(selectedTree);

		            // Prepare for display and update the UI
		            
		            selectedTree.expandAll(true);
		            OntologyOutline.this.updateUI();
				}
				catch (Exception e) {
					hadError(e);
				}
			}
			
			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(OntologyOutline.this, "Error loading ontology", "Ontology Load Error", JOptionPane.ERROR_MESSAGE);
	            treesPanel.removeAll();
	            OntologyOutline.this.updateUI();
			}
            
        };


        loadingWorker.execute();
    }
    
    private DynamicTree createNewTree(OntologyElement root) {
    	
    	DynamicTree dtree = new DynamicTree(root) {

            protected void showPopupMenu(MouseEvent e) {

                popupMenu.removeAll();
                popupMenu.add(assignShortcutMenuItem);
                
                if (isEditable()) {

                    OntologyElement curr = getOntologyElement(getCurrentNode());
                    OntologyElementType type = curr.getType();
                    
                    if (type instanceof Enum) {
                    	popupMenu.add(addItemPopup);
                    }
                    else if (type.allowsChildren() || type instanceof Tag) {
                    	popupMenu.add(addMenuPopup); 
                    }

                    // Disallow deletion of root nodes. You've gotta use the OntologyManager for that.
                    if (curr.getParent() != null) {
                        popupMenu.add(removeNodeMenuItem);
                    }
                }
                
                popupMenu.show((JComponent)e.getSource(), e.getX(), e.getY());
            }
            
        };
        
        // Replace the cell renderer
        
        dtree.setCellRenderer(new OntologyTreeCellRenderer(dtree));
        
        // Replace the default key listener on the tree
        
        final JTree tree = dtree.getTree();
        KeyListener defaultKeyListener = tree.getKeyListeners()[0];
        tree.removeKeyListener(defaultKeyListener);
        tree.addKeyListener(keyListener);

        return dtree;
    }
    

    private void addNodes(DefaultMutableTreeNode parentNode, OntologyElement element) {
    	
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, element);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.rootNode;
        }
        
        // Define an action for this node
    	OntologyElementType type = element.getType();
    	OntologyElementAction action = null;
    	if (type instanceof Category || type instanceof Enum) {
    		action = new NavigateToNodeAction();
    	}
    	else {
    		action = new AnnotateAction();
    	}
    	action.init(element);
    	ontologyActionMap.put(element.getId(), action);
    	selectedTree.setActionForNode(newNode, action);
    	
    	// Load the node's children
//    	for(EntityData data : element.getEntity().getEntityData()) {
//    		Entity childEntity = data.getChildEntity();
//    		if (childEntity != null) {
//    			data.setChildEntity(EJBFactory.getRemoteAnnotationBean().getEntityById(childEntity.getId().toString()));
//    		}
//    	}
    	
    	// Add the node's children
        for(OntologyElement child : element.getChildren()) {
        	addNodes(newNode, child);
        }
    }

    private OntologyElement getOntologyElement(DefaultMutableTreeNode targetNode) {
        return (OntologyElement)targetNode.getUserObject();
    }

    private String getEntityNameFromTreeNode(DefaultMutableTreeNode targetNode) {
        return ((OntologyElement)targetNode.getUserObject()).getName();
    }

    private Long getEntityIdFromTreeNode(DefaultMutableTreeNode targetNode) {
        return ((OntologyElement)targetNode.getUserObject()).getId();
    }

    public void navigateToOntologyElement(OntologyElement element) {
        selectedTree.navigateToNodeWithObject(element);
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
                OntologyElement element = getOntologyElement(treeNode);
                if (element != null) {
                	Action action = selectedTree.getActionForNode(treeNode);
                    keyBindDialog.showForAction(action);
                }
            }
        }
        else if (BIND_MODE_COMMAND.equals(command)) {
            if (keyBindButton.isSelected()) {
            	// Transfer focus to a node in the tree in preparation for key presses
            	selectedTree.getTree().grabFocus();
            	if (selectedTree.getCurrentNode() == null) {
            		selectedTree.setCurrentNode(selectedTree.getRootNode());
            	}	
            }
            else {
            	ConsoleApp.getKeyBindings().saveOntologyKeybinds(root);
            }
        }
        else if (command.startsWith(ADD_COMMAND)) {

            if (selectedTree == null) {
                JOptionPane.showMessageDialog(this, "No ontology selected.");
            }

            DefaultMutableTreeNode treeNode = selectedTree.getCurrentNode();
            OntologyElement element = getOntologyElement(treeNode);
            OntologyElementType parentType = element.getType();

            String className = command.split(DELIMITER)[1];
            OntologyElementType childType = OntologyElementType.createTypeByName(className);
            
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
            	
            	EntityData ed = element.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
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
    	return !root.isPublic();
    }

    // TODO: This is toooooooo brute-force
    private void updateSelectedTreeEntity(){
        Entity entity = EJBFactory.getRemoteAnnotationBean().getUserEntityById(System.getenv("USER"),getEntityIdFromTreeNode(selectedTree.rootNode));
        if (null != selectedTree || entity.getName().equals(getEntityNameFromTreeNode(selectedTree.rootNode))){
            initializeTree(entity.getId());
        }
        this.updateUI();
    }

    
    @Override
	public void dataReady(DataReadyEvent evt) {
    	AbstractOntologyTable privateTable = ontologyManager.getPrivateTable();
    	if (evt.getSource() != privateTable) return;
    	if (selectedTree == null) {
	    	List<OntologyRoot> roots = privateTable.getOntologyRoots();
	    	if (roots == null || roots.isEmpty()) {
	    		initializeTree(null);
	    		return;
	    	}
	    	initializeTree(roots.get(0).getId());
    	}
    	// We got the data, no need to listen any longer
    	privateTable.removeDataListener(this);
	}


}
