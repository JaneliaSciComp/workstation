package org.janelia.workstation.controller.action;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmMappedNeuron;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.controller.TmViewerManager;
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
        id = "OpenTmSampleOrWorkspaceAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenTmSampleOrWorkspaceAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Horta", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_OpenTmSampleOrWorkspaceAction=Open in Horta")
public class OpenTmSampleOrWorkspaceAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(OpenTmSampleOrWorkspaceAction.class);
    private DomainObject domainObject;

    public static OpenTmSampleOrWorkspaceAction get() {
        return SystemAction.get(OpenTmSampleOrWorkspaceAction.class);
    }

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
        else if (getNodeContext().isSingleObjectOfType(TmMappedNeuron.class)) {
            domainObject = getNodeContext().getSingleObjectOfType(TmMappedNeuron.class);
            setEnabledAndVisible(true);
        }
        else {
            domainObject = null;
            setEnabledAndVisible(false);
        }
    }

    public void setDomainObject(DomainObject obj) {
        domainObject = obj;
    }

    @Override
    public void performAction() {

        DomainObject objectToOpen = domainObject;
        if (objectToOpen==null) {
            log.warn("Action performed with null domain object");
            return;
        }

        if (domainObject instanceof TmMappedNeuron) {
            TmMappedNeuron neuron = (TmMappedNeuron) this.domainObject;

            SimpleWorker worker = new SimpleWorker() {

                private TmWorkspace workspace;

                @Override
                protected void doStuff() throws Exception {
                    workspace = DomainMgr.getDomainMgr().getModel().getDomainObject(neuron.getWorkspaceRef());
                }

                @Override
                protected void hadSuccess() {
                    openProject(workspace);
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };

            worker.execute();
        }
        else {
            openProject(domainObject);
        }
    }

    private void openProject(DomainObject project) {
        TmViewerManager.getInstance().loadProject(project);
        FrameworkAccess.getBrowsingController().updateRecentlyOpenedHistory(Reference.createFor(project));
    }
}
