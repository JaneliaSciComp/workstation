package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.workstation.integration.util.FrameworkAccess;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.common.nodes.TreeNodeNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.workspace.Node;

public final class NewFilterActionListener implements ActionListener {

    private final TreeNodeNode parentNode;
    private final Class<?> searchClass;
    private final String searchString;
    
    public NewFilterActionListener() {
        this.searchString = null;
        this.searchClass = FilterEditorPanel.DEFAULT_SEARCH_CLASS;
        this.parentNode = null;
    }

    public NewFilterActionListener(String searchString, Class<?> searchClass) {
        this.searchString = searchString;
        this.searchClass = searchClass == null ? FilterEditorPanel.DEFAULT_SEARCH_CLASS : searchClass;
        this.parentNode = null;
    }
    
    public NewFilterActionListener(TreeNodeNode parentNode) {
        this.searchString = null;
        this.searchClass = FilterEditorPanel.DEFAULT_SEARCH_CLASS;
        this.parentNode = parentNode;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewFilterActionListener.actionPerformed");

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        if (parentNode==null) {
            Filter filter = createUnsavedFilter(null);
            // If there is no parent node specified, we don't actually have to
            // save a new filter. Just open up the editor:
            DomainListViewTopComponent browser = initView();
            browser.loadDomainObject(filter, true);
            return;
        }

        // Since we're putting the filter under a parent, we need the name up front
        final String name = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(),
                "Filter Name:\n", "Create new filter", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(name)) {
            return;
        }

        // Save the filter and select it in the explorer so that it opens
        SimpleWorker newFilterWorker = new SimpleWorker() {

            private Filter filter;

            @Override
            protected void doStuff() throws Exception {
                filter = createUnsavedFilter(name);
                filter = model.save(filter);
                Node parentFolder = parentNode.getNode();
                model.addChild(parentFolder, filter);
            }

            @Override
            protected void hadSuccess() {
                initView();
                final Long[] idPath = NodeUtils.createIdPath(parentNode, filter);
                SwingUtilities.invokeLater(() -> explorer.selectAndNavigateNodeByPath(idPath));
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        newFilterWorker.execute();
    }

    private Filter createUnsavedFilter(String name) {
        Filter filter = FilterEditorPanel.createUnsavedFilter(searchClass, name);
        if (searchString!=null) {
            filter.setSearchString(searchString);
        }
        return filter;
    }

    private DomainListViewTopComponent initView() {
        final DomainListViewTopComponent browser = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
        browser.setEditorClass(FilterEditorPanel.class);
        browser.requestActive();
        return browser;
    }
}
