package org.janelia.it.workstation.browser.nb_action;

import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.it.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.FilterNode;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.gui.search.criteria.TreeNodeCriteria;
import org.janelia.model.domain.workspace.Node;
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

    private AbstractDomainObjectNode<?> selected;

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
    protected boolean enable(org.openide.nodes.Node[] activatedNodes) {
        selected = null;
        if (activatedNodes.length!=1) return false;
        for(org.openide.nodes.Node node : activatedNodes) {
            if (node instanceof TreeNodeNode || node instanceof FilterNode) {
                this.selected = (AbstractDomainObjectNode<?>)node;
            }
        }
        return selected!=null;
    }

    @Override
    protected void performAction(org.openide.nodes.Node[] activatedNodes) {

        ActivityLogHelper.logUserAction("SearchHereAction.performAction");

        if (selected==null) return;

        Filter filter;
        if (selected instanceof TreeNodeNode) {
            Node treeNode = ((TreeNodeNode)selected).getNode();
            TreeNodeCriteria criteria = new TreeNodeCriteria();
            criteria.setTreeNodeName(treeNode.getName());
            criteria.setTreeNodeReference(Reference.createFor(treeNode));
            filter = new Filter();
            filter.addCriteria(criteria);
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
        editor.resetState();
        editor.loadDomainObject(filter, true, null);
    }
    
    private DomainListViewTopComponent initView() {
        final DomainListViewTopComponent browser = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
        browser.setEditorClass(FilterEditorPanel.class);
        browser.requestActive();
        return browser;
    }
}
