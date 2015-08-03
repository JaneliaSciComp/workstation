package org.janelia.it.workstation.gui.browser.nb_action;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.gui.support.NodeChooser;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.browser.nodes.UserViewTreeNodeNode;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.Presenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MoveToFolderAction extends NodeAction implements Presenter.Popup {

    private final static Logger log = LoggerFactory.getLogger(MoveToFolderAction.class);
    
    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    
    protected final Component mainFrame = SessionMgr.getMainFrame();

    private final static MoveToFolderAction singleton = new MoveToFolderAction();
    public static MoveToFolderAction get() {
        return singleton;
    }
    
    private final List<Node> selected = new ArrayList<>();
    
    private MoveToFolderAction() {
    }
    
    @Override
    public String getName() {
        // Implemented by popup presenter
        return "";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        // Implemented by popup presenter
    }

    @Override
    protected boolean asynchronous() {
        // We do our own background processing
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            selected.add(node);
        }
        // Enable state is determined by the popup presenter
        return true;
    }
    
    @Override
    public JMenuItem getPopupPresenter() {

        assert !selected.isEmpty() : "No nodes are selected";
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();

        int numOwned = 0;
        for(Node node : selected) {
            DomainObjectNode domainNode = (DomainObjectNode)node;
            if (DomainUtils.isOwner(domainNode.getDomainObject())) {
                numOwned++;
            }
        }
        
        boolean owned = numOwned>0;
        JMenu newFolderMenu = new JMenu(owned ? "Move To Folder" : "Create Shortcut In Folder");
        
        if (owned && numOwned<selected.size()) {
            // Not everything is owned, so let's just disable the item to eliminate confusion as to what happens in this case
            newFolderMenu.setEnabled(false);
            return newFolderMenu;
        }
        
        JMenuItem createNewItem = new JMenuItem("Create New Folder...");
        
        final DomainDAO dao = DomainMgr.getDomainMgr().getDao();
        final Workspace workspace = dao.getDefaultWorkspace(SessionMgr.getSubjectKey());
        
        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

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
                        dao.save(SessionMgr.getSubjectKey(), folder);
                        idPath = NodeUtils.createIdPath(workspace, folder);
                        dao.addChild(SessionMgr.getSubjectKey(), workspace, folder);
                        moveSelectedObjectsToFolder(folder, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.debug("Added to folder {}",folder.getId());
                        explorer.refresh(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                explorer.expand(idPath);
                                return null;
                            }
                        });
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating folder...", ""));
                worker.execute();
            }
        });

        newFolderMenu.add(createNewItem);

        JMenuItem chooseItem = new JMenuItem("Choose Folder...");

        chooseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                NodeChooser nodeChooser = new NodeChooser("Choose folder to add to", false);
                
                int returnVal = nodeChooser.showDialog(explorer);
                if (returnVal != NodeChooser.CHOOSE_OPTION) return;
                if (nodeChooser.getChosenElements().isEmpty()) return;
                final UserViewTreeNodeNode selectedNode = (UserViewTreeNodeNode)nodeChooser.getChosenElements().get(0);

                if (selectedNode instanceof UserViewTreeNodeNode) {
                    final TreeNode folder = selectedNode.getTreeNode();
                    SimpleWorker worker = new SimpleWorker() {

                        private Long[] idPath;
                    
                        @Override
                        protected void doStuff() throws Exception {
                            idPath = NodeUtils.createIdPath(selectedNode);
                            moveSelectedObjectsToFolder(folder, idPath);
                        }

                        @Override
                        protected void hadSuccess() {
                            log.info("Added to folder {}",folder.getId());
                            explorer.refresh(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    explorer.expand(idPath);
                                    return null;
                                }
                            });
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.execute();
                }
                else {
                    JOptionPane.showMessageDialog(mainFrame, "You must select a folder to add items",
                            "Invalid folder", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        newFolderMenu.add(chooseItem);
        newFolderMenu.addSeparator();

        List<String> addHistory = (List<String>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_FOLDER_HISTORY);
        if (addHistory!=null && !addHistory.isEmpty()) {

            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            newFolderMenu.add(item);

            for (String path : addHistory) {

                final Long[] idPath = NodeUtils.createIdPath(path);
                final Long folderId = idPath[idPath.length-1];
                
                final TreeNode folder = (TreeNode)dao.getDomainObject(SessionMgr.getSubjectKey(), TreeNode.class, folderId);
                if (folder == null) continue;

                DomainUtils.hasWriteAccess(folder);

                JMenuItem commonRootItem = new JMenuItem(folder.getName());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {

                        SimpleWorker worker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                moveSelectedObjectsToFolder(folder, idPath);
                            }

                            @Override
                            protected void hadSuccess() {
                                log.debug("Added to folder {}",folder.getId());
                                explorer.refresh(new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        // TODO: this only works if the folder is at the top level, we should fix this
                                        explorer.expand(idPath);
                                        return null;
                                    }
                                });
                            }

                            @Override
                            protected void hadError(Throwable error) {
                                SessionMgr.getSessionMgr().handleException(error);
                            }
                        };
                        worker.execute();
                    }
                });

                newFolderMenu.add(commonRootItem);
            }
        }

        return newFolderMenu;
    }
    
    private void moveSelectedObjectsToFolder(TreeNode folder, Long[] idPath) throws Exception {
        DomainDAO dao = DomainMgr.getDomainMgr().getDao();
        
        // Build list of things to remove
        Multimap<TreeNode,DomainObject> removeMap = ArrayListMultimap.create();
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Node node : selected) {
            DomainObjectNode selectedNode = (DomainObjectNode)node;
            TreeNodeNode parentNode = (TreeNodeNode)node.getParentNode();
            DomainObject domainObject = selectedNode.getDomainObject();
            if (DomainUtils.hasChild(folder, domainObject)) {
                log.debug("Folder {} already has child {}",folder.getId(),domainObject.getId());
            }
            else {
                domainObjects.add(domainObject);
            }
            removeMap.put(parentNode.getTreeNode(), selectedNode.getDomainObject());
        }
        
        // Add them to the given folder
        dao.addChildren(SessionMgr.getSubjectKey(), folder, domainObjects);
        
        // Remove from existing folders
        for(TreeNode treeNode : removeMap.keys()) {
            if (DomainUtils.isOwner(treeNode)) {
                dao.removeChildren(SessionMgr.getSubjectKey(), treeNode, removeMap.get(treeNode));
            }
        }
        
        // Update history
        updateAddToFolderHistory(idPath);                
    }
    
    private void updateAddToFolderHistory(Long[] idPath) {
        String pathString = NodeUtils.createPathString(idPath);
        List<String> addHistory = (List<String>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_FOLDER_HISTORY);
        if (addHistory==null) {
            addHistory = new ArrayList<>();
        }
        if (addHistory.contains(pathString)) {
            return;
        }
        if (addHistory.size()>=MAX_ADD_TO_ROOT_HISTORY) {
            addHistory.remove(addHistory.size()-1);
        }
        addHistory.add(0, pathString);
        SessionMgr.getSessionMgr().setModelProperty(Browser.ADD_TO_FOLDER_HISTORY, addHistory);
    }
}
