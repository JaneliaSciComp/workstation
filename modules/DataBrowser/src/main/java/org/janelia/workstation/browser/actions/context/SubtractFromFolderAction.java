package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.browser.gui.support.TreeNodeChooser;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.nodes.UserViewConfiguration;
import org.janelia.workstation.common.nodes.UserViewRootNode;
import org.janelia.workstation.common.nodes.UserViewTreeNodeNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
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
import java.util.HashSet;

/**
 * Remove items from the current folder by selecting another folder as the set to remove.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "SubtractFromFolderAction"
)
@ActionRegistration(
        displayName = "#CTL_SubtractFromFolderAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 410)
})
@NbBundle.Messages("CTL_SubtractFromFolderAction=Subtract items from this folder")
public class SubtractFromFolderAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(SubtractFromFolderAction.class);
    private final Component mainFrame = FrameworkAccess.getMainFrame();

    private Node nodeToRemoveFrom;

    @Override
    protected void processContext() {
        this.nodeToRemoveFrom = null;
        if (getNodeContext().isSingleObjectOfType(Node.class)) {
            this.nodeToRemoveFrom = getNodeContext().getSingleObjectOfType(Node.class);
            log.info("Node selected: "+nodeToRemoveFrom);
            setEnabledAndVisible(true);
        }
        else {
            log.info("Not node selected: "+getNodeContext());
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        ActivityLogHelper.logUserAction("SubtractFromFolderAction.chooseFolder");

        TreeNodeChooser nodeChooser = new TreeNodeChooser(new UserViewRootNode(UserViewConfiguration.create(TreeNode.class)), "Choose a folder to subtract from this folder", true);
        nodeChooser.setRootVisible(false);

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        int returnVal = nodeChooser.showDialog(explorer);
        if (returnVal != TreeNodeChooser.CHOOSE_OPTION) return;
        if (nodeChooser.getChosenElements().isEmpty()) return;
        final UserViewTreeNodeNode selectedNode = (UserViewTreeNodeNode)nodeChooser.getChosenElements().get(0);
        final TreeNode removalSet = selectedNode.getTreeNode();

        HashSet<Reference> referencesToRemove = new HashSet<>(removalSet.getChildren());

        if (referencesToRemove.size() == 0) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Selected folder has no children");
            return;
        }

        log.info("Subtracting {} from {}", removalSet, nodeToRemoveFrom);

        SimpleWorker worker = new SimpleWorker() {

            int countBefore;
            int countAfter;

            @Override
            protected void doStuff() throws Exception {
                countBefore = nodeToRemoveFrom.getNumChildren();
                nodeToRemoveFrom.getChildren().removeIf(referencesToRemove::contains);
                DomainMgr.getDomainMgr().getModel().save(nodeToRemoveFrom);
                countAfter = nodeToRemoveFrom.getNumChildren();
            }

            @Override
            protected void hadSuccess() {
                int countRemoved = countBefore - countAfter;
                log.info("before:{}, after:{}, subtracted:{}", countBefore, countAfter, countRemoved);
                if (countRemoved < 0) {
                    throw new IllegalStateException("SubtractFromFolderAction failed: countBefore="+countBefore+", countAfter="+countAfter);
                }
                else if (countRemoved == 0) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "No items in '"+removalSet+"' were found in '"+ nodeToRemoveFrom.getName()+"'");
                }
                else {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "Removed "+countRemoved+" items from folder '"+ nodeToRemoveFrom.getName()+"'");
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Subtracting from folder...", ""));
        worker.execute();
    }
}
