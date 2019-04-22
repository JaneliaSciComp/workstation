package org.janelia.workstation.browser.actions;

import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.DomainObjectAcceptorHelper;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
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
    private ViewerContext<DomainObject,Reference> viewerContext;

    public DomainObjectContextMenu(
            ChildSelectionModel<DomainObject,Reference> selectionModel,
            ChildSelectionModel<DomainObject,Reference> editSelectionModel,
            ImageModel<DomainObject,Reference> imageModel) {
        this.viewerContext = new ViewerContext<>(
                selectionModel, editSelectionModel, imageModel,null);
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.create");
    }

    public void runDefaultAction() {
//        if (multiple) return;
//        if (DomainViewerTopComponent.isSupported(domainObject)) {
//            DomainViewerTopComponent viewer = ViewerUtils.getViewer(DomainViewerManager.getInstance(), "editor2");
//            if (viewer == null || !viewer.isCurrent(domainObject)) {
//                viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
//                viewer.requestActive();
//                viewer.loadDomainObject(domainObject, true);
//            }
//        }
//        else if (DomainExplorerTopComponent.isSupported(domainObject)) {
//            // TODO: here we should select by path to ensure we get the right one, but for that to happen the domain object needs to know its path
//            DomainExplorerTopComponent.getInstance().expandNodeById(contextObject.getId());
//            if (DomainExplorerTopComponent.getInstance().selectAndNavigateNodeById(domainObject.getId()) == null) {
//                // Node could not be found in tree. Try navigating directly.
//                log.info("Node not found in tree: {}", domainObject);
//                Events.getInstance().postOnEventBus(new DomainObjectSelectionEvent(this, Arrays.asList(domainObject), true, true, true));
//            }
//        }
//        else {
//            if (DomainObjectAcceptorHelper.isSupported(domainObject)) {
//                DomainObjectAcceptorHelper.service(domainObject);
//            }
//        }
    }

    public void addMenuItems() {

        ChildSelectionModel<DomainObject,Reference> selectionModel = viewerContext.getSelectionModel();

        if (selectionModel.getSelectedIds().isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        DomainObject domainObject = viewerContext.getLastSelectedObject();
        Collection<JComponent> contextMenuItems = DomainObjectAcceptorHelper.getContextMenuItems(domainObject, viewerContext);
        for (JComponent item : contextMenuItems) {
            add(item);
        }
    }

}
