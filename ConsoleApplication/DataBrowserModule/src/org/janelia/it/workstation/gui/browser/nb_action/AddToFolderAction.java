package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
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
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
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
public class AddToFolderAction extends NodeAction implements Presenter.Popup {

    private final static Logger log = LoggerFactory.getLogger(AddToFolderAction.class);
    
    private static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    
    protected final Component mainFrame = SessionMgr.getMainFrame();

    private final static AddToFolderAction singleton = new AddToFolderAction();
    public static AddToFolderAction get() {
        return singleton;
    }
    
    private final List<Node> selected = new ArrayList<>();
    
    private AddToFolderAction() {
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        enable(activatedNodes);
        for(Node node : selected) {
            log.info("add to folder: "+node.getDisplayName()+" from "+node.getParentNode().getDisplayName());
        }
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            selected.add(node);
        }
        return selected.size()==activatedNodes.length;
    }
    
    @Override
    public JMenuItem getPopupPresenter() {

        if (selected.isEmpty()) return null;
        
        final DomainExplorerTopComponent explorer = (DomainExplorerTopComponent)WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);

        JMenu newFolderMenu = new JMenu("Add To Folder");

        JMenuItem createNewItem = new JMenuItem("Create New Top-Level Folder...");
        
        final DomainDAO dao = DomainMgr.getDomainMgr().getDao();
        final Workspace workspace = dao.getDefaultWorkspace(SessionMgr.getSubjectKey());
        
        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                // Add button clicked
                final String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
                        "Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if ((folderName == null) || (folderName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    
                    private TreeNode folder;

                    @Override
                    protected void doStuff() throws Exception {
                        folder = new TreeNode();
                        folder.setName(folderName);
                        dao.save(SessionMgr.getSubjectKey(), folder);
                        dao.addChild(SessionMgr.getSubjectKey(), workspace, folder);
                        addSelectedObjectsToCommonRoot(folder);
                    }

                    @Override
                    protected void hadSuccess() {
                        explorer.refresh();
                        // No need to update the UI, the event bus will get it done
                        log.debug("Added to folder {}",folder.getId());
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

                NodeChooser nodeChooser = new NodeChooser("Choose folder to add to", explorer.getRoot(), false);
                
                int returnVal = nodeChooser.showDialog(explorer);
                if (returnVal != NodeChooser.CHOOSE_OPTION) return;
                if (nodeChooser.getChosenElements().isEmpty()) return;
                final Node selectedNode = nodeChooser.getChosenElements().get(0);

                if (selectedNode instanceof TreeNodeNode) {
                    final TreeNode folder = ((TreeNodeNode)selectedNode).getTreeNode();
                    SimpleWorker worker = new SimpleWorker() {

                        @Override
                        protected void doStuff() throws Exception {
                            addSelectedObjectsToCommonRoot(folder);
                        }

                        @Override
                        protected void hadSuccess() {
                            explorer.refresh();
                            // No need to update the UI, the event bus will get it done
                            log.debug("Added to folder {}",folder.getId());
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

        List<Long> addHistory = (List<Long>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_ROOT_HISTORY);
        if (addHistory!=null && !addHistory.isEmpty()) {

            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            newFolderMenu.add(item);

            for (Long rootId : addHistory) {

                final TreeNode folder = (TreeNode)dao.getDomainObject(SessionMgr.getSubjectKey(), TreeNode.class, rootId);
                if (folder == null) continue;

                DomainUtils.hasWriteAccess(folder);

                JMenuItem commonRootItem = new JMenuItem(folder.getName());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {

                        SimpleWorker worker = new SimpleWorker() {
                            @Override
                            protected void doStuff() throws Exception {
                                addSelectedObjectsToCommonRoot(folder);
                            }

                            @Override
                            protected void hadSuccess() {
                                explorer.refresh();
                                // No need to update the UI, the event bus will get it done
                                log.debug("Added to folder {}",folder.getId());
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
    
    @Override
    public String getName() {
        return "";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }
    
    private void addSelectedObjectsToCommonRoot(TreeNode folder) throws Exception {
        // Update database
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Node node : selected) {
            DomainObjectNode selectedNode = (DomainObjectNode)node;
            domainObjects.add(selectedNode.getDomainObject());
        }
        DomainDAO dao = DomainMgr.getDomainMgr().getDao();
        dao.addChildren(SessionMgr.getSubjectKey(), folder, domainObjects);
        log.info("Added {} objects to folder {}",domainObjects.size(),folder.getName());
        // Update history
        updateAddToFolderHistory(folder);                
    }
    
    private void updateAddToFolderHistory(TreeNode folder) {
        List<Long> addHistory = (List<Long>)SessionMgr.getSessionMgr().getModelProperty(Browser.ADD_TO_ROOT_HISTORY);
        if (addHistory==null) {
            addHistory = new ArrayList<>();
        }
        if (addHistory.contains(folder.getId())) {
            return;
        }
        if (addHistory.size()>=MAX_ADD_TO_ROOT_HISTORY) {
            addHistory.remove(addHistory.size()-1);
        }
        addHistory.add(0, folder.getId());
        SessionMgr.getSessionMgr().setModelProperty(Browser.ADD_TO_ROOT_HISTORY, addHistory);
    }
}
