package org.janelia.workstation.browser.actions.context;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
        id = "RefreshSyncedRootAction"
)
@ActionRegistration(
        displayName = "#CTL_RefreshSyncedRootAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 2510)
})
@NbBundle.Messages("CTL_RefreshSyncedRootAction=Refresh Synchronized Folder")
public class RefreshSyncedRootAction extends BaseContextualNodeAction {

    private SyncedRoot syncedRoot;

    @Override
    protected void processContext() {
        this.syncedRoot = null;
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(SyncedRoot.class)) {
            this.syncedRoot = getNodeContext().getSingleObjectOfType(SyncedRoot.class);
        }
        if (syncedRoot != null) {
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(syncedRoot));
        }
    }

    @Override
    public void performAction() {
        refreshSyncedRoot(syncedRoot);
    }

    public static void refreshSyncedRoot(SyncedRoot syncedRoot) {

        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private String taskDisplayName = "Synchronizing Folder "+syncedRoot.getName();

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {

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