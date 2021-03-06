package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.gui.search.criteria.TreeNodeCriteria;
import org.janelia.model.domain.workspace.Node;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.workstation.browser.gui.options.BrowserOptions;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "Actions",
        id = "SearchHereAction"
)
@ActionRegistration(
        displayName = "#CTL_SearchHereAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 450, separatorAfter = 499)
})
@NbBundle.Messages("CTL_SearchHereAction=Search Here")
public class SearchHereAction extends BaseContextualNodeAction {

    private DomainObject domainObject;

    @Override
    protected void processContext() {
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            if (domainObject != null) {
                // Only show search here if the user enables advanced features
                boolean visible = BrowserOptions.getInstance().isShowSearchHere()
                        && (domainObject instanceof Node || domainObject instanceof Filter);
                setEnabledAndVisible(visible);
            }
        }
    }

    public void performAction() {
        searchHere(domainObject);
    }

    private static void searchHere(DomainObject domainObject) {

        ActivityLogHelper.logUserAction("SearchHereAction.performAction");

        if (domainObject==null) return;

        Filter filter;
        if (domainObject instanceof Node) {
            Node node = (Node)domainObject;
            TreeNodeCriteria criteria = new TreeNodeCriteria();
            criteria.setTreeNodeName(node.getName());
            criteria.setTreeNodeReference(Reference.createFor(node));
            filter = new Filter();
            filter.addCriteria(criteria);
            filter.setSearchClass(FilterEditorPanel.DEFAULT_SEARCH_CLASS.getName());
        }
        else if (domainObject instanceof Filter) {
            filter = DomainUtils.cloneFilter((Filter)domainObject);
        }
        else {
            throw new IllegalStateException("Cannot search in node of type "+domainObject.getClass());
        }

        DomainListViewTopComponent browser = initView();
        FilterEditorPanel editor = ((FilterEditorPanel)browser.getEditor());
        editor.resetState();
        editor.loadDomainObject(filter, true, null);
    }

    private static DomainListViewTopComponent initView() {
        final DomainListViewTopComponent browser = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
        browser.setEditorClass(FilterEditorPanel.class);
        browser.requestActive();
        return browser;
    }
}
