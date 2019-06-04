package org.janelia.workstation.browser.actions;

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
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.Action;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=420)
public class SearchHereBuilder implements ContextualActionBuilder {

    private static final SearchHereAction action = new SearchHereAction();

    private static final String ACTION_NAME = "Search Here";

    @Override
    public boolean isCompatible(Object obj) {
        // Only show search here if the user enables advanced features
        return (obj instanceof TreeNode || obj instanceof Filter);
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class SearchHereAction extends DomainObjectNodeAction {

        private DomainObject domainObject;

        @Override
        public String getName() {
            return ACTION_NAME;
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
            ContextualActionUtils.setVisible(this, domainObject!=null && !viewerContext.isMultiple());
        }

        @Override
        protected void executeAction() {
            searchHere(domainObject);
        }
    }

    private static void searchHere(DomainObject domainObject) {

        ActivityLogHelper.logUserAction("SearchHereBuilder.performAction");

        if (domainObject==null) return;

        Filter filter;
        if (domainObject instanceof Node) {
            Node treeNode = (Node)domainObject;
            TreeNodeCriteria criteria = new TreeNodeCriteria();
            criteria.setTreeNodeName(treeNode.getName());
            criteria.setTreeNodeReference(Reference.createFor(treeNode));
            filter = new Filter();
            filter.addCriteria(criteria);
            filter.setSearchClass(FilterEditorPanel.DEFAULT_SEARCH_CLASS.getName());
        }
        else if (domainObject instanceof Filter) {
            filter = DomainUtils.cloneFilter((Filter)domainObject);
            filter.setName(null);
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
