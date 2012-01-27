/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.KeyBindDialog;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.AnnotateAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.NavigateToNodeAction;
import org.janelia.it.FlyWorkstation.gui.framework.actions.OntologyElementAction;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.choose.OntologyElementChooser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
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
public class OntologyOutline extends OntologyTree implements ActionListener, Outline {

    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "remove";
    private static final String REMOVE_ANNOT_COMMAND = "removeAnnotations";
    private static final String SHOW_MANAGER_COMMAND = "manager";
    private static final String BIND_EDIT_COMMAND = "change_bind";
    private static final String BIND_MODE_COMMAND = "bind_mode";
    private static final String DELIMITER = "#";

    private final KeyListener keyListener;
    private final KeyBindDialog keyBindDialog;
    private final JToggleButton keyBindButton;
    private final JButton manageButton;
    private final OntologyManager ontologyManager;

    private final Map<Long, Action> ontologyActionMap = new HashMap<Long, Action>();

    public OntologyOutline() {
        super();

        setMaximumSize(new Dimension(500, 1300));

        // Create the components

        manageButton = new JButton("Ontology Manager");
        manageButton.setActionCommand(SHOW_MANAGER_COMMAND);
        manageButton.addActionListener(this);

        keyBindButton = new JToggleButton("Set Shortcuts");
        keyBindButton.setActionCommand(BIND_MODE_COMMAND);
        keyBindButton.addActionListener(this);

        // Create input listeners which will be added to the DynamicTree later

        keyListener = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (KeymapUtil.isModifier(e)) return;
                    KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);

                    if (keyBindButton.isSelected()) {

                        // Set the key bind
                        Action action = getActionForNode(selectedTree.getCurrentNode());
                        SessionMgr.getKeyBindings().setBinding(shortcut, action);

                        // Refresh the entire tree (another key bind may have been overridden)
                        // TODO: this is very slow on large trees...

                        JTree tree = selectedTree.getTree();
                        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
                        selectedTree.refreshDescendants((DefaultMutableTreeNode) treeModel.getRoot());

                        // Move to the next row

                        selectedTree.navigateToNextRow();
                    }
                    else {
                        SessionMgr.getKeyBindings().executeBinding(shortcut);
                    }
                }
            }
        };


        add(new JLabel("Ontology Editor"), BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(manageButton);
        buttonPanel.add(keyBindButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Prepare the key binding dialog box

        this.keyBindDialog = new KeyBindDialog(this);
        keyBindDialog.pack();

        keyBindDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                // refresh the tree in case the key bindings were updated
                DefaultTreeModel treeModel = (DefaultTreeModel) selectedTree.getTree().getModel();
                treeModel.nodeChanged(selectedTree.getCurrentNode());
            }
        });

        // Listen for changes to the model
        
        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

			@Override
			public void ontologySelected(long rootId) {
				try {
        			loadOntology(rootId);	
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
        });
        
        // Prepare the ontology manager and start preloading ontologies

        this.ontologyManager = new OntologyManager(this);
        ontologyManager.pack();
        ontologyManager.getPrivateTable().addDataListener(new DataAvailabilityListener() {

            @Override
            public void dataReady(DataReadyEvent evt) {
                AbstractOntologyTable privateTable = ontologyManager.getPrivateTable();
                if (evt.getSource() != privateTable) return;
                if (selectedTree == null) {
                    String lastSessionId = (String) SessionMgr.getSessionMgr().getModelProperty("lastSelectedOntology");
                    if (null == lastSessionId) {
                        java.util.List<OntologyRoot> roots = privateTable.getOntologyRoots();
                        if (roots != null && !roots.isEmpty()) {
                        	System.out.println("Loading the user's first private ontology");
                        	ModelMgr.getModelMgr().setCurrentOntology(roots.get(0));
                        }
                    }
                    else {
                    	System.out.println("Loading last used ontology "+lastSessionId);
                    	try {
	                    	Entity ontology = ModelMgr.getModelMgr().getOntologyTree(Long.valueOf(lastSessionId));
	                    	ModelMgr.getModelMgr().setCurrentOntology(new OntologyRoot(ontology));
                    	}
                    	catch (Exception e) {
                    		e.printStackTrace();
                    	}
                    }
                }
                // We got the data, no need to listen any longer
                privateTable.removeDataListener(this);
            }
		});
        ontologyManager.preload();
    }

    public void loadOntology(long rootId) {
    	if (getCurrentOntology()!=null && getCurrentOntology().getId().equals(rootId)) return;
    	initializeTree(rootId, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
		        selectedTree.expandAll(true);
				return null;
			}
    		
    	});
    }

//    @Override
//    public void initializeTree(final OntologyRoot root) {
//		super.initializeTree(root);
//
//        JTree tree = getTree();
//        tree.setDragEnabled(true);  
//        tree.setDropMode(DropMode.ON_OR_INSERT);
//        tree.setTransferHandler(new TreeTransferHandler(getDynamicTree()) {
//
//        	protected boolean allowTransfer(DefaultMutableTreeNode node, DefaultMutableTreeNode destination) {
//        		OntologyElement element = getElement(node);
//        		OntologyElement destElement = getElement(destination);
//        		if (!ModelMgrUtils.isOwner(element.getEntity()) || !ModelMgrUtils.isOwner(destElement.getEntity())) return false;
//        		
//        		if (element.getType() instanceof EnumItem) return false;
//        		if (!(destElement.getType() instanceof Category) || !(destElement.getType() instanceof Tag)) return false;
//        		
//        		return true;
//        	}
//        	
//        	protected boolean updateUserData(DefaultMutableTreeNode nodeRemoved, DefaultMutableTreeNode nodeAdded, DefaultMutableTreeNode newParent, int destIndex) {
//        		
//        		try {
//            		if (nodeRemoved!=null) {
//            			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)nodeRemoved.getParent();
//            			OntologyElement element = getElement(nodeRemoved);
//            			OntologyElement parent = getElement(parentNode);
//                		ModelMgr.getModelMgr().removeEntityFromParent(parent, entity);
//            		}
//
//            		if (nodeAdded!=null) {
//	        			DefaultMutableTreeNode parentNode = newParent;
//	        			OntologyElement element = getElement(nodeAdded);
//	        			OntologyElement parent = getElement(parentNode);
//	            		
//	            		// Add to parent
//	            		EntityData newEd = parent.addChildEntity(entity);
//	            		// Temporarily remove it so that it can be inserted with the correct index
//	            		parent.getEntityData().remove(newEd); 
//	            		
//	            		List<EntityData> eds = EntityUtils.getOrderedEntityDataOfType(parent, EntityConstants.ATTRIBUTE_ENTITY);
//	            		if (destIndex>eds.size()) {
//	            			eds.add(newEd);
//	            		}
//	            		else {
//	            			eds.add(destIndex, newEd);	
//	            		}
//	            		
//	            		// Renumber the children
//	            		int index = 0;
//	            		for(EntityData ed : eds) {
//	            			if (ed.getOrderIndex()==null || ed.getOrderIndex()!=index) {
//	            				ed.setOrderIndex(index);
//	            				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
//	                    		if (index==destIndex) {
//	                        		// Re-add the saved entity data to the parent
//	                        		parent.getEntityData().add(savedEd);
//	                    		}
//	            			}
//	            			index++;
//	            		}
//            		}
//            		
//            		return true;
//        		}
//        		catch (Exception e) {
//        			SessionMgr.getSessionMgr().handleException(e);
//        			return false;
//        		}
//        	}
//        	
//        	protected boolean addNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode node, int index) {
//        		addNodes(parent, (Entity)node.getUserObject(), index);
//        		return true;
//        	}
//        	
//        }); 
//	}
    
    /**
     * Get the associated action for the given node.
     *
     * @param node
     * @return
     */
    public Action getActionForNode(DefaultMutableTreeNode node) {
        OntologyElement element = (OntologyElement) node.getUserObject();
        return ontologyActionMap.get(element.getId());
    }

    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {

        DefaultMutableTreeNode node = getDynamicTree().getCurrentNode();
        if (node == null) return;
        OntologyElement curr = getOntologyElement(node);
        
        // Create context menus

    	JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JMenuItem titleMenuItem = new JMenuItem(curr.getName());
        titleMenuItem.setEnabled(false);
        popupMenu.add(titleMenuItem);
        
        JMenuItem assignShortcutMenuItem = new JMenuItem("  Assign shortcut...");
        assignShortcutMenuItem.addActionListener(this);
        assignShortcutMenuItem.setActionCommand(BIND_EDIT_COMMAND);
        popupMenu.add(assignShortcutMenuItem);

        if (isEditable()) {

            OntologyElementType type = curr.getType();

            JMenu addMenuPopup = new JMenu("  Add...");
            
            if (type instanceof Enum) {
                // Alternative "Add" menu for enumeration nodes
                JMenuItem smi = new JMenuItem("Item");
                smi.addActionListener(this);
                smi.setActionCommand(ADD_COMMAND + DELIMITER + EnumItem.class.getSimpleName());
                addMenuPopup.add(smi);
                popupMenu.add(addMenuPopup);
            }
            else if (type.allowsChildren() || type instanceof Tag) {

                Class[] nodeTypes = {Category.class, Tag.class, Enum.class, EnumText.class, Interval.class, Text.class};
                for (Class<? extends OntologyElementType> nodeType : nodeTypes) {
                    try {
                        JMenuItem smi = new JMenuItem(nodeType.newInstance().getName());
                        smi.addActionListener(this);
                        smi.setActionCommand(ADD_COMMAND + DELIMITER + nodeType.getSimpleName());
                        addMenuPopup.add(smi);
                    }
                    catch (Exception x) {
                        x.printStackTrace();
                    }
                }
                popupMenu.add(addMenuPopup);
            }

            // Disallow deletion of root nodes. You've gotta use the OntologyManager for that.
            if (curr.getParent() != null) {
            	JMenuItem removeNodeMenuItem = new JMenuItem("  Remove this term");
                removeNodeMenuItem.addActionListener(this);
                removeNodeMenuItem.setActionCommand(REMOVE_COMMAND);
                popupMenu.add(removeNodeMenuItem);
            
            	JMenuItem removeAnnotNodeMenuItem = new JMenuItem("  Remove from all selected entities");
            	removeAnnotNodeMenuItem.addActionListener(this);
            	removeAnnotNodeMenuItem.setActionCommand(REMOVE_ANNOT_COMMAND);
                popupMenu.add(removeAnnotNodeMenuItem);
            }
        }

        popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
    }

    /**
     * Override this method to do something when the user double clicks a node.
     *
     * @param e
     */
    protected void nodeDoubleClicked(MouseEvent e) {
        Action action = getActionForNode(getDynamicTree().getCurrentNode());
        if (action != null && !(action instanceof NavigateToNodeAction)) {
            action.doAction();
        }
    }
    
    /**
     * Reload the data for the current tree.
     */
    @Override
	public void refresh() {
    	if (getRootEntity()!=null) {
            Utils.setWaitingCursor(OntologyOutline.this);
        	final ExpansionState expansionState = new ExpansionState();
        	expansionState.storeExpansionState(getDynamicTree());
			initializeTree(getRootEntity().getId(), new Callable<Void>() {
				@Override
				public Void call() throws Exception {
			    	expansionState.restoreExpansionState(getDynamicTree());
	                Utils.setDefaultCursor(OntologyOutline.this);
					return null;
				}
			});
    	}
    }
    
    protected void createNewTree(OntologyRoot root) {

        super.createNewTree(root);

        // Replace the cell renderer with one that knows about the outline so that it can retrieve key binds

        selectedTree.setCellRenderer(new OntologyTreeCellRenderer(this));

        // Replace the default key listener on the tree

        final JTree tree = selectedTree.getTree();
        KeyListener defaultKeyListener = tree.getKeyListeners()[0];
        tree.removeKeyListener(defaultKeyListener);
        tree.addKeyListener(keyListener);

        // Build a lookup table of the action for each node

        ontologyActionMap.clear();
        populateActionMap(root);

        // Load key bind preferences and bind keys to actions 

        ModelMgr.getModelMgr().setCurrentOntology(getCurrentOntology());
        SessionMgr.getKeyBindings().loadOntologyKeybinds(root, ontologyActionMap);
    }

    /**
     * Register a corresponding Action for the given element, based on its term type. Recurses through the
     * element's children if there are any.
     *
     * @param element
     */
    private void populateActionMap(OntologyElement element) {
    	
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

        for (OntologyElement child : element.getChildren()) {
            populateActionMap(child);
        }
    }

    private OntologyElement getOntologyElement(DefaultMutableTreeNode targetNode) {
        return (OntologyElement) targetNode.getUserObject();
    }

    public void navigateToOntologyElement(OntologyElement element) {
        selectedTree.navigateToNodeWithObject(element);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (REMOVE_COMMAND.equals(command)) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently remove this term?", "Remove Term", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }

            try {
                // Update object model
                OntologyElement element = getOntologyElement(selectedTree.getCurrentNode());
                OntologyElement parent = element.getParent();
                parent.removeChild(element);

                // Update database
                ModelMgr.getModelMgr().removeOntologyTerm(element.getId());

                // Update Tree UI
                selectedTree.removeNode(selectedTree.getCurrentNode());

            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(OntologyOutline.this, "Error removing ontology term", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (REMOVE_ANNOT_COMMAND.equals(command)) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently remove annotations using this term in all selected entities?", "Remove Annotations", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }

            try {
            	final List<Long> selectedEntities = new ArrayList<Long>(ModelMgr.getModelMgr().getSelectedEntitiesIds());
            	
            	// TODO: this should really use the ModelMgr
            	final Annotations annotations = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().getAnnotations();
                final Map<Long, List<OntologyAnnotation>> annotationMap = annotations.getFilteredAnnotationMap();
                
                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        OntologyElement element = getOntologyElement(selectedTree.getCurrentNode());
                        
                        int i=1;
                    	for(Long entityId : selectedEntities) {
                            List<OntologyAnnotation> entityAnnotations = annotationMap.get(entityId);
                            if (entityAnnotations==null) {
                            	continue;
                            }
                            for(OntologyAnnotation annotation : entityAnnotations) {
                            	if (annotation.getKeyEntityId().equals(element.getEntity().getId())) {
                            		ModelMgr.getModelMgr().removeAnnotation(annotation.getId());
                            	}
                            }

        		            setProgress(i++, selectedEntities.size());
                    	}
                    }

                    @Override
                    protected void hadSuccess() {
        				// No need to do anything
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(OntologyOutline.this, "Error removing annotations", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                };

                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Removing annotations", "", 0, 100));
                worker.execute();
                
            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(OntologyOutline.this, "Error removing annotations", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (SHOW_MANAGER_COMMAND.equals(command)) {
            ontologyManager.showDialog();
        }
        else if (BIND_EDIT_COMMAND.equals(command)) {
            DefaultMutableTreeNode treeNode = selectedTree.getCurrentNode();
            if (treeNode != null) {
                OntologyElement element = getOntologyElement(treeNode);
                if (element != null) {
                    Action action = getActionForNode(treeNode);
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
                SessionMgr.getKeyBindings().saveOntologyKeybinds(getCurrentOntology());
            }
        }
        else if (command.startsWith(ADD_COMMAND)) {

            if (selectedTree == null) {
                JOptionPane.showMessageDialog(this, "No ontology selected.");
            }

            DefaultMutableTreeNode treeNode = selectedTree.getCurrentNode();
            OntologyElement element = getOntologyElement(treeNode);

            String className = command.split(DELIMITER)[1];
            OntologyElementType childType = OntologyElementType.createTypeByName(className);

            // Add button clicked
            String termName = (String) JOptionPane.showInputDialog(this, "Ontology Term:\n", "Adding to " + 
            		getOntologyElement(selectedTree.getCurrentNode()).getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

            if ((termName == null) || (termName.length() <= 0)) {
                return;
            }

            if (childType instanceof Interval) {

                String lowerBoundStr = (String) JOptionPane.showInputDialog(this, "Lower bound:\n", 
                		"Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);
                String upperBoundStr = (String) JOptionPane.showInputDialog(this, "Upper bound:\n", 
                		"Adding an interval", JOptionPane.PLAIN_MESSAGE, null, null, null);

                try {
                    ((Interval) childType).init(lowerBoundStr, upperBoundStr);
                }
                catch (Exception ex) {
                    SessionMgr.getSessionMgr().handleException(ex);
                    return;
                }
            }
            else if (childType instanceof EnumText) {

                OntologyElementChooser ontologyChooser = new OntologyElementChooser("Choose an enumeration", getCurrentOntology());
                ontologyChooser.setMultipleSelection(false);
                int returnVal = ontologyChooser.showDialog(SessionMgr.getSessionMgr().getActiveBrowser());
                if (returnVal != OntologyElementChooser.CHOOSE_OPTION) return;
                
                List<OntologyElement> chosenElements = ontologyChooser.getChosenElements();
                if (chosenElements.size()!=1) return;
                
                OntologyElement chosenEnum = chosenElements.get(0);
                if (!(chosenEnum.getType() instanceof Enum)) {
                    JOptionPane.showMessageDialog(OntologyOutline.this, "You must choosen an enumeration", "Error", JOptionPane.ERROR_MESSAGE);
                }

                try {
                    ((EnumText) childType).init(chosenEnum);
                }
                catch (Exception ex) {
                    SessionMgr.getSessionMgr().handleException(ex);
                    return;
                }
            }
            
            try {
                // Update database
                EntityData newData = ModelMgr.getModelMgr().createOntologyTerm(element.getId(), termName, childType, null);

                // Update object model
                element.getEntity().getEntityData().add(newData);
                element.init();

                // Update Tree UI
                OntologyElement newElement = new OntologyElement(newData.getChildEntity(), element);
                
                // Update secondary attributes
                if (newElement.getType() instanceof EnumText) {
                	((EnumText)newElement.getType()).init(((EnumText)childType).getValueEnum());
                }
                
                addNodes(treeNode, newElement);

                populateActionMap(newElement);

                selectedTree.expand(treeNode, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(OntologyOutline.this, "Error creating ontology term", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
