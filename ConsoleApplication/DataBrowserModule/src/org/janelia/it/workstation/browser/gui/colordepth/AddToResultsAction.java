package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.janelia.it.workstation.browser.nb_action.NodePresenterAction;
import org.janelia.it.workstation.browser.nodes.GroupedFolderNode;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.UserViewConfiguration;
import org.janelia.it.workstation.browser.nodes.UserViewRootNode;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.workspace.ProxyGroup;
import org.janelia.model.domain.workspace.GroupedFolder;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to add the currently selected matches to a curated result set.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AddToResultsAction extends NodePresenterAction {

    private final static Logger log = LoggerFactory.getLogger(AddToResultsAction.class);

    protected final Component mainFrame = ConsoleApp.getMainFrame();

    private final static AddToResultsAction singleton = new AddToResultsAction();
    public static AddToResultsAction get() {
        return singleton;
    }

    protected AddToResultsAction() {
    }

    private Collection<DomainObject> domainObjects = new ArrayList<>();
    private ColorDepthMask mask;
    
    public void setMask(ColorDepthMask mask) {
        this.mask = mask;
    }

    public void setDomainObjects(Collection<? extends DomainObject> domainObjectList) {
        domainObjects.clear();
        domainObjects.addAll(domainObjectList);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        String name = domainObjects.size() > 1 ? "Add " + domainObjects.size() + " Items To Folder" : "Add To Folder";
        JMenu newFolderMenu = new JMenu(name);

        JMenuItem createNewItem = new JMenuItem("Create New Result Set...");
        
        createNewItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                ActivityLogHelper.logUserAction("AddToResultsAction.createNewResultSet");

                // Add button clicked
                final String folderName = (String) JOptionPane.showInputDialog(mainFrame, "Result Set Name:\n",
                        "Create new result set", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if ((folderName == null) || (folderName.length() <= 0)) {
                    return;
                }

                SimpleWorker worker = new SimpleWorker() {
                    
                    private GroupedFolder folder;
                    private Long[] idPath;

                    @Override
                    protected void doStuff() throws Exception {
                        folder = new GroupedFolder();
                        folder.setName(folderName);
                        folder = model.save(folder);
                        Workspace workspace = model.getDefaultWorkspace();
                        idPath = NodeUtils.createIdPath(workspace, folder);
                        model.addChild(workspace, folder);
                        addUniqueItemsToFolder(folder, idPath);
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
                
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating result set...", ""));
                worker.execute();
            }
        });

        newFolderMenu.add(createNewItem);

        JMenuItem chooseItem = new JMenuItem("Choose Result Set...");

        chooseItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

                ActivityLogHelper.logUserAction("AddToResultsAction.chooseResultSet");

                TreeNodeChooser nodeChooser = new TreeNodeChooser(new UserViewRootNode(
                        UserViewConfiguration.create(TreeNode.class, GroupedFolder.class)), 
                        "Choose result set to add to", true) {
                    @Override
                    protected boolean allowChoose(Node selectedNode) {
                        return selectedNode instanceof GroupedFolderNode;
                    }
                };
                nodeChooser.setRootVisible(false);
                
                int returnVal = nodeChooser.showDialog(explorer);
                if (returnVal != TreeNodeChooser.CHOOSE_OPTION) return;
                if (nodeChooser.getChosenElements().isEmpty()) return;
                Node node = nodeChooser.getChosenElements().get(0);
                if (node instanceof GroupedFolderNode) {
                    final GroupedFolderNode selectedNode = (GroupedFolderNode)nodeChooser.getChosenElements().get(0);
                    final GroupedFolder folder = selectedNode.getGroupedFolder();
                    addUniqueItemsToFolder(folder, NodeUtils.createIdPath(selectedNode));
                }
                else {
                    // TODO: we need a better way to prevent the user from selecting a node of the wrong type
                    throw new IllegalArgumentException("Chosen item was not a grouped folder");
                }
            }
        });

        newFolderMenu.add(chooseItem);
        newFolderMenu.addSeparator();

        List<String> addHistory = StateMgr.getStateMgr().getAddToFolderHistory();
        if (addHistory!=null && !addHistory.isEmpty()) {

            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            newFolderMenu.add(item);

            for (String path : addHistory) {

                if (path.contains("#")) {
                    log.warn("Ignoring reference in add history: "+path);
                    continue;
                }
                
                final Long[] idPath = NodeUtils.createIdPath(path);
                final Long folderId = idPath[idPath.length-1];
                
                GroupedFolder folder;
                try {
                    folder = model.getDomainObject(GroupedFolder.class, folderId);
                    if (folder == null) continue;
                }
                catch (Exception e) {
                    log.error("Error getting recent folder with id "+folderId,e);
                    continue;
                }

                final GroupedFolder finalFolder = folder;

                JMenuItem commonRootItem = new JMenuItem(folder.getName());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        ActivityLogHelper.logUserAction("AddToResultsAction.recentResultSet", folderId);
                        addUniqueItemsToFolder(finalFolder, idPath);
                    }
                });

                commonRootItem.setEnabled(ClientDomainUtils.hasWriteAccess(finalFolder));
                newFolderMenu.add(commonRootItem);
            }
        }

        return newFolderMenu;
    }

    private void addUniqueItemsToFolder(final GroupedFolder groupedFolder, final Long[] idPath) {

        Reference groupRef = Reference.createFor(mask);
        DomainModel model = DomainMgr.getDomainMgr().getModel();

        SimpleWorker worker = new SimpleWorker() {
            
            List<ProxyGroup> resultGroups; 
            
            @Override
            protected void doStuff() throws Exception {
                resultGroups = model.getDomainObjectsAs(ProxyGroup.class, groupedFolder.getChildren());
            }

            @Override
            protected void hadSuccess() {

                int existing = 0;
                ProxyGroup resultGroup = getGroupByProxy(resultGroups, groupRef);
                if (resultGroup!=null) {
                    for(DomainObject domainObject : domainObjects) {
                        if (resultGroup.hasChild(domainObject)) {
                            existing++;
                        }
                    }
                }

                if (existing>0) {
                    String message;
                    if (existing>domainObjects.size()) {
                        message = "All items are already in the target result set, no items will be added.";
                    }
                    else {
                        message = existing + " items are already in the target result set. "+(domainObjects.size()-existing)+" item(s) will be added.";
                    }
                    int result = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), 
                            message, "Items already present", JOptionPane.OK_CANCEL_OPTION);
                    if (result != 0) {
                        return;
                    }
                }

                final int numAdded = domainObjects.size()-existing;

                SimpleWorker worker = new SimpleWorker() {
                    
                    ProxyGroup group = resultGroup;
                    
                    @Override
                    protected void doStuff() throws Exception {

                        if (group==null) {
                            group = new ProxyGroup();
                            group.setName("Mask Result Set");
                            group.setProxyObject(groupRef);
                            group.setChildren(DomainUtils.getReferences(domainObjects));
                            group = model.save(group);
                            model.addChild(groupedFolder, group);
                        }
                        
                        model.addChildren(group, domainObjects);

                        updateHistory(idPath);
                    }

                    @Override
                    protected void hadSuccess() {
                        log.info("Added {} items to {} in {}", numAdded, group, groupedFolder);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        ConsoleApp.handleException(error);
                    }
                };
                worker.execute();
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        worker.execute();
    }

    private ProxyGroup getGroupByProxy(List<ProxyGroup> groups, Reference proxyObjectRef) {
        for (ProxyGroup resultGroup : groups) {
            if (resultGroup.getProxyObject().equals(proxyObjectRef)) {
                return resultGroup;
            }
        }
        return null;
    }
    
    protected void updateHistory(final Long[] idPath) throws Exception {
        // Update history
        String pathString = NodeUtils.createPathString(idPath);
        StateMgr.getStateMgr().updateAddToFolderHistory(pathString);
    }
}
