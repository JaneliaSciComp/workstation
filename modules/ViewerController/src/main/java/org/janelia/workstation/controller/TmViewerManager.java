package org.janelia.workstation.controller;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.network.TiledMicroscopeDomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class TmViewerManager {
    private final Logger logger = LoggerFactory.getLogger(TmViewerManager.class);
    private static final TmViewerManager instance = new TmViewerManager();
    private TmSample currentSample;
    private TmWorkspace currentWorkspace;

    public static TmViewerManager getInstance() {
        return instance;
    }

    public TmViewerManager() {

    }

    public void loadDomainObject(final DomainObject domainObject) {
        logger.info("loadDomainObject({})", domainObject);

        // clear out the current model and send events to viewers to refresh

        // Clear existing UI state
       // if (annotationModel!=null) {
           // annotationModel.clear();
       // }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (domainObject instanceof TmSample) {
                    currentSample = (TmSample) domainObject;
                } else if (domainObject instanceof TmWorkspace) {
                    currentWorkspace = (TmWorkspace) domainObject;
                    try {
                        currentSample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(currentWorkspace);
                    } catch (Exception e) {
                        logger.error("Error getting sample for {}", currentWorkspace, e);
                    }
                }

                // make sure all current TmViewer top components get synced with the model
            }

            @Override
            protected void hadSuccess() {
                if (currentSample == null) {
                  //  JOptionPane.showMessageDialog(LargeVolumeViewViewer.this.getParent(),
                  //          "Could not find sample entity for this workspace!",
                  //          "Could not open workspace",
                  //          JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // make sure all viewers get refreshed

                logger.info("Found sample {}", currentSample.getId());

                // track whether volume load succeeded; see note later
                AtomicBoolean volumeLoaded = new AtomicBoolean(false);

                // Figure out best way to block while workspace is being loaded
                // Load the Imagery data
                loadVolume(volumeLoaded);

                // Load the Neurons and other data
                loadWorkspace();

                if (volumeLoaded.get()) {
                    logger.info("Loading completed");
                    // push loadComplete event
                    //annotationModel.loadComplete();

                } else {
                    logger.error("Error loading empty workspace after failed workspace load");
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }

            private void loadWorkspace() {
                // independently of the image volume load, we load the annotation data:
                final ProgressHandle progress2 = ProgressHandleFactory.createHandle("Loading metadata...");
                progress2.start();
                progress2.setDisplayName("Loading metadata");
                progress2.switchToIndeterminate();

                SimpleWorker workspaceLoader = new SimpleWorker() {
                   @Override
                   protected void doStuff() throws Exception {

                       //annotationModel.loadSample((TmSample) initialObject);
                       //annotationModel.loadWorkspace((TmWorkspace) initialObject);
                    }

                    @Override
                    protected void hadSuccess() {
                        logger.info("Metadata loading completed");
                        progress2.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        logger.error("workspace loader failed", error);
                        progress2.finish();
                        FrameworkAccess.handleException(error);
                    }
                };
            }

            private void loadVolume(AtomicBoolean volumeLoaded) {

                final ProgressHandle progress = ProgressHandle.createHandle("Loading image data...");
                progress.start();
                progress.setDisplayName("Loading image data");
                progress.switchToIndeterminate();

                SimpleWorker volumeLoader = new SimpleWorker() {
                    boolean success = false;

                    @Override
                    protected void doStuff() throws Exception {
                        // move viewUI to ViewerController
                        //success = viewUI.loadData(sliceSample);
                        volumeLoaded.set(true);
                    }

                    @Override
                    protected void hadSuccess() {
                        // send init event to all viewers with optional initial view state
                       logger.info("Setting initial camera focus: {}");
                       //viewUI.setCameraFocus(initialViewFocus);
                       //viewUI.setPixelsPerSceneUnit(initialZoom);
                       progress.finish();
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        progress.finish();
                        FrameworkAccess.handleException(error);
                    }
                };
            }
        };
        worker.execute();
    }
}
