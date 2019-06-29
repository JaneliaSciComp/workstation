package org.janelia.workstation.common.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NotImplementedException;
import org.openide.util.actions.NodeAction;

/**
 * A class which unifies normal Swing Actions (manipulating domain objects) with the NetBeans NodeAction model
 * of acting on selected Nodes. This action can then be returned from both getAction and getNodeAction of
 * a ContextualActionBuilder.
 *
 * When you extend this class, just implement getName(), isVisible(), and executeAction(). Note that
 * domainObjectNodeList will be only populated in the case of the action being invoked via a node context menu.
 *
 * You should not use the NodeAction API to do things like check isEnabled(). Instead, use ContextualActionUtils
 * where possible, as this will produce results that are compatible with both APIs.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DomainObjectNodeAction extends NodeAction implements ViewerContextReceiver, PopupMenuGenerator {

    protected final List<DomainObjectNode> domainObjectNodeList = new ArrayList<>();
    protected final List<DomainObject> domainObjectList = new ArrayList<>();

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        executeAction();
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (!ContextualActionUtils.isVisible(this)) {
            return null;
        }
        String name = ContextualActionUtils.getName(this);
        if (name == null) name = getName();
        JMenuItem item = ContextualActionUtils.getNamedActionItem(name, actionEvent -> {
            try {
                executeAction();
            }
            catch (Exception e) {
                FrameworkAccess.handleException(e);
            }
        });
        item.setEnabled(ContextualActionUtils.isEnabled(this));
        return item;
    }

    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        domainObjectList.clear();
        for(Object obj : viewerContext.getSelectedObjects()) {
            if (obj instanceof DomainObject) {
                domainObjectList.add((DomainObject)obj);
            }
        }
        ContextualActionUtils.setVisible(this, isVisible());
    }

    protected boolean isVisible() {
        return true;
    }

    protected void executeAction() {
    }

    @Override
    protected boolean asynchronous() {
        // We do our own background processing
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        List<DomainObject> domainObjectList = new ArrayList<>();
        domainObjectList.clear();
        domainObjectNodeList.clear();
        for(Node node : activatedNodes) {
            if (node instanceof DomainObjectNode) {
                DomainObjectNode domainObjectNode = (DomainObjectNode)node;
                domainObjectNodeList.add(domainObjectNode);
                domainObjectList.add(domainObjectNode.getDomainObject());
            }
        }

        // Create dummy models
        FixedDomainObjectSelectionModel selectionModel = new FixedDomainObjectSelectionModel();
        selectionModel.select(domainObjectList, true, false);
        FixedDomainObjectImageModel imageModel = new FixedDomainObjectImageModel();

        // Inject viewer context
        ViewerContext viewerContext = new ViewerContext<>(selectionModel, null,
                imageModel);
        setViewerContext(viewerContext);

        // Enable state is determined by the popup presenter
        return true;
    }

    public class FixedDomainObjectSelectionModel extends ChildSelectionModel<DomainObject, Reference> {

        @Override
        protected void selectionChanged(List<DomainObject> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        }

        @Override
        public Reference getId(DomainObject domainObject) {
            return Reference.createFor(domainObject);
        }
    }

    public class FixedDomainObjectImageModel extends DomainObjectImageModel {

        @Override
        public ArtifactDescriptor getArtifactDescriptor() {
            return null;
        }

        @Override
        public String getImageTypeName() {
            return null;
        }

        @Override
        public List<Annotation> getAnnotations(DomainObject domainObject) {
            throw new NotImplementedException();
        }

        @Override
        public List<Decorator> getDecorators(DomainObject imageObject) {
            throw new NotImplementedException();
        }
    };
}
