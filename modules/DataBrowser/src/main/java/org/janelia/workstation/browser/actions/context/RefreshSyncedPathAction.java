package org.janelia.workstation.browser.actions.context;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.model.domain.files.SyncedPath;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.util.concurrent.CancellationException;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "RefreshSyncedPathAction"
)
@ActionRegistration(
        displayName = "#CTL_RefreshSyncedPathAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 2510)
})
@NbBundle.Messages("CTL_RefreshSyncedPathAction=Refresh Synchronized Folder")
public class RefreshSyncedPathAction extends BaseContextualNodeAction {

    private SyncedPath domainObject;

    @Override
    protected void processContext() {
        this.domainObject = null;
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(SyncedPath.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(SyncedPath.class);
        }
        if (domainObject != null) {
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(domainObject));
        }
    }

    @Override
    public void performAction() {
        refreshSyncedRoot(domainObject);
    }

    public static void refreshSyncedRoot(SyncedPath syncedPath) {

        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private SyncedRoot syncedRoot;
            private String taskDisplayName = "Synchronizing Folder "+syncedPath.getName();

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {

                if (syncedPath instanceof SyncedRoot) {
                    syncedRoot = (SyncedRoot)syncedPath;
                }
                else {
                    syncedRoot = DomainMgr.getDomainMgr().getModel().getDomainObject(syncedPath.getRootRef());
                }

                setSuccessCallback(() -> {
                    SimpleWorker.runInBackground(() -> DomainMgr.getDomainMgr().getModel().invalidateAll());
                    return null;
                });

                setStatus("Submitting task " + taskDisplayName);

                AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
                ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                        .add("-syncedRootId", syncedRoot.getId().toString());

                Long taskId = asyncServiceClient.invokeService("syncedRoot",
                        serviceArgsBuilder.build(),
                        null,
                        ImmutableMap.of()
                );

                setServiceId(taskId);

                // Wait until task is finished
                super.doStuff();

                if (isCancelled()) throw new CancellationException();
                setStatus("Done");
            }

        };
        worker.executeWithEvents();
    }
}