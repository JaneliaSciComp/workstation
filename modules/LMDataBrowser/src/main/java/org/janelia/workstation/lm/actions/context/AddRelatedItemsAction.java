package org.janelia.workstation.lm.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.model.domain.sample.SamplePipelineRun;
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
import org.janelia.workstation.core.model.DomainObjectMapper;
import org.janelia.workstation.core.model.MappingType;
import org.janelia.workstation.core.model.RecentFolder;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

@ActionID(
        category = "actions",
        id = "AddRelatedItemsAction"
)
@ActionRegistration(
        displayName = "#CTL_AddRelatedItemsAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 150),
})
@NbBundle.Messages("CTL_AddRelatedItemsAction=Add Related Items To Folder")
public class AddRelatedItemsAction extends BaseContextualPopupAction {

    private final static Logger log = LoggerFactory.getLogger(AddRelatedItemsAction.class);
    private final static String LATEST = "Latest";
    private final static Component mainFrame = FrameworkAccess.getMainFrame();

    private Collection<DomainObject> domainObjects;
    private Collection<MappingType> mappableTypes;

    @Override
    protected void processContext() {
        this.domainObjects = getNodeContext().getOnlyObjectsOfType(DomainObject.class);
        DomainObjectMapper mapper = new DomainObjectMapper(domainObjects);
        this.mappableTypes = mapper.getMappableTypes();
        setEnabledAndVisible(!mappableTypes.isEmpty());
    }
    @Override
    protected List<JComponent> getItems() {

        List<JComponent> items = new ArrayList<>();

        if (!isVisible()) return items;
        if (domainObjects==null || mappableTypes==null) return items;

        for (final MappingType targetType : mappableTypes) {
            items.add(createClassMenu(domainObjects, targetType));
        }

        return items;
    }

    private JMenu createClassMenu(Collection<DomainObject> domainObjects, MappingType targetType) {

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();

        JMenu classMenu = new JMenu(targetType.getLabel());
        JMenuItem createNewItem = new JMenuItem("Create New Folder...");

        createNewItem.addActionListener(actionEvent -> {

            ActivityLogHelper.logUserAction("AddToFolderAction.createNewFolder");

            // Add button clicked
            final String folderName = (String) JOptionPane.showInputDialog(
                    mainFrame, "Folder Name:\n",
                    "Create new folder in workspace", JOptionPane.PLAIN_MESSAGE, null, null, null);
            if ((folderName == null) || (folderName.length() <= 0)) {
                return;
            }

            SimpleWorker worker = new SimpleWorker() {

                private TreeNode folder;
                private Long[] idPath;

                @Override
                protected void doStuff() throws Exception {
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    folder = new TreeNode();
                    folder.setName(folderName);
                    folder = model.create(folder);
                    Workspace workspace = model.getDefaultWorkspace();
                    idPath = NodeUtils.createIdPath(workspace, folder);
                    model.addChild(workspace, folder);
                    addUniqueItemsToFolder(domainObjects, folder, idPath, targetType);
                }

                @Override
                protected void hadSuccess() {
                    log.debug("Added selected items to folder {}",folder.getId());
                    SwingUtilities.invokeLater(() -> {
                        explorer.expand(idPath);
                        explorer.selectNodeByPath(idPath);
                    });
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Creating folder...", ""));
            worker.execute();
        });

        classMenu.add(createNewItem);

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

            addUniqueItemsToFolder(domainObjects, folder, NodeUtils.createIdPath(selectedNode), targetType);
        });

        classMenu.add(chooseItem);
        classMenu.addSeparator();

        List<RecentFolder> addHistory = DataBrowserMgr.getDataBrowserMgr().getAddToFolderHistory();
        if (addHistory!=null && !addHistory.isEmpty()) {

            JMenuItem item = new JMenuItem("Recent:");
            item.setEnabled(false);
            classMenu.add(item);

            for (RecentFolder recentFolder : addHistory) {

                String path = recentFolder.getPath();
                if (path.contains("#")) {
                    log.warn("Ignoring reference in add history: "+path);
                    continue;
                }

                final Long[] idPath = NodeUtils.createIdPath(path);
                final Long folderId = idPath[idPath.length-1];

                JMenuItem commonRootItem = new JMenuItem(recentFolder.getLabel());
                commonRootItem.addActionListener(actionEvent -> {
                    ActivityLogHelper.logUserAction("AddToFolderAction.recentFolder", folderId);
                    addUniqueItemsToFolder(domainObjects, folderId, idPath, targetType);
                });

                classMenu.add(commonRootItem);
            }
        }

        return classMenu;
    }

    private void addUniqueItemsToFolder(Collection<DomainObject> domainObjects, Long folderId, Long[] idPath, final MappingType targetType) {

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
                    addUniqueItemsToFolder(domainObjects, treeNode, idPath, targetType);
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

    private void addUniqueItemsToFolder(Collection<DomainObject> domainObjects, final TreeNode treeNode, final Long[] idPath, final MappingType targetType) {

        final DomainObjectMapper mapper = new DomainObjectMapper(domainObjects);

        SimpleWorker worker = new SimpleWorker() {

            private int existing;
            private int numAdded;

            @Override
            protected void doStuff() throws Exception {

                // Map the items first
                List<DomainObject> mapped = mapper.map(targetType);
                log.info("Mapped {} objects to {} objects of type {}", domainObjects.size(), mapped.size(), targetType.getLabel());

                existing = 0;
                for(DomainObject domainObject : mapped) {
                    if (treeNode.hasChild(domainObject)) {
                        existing++;
                    }
                }

                addItemsToFolder(treeNode, idPath, targetType, mapped);
                numAdded = mapped.size()-existing;
            }

            @Override
            protected void hadSuccess() {

                if (existing>0) {
                    String message;
                    if (existing==domainObjects.size()) {
                        message = "All items are already in the target folder, no items will be added.";
                    }
                    else {
                        message = existing + " items are already in the target folder. "+(domainObjects.size()-existing)+" item(s) will be added.";
                    }

                    log.info("message");
                    int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                            message, "Items already present", JOptionPane.OK_CANCEL_OPTION);
                    if (result != 0) {
                        return;
                    }
                }

                log.info("Added {} items to folder {}", numAdded, treeNode.getId());
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Adding items to folder...", ""));

        if (targetType==MappingType.AlignedNeuronFragment) {

            // We need to first ask the user which alignment space to use

            SimpleWorker worker2 = new SimpleWorker() {

                private String alignmentSpace;

                @Override
                protected void doStuff() throws Exception {

                    // Map the fragments to samples first
                    List<Sample> samples = mapper.map(MappingType.Sample);

                    Set<String> alignmentSpaces = new HashSet<>();

                    for (Sample sample : samples) {
                        for (ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                            for (SamplePipelineRun samplePipelineRun : objectiveSample.getPipelineRuns()) {
                                for (SampleAlignmentResult sampleAlignmentResult : samplePipelineRun.getAlignmentResults()) {
                                    alignmentSpaces.add(sampleAlignmentResult.getAlignmentSpace());
                                }
                            }
                        }
                    }

                    List<String> alignmentSpacesList = new ArrayList<>();
                    alignmentSpacesList.add(LATEST);
                    alignmentSpacesList.addAll(alignmentSpaces);
                    String[] values = alignmentSpacesList.toArray(new String[alignmentSpacesList.size()]);

                    alignmentSpace = (String)JOptionPane.showInputDialog(
                            FrameworkAccess.getMainFrame(),
                            "Choose an alignment space",
                            "Choose alignment space",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            values, values[0]);
                }

                @Override
                protected void hadSuccess() {
                    if (alignmentSpace==null) return;
                    if (!LATEST.equals(alignmentSpace)) {
                        mapper.setAlignmentSpace(alignmentSpace);
                    }
                    worker.execute();
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            worker2.execute();
        }
        else {
            worker.execute();
        }
    }

    private <T extends DomainObject> void addItemsToFolder(final TreeNode treeNode, final Long[] idPath, MappingType targetType, List<? extends DomainObject> objects) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();

        // Add them to the given folder
        model.addChildren(treeNode, objects);

        // Update history
        String pathString = NodeUtils.createPathString(idPath);
        DataBrowserMgr.getDataBrowserMgr().updateAddToFolderHistory(new RecentFolder(pathString, treeNode.getName()));

        log.info("Added {} objects to folder '{}' ({})", objects.size(), treeNode.getName(), treeNode);
    }
}