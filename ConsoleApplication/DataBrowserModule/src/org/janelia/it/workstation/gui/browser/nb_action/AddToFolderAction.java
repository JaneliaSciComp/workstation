package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.gui.support.NodeChooser;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.UserViewConfiguration;
import org.janelia.it.workstation.gui.browser.nodes.UserViewRootNode;
import org.janelia.it.workstation.gui.browser.nodes.UserViewTreeNodeNode;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to move currently selected nodes to a folder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddToFolderAction extends NodePresenterAction {

    private final static Logger log = LoggerFactory.getLogger(AddToFolderAction.class);

    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;

    protected final Component mainFrame = SessionMgr.getMainFrame();

    private final static AddToFolderAction singleton = new AddToFolderAction();
    public static AddToFolderAction get() {
        return singleton;
    }

    protected AddToFolderAction() {
    }
    
    @Override
    public JMenuItem getPopupPresenter() {

        List<Node> selectedNodes = getSelectedNodes();
        assert !selectedNodes.isEmpty() : "No nodes are selected";
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        JMenu newFolderMenu = new JMenu("Add To Folder");

        JMenuItem createNewItem = new JMenuItem("Create New Folder...");
        
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
                        folder = model.create(folder);
                        Workspace workspace = model.getDefaultWorkspace();
                        idPath = NodeUtils.createIdPath(workspace, folder);
                        model.addChild(workspace, folder);
                        addSelectedObjectsToFolder(folder, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.debug("Added to folder {}",folder.getId());
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

                NodeChooser nodeChooser = new NodeChooser(new UserViewRootNode(UserViewConfiguration.create(TreeNode.class)), "Choose folder to add to");
                nodeChooser.setRootVisible(false);
                
                int returnVal = nodeChooser.showDialog(explorer);
                if (returnVal != NodeChooser.CHOOSE_OPTION) return;
                if (nodeChooser.getChosenElements().isEmpty()) return;
                final UserViewTreeNodeNode selectedNode = (UserViewTreeNodeNode)nodeChooser.getChosenElements().get(0);
                final TreeNode folder = selectedNode.getTreeNode();
                SimpleWorker worker = new SimpleWorker() {

                    private Long[] idPath;
                
                    @Override
                    protected void doStuff() throws Exception {
                        idPath = NodeUtils.createIdPath(selectedNode);
                        addSelectedObjectsToFolder(folder, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Added to folder {}",folder.getId());
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
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
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

                        SimpleWorker worker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                addSelectedObjectsToFolder(finalFolder, idPath);
                            }

                            @Override
                            protected void hadSuccess() {
                                log.info("Added to folder {}",finalFolder.getId());
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
                                SessionMgr.getSessionMgr().handleException(error);
                            }
                        };
                        worker.execute();
                    }
                });

                commonRootItem.setEnabled(ClientDomainUtils.hasWriteAccess(finalFolder));
                newFolderMenu.add(commonRootItem);
            }
        }

        return newFolderMenu;
    }
    
    protected void addSelectedObjectsToFolder(TreeNode folder, Long[] idPath) throws Exception {
        List<Node> selectedNodes = getSelectedNodes();
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        // Build list of things to remove
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Node node : selectedNodes) {
            log.info("Moving selected node '{}' to folder '{}'",node.getDisplayName(),folder.getName());
            DomainObjectNode selectedNode = (DomainObjectNode)node;
            DomainObject domainObject = selectedNode.getDomainObject();
            if (folder.hasChild(domainObject)) {
                log.debug("Folder {} already has child {}",folder.getId(),domainObject.getId());
            }
            else {
                domainObjects.add(domainObject);
            }
        }
        
        // Add them to the given folder
        model.addChildren(folder, domainObjects);

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
