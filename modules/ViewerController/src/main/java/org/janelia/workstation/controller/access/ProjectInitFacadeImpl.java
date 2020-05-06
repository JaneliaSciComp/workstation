package org.janelia.workstation.controller.access;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.controller.EventBusRegistry;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.eventbus.LoadEvent;
import org.janelia.workstation.controller.eventbus.WorkspaceEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSpatialFilter;
import org.janelia.workstation.controller.tileimagery.FileBasedTileLoader;
import org.janelia.workstation.controller.tileimagery.TileLoader;
import org.janelia.workstation.controller.tileimagery.URLBasedTileLoader;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class ProjectInitFacadeImpl implements ProjectInitFacade {
    private final Logger log = LoggerFactory.getLogger(TmViewerManager.class);
    DomainObject project;
    TmWorkspace workspace;
    TmViewerManager viewerManager;
    TmModelManager modelManager;

    private static final int NUMBER_FRAGMENTS_THRESHOLD = 1000;

    public ProjectInitFacadeImpl(DomainObject project) {
        this.project = project;
        viewerManager = TmViewerManager.getInstance();
        modelManager = TmModelManager.getInstance();
        TopComponent tc = WindowManager.getDefault().findTopComponent("InfoPanelTopComponent");
        if (tc != null) {
            tc.open();
            tc.requestActive();
        }
    }

    public void loadImagery(TmSample sample) {
        final ProgressHandle progress = ProgressHandle.createHandle("Loading image data...");
        progress.start();
        progress.setDisplayName("Loading image data");
        progress.switchToIndeterminate();

        SimpleWorker volumeLoader = new SimpleWorker() {
            boolean success = false;

            @Override
            protected void doStuff() throws Exception {
                JadeServiceClient jadeServiceClient = new JadeServiceClient(
                        ConsoleProperties.getString("jadestorage.rest.url"),
                        () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false)
                );
                TileLoader loader;
                if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
                    loader = new URLBasedTileLoader(jadeServiceClient);
                } else {
                    loader = new FileBasedTileLoader(jadeServiceClient);
                }
                modelManager.setTileLoader(loader);
                loader.loadData(sample);
            }

            @Override
            protected void hadSuccess() {
                LoadEvent event = new LoadEvent(LoadEvent.Type.PROJECT_COMPLETE);
                event.setSample(sample);
                event.setWorkspace(workspace);
                EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SAMPLEWORKSPACE).post(event);
                progress.finish();
            }

            @Override
            protected void hadError(Throwable error) {
                progress.finish();
                FrameworkAccess.handleException(error);
            }
        };
        volumeLoader.execute();
    }


    /**
     * loads appropriate metadata for this project (TmWorkspace/TmSample).  For now,
     * we assume this is neuron annotations, but it could be different annotations depending on
     * the toolset associated with the project.
     */
    @Override
    public void loadAnnotationData(TmWorkspace currProject) {
        log.info("loadAnnotationData({})", project);
        // independently of the image volume load, we load the annotation data:
        final ProgressHandle progress2 = ProgressHandleFactory.createHandle("Loading metadata...");
        workspace = currProject;
        SimpleWorker worker = new SimpleWorker() {
            TmSample sample;

            @Override
            protected void doStuff() throws Exception {
                TmModelManager modelManager = TmModelManager.getInstance();
                sample = TiledMicroscopeDomainMgr.getDomainMgr().getSample(workspace);
                progress2.start();
                progress2.setDisplayName("Loading metadata");
                progress2.switchToIndeterminate();

                log.info("Loading workspace {}", currProject.getId());
                // determine current toolset
                // TO DO
                // for now, just assume neuron toolset

                // Neurons need to be loaded en masse from raw data from server.
                NeuronModel manager = modelManager.getNeuronModel();
                log.info("Loading neurons for workspace {}", workspace.getId());
                manager.loadWorkspaceNeurons(workspace);

                // if workspace contains more system-owned fragments than a threshold , enable filter
                String systemNeuron = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
                boolean applyFilter = false;
                modelManager.getCurrentView().setFilter(applyFilter);
                int nFragments = 0;
                for (TmNeuronMetadata neuron: manager.getNeurons()) {
                    if (neuron.getOwnerKey().equals(systemNeuron)) {
                        nFragments += 1;
                        if (nFragments >= NUMBER_FRAGMENTS_THRESHOLD) {
                            applyFilter = true;
                            modelManager.getCurrentView().setFilter(applyFilter);
                            break;
                        }
                    }
                }
                log.info("Spatial Filtering applied: {}", applyFilter);
                modelManager.getCurrentView().init();

                // if spatial filter is applied, use it to filter neurons
                NeuronSpatialFilter neuronFilter = modelManager.getCurrentView().getSpatialFilter();
                if (applyFilter) {
                    neuronFilter.initFilter(modelManager.getNeuronModel().getNeurons());
                }
                //  fireNeuronSpatialFilterUpdated(applyFilter, neuronFilter);

                // Create the local tag map for cached access to tags
                log.info("Creating tag map for workspace {}", workspace.getId());
                TmNeuronTagMap currentTagMap = new TmNeuronTagMap();
                modelManager.setCurrentTagMap(currentTagMap);
                Collection<TmNeuronMetadata> neuronList;
                if (applyFilter) {
                    neuronList = new ArrayList<>();
                    for (Long neuronId : neuronFilter.filterNeurons()) {
                        TmNeuronMetadata neuron = modelManager.getNeuronModel().getNeuronById(neuronId);
                        if (neuron != null)
                            neuronList.add(neuron);
                    }
                } else
                    neuronList = modelManager.getNeuronModel().getNeurons();
                for (TmNeuronMetadata tmNeuronMetadata : neuronList) {
                    for(String tag : tmNeuronMetadata.getTags()) {
                        currentTagMap.addTag(tag, tmNeuronMetadata);
                    }
                }

                // Clear neuron selection
                log.info("Clearing current neuron for workspace {}", workspace.getId());
                modelManager.getCurrentSelections().clearAllSelections();
            }

            @Override
            protected void hadSuccess() {
                log.info("Metadata loading completed");
                progress2.finish();
                // now that data and tileimagery has been loaded sent out events to refresh the viewers
                LoadEvent event = new LoadEvent(LoadEvent.Type.METADATA_COMPLETE);
                event.setWorkspace(workspace);
                event.setSample(sample);
                EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SAMPLEWORKSPACE).post(event);
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("workspace loader failed", error);
                progress2.finish();
                FrameworkAccess.handleException(error);
            }
        };
        worker.execute();

    }

    @Override
    public void clearViewers() {
        // clear out the current model and send events to viewers to refresh
        WorkspaceEvent event = new WorkspaceEvent(WorkspaceEvent.Type.CLEAR);
        EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SAMPLEWORKSPACE).post(event);
    }

    @Override
    public void notifyViewers() {

    }
}
