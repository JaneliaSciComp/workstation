package org.janelia.it.workstation.gui.browser.actions;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.gui.support.NodeChooser;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.UserViewConfiguration;
import org.janelia.it.workstation.gui.browser.nodes.UserViewRootNode;
import org.janelia.it.workstation.gui.browser.nodes.UserViewTreeNodeNode;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Menu system for adding the selected items to a folder.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddItemsToFolderAction implements NamedAction {

    private final static Logger log = LoggerFactory.getLogger(AddItemsToFolderAction.class);

    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    private final Component mainFrame = SessionMgr.getMainFrame();
    private final Collection<DomainObject> domainObjects;

    public AddItemsToFolderAction(Collection<DomainObject> domainObjects) {
        this.domainObjects = domainObjects;
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Add \"" + domainObjects.size() + "\" Items To Folder" : "Add Item To Folder";
    }

    @Override
    public void doAction() {
        throw new IllegalStateException("This action must be executed via its popup presenter");
    }
    
    public JMenuItem getPopupPresenter() {
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        JMenu newFolderMenu = new JMenu(getName());
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
                    
                    private TreeNode treeNode;
                    private Long[] idPath;

                    @Override
                    protected void doStuff() throws Exception {
                        treeNode = new TreeNode();
                        treeNode.setName(folderName);
                        treeNode = model.create(treeNode);
                        Workspace workspace = model.getDefaultWorkspace();
                        idPath = NodeUtils.createIdPath(workspace, treeNode);
                        model.addChild(workspace, treeNode);
                        addObjectsToFolder(treeNode, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.debug("Added to tree node {}", treeNode.getId());
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

                // TODO: we should only display object sets of a particular type (the type of the selected domain objects)
                NodeChooser nodeChooser = new NodeChooser(new UserViewRootNode(UserViewConfiguration.create(TreeNode.class)), "Choose folder to add to");
                nodeChooser.setRootVisible(false);
                
                int returnVal = nodeChooser.showDialog(explorer);
                if (returnVal != NodeChooser.CHOOSE_OPTION) return;
                if (nodeChooser.getChosenElements().isEmpty()) return;
                final UserViewTreeNodeNode selectedNode = (UserViewTreeNodeNode)nodeChooser.getChosenElements().get(0);
                final TreeNode treeNode = selectedNode.getTreeNode();
                SimpleWorker worker = new SimpleWorker() {

                    private Long[] idPath;
                
                    @Override
                    protected void doStuff() throws Exception {
                        idPath = NodeUtils.createIdPath(selectedNode);
                        addObjectsToFolder(treeNode, idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Added to tree node {}",treeNode.getId());
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
                final Long treeNodeId = idPath[idPath.length-1];

                TreeNode treeNode;
                try {
                    treeNode = model.getDomainObject(TreeNode.class, treeNodeId);
                    if (treeNode == null) continue;
                }
                catch (Exception e) {
                    log.error("Error getting recent folder with id "+treeNodeId,e);
                    continue;
                }

                final TreeNode finalTreeNode = treeNode;

                JMenuItem commonRootItem = new JMenuItem(finalTreeNode.getName());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {

                        SimpleWorker worker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                addObjectsToFolder(finalTreeNode, idPath);
                            }

                            @Override
                            protected void hadSuccess() {
                                log.info("Added to folder {}",finalTreeNode.getId());
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

                commonRootItem.setEnabled(ClientDomainUtils.hasWriteAccess(finalTreeNode));
                newFolderMenu.add(commonRootItem);
            }
        }

        return newFolderMenu;
    }
    
    private void addObjectsToFolder(TreeNode treeNode, Long[] idPath) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        // Add them to the given folder
        model.addChildren(treeNode, domainObjects);
        
        // Update history
        updateAddToSetHistory(idPath);                
    }
    
    private void updateAddToSetHistory(Long[] idPath) {
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
