package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.gui.editor.ObjectSetEditorPanel;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Allows the user to create new object sets, either in their default workspace, 
 * or underneath another existing tree node. Once the set is created, 
 * it is opened in the object set editor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "NewSetAction"
)
@ActionRegistration(
        displayName = "#CTL_NewSetAction"
)
@ActionReference(path = "Menu/File/New", position = 2)
@Messages("CTL_NewSetAction=Set")
public final class NewSetAction implements ActionListener {

    protected final Component mainFrame = SessionMgr.getMainFrame();
    
    private TreeNodeNode parentNode;
    
    public NewSetAction() {
    }
    
    public NewSetAction(TreeNodeNode parentNode) {
        this.parentNode = parentNode;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        
        if (parentNode==null) {
            // If there is no parent node specified, we'll just use the default workspace. 
            parentNode = explorer.getWorkspaceNode();
        }
        
        final String name = (String) JOptionPane.showInputDialog(mainFrame, "Set Name:\n",
                "Create new set", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(name)) {
            return;
        }
        
        final ObjectSet objectSet = new ObjectSet();
        objectSet.setName(name);

        // Save the set and select it in the explorer so that it opens
        SimpleWorker newSetWorker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                DomainDAO dao = DomainMgr.getDomainMgr().getDao();
                dao.save(SessionMgr.getSubjectKey(), objectSet);
                TreeNode parentFolder = parentNode.getTreeNode();
                dao.addChild(SessionMgr.getSubjectKey(), parentFolder, objectSet);
            }

            @Override
            protected void hadSuccess() {
                initView();
                final Long[] idPath = NodeUtils.createIdPath(parentNode, objectSet);
                explorer.refresh(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        explorer.select(idPath);
                        return null;
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
    
    private DomainListViewTopComponent initView() {
        DomainListViewTopComponent browser = DomainListViewTopComponent.getActiveInstance();
        if (browser==null) {
            browser = new DomainListViewTopComponent();
            browser.open();
            browser.requestActive();
        }
        browser.setEditorClass(ObjectSetEditorPanel.class);
        return browser;
    }
}
