package org.janelia.it.workstation.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.gui.support.TreeNodeChooser;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.UserViewConfiguration;
import org.janelia.it.workstation.browser.nodes.UserViewRootNode;
import org.janelia.it.workstation.browser.nodes.UserViewTreeNodeNode;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the related items for a given set of items, and add them to a folder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetRelatedItemsAction extends NodePresenterAction {

    private final static Logger log = LoggerFactory.getLogger(GetRelatedItemsAction.class);

    protected final Component mainFrame = ConsoleApp.getMainFrame();

    private final static GetRelatedItemsAction singleton = new GetRelatedItemsAction();
    public static GetRelatedItemsAction get() {
        return singleton;
    }

    protected GetRelatedItemsAction() {
    }

    private Collection<DomainObject> domainObjects = new ArrayList<>();

    /**
     * Activation method #1: set the domain objects directly
     * @param domainObjectList
     */
    public void setDomainObjects(Collection<DomainObject> domainObjectList) {
        domainObjects.clear();
        domainObjects.addAll(domainObjectList);
    }

    /**
     * Activation method #2: set the domain objects via the NetBeans Nodes API
     * @param activatedNodes
     * @return
     */
    @Override
    protected boolean enable(Node[] activatedNodes) {
        boolean enabled = super.enable(activatedNodes);
        List<Node> selectedNodes = getSelectedNodes();

        // Build list of things to add
        domainObjects.clear();
        for(Node node : selectedNodes) {
            if (node instanceof AbstractDomainObjectNode) {
                @SuppressWarnings("unchecked")
                AbstractDomainObjectNode<DomainObject> selectedNode = (AbstractDomainObjectNode<DomainObject>)node;
                domainObjects.add(selectedNode.getDomainObject());
            }
        }

        return enabled;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        String name = "Add Related Items To Folder";
        JMenu getRelatedMenu = new JMenu(name);
        
        Set<Class<? extends DomainObject>> mappableTypes = getMappableTypes(domainObjects);
        if (mappableTypes.isEmpty()) {
            return null;
        }
        
        for (final Class<? extends DomainObject> targetClass : mappableTypes) {
            
            String typeName = DomainUtils.getTypeName(targetClass);
            JMenu classMenu = new JMenu(typeName);
            getRelatedMenu.add(classMenu);
    
            JMenuItem createNewItem = new JMenuItem("Create New Folder...");
            
            createNewItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
    
                    ActivityLogHelper.logUserAction("AddToFolderAction.createNewFolder");
    
                    // Add button clicked
                    final String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
                            "Create new folder in workspace", JOptionPane.PLAIN_MESSAGE, null, null, null);
                    if ((folderName == null) || (folderName.length() <= 0)) {
                        return;
                    }
    
                    SimpleWorker worker = new SimpleWorker() {
                        
                        private TreeNode folder;
                        private Long[] idPath;
    
                        @Override
                        protected void doStuff() throws Exception {
                            folder = new TreeNode();
                            folder.setName(folderName);
                            folder = model.create(folder);
                            Workspace workspace = model.getDefaultWorkspace();
                            idPath = NodeUtils.createIdPath(workspace, folder);
                            model.addChild(workspace, folder);
                            addUniqueItemsToFolder(folder, idPath, targetClass);
                        }
    
                        @Override
                        protected void hadSuccess() {
                            log.debug("Added selected items to folder {}",folder.getId());
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    explorer.expand(idPath);
                                    explorer.selectNodeByPath(idPath);
                                }
                            });
                        }
    
                        @Override
                        protected void hadError(Throwable error) {
                            ConsoleApp.handleException(error);
                        }
                    };
                    
                    worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating folder...", ""));
                    worker.execute();
                }
            });
    
            classMenu.add(createNewItem);
    
            JMenuItem chooseItem = new JMenuItem("Choose Folder...");
    
            chooseItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
    
                    ActivityLogHelper.logUserAction("AddToFolderAction.chooseFolder");
    
                    TreeNodeChooser nodeChooser = new TreeNodeChooser(new UserViewRootNode(UserViewConfiguration.create(TreeNode.class)), "Choose folder to add to", true);
                    nodeChooser.setRootVisible(false);
                    
                    int returnVal = nodeChooser.showDialog(explorer);
                    if (returnVal != TreeNodeChooser.CHOOSE_OPTION) return;
                    if (nodeChooser.getChosenElements().isEmpty()) return;
                    final UserViewTreeNodeNode selectedNode = (UserViewTreeNodeNode)nodeChooser.getChosenElements().get(0);
                    final TreeNode folder = selectedNode.getTreeNode();
                    
                    addUniqueItemsToFolder(folder, NodeUtils.createIdPath(selectedNode), targetClass);
                }
            });
    
            classMenu.add(chooseItem);
            classMenu.addSeparator();
    
            List<String> addHistory = StateMgr.getStateMgr().getAddToFolderHistory();
            if (addHistory!=null && !addHistory.isEmpty()) {
    
                JMenuItem item = new JMenuItem("Recent:");
                item.setEnabled(false);
                classMenu.add(item);
    
                for (String path : addHistory) {
    
                    if (path.contains("#")) {
                        log.warn("Ignoring reference in add history: "+path);
                        continue;
                    }
                    
                    final Long[] idPath = NodeUtils.createIdPath(path);
                    final Long folderId = idPath[idPath.length-1];
                    
                    TreeNode folder;
                    try {
                        folder = model.getDomainObject(TreeNode.class, folderId);
                        if (folder == null) continue;
                    }
                    catch (Exception e) {
                        log.error("Error getting recent folder with id "+folderId,e);
                        continue;
                    }
    
                    final TreeNode finalFolder = folder;
    
                    JMenuItem commonRootItem = new JMenuItem(folder.getName());
                    commonRootItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent actionEvent) {
                            ActivityLogHelper.logUserAction("AddToFolderAction.recentFolder", folderId);
                            addUniqueItemsToFolder(finalFolder, idPath, targetClass);
                        }
                    });
    
                    commonRootItem.setEnabled(ClientDomainUtils.hasWriteAccess(finalFolder));
                    classMenu.add(commonRootItem);
                }
            }
        }
        
        return getRelatedMenu;
    }

    private <T extends DomainObject> void addUniqueItemsToFolder(final TreeNode treeNode, final Long[] idPath, final Class<T> targetClass) {

        List<DomainObject> objects = new ArrayList<>(domainObjects);
        
        int existing = 0;
        for(DomainObject domainObject : objects) {
            if (treeNode.hasChild(domainObject)) {
                existing++;
            }
        }

        if (existing>0) {
            String message;
            if (existing==objects.size()) {
                message = "All items are already in the target folder, no items will be added.";
            }
            else {
                message = existing + " items are already in the target folder. "+(domainObjects.size()-existing)+" item(s) will be added.";
            }
            JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), message, "Items already present", JOptionPane.OK_OPTION);
        }

        final int numAdded = objects.size()-existing;

        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                addItemsToFolder(treeNode, idPath, targetClass, objects);
            }

            @Override
            protected void hadSuccess() {
                log.info("Added {} items to folder {}", numAdded, treeNode.getId());
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Adding items to folder...", ""));
        worker.execute();
    }

    protected <T extends DomainObject> void addItemsToFolder(final TreeNode treeNode, final Long[] idPath, Class<T> targetClass, List<DomainObject> objects) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();

        // Map the items first
        List<T> mapped = DomainModelViewUtils.map(objects, targetClass);
        
        // Add them to the given folder
        model.addChildren(treeNode, mapped);

        // Update history
        String pathString = NodeUtils.createPathString(idPath);
        StateMgr.getStateMgr().updateAddToFolderHistory(pathString);
    }
    
    private Set<Class<? extends DomainObject>> getMappableTypes(Collection<DomainObject> domainObjects) {
        
        Set<Class<? extends DomainObject>> types = new HashSet<>();
        for (DomainObject domainObject : domainObjects) {
            types.addAll(DomainModelViewUtils.getMappableTypes(domainObject));
        }
        
        return types;
    }
    
}
