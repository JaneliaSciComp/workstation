package org.janelia.workstation.browser.actions.context;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.browser.gui.support.TreeNodeChooser;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.common.nodes.UserViewConfiguration;
import org.janelia.workstation.common.nodes.UserViewRootNode;
import org.janelia.workstation.common.nodes.UserViewTreeNodeNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.model.RecentFolder;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to move currently selected nodes to a folder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "AddToFolderAction"
)
@ActionRegistration(
        displayName = "#CTL_AddToFolderAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 140)
})
@NbBundle.Messages("CTL_AddToFolderAction=Add to Folder")
public class AddToFolderAction extends BaseContextualPopupAction {

    private final static Logger log = LoggerFactory.getLogger(AddToFolderAction.class);
    private final Component mainFrame = FrameworkAccess.getMainFrame();

    public static AddToFolderAction get() {
        return SystemAction.get(AddToFolderAction.class);
    }

    private Collection<DomainObject> domainObjects = new ArrayList<>();

    @Override
    protected void processContext() {
        domainObjects.clear();
        if (getNodeContext().isOnlyObjectsOfType(DomainObject.class)
                && !getNodeContext().isSingleObjectOfType(Ontology.class)) { // exclude ontologies from being added to folders
            domainObjects.addAll(getNodeContext().getOnlyObjectsOfType(DomainObject.class));
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Add " + domainObjects.size() + " Items To Folder" : "Add To Folder";
    }

    @Override
    protected List<JComponent> getItems() {
        Collection<DomainObject> domainObjects = new ArrayList<>(this.domainObjects);

        List<JComponent> items = new ArrayList<>();

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        JMenuItem createNewItem = new JMenuItem("Create New Folder...");
        
        Consumer<Long[]> success = idPath -> SwingUtilities.invokeLater(() -> {
            explorer.expand(idPath);
            explorer.selectNodeByPath(idPath);
        });
        
        createNewItem.addActionListener(actionEvent -> {

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
                    log.info("Created new folder: {}", folder);
                    Workspace workspace = model.getDefaultWorkspace();
                    idPath = NodeUtils.createIdPath(workspace, folder);
                    workspace = model.addChild(workspace, folder);
                    log.info("Added new folder to {}", workspace);
                }

                @Override
                protected void hadSuccess() {
                    addUniqueItemsToFolder(domainObjects, folder, idPath, success);
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating folder...", ""));
            worker.execute();
        });

        items.add(createNewItem);

        JMenuItem chooseItem = new JMenuItem("Choose Folder...");

        chooseItem.addActionListener(actionEvent -> {

            ActivityLogHelper.logUserAction("AddToFolderAction.chooseFolder");

            TreeNodeChooser nodeChooser = new TreeNodeChooser(new UserViewRootNode(UserViewConfiguration.create(TreeNode.class)), "Choose folder to add to", true);
            nodeChooser.setRootVisible(false);

            int returnVal = nodeChooser.showDialog(explorer);
            if (returnVal != TreeNodeChooser.CHOOSE_OPTION) return;
            if (nodeChooser.getChosenElements().isEmpty()) return;
            final UserViewTreeNodeNode selectedNode = (UserViewTreeNodeNode)nodeChooser.getChosenElements().get(0);
            final TreeNode folder = selectedNode.getTreeNode();

            addUniqueItemsToFolder(domainObjects, folder, NodeUtils.createIdPath(selectedNode), success);
        });

        items.add(chooseItem);
        items.add(new JSeparator());

        List<RecentFolder> addHistory = DataBrowserMgr.getDataBrowserMgr().getAddToFolderHistory();
        if (addHistory!=null && !addHistory.isEmpty()) {

            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            items.add(item);

            for (RecentFolder recentFolder : addHistory) {

                String path = recentFolder.getPath();
                if (path.contains("#")) {
                    log.warn("Ignoring reference in add history: "+path);
                    continue;
                }
                
                final Long[] idPath = NodeUtils.createIdPath(path);
                final Long folderId = idPath[idPath.length-1];
                
                JMenuItem commonRootItem = new JMenuItem(recentFolder.getLabel());
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        ActivityLogHelper.logUserAction("AddToFolderAction.recentFolder", folderId);
                        addUniqueItemsToFolder(domainObjects, folderId, idPath, success);
                    }
                });

                items.add(commonRootItem);
            }
        }

        return items;
    }

    private void addUniqueItemsToFolder(Collection<DomainObject> domainObjects, Long folderId, Long[] idPath, Consumer<Long[]> success) {

        SimpleWorker worker = new SimpleWorker() {
            
            private TreeNode treeNode;
            
            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                treeNode = model.getDomainObject(TreeNode.class, folderId);
                
            }

            @Override
            protected void hadSuccess() {
                if (treeNode==null) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "This folder no longer exists.", "Folder no longer exists", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    addUniqueItemsToFolder(domainObjects, treeNode, idPath, success);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Adding items to folder...", ""));
        worker.execute();
        
    }
    
    private void addUniqueItemsToFolder(Collection<DomainObject> domainObjects, TreeNode treeNode, Long[] idPath, Consumer<Long[]> success) {

        int existing = 0;
        for(DomainObject domainObject : domainObjects) {
            if (treeNode.hasChild(domainObject)) {
                existing++;
            }
        }

        if (existing>0) {
            String message;
            if (existing==domainObjects.size()) {
                message = "All items are already in the target folder, no items will be added.";
            }
            else {
                message = existing + " items are already in the target folder. "+(domainObjects.size()-existing)+" item(s) will be added.";
            }
            int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                    message, "Items already present", JOptionPane.OK_CANCEL_OPTION);
            if (result != 0) {
                return;
            }
        }

        int numAdded = domainObjects.size()-existing;

        SimpleWorker worker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                addItemsToFolder(domainObjects, treeNode, idPath);
            }

            @Override
            protected void hadSuccess() {
                log.info("Added {} items to folder {}", numAdded, treeNode.getId());
                if (success!=null) {
                    try {
                        success.accept(idPath);
                    }
                    catch (Exception e) {
                        FrameworkAccess.handleException(e);
                    }
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Adding items to folder...", ""));
        worker.execute();
    }

    protected void addItemsToFolder(Collection<DomainObject> domainObjects, TreeNode treeNode, Long[] idPath) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();

        // Add them to the given folder
        model.addChildren(treeNode, domainObjects);

        // Update history
        String pathString = NodeUtils.createPathString(idPath);
        DataBrowserMgr.getDataBrowserMgr().updateAddToFolderHistory(new RecentFolder(pathString, treeNode.getName()));
    }
}
