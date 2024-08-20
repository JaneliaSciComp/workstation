package org.janelia.workstation.controller.action;

import com.google.common.eventbus.Subscribe;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmMappedNeuron;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.PostSampleLoadEvent;
import org.janelia.workstation.controller.eventbus.UnloadProjectEvent;
import org.janelia.workstation.controller.eventbus.ViewEvent;
import org.janelia.workstation.controller.model.DeepLink;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ActionID(
        category = "actions",
        id = "OpenDeepLinkAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenDeepLinkAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Horta", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_OpenDeepLinkAction=Open Deep Link In Horta")
public class OpenDeepLinkAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(OpenDeepLinkAction.class);
    private DeepLink deepLink;

    public static OpenDeepLinkAction get() {
        return SystemAction.get(OpenDeepLinkAction.class);
    }

    public void setDeepLink(DeepLink link) {
        deepLink = link;
    }

    @Subscribe
    public void finishDeepLinkOpen(PostSampleLoadEvent event) {
        TmViewState view = deepLink.getViewpoint();
        TmModelManager.getInstance().getCurrentView().setCameraFocusX(view.getCameraFocusX());
        TmModelManager.getInstance().getCurrentView().setCameraFocusY(view.getCameraFocusY());
        TmModelManager.getInstance().getCurrentView().setCameraFocusZ(view.getCameraFocusZ());
        float[] rot = view.getCameraRotation();
        TmModelManager.getInstance().getCurrentView().setZoomLevel(view.getZoomLevel());
        ViewEvent viewEvent = new ViewEvent(this,(double)view.getCameraFocusX(),
                (double)view.getCameraFocusY(),
                (double)view.getCameraFocusZ(),
                view.getZoomLevel(),
                rot,
                false);
        ViewerEventBus.postEvent(viewEvent);
    }

    @Override
    public void performAction() {
        DomainObject sample = (deepLink.getSample()==null)? deepLink.getWorkspace() : deepLink.getSample();
        if (sample==null) {
            log.warn("Action performed with null domain object");
            return;
        }

        ViewerEventBus.registerForEvents(this);
        openProject(sample);
    }

    private void
    openProject(DomainObject sample) {
        TmViewerManager.getInstance().loadProject(sample);
        FrameworkAccess.getBrowsingController().updateRecentlyOpenedHistory(Reference.createFor(sample));
    }
}
