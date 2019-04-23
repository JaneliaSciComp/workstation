package org.janelia.workstation.common.actions;

import java.util.Collection;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.interfaces.HasName;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
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
        return obj instanceof HasName;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return nodeAction;
    }

    private static class PopupLabelAction extends ViewerContextAction {

        @Override
        public String getName() {
            Collection selectedObjects = getViewerContext().getSelectedObjects();
            if (selectedObjects.size()>1) {
                return "(Multiple selected)";
            }
            else {
                Object lastSelectedObject = getViewerContext().getLastSelectedObject();
                if (lastSelectedObject != null) {
                    String name;
                    if (lastSelectedObject instanceof HasName) {
                        HasName named = (HasName) lastSelectedObject;
                        name = named.getName();
                    }
                    else {
                        name = lastSelectedObject.toString();
                    }
                    return StringUtils.abbreviate(name, 50);
                }
            }
            return "(Nothing selected)";
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    private static class PopupLabelNodeAction extends NodePresenterAction {

        @Override
        protected boolean enable(Node[] activatedNodes) {
            super.enable(activatedNodes);
            // The label is never enabled, because it's not meant to be clicked on
            return false;
        }

        @Override
        public String getName() {
            List<Node> selected = getSelectedNodes();
            if (selected.isEmpty()) {
                return "(Nothing selected)";
            }
            else if (selected.size()>1) {
                return "(Multiple selected)";
            }
            Node node = selected.get(0);
            return StringUtils.abbreviate(node.getDisplayName(), 50);
        }
    }

}
