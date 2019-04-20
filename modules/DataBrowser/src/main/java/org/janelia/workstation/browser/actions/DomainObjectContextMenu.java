package org.janelia.workstation.browser.actions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.core.actions.DomainObjectAcceptorHelper;
import org.janelia.workstation.browser.gui.components.DomainViewerManager;
import org.janelia.workstation.browser.gui.components.DomainViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectContextMenu.class);

    // Current selection
    protected DomainObject contextObject;
    protected ChildSelectionModel<DomainObject,Reference> editSelectionModel;
    protected List<DomainObject> domainObjectList;
    protected DomainObject domainObject;
    protected boolean multiple;
    protected ArtifactDescriptor resultDescriptor;
    protected String typeName;

    public DomainObjectContextMenu(DomainObject contextObject, List<DomainObject> domainObjectList, 
            ArtifactDescriptor resultDescriptor, String typeName, ChildSelectionModel<DomainObject,Reference> editSelectionModel) {
        this.contextObject = contextObject;
        this.domainObjectList = domainObjectList;
        this.domainObject = domainObjectList.isEmpty() ? null : domainObjectList.get(0);
        this.multiple = domainObjectList.size() > 1;
        this.resultDescriptor = resultDescriptor;
        this.typeName = typeName;
        this.editSelectionModel = editSelectionModel;
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.create", domainObject);
    }

    public void runDefaultAction() {
        if (multiple) return;
        if (DomainViewerTopComponent.isSupported(domainObject)) {
            DomainViewerTopComponent viewer = ViewerUtils.getViewer(DomainViewerManager.getInstance(), "editor2");
            if (viewer == null || !viewer.isCurrent(domainObject)) {
                viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(domainObject, true);
            }
        }
        else if (DomainExplorerTopComponent.isSupported(domainObject)) {
            // TODO: here we should select by path to ensure we get the right one, but for that to happen the domain object needs to know its path
            DomainExplorerTopComponent.getInstance().expandNodeById(contextObject.getId());
            if (DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(domainObject.getId()) == null) {
                // Node could not be found in tree. Try navigating directly.
                log.info("Node not found in tree: {}", domainObject);
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(domainObject), true, true, true));
            }
        }
        else {
            if (DomainObjectAcceptorHelper.isSupported(domainObject)) {
                DomainObjectAcceptorHelper.service(domainObject);
            }
        }
    }

    public void addMenuItems() {

        if (domainObjectList.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        for (JComponent item : getContextMenuItems()) {
            add(item);
        }
    }

    private Collection<JComponent> getContextMenuItems() {
        ViewerContext viewerContext = new ViewerContext(
                contextObject, domainObjectList, resultDescriptor, typeName, editSelectionModel);
        return DomainObjectAcceptorHelper.getContextMenuItems(domainObject, viewerContext);
    }

}
