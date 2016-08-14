package org.janelia.it.workstation.gui.browser.nb_action;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.TreeNodeCriteria;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.components.DomainListViewManager;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.components.ViewerUtils;
import org.janelia.it.workstation.gui.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * Allows the user to create a new filter with the current node as the ancestor context.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class SearchHereAction extends NodeAction {

    private final static SearchHereAction singleton = new SearchHereAction();
    public static SearchHereAction get() {
        return singleton;
    }

    private DomainObjectNode selected;

    private SearchHereAction() {
    }

    @Override
    public String getName() {
        return "Search Here";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected = null;
        if (activatedNodes.length!=1) return false;
        for(Node node : activatedNodes) {
            if (node instanceof TreeNodeNode || node instanceof FilterNode) {
                this.selected = (DomainObjectNode)node;
            }
        }
        return selected!=null;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {

        ActivityLogHelper.logUserAction("SearchHereAction.performAction");

        if (selected==null) return;

        Filter filter;
        if (selected instanceof TreeNodeNode) {
            TreeNode treeNode = ((TreeNodeNode)selected).getTreeNode();
            TreeNodeCriteria criteria = new TreeNodeCriteria();
            criteria.setTreeNodeName(treeNode.getName());
            criteria.setTreeNodeReference(Reference.createFor(treeNode));

            filter = new Filter();
            filter.addCriteria(criteria);
            // TODO: need "all" class
            filter.setSearchClass(FilterEditorPanel.DEFAULT_SEARCH_CLASS.getName());
        }
        else if (selected instanceof FilterNode) {
            FilterNode filterNode = (FilterNode)selected;
            filter = DomainUtils.cloneFilter(filterNode.getFilter());
            filter.setName(null);
        }
        else {
            throw new IllegalStateException("Cannot search in node of type "+selected.getClass());
        }

        DomainListViewTopComponent browser = initView();
        FilterEditorPanel editor = ((FilterEditorPanel)browser.getEditor());
        editor.loadDomainObject(filter, true, null);
    }
    
    private DomainListViewTopComponent initView() {
        final DomainListViewTopComponent browser = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
        browser.setEditorClass(FilterEditorPanel.class);
        browser.requestActive();
        return browser;
    }
}
