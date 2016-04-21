package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.components.DomainListViewManager;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.components.ViewerUtils;
import org.janelia.it.workstation.gui.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Allows the user to create new filters, either in their default workspace, 
 * or underneath another existing tree node. Once the filter is created, 
 * it is opened in the filter editor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "NewFilterAction"
)
@ActionRegistration(
        displayName = "#CTL_NewFilterAction"
)
@ActionReferences({
        @ActionReference(path = "Menu/File/New", position = 2),
        @ActionReference(path = "Shortcuts", name = "M-S")
})
@Messages("CTL_NewFilterAction=Filter")
public final class NewFilterAction implements ActionListener {

    private TreeNodeNode parentNode;
    
    public NewFilterAction() {
    }
    
    public NewFilterAction(TreeNodeNode parentNode) {
        this.parentNode = parentNode;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();
                    
        if (parentNode==null) {
            // If there is no parent node specified, we don't actually have to 
            // save a new filter. Just open up the editor:
            DomainListViewTopComponent browser = initView();
            FilterEditorPanel editor = ((FilterEditorPanel)browser.getEditor());
            editor.loadNewFilter();
            return;
        }
        
        // Since we're putting the filter under a parent, we need the name up front
        final String name = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
                        "Filter Name:\n", "Create new filter", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(name)) {
            return;
        }
        
        // Save the filter and select it in the explorer so that it opens 
        SimpleWorker newFilterWorker = new SimpleWorker() {

            private Filter filter;

            @Override
            protected void doStuff() throws Exception {
                filter = new Filter();
                filter.setName(name);
                filter.setSearchClass(FilterEditorPanel.DEFAULT_SEARCH_CLASS.getName());
                filter = model.save(filter);
                TreeNode parentFolder = parentNode.getTreeNode();
                model.addChild(parentFolder, filter);
            }

            @Override
            protected void hadSuccess() {
                initView();
                final Long[] idPath = NodeUtils.createIdPath(parentNode, filter);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        explorer.selectNodeByPath(idPath);
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        newFilterWorker.execute();
    }
    
    private DomainListViewTopComponent initView() {
        final DomainListViewTopComponent browser = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
        browser.setEditorClass(FilterEditorPanel.class);
        browser.requestActive();
        return browser;
    }
}
