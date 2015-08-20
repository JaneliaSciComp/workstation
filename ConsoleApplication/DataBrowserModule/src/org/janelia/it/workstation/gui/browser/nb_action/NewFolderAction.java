package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Allows the user to create new folders, either in their default workspace, 
 * or underneath another existing tree node. Once the folder is created, it is
 * selected in the tree.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "NewFolderAction"
)
@ActionRegistration(
        displayName = "#CTL_NewFolderAction"
)
@ActionReference(path = "Menu/File/New", position = 2)
@Messages("CTL_NewFolderAction=Folder")
public final class NewFolderAction implements ActionListener {

    protected final Component mainFrame = SessionMgr.getMainFrame();
    
    private TreeNodeNode parentNode;
    
    public NewFolderAction() {
    }
    
    public NewFolderAction(TreeNodeNode parentNode) {
        this.parentNode = parentNode;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        
        if (parentNode==null) {
            // If there is no parent node specified, we'll just use the default workspace. 
            parentNode = explorer.getWorkspaceNode();
        }
        
        final String name = (String) JOptionPane.showInputDialog(mainFrame, "Folder Name:\n",
                "Create new folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(name)) {
            return;
        }
        
        final TreeNode folder = new TreeNode();
        folder.setName(name);

        // Save the set and select it in the explorer so that it opens
        SimpleWorker newSetWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                model.create(folder);
                TreeNode parentFolder = parentNode.getTreeNode();
                model.addChild(parentFolder, folder);
            }

            @Override
            protected void hadSuccess() {
                final Long[] idPath = NodeUtils.createIdPath(parentNode, folder);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        explorer.select(idPath);
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        newSetWorker.execute();
    }
}
