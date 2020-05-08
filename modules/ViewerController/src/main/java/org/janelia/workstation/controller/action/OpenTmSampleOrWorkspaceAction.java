package org.janelia.workstation.controller.action;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.controller.TmViewerManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ActionID(
        category = "actions",
        id = "OpenTmSampleOrWorkspaceAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenTmSampleOrWorkspaceAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/Large Volume", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_OpenTmSampleOrWorkspaceAction=Load Workspace into 3D Viewers")
public class OpenTmSampleOrWorkspaceAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(OpenTmSampleOrWorkspaceAction.class);
    private DomainObject domainObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmSample.class)) {
            domainObject = getNodeContext().getSingleObjectOfType(TmSample.class);
            setEnabledAndVisible(true);
        }
        else if (getNodeContext().isSingleObjectOfType(TmWorkspace.class)) {
            domainObject = getNodeContext().getSingleObjectOfType(TmWorkspace.class);
            setEnabledAndVisible(true);
        }
        else {
            domainObject = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        DomainObject objectToOpen = domainObject;
        if (objectToOpen==null) {
            log.warn("Action performed with null domain object");
            return;
        }

        TmViewerManager.getInstance().loadProject(domainObject);

    }
}
