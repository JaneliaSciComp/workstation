package org.janelia.workstation.browser.actions;

import java.awt.Component;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.browser.gui.components.DomainViewerManager;
import org.janelia.workstation.browser.gui.components.DomainViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.DomainObjectAcceptorHelper;
import org.janelia.workstation.core.actions.PopupMenuGenerator;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.model.ImageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(DomainObjectContextMenu.class);

    private ChildSelectionModel<DomainObject,Reference> selectionModel;

    public DomainObjectContextMenu(
            ChildSelectionModel<DomainObject,Reference> selectionModel,
            ChildSelectionModel<DomainObject,Reference> editSelectionModel,
            ImageModel<DomainObject,Reference> imageModel) {
        this.selectionModel = selectionModel;
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.create");
    }

    // TODO: this should be made into a SPI
    public void runDefaultAction() {
        if (selectionModel.getObjects().size()>1) return;
        DomainObject domainObject = selectionModel.getLastSelectedObject();
        DomainObject contextObject = (DomainObject)selectionModel.getParentObject();
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
                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Collections.singletonList(domainObject), true, true, true));
            }
        }
        else {
            if (DomainObjectAcceptorHelper.isSupported(domainObject)) {
                DomainObjectAcceptorHelper.service(domainObject);
            }
        }
    }

    public void addMenuItems() {
        for (Component currentContextMenuItem : DomainObjectAcceptorHelper.getCurrentContextMenuItems()) {
            add(currentContextMenuItem);
        }
    }
}
