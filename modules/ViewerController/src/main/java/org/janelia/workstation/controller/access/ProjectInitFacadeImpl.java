package org.janelia.workstation.controller.access;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.util.MatrixUtilities;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.LoadMetadataEvent;
import org.janelia.workstation.controller.eventbus.LoadProjectEvent;
import org.janelia.workstation.controller.eventbus.UnloadProjectEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSpatialFilter;
import org.janelia.workstation.controller.tileimagery.*;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
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
        if (tc!= null && !tc.isOpened()) {
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
                TmModelManager.getInstance().setCurrentSample(sample);
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

                SharedVolumeImage sharedVolumeImage = new SharedVolumeImage();
                TileServer tileServer = new TileServer(sharedVolumeImage);
                TmModelManager.getInstance().setTileServer(tileServer);
                URL url = TmModelManager.getInstance().getTileLoader().getUrl();

                TileFormat tileFormat = new TileFormat();
                TmModelManager.getInstance().setTileFormat(tileFormat);
                sharedVolumeImage.setTileLoaderProvider(new BlockTiffOctreeTileLoaderProvider() {
                    int concurrency = 15;

                    @Override
                    public BlockTiffOctreeLoadAdapter createLoadAdapter(String baseURI) {
                        return TileStackCacheController.createInstance(
                                new TileStackOctreeLoadAdapter(tileFormat, URI.create(baseURI), concurrency));
                    }
                });
                sharedVolumeImage.loadURL(url);

                // TODO: Why is this happening in the client???
                //       This kind of caching logic should be encapsulated in the services,
                //       and it shouldn't depend on the user having write access to the sample.
                if (sample.getVoxToMicronMatrix()==null) {
                    // init sample from tileformat
                    sample.setVoxToMicronMatrix(MatrixUtilities.serializeMatrix(
                            tileFormat.getVoxToMicronMatrix(),
                            "voxToMicronMatrix"));
                    sample.setMicronToVoxMatrix(MatrixUtilities.serializeMatrix(
                            tileFormat.getMicronToVoxMatrix(),
                            "micronToVoxMatrix"));
                    if (DomainUtils.hasWriteAccess(sample, AccessManager.getWriterSet())) {
                        TiledMicroscopeDomainMgr.getDomainMgr().save(sample);
                    }
                }
                modelManager.updateVoxToMicronMatrices();
                BoundingBox3d box = sharedVolumeImage.getBoundingBox3d();
                TmModelManager.getInstance().setSampleBoundingBox (box);
                TmModelManager.getInstance().setVoxelCenter (sharedVolumeImage.getVoxelCenter());
            }

            @Override
            protected void hadSuccess() {
                LoadProjectEvent event;
                modelManager.setCurrentSample(sample);
                if (workspace!=null)
                    event = new LoadProjectEvent(this,
                            workspace, sample, true);
                else
                    event = new LoadProjectEvent(this,
                            null, sample, true);
                ViewerEventBus.postEvent(event);
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

                // Clear neuron selection
                log.info("Clearing current neuron for workspace {}", workspace.getId());
                modelManager.getCurrentSelections().clearAllSelections();
            }

            @Override
            protected void hadSuccess() {
                log.info("Metadata loading completed");
                progress2.finish();
                // now that data and tileimagery has been loaded sent out events to refresh the viewers
                LoadMetadataEvent event;
                if (workspace!=null)
                    event = new LoadMetadataEvent(this,
                            workspace, sample, true);
                else
                    event = new LoadMetadataEvent(this,
                            null, sample, true);
                ViewerEventBus.postEvent(event);
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
        ViewerEventBus.postEvent(new UnloadProjectEvent(this,
                null, null,
                modelManager.getCurrentWorkspace()==null));
    }

    @Override
    public void notifyViewers() {

    }
}
