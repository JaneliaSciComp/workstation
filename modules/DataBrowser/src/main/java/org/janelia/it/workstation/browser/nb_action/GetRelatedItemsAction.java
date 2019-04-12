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

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.model.DomainObjectMapper;
import org.janelia.it.workstation.browser.model.MappingType;
import org.janelia.it.workstation.browser.model.RecentFolder;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.gui.support.TreeNodeChooser;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.UserViewConfiguration;
import org.janelia.it.workstation.browser.nodes.UserViewRootNode;
import org.janelia.it.workstation.browser.nodes.UserViewTreeNodeNode;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.model.domain.sample.SamplePipelineRun;
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

    private final static String LATEST = "Latest";
    
    protected final Component mainFrame = FrameworkImplProvider.getMainFrame();

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

        String name = "Add Related Items To Folder";
        JMenu getRelatedMenu = new JMenu(name);
        
        DomainObjectMapper mapper = new DomainObjectMapper(domainObjects);
        Collection<MappingType> mappableTypes = mapper.getMappableTypes();
        if (mappableTypes.isEmpty()) {
            return null;
        }
        
        for (final MappingType targetType : mappableTypes) {
            getRelatedMenu.add(createClassMenu(targetType));
        }
        
        return getRelatedMenu;
    }
    
    private JMenu createClassMenu(MappingType targetType) {
        
        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        
        JMenu classMenu = new JMenu(targetType.getLabel());

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
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        folder = new TreeNode();
                        folder.setName(folderName);
                        folder = model.create(folder);
                        Workspace workspace = model.getDefaultWorkspace();
                        idPath = NodeUtils.createIdPath(workspace, folder);
                        model.addChild(workspace, folder);
                        addUniqueItemsToFolder(folder, idPath, targetType);
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
                        FrameworkImplProvider.handleException(error);
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
                
                addUniqueItemsToFolder(folder, NodeUtils.createIdPath(selectedNode), targetType);
            }
        });

        classMenu.add(chooseItem);
        classMenu.addSeparator();

        List<RecentFolder> addHistory = StateMgr.getStateMgr().getAddToFolderHistory();
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
                commonRootItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        ActivityLogHelper.logUserAction("AddToFolderAction.recentFolder", folderId);
                        addUniqueItemsToFolder(folderId, idPath, targetType);
                    }
                });

                classMenu.add(commonRootItem);
            }
        }
        
        return classMenu;
    }

    private void addUniqueItemsToFolder(Long folderId, Long[] idPath, final MappingType targetType) {

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
                    JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(), "This folder no longer exists.", "Folder no longer exists", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    addUniqueItemsToFolder(treeNode, idPath, targetType);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Adding items to folder...", ""));
        worker.execute();
        
    }
    
    private void addUniqueItemsToFolder(final TreeNode treeNode, final Long[] idPath, final MappingType targetType) {

        final DomainObjectMapper mapper = new DomainObjectMapper(domainObjects);

        SimpleWorker worker = new SimpleWorker() {

            private int existing;
            private int numAdded;

            @Override
            protected void doStuff() throws Exception {
                
                // Map the items first
                List<DomainObject> mapped = mapper.map(targetType, DomainObject.class);
                
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

                    int result = JOptionPane.showConfirmDialog(FrameworkImplProvider.getMainFrame(),
                            message, "Items already present", JOptionPane.OK_CANCEL_OPTION);
                    if (result != 0) {
                        return;
                    }
                }
                
                log.info("Added {} items to folder {}", numAdded, treeNode.getId());
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkImplProvider.handleException(error);
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
                    List<Sample> samples = mapper.map(MappingType.Sample, Sample.class);
                    
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
                            FrameworkImplProvider.getMainFrame(),
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
                    FrameworkImplProvider.handleException(error);
                }
            };

            worker2.execute();
        }
        else {
            worker.execute();
        }
    }

    protected <T extends DomainObject> void addItemsToFolder(final TreeNode treeNode, final Long[] idPath, MappingType targetType, List<? extends DomainObject> objects) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        
        // Add them to the given folder
        model.addChildren(treeNode, objects);

        // Update history
        String pathString = NodeUtils.createPathString(idPath);
        StateMgr.getStateMgr().updateAddToFolderHistory(new RecentFolder(pathString, treeNode.getName()));
    }
        
}
