package org.janelia.workstation.controller;

import com.google.common.eventbus.EventBus;
import org.janelia.console.viewerapi.dialogs.NeuronGroupsDialog;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.controller.network.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.spatialfilter.NeuronSpatialFilter;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TmViewerManager implements GlobalViewerController {
    private final Logger log = LoggerFactory.getLogger(TmViewerManager.class);
    private static final TmViewerManager instance = new TmViewerManager();
    private TmModelManager modelManager = new TmModelManager();
    private TiledMicroscopeDomainMgr tmDomainMgr;
    private Map<EventBusType, EventBus> eventRegistry;
    private NeuronManager neuronManager;
    private DomainObject currProject;
    private static final int NUMBER_FRAGMENTS_THRESHOLD = 1000;

    public enum EventBusType {
        SAMPLEWORKSPACE, ANNOTATION, VIEWSTATE, SELECTION, IMAGERY, SCENEMANAGEMENT
    }
    public enum ToolSet {
        NEURON
    }

    public static TmViewerManager getInstance() {
        return instance;
    }

    public TmViewerManager() {
        eventRegistry = new HashMap<>();
        EventBus sampleWorkspaceBus = new EventBus();
        eventRegistry.put(EventBusType.SAMPLEWORKSPACE, sampleWorkspaceBus);

        EventBus annotationBus = new EventBus();
        eventRegistry.put(EventBusType.ANNOTATION, annotationBus);

        EventBus viewStateBus = new EventBus();
        eventRegistry.put(EventBusType.VIEWSTATE, viewStateBus);

        this.tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
        setNeuronManager(new NeuronManager(modelManager));
    }

    public NeuronManager getNeuronManager() {
        return neuronManager;
    }

    public void setNeuronManager(NeuronManager neuronManager) {
        this.neuronManager = neuronManager;
    }

    // These are cases where we want to load a tilestack live
    public void loadTileStack() {

    }

    public void loadDomainObject(final DomainObject domainObject) {
        log.info("loadDomainObject({})", domainObject);

        // clear out the current model and send events to viewers to refresh
        WorkspaceEvent event = new WorkspaceEvent();
        event.setEventType(WorkspaceEvent.Type.CLEAR);
        eventRegistry.get(EventBusType.SAMPLEWORKSPACE).post(event);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                // make sure all current TmViewer top components get synced with the model
                if (domainObject instanceof TmSample) {
                    TmSample currSample = (TmSample) domainObject;
                    modelManager.setCurrentSample(currSample);
                } else if (domainObject instanceof TmWorkspace) {
                    TmWorkspace currWorkspace = (TmWorkspace) domainObject;
                    modelManager.setCurrentWorkspace(currWorkspace);
                    try {
                        modelManager.setCurrentSample(tmDomainMgr.getSample(currWorkspace));
                    } catch (Exception e) {
                        FrameworkAccess.handleException(error);
                        log.error("Error getting sample for {}", modelManager.getCurrentWorkspace(), e);
                    }
                }
                // make sure all viewers get refreshed

                log.info("Found sample {}", modelManager.getCurrentSample().getId());

                // Load the Imagery data and fire imagery event
                //loadVolume(volumeLoaded);

                // Load annotations and other data
                loadData();

            }

            @Override
            protected void hadSuccess() {
                // now that data and imagery has been loaded sent out events to refresh the viewers
                initProject();
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        worker.execute();
    }

    /**
     * this method assumes the model has been loaded correctly for a project (TmWorkspace/TmSample).
     * It then sends out initialization events so the various local controllers can clear out
     * their state and refresh from the central model
     */
    private void initProject() {

    }

    /**
     * loads appropriate metadata for this project (TmWorkspace/TmSample).  For now,
     * we assume this is neuron annotations, but it could be different annotations depending on
     * the toolset associated with the project.
     */
    private void loadData() {
        // independently of the image volume load, we load the annotation data:
        final ProgressHandle progress2 = ProgressHandleFactory.createHandle("Loading metadata...");
        progress2.start();
        progress2.setDisplayName("Loading metadata");
        progress2.switchToIndeterminate();

        SimpleWorker dataLoader = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                if (currProject == null) {
                    throw new IllegalArgumentException("Cannot load null workspace/sample");
                }

                //loadVolume(true);

                if (currProject instanceof TmSample) {
                    // we just need to load imagery
                    return;
                }

                log.info("Loading workspace {}", currProject.getId());
                // determine current toolset
                // TO DO
                // for now, just assume neuron toolset

                // Neurons need to be loaded en masse from raw data from server.
                NeuronModel manager = modelManager.getNeuronDAO();
                TmWorkspace currWorkspace = (TmWorkspace)currProject;
                log.info("Loading neurons for workspace {}", currWorkspace.getId());
                manager.loadWorkspaceNeurons(currWorkspace);

                // if workspace contains more system-owned fragments than a threshold , enable filter
                String systemNeuron = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
                boolean applyFilter = false;
                modelManager.getCurrentView().setApplyFilter(applyFilter);
                int nFragments = 0;
                for (TmNeuronMetadata neuron: manager.getNeurons()) {
                    if (neuron.getOwnerKey().equals(systemNeuron)) {
                        nFragments += 1;
                        if (nFragments >= NUMBER_FRAGMENTS_THRESHOLD) {
                            applyFilter = true;
                            modelManager.getCurrentView().setApplyFilter(applyFilter);
                            break;
                        }
                    }
                }
                log.info("Spatial Filtering applied: {}", applyFilter);

                // if spatial filter is applied, use it to filter neurons
                NeuronSpatialFilter neuronFilter = modelManager.getCurrentView().getNeuronFilter();
                if (applyFilter) {
                    neuronFilter.initFilter(modelManager.getNeuronDAO().getNeurons());
                }
              //  fireNeuronSpatialFilterUpdated(applyFilter, neuronFilter);

                // Create the local tag map for cached access to tags
                log.info("Creating tag map for workspace {}", currWorkspace.getId());
                TmNeuronTagMap currentTagMap = new TmNeuronTagMap();
                modelManager.setCurrentTagMap(currentTagMap);
                Collection<TmNeuronMetadata> neuronList;
                if (applyFilter) {
                    neuronList = new ArrayList<>();
                    for (Long neuronId : neuronFilter.filterNeurons()) {
                        TmNeuronMetadata neuron = modelManager.getNeuronDAO().getNeuronById(neuronId);
                        if (neuron != null)
                            neuronList.add(neuron);
                    }
                } else
                    neuronList = modelManager.getNeuronDAO().getNeurons();
                for (TmNeuronMetadata tmNeuronMetadata : neuronList) {
                    for(String tag : tmNeuronMetadata.getTags()) {
                        currentTagMap.addTag(tag, tmNeuronMetadata);
                    }
                }

                // Clear neuron selection
                log.info("Clearing current neuron for workspace {}", currWorkspace.getId());
                modelManager.getCurrentSelections().setCurrNeuron(null);
            }

            @Override
            protected void hadSuccess() {
                log.info("Metadata loading completed");
                progress2.finish();
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("workspace loader failed", error);
                progress2.finish();
                FrameworkAccess.handleException(error);
            }
        };
    }

    public boolean editsAllowed() {
        if (modelManager.getCurrentWorkspace()==null) return false;
        return ClientDomainUtils.hasWriteAccess(modelManager.getCurrentWorkspace());
    }

    public void loadUserPreferences() throws Exception {
        TmNeuronTagMap currentTagMap = modelManager.getCurrentTagMap();
        if (modelManager.getCurrentSample()==null || modelManager.getCurrentSample().getId()==null) return;
        Map<String,Map<String,Object>> tagGroupMappings = FrameworkAccess.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MOUSELIGHT, modelManager.getCurrentSample().getId().toString(), null);
        if (tagGroupMappings!=null && currentTagMap!=null) {
            currentTagMap.saveTagGroupMappings(tagGroupMappings);
            //   if (neuronSetAdapter!=null && neuronSetAdapter.getMetaWorkspace()!=null) {
            //     neuronSetAdapter.getMetaWorkspace().setTagMetadata(currentTagMap);
            //   }

            // set toggled group properties on load-up
            Iterator<String> groupTags = tagGroupMappings.keySet().iterator();
            while (groupTags.hasNext()) {
                String groupKey = groupTags.next();
                Set<TmNeuronMetadata> neurons = getNeuronManager().getNeuronsForTag(groupKey);
                List<TmNeuronMetadata> neuronList = new ArrayList<TmNeuronMetadata>(neurons);
                Map<String,Object> groupMapping = currentTagMap.geTagGroupMapping(groupKey);
                if (groupMapping!=null && groupMapping.get("toggled")!=null && ((Boolean)groupMapping.get("toggled"))) {
                    String property = (String)groupMapping.get("toggleprop");
                    // these two prop changes ought to be in annmodel, not annmgr, and annmgr should call into model;
                    //  fixed for visiblity, but not for others yet
                    if (property.equals(NeuronGroupsDialog.PROPERTY_RADIUS)) {
                        //LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronUserToggleRadius(neuronList, true);
                    } else if (property.equals(NeuronGroupsDialog.PROPERTY_VISIBILITY)) {
                        //setNeuronVisibility(neuronList, false);
                    } else if (property.equals(NeuronGroupsDialog.PROPERTY_READONLY)) {
                        //LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronNonInteractable(neuronList, true);
                    }
                }
            }
        }
    }

    public void loadComplete() {
        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        // Update TC, in case the load bypassed it
        // LargeVolumeViewerTopComponent.getInstance().setCurrent(workspace==null ? getCurrentSample() : workspace);
        //SwingUtilities.invokeLater(() -> fireWorkspaceLoaded(workspace));
        // load user preferences
        try {
            loadUserPreferences();
            // register with Message Server to receive async updates
            RefreshHandler.getInstance().ifPresent(rh -> rh.setAnnotationModel(getNeuronManager()));
            //TaskWorkflowViewTopComponent.getInstance().loadHistory();
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
        SwingUtilities.invokeLater(() -> {
            EventBus selectBus = eventRegistry.get(EventBusType.SELECTION);
            SelectionEvent evt = new SelectionEvent();
            evt.setType(SelectionEvent.Type.CLEAR);
            evt.setCategory(SelectionEvent.Category.NEURON);
            selectBus.post(evt);
        });
        if (workspace!=null) {
            ////activityLog.logLoadWorkspace(workspace.getId());
        }

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
                log.info("Setting initial camera focus: {}");
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

    public synchronized void postWorkspaceUpdate(TmNeuronMetadata neuron) {
        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        // update workspace; update and select new neuron; this will draw points as well
        SwingUtilities.invokeLater(() -> {
           // fireWorkspaceLoaded(workspace);
            if (neuron!=null) {
                modelManager.getCurrentSelections().setCurrNeuron(neuron);
            }
        });
    }
}
