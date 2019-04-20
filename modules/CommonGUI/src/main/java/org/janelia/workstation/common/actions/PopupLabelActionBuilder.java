package org.janelia.workstation.common.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.common.nb_action.NodePresenterAction;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * First item in a popup list is a label describing the context.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=1)
public class PopupLabelActionBuilder implements ContextualActionBuilder {

    private static final PopupLabelAction action = new PopupLabelAction();
    private static final PopupLabelNodeAction nodeAction = new PopupLabelNodeAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return nodeAction;
    }

    private static class PopupLabelAction extends AbstractAction implements ViewerContextReceiver {

        private Collection<DomainObject> domainObjects = new ArrayList<>();

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            domainObjects.clear();
            domainObjects.addAll(viewerContext.getDomainObjectList());

            if (domainObjects.isEmpty()) {
                ContextualActionUtils.setName(this,"(Nothing selected)");
            }
            else if (domainObjects.size()>1) {
                ContextualActionUtils.setName(this,"(Multiple selected)");
            }
            else {
                String name = StringUtils.abbreviate(
                        viewerContext.getDomainObject().getName(), 50);
                ContextualActionUtils.setName(this, name);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    private static class PopupLabelNodeAction extends NodePresenterAction {

        @Override
        protected boolean enable(Node[] activatedNodes) {
            super.enable(activatedNodes);
            return false;
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
    }

}
