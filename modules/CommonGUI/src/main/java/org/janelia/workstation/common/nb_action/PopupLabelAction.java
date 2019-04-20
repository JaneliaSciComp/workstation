package org.janelia.workstation.common.nb_action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.nodes.Node;

/**
 * Not really an action, just the disabled top label on a pop-up menu.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class PopupLabelAction extends NodePresenterAction implements ViewerContextReceiver {

    private final static PopupLabelAction singleton = new PopupLabelAction();
    public static PopupLabelAction get() {
        return singleton;
    }

    private PopupLabelAction() {
    }

    private Collection<DomainObject> domainObjects = new ArrayList<>();

    @Override
    protected boolean enable(Node[] activatedNodes) {
        super.enable(activatedNodes);
        return false;
    }

    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        domainObjects.clear();
        domainObjects.addAll(viewerContext.getDomainObjectList());
    }

    @Override
    public String getName() {
        List<Node> selected = getSelectedNodes();

        if (selected.isEmpty()) {
            return "(Nothing selected)";
        }

        if (selected.size()>1) {
            return "(Multiple selected)";
        }

        Node node = selected.get(0);
        return StringUtils.abbreviate(node.getDisplayName(), 50);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return ContextualActionUtils.getNamedActionItem(getName(), e -> {});
    }
}
