package org.janelia.workstation.controller;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;

import org.apache.commons.io.FilenameUtils;
import org.janelia.console.viewerapi.controller.TransactionManager;
import org.janelia.console.viewerapi.model.DefaultNeuron;
import org.janelia.it.jacs.shared.geom.ParametrizedLine;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.swc.SWCData;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
import org.janelia.it.jacs.shared.swc.SWCNode;
import org.janelia.it.jacs.shared.utils.Progress;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;
import org.janelia.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.security.Subject;
import org.janelia.workstation.controller.infopanel.FilteredAnnotationModel;
import org.janelia.workstation.controller.listener.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.controller.model.annotations.neuron.PredefinedNote;
import org.janelia.workstation.controller.spatialfilter.NeuronMessageConstants;
import org.janelia.workstation.controller.spatialfilter.NeuronSpatialFilter;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionModel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionSupport;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.controller.network.TiledMicroscopeDomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for handling model changes related to neuron annotation.
 * Generally, this works with the NeuronModel to make c
 * 
 * Public methods in this class that throw exceptions are the ones that involve db calls, and
 * they should all be called from worker threads.  the others, typically getters of
 * various info, do not.  private methods don't necessarily follow that pattern.
 * 
 * A note on entities: as of the early 2016 update, we act almost exclusively
 * on domain objects and then persist them rather than calling through a DAO
 * to make changes on entities.  be careful that the workspace and neuron objects
 * are kept up to date locally!
 * 
 * This class does not interact directly with the UI.  it observes
 * UI elements that select, and its events are connected with a variety of UI
 * elements that need to respond to changing data.  this sometimes makes it hard
 * to know whether methods are called in the Java EDT (event thread) or not, and
 * that's important for knowing how you have to call the UI updates.  the answer
 * is that all the calls that hit the db go through TiledMicroscopeDomainMgr and could throw
 * exceptions.  so any call that calls TiledMicroscopeDomainMgr must catch Exceptions, and
 * therefore it should do its updates on the EDT, because it's probably being
 * called from a  SimpleWorker thread.
 *
 */
public class NeuronManager implements DomainObjectSelectionSupport {

    private static final Logger log = LoggerFactory.getLogger(NeuronManager.class);
    
    public static final String STD_SWC_EXTENSION = SWCData.STD_SWC_EXTENSION;
    private static final String COLOR_FORMAT = "# COLOR %f,%f,%f";
    private static final String NAME_FORMAT = "# NAME %s";

    private static final String NEURON_TAG_VISIBILITY = "hidden";

    private static final int NUMBER_FRAGMENTS_THRESHOLD = 1000;

    private final TiledMicroscopeDomainMgr tmDomainMgr;

    private SWCDataConverter swcDataConverter;

    private TmSample currentSample;
    private TmWorkspace currentWorkspace;
    private TmNeuronMetadata currentNeuron;
    private TmGeoAnnotation currentVertex;
    private List<TmNeuronMetadata> currentFilteredNeuronList;
    private TmNeuronTagMap currentTagMap;

    private NeuronSpatialFilter neuronFilter;
   // private final FilteredAnnotationModel filteredAnnotationModel;

    private final Collection<TmGeoAnnotationModListener> tmGeoAnnoModListeners = new ArrayList<>();
    private final Collection<TmAnchoredPathListener> tmAnchoredPathListeners = new ArrayList<>();
    private final Collection<GlobalAnnotationListener> globalAnnotationListeners = new ArrayList<>();
    private final Collection<BackgroundAnnotationListener> backgroundAnnotationListeners = new ArrayList<>();
    private final Collection<TaskReviewListener> taskReviewListeners = new ArrayList<>();
    private final Collection<GlobalNeuronSpatialFilterListener> neuronSpatialFilterListeners = new ArrayList<>();

    private static NeuronManager annotationModel;
    private TmModelManager modelManager;
    private NeuronModel neuronManager;
    private final LoadTimer addTimer = new LoadTimer();

    private boolean select = true;
    private boolean applyFilter = false;

    private final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();

    // ----- constants
    // how far away to try to put split anchors (pixels)
    private static final Double SPLIT_ANCHOR_DISTANCE = 60.0;
    private FilteredAnnotationModel filteredAnnotationModel;

    public static NeuronManager getInstance() {
        return annotationModel;
    }

    public void branchReviewed (TmNeuronMetadata neuron, List<TmGeoAnnotation> annotations) {
        for (TaskReviewListener listener: taskReviewListeners) {
            listener.neuronBranchReviewed(neuron, annotations);
        }
    }

    public NeuronManager(TmModelManager modelManager) {
        this.modelManager = modelManager;
        this.tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    public Collection<TmNeuronMetadata> getNeuronList() {
        if (applyFilter) {
            Set<Long> filteredIds = neuronFilter.filterNeurons();
            List<TmNeuronMetadata> neuronList = new ArrayList<>();
            for (Long id: filteredIds) {
                TmNeuronMetadata neuron = neuronManager.getNeuronById(id);
                if (neuron!=null)
                    neuronList.add(neuron);
            }
            return neuronList;
        }
        else
            return neuronManager.getNeurons();
    }

    public void addNeuron(TmNeuronMetadata neuron) {
        neuronManager.addNeuron(neuron);
    }
    
   public FilteredAnnotationModel getFilteredAnnotationModel() {
       return filteredAnnotationModel;
   }
    
    public synchronized void clear() {
        log.info("Clearing annotation model");
        currentWorkspace = null;
        currentSample = null;
        currentTagMap = null;
        setCurrentNeuron(null);
        SwingUtilities.invokeLater(() -> fireWorkspaceUnloaded(currentWorkspace));
    }

    public void setSWCDataConverter(SWCDataConverter converter) {
        this.swcDataConverter = converter;
    }

    // current neuron methods
    public TmNeuronMetadata getCurrentNeuron() {
        log.trace("getCurrentNeuron = {}",currentNeuron);
        return currentNeuron;
    }

    // current neuron methods
    public TmGeoAnnotation getCurrentPoint() {
        log.trace("getCurrentPoint = {}",currentVertex);
        return currentVertex;
    }

    // this method sets the current neuron but does not fire an event to update the UI
    private synchronized void setCurrentNeuron(TmNeuronMetadata neuron) {
        log.info("setCurrentNeuron({})",neuron);
        // be sure we're using the neuron object from the current workspace
        if (neuron != null) {
            this.currentNeuron = getNeuronFromNeuronID(neuron.getId());
        } 
        else {
            this.currentNeuron = null;
        }
    }
    
    // used to get current filtered list in annotation panel
    public List<TmNeuronMetadata> getCurrentFilteredNeuronList() {
        return currentFilteredNeuronList ;
    }
    
    public void setCurrentFilteredNeuronList(List<TmNeuronMetadata> neuronList) {
        currentFilteredNeuronList = neuronList;
    }

    // this method sets the current neuron *and* updates the UI; null neuron means deselect
    public void selectNeuron(TmNeuronMetadata neuron) {
        if (neuron != null && getCurrentNeuron() != null && neuron.getId().equals(getCurrentNeuron().getId())) {
            return;
        }
        setCurrentNeuron(neuron); // synchronized on this NeuronManager here
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronSelected(neuron);
            }
        });
        if (modelManager.getCurrentWorkspace()!=null && neuron!=null) {
            //activityLog.logSelectNeuron(modelManager.getCurrentWorkspace().getId(), neuron.getId());
        }
    }

    // placeholder for any non listener methods that need to update neuron information
    // prior to listener methods
    public void updateNeuronFilter(TmNeuronMetadata neuronMeta, NeuronMessageConstants.MessageType remoteAction) {
        if (!applyFilter)
            return;
        
        TmNeuronMetadata neuron = neuronManager.getNeuronById(neuronMeta.getId());
        if (neuron==null)
            return;
        
        String systemUser = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
        if (systemUser==null)
            return;
        
        if (!neuron.getOwnerKey().equals(systemUser)) {                      
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    NeuronUpdates updates = null;
                    switch (remoteAction) {
                        case NEURON_CREATE:
                            updates = neuronFilter.addNeuron(neuron);
                            break;
                        case NEURON_DELETE:
                            updates = neuronFilter.deleteNeuron(neuron);
                            break;
                        case NEURON_SAVE_NEURONDATA:
                        case NEURON_OWNERSHIP_DECISION:
                            updates = neuronFilter.updateNeuron(neuron);
                            break;
                    }
            
                    if (updates!=null) {
                        updateFrags(updates);
                        
                    }
                }
            });
        }
            
        
    }
    
    // purely used for updating the spatial filter for selection-oriented filtering strategies
    public void selectPoint(Long neuronId, Long annotationId) {
        TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronId);
        TmGeoAnnotation annotation = neuron.getGeoAnnotationMap().get(annotationId);

        if (annotation == null) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (applyFilter) {
                    Stopwatch stopwatch = Stopwatch.createStarted();

                    NeuronUpdates updates = neuronFilter.selectVertex(annotation);                    
                    updateFrags(updates);
                    setCurrentNeuron(neuron);
                    currentVertex = annotation;
                    selectNeuron(neuron);
                    log.info("TOTAL FRAG UPDATE TIME: {}",stopwatch.elapsed().toMillis());
                    stopwatch.stop();
                }
            }
        });
    }

    // convenience methods

    public TmGeoAnnotation getGeoAnnotationFromID(Long neuronID, Long annotationID) {
        TmNeuronMetadata foundNeuron = getNeuronFromNeuronID(neuronID);
        if (foundNeuron == null) {
            log.warn("There is no neuron with id: {}", neuronID);
        }
        return getGeoAnnotationFromID(foundNeuron, annotationID);
    }

    /**
     * given the ID of an annotation, return an object wrapping it (or null)
     */
    public TmGeoAnnotation getGeoAnnotationFromID(TmNeuronMetadata foundNeuron, Long annotationID) {
        if (foundNeuron == null) {
            return null;
        }
        TmGeoAnnotation annotation = foundNeuron.getGeoAnnotationMap().get(annotationID);
        if (annotation == null) {
            log.warn("There is no annotation with id {} in neuron {}", annotationID, foundNeuron.getId());
        }
        return annotation;
    }
    
    public TmNeuronMetadata getNeuronFromNeuronID(Long neuronID) {
        TmNeuronMetadata foundNeuron = neuronManager.getNeuronById(neuronID);
        if (foundNeuron == null) {
            // This happens, for example, when a new workspace is loaded and we try to find the previous nextParent anchor.
            log.warn("There is no neuron with id: {}", neuronID);
        }
        return foundNeuron;
    }

    /**
     * given an annotation, find its ultimate parent, which is the root of
     * its neurite
     */
    public TmGeoAnnotation getNeuriteRootAnnotation(TmGeoAnnotation annotation) {
        if (annotation == null) {
            return annotation;
        }

        TmNeuronMetadata neuron = getNeuronFromNeuronID(annotation.getNeuronId());   
        TmGeoAnnotation current = annotation;
        TmGeoAnnotation parent = neuron.getParentOf(current);
        while (parent !=null) {
            current = parent;
            parent = neuron.getParentOf(current);
        }
        return current;
    }

    /**
     * given two annotations, return true if they are on the same neurite
     * (ie, share the same ultimate root annotation)
     */
    public boolean sameNeurite(TmGeoAnnotation ann1, TmGeoAnnotation ann2) {
        return getNeuriteRootAnnotation(ann1).getId().equals(getNeuriteRootAnnotation(ann2).getId());
    }

    /** given two annotations, find the first common shared ancestor, or null if
     * there isn't one
     */
    public TmGeoAnnotation findCommonParent(TmGeoAnnotation ann1, TmGeoAnnotation ann2) {
        // get the list of ancestors for the first annotation
        Set<TmGeoAnnotation> path1 = new HashSet<>();
        path1.add(ann1);
        TmGeoAnnotation temp = ann1;
        while (!temp.isRoot()) {
            temp = getGeoAnnotationFromID(temp.getNeuronId(), temp.getParentId());
            path1.add(temp);
        }

        // now traverse up the list of ancestors of the second annotation and look for a match
        temp = ann2;
        if (path1.contains(temp)) {
            return temp;
        }
        while (!temp.isRoot()) {
            temp = getGeoAnnotationFromID(temp.getNeuronId(), temp.getParentId());
            if (path1.contains(temp)) {
                return temp;
            }
        }
        // no common root found
        return null;
    }

    /**
     * find the annotation closest to the input location, excluding
     * the input annotation (null = don't exclude any)
     */
    public TmGeoAnnotation getClosestAnnotation(Vec3 micronLocation, TmGeoAnnotation excludedAnnotation) {

        double x = micronLocation.getX();
        double y = micronLocation.getY();
        double z = micronLocation.getZ();

        TmGeoAnnotation closest = null;
        // our valid IDs are positive, so this will never match
        final Long excludedAnnotationID = excludedAnnotation == null ? -1L : excludedAnnotation.getId();

        log.trace("getClosestAnnotation to {}", excludedAnnotationID);
        
       /* List<NeuronVertex> vertexList = neuronSetAdapter.getAnchorClosestToMicronLocation(new double[]{x, y, z}, 1, new SpatialFilter() {
            @Override
            public boolean include(NeuronVertex vertex, TmGeoAnnotation annotation) {
                boolean notItself = !annotation.getId().equals(excludedAnnotationID);
                NeuronStyle style = getNeuronStyle(getNeuronFromNeuronID(annotation.getNeuronId()));
                boolean visible = style.isVisible();
                return notItself && visible;
            }
        });
       
        if (vertexList != null && !vertexList.isEmpty()) {
            log.trace("Got {} anchors closest to {}", vertexList.size(), micronLocation);
            NeuronVertex vertex = vertexList.get(0);
           // closest = ((NeuronVertexAdapter) vertex).getTmGeoAnnotation();
        }
*/
        if (closest!=null) {
            log.trace("Returning closest anchor: {}", closest.getId());
        }
        
        return closest;
    }

    /**
     * rename the given neuron
     */
    public synchronized void renameCurrentNeuron(String name) throws Exception {
        // rename whatever neuron was current at time of start of this call.
        final TmNeuronMetadata neuron = getCurrentNeuron();
        neuron.setName(name);
        this.neuronManager.saveNeuronData(neuron);
        log.info("Neuron was renamed: "+neuron);

        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronRenamed(neuron);
                //activityLog.logRenameNeuron(workspace.getId(), neuron.getId());
            }
        });
    }

    /**
     * change the ownership of the input neuron
     */
    public synchronized void changeNeuronOwner(Long neuronID, Subject newOwner) throws Exception {
        final TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronID);
        CompletableFuture<Boolean> future = getNeuronManager().requestAssignmentChange(neuron, newOwner.getKey());
        if (future == null) {
            presentError("Error while attempting to request ownership of " + neuron.getName(), "Error changing owner");
            log.error("Completeable future is null when requesting owner change of neuron " + neuron.getName() +
                " to owner " + newOwner.getKey());
            return;
        } else {
            // wait for the future to finish (blocks)
            Boolean ownershipDecision = future.get();
            if (!ownershipDecision) {
                presentError("Error while attempting to request ownership of " + neuron.getName(), "Error changing owner");
                log.error("Ownership change rejected; neuron " + neuron.getName() + " owned by " +
                    neuron.getOwnerName() + " not " + newOwner.getKey());
                return;
            }
        }

        // it's now safe to change local object
        neuron.setOwnerKey(newOwner.getKey());

        // if filter, find new fragments that might be affected
        if (applyFilter && neuron.getOwnerKey().equals(AccessManager.getAccessManager().getActualSubject().getKey())) {
            NeuronUpdates updates = neuronFilter.addNeuron(neuron);
            updateFrags(updates);
        }

        log.info("Neuron " + neuron.getName() + " owner changed to  " + newOwner.getKey());

        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        final List<TmNeuronMetadata> neuronList = new ArrayList<>();
        neuronList.add(neuron);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronsOwnerChanged(neuronList);
                //activityLog.logChangeNeuronOwner(workspace.getId(), neuron.getId());
            }
        });
    }

    public synchronized void deleteCurrentNeuron() throws Exception {
        TmNeuronMetadata currentNeuron = getCurrentNeuron();
        if (currentNeuron == null) {
            return;
        }
        deleteNeuron(currentNeuron);
        setCurrentNeuron(null);
    }

    public synchronized void deleteNeuron(final TmNeuronMetadata deletedNeuron) throws Exception {

        // delete
        neuronManager.deleteNeuron(currentWorkspace, deletedNeuron);
        log.info("Neuron was deleted: "+deletedNeuron);

        // updates
        final TmWorkspace workspace = modelManager.getCurrentWorkspace();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // if filter, add and remove fragments as necessary
                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.deleteNeuron(deletedNeuron);
                        updateFrags(updates);
                    }
        
                    fireNeuronSelected(null);
                    fireNeuronDeleted(deletedNeuron);
                }
                finally {
                }
                ////activityLog.logDeleteNeuron(workspace.getId(), deletedNeuron.getId());
            }
        });
    }

    /**
     * Create a new workspace for the given sample, owned by the current user.
     * @param sampleId = tiled microscope sample ID
     * @param name = name of new workspace
     * @throws Exception
     */
    public synchronized TmWorkspace createWorkspace(Long sampleId, String name) throws Exception {
        TmWorkspace workspace = tmDomainMgr.createWorkspace(sampleId, name);
        //activityLog.logCreateWorkspace(workspace.getId());
        return workspace;
    }

    /**
     * create a neuron in the current workspace
     *
     * @param name = name of neuron
     * @throws Exception
     */
    public synchronized TmNeuronMetadata createNeuron(String name) throws Exception {
        neuronManager.createTiledMicroscopeNeuron(modelManager.getCurrentWorkspace(), name);
        // replace with code from prod
        TmNeuronMetadata neuron = null;

        // Update local workspace
        log.info("Neuron was created: "+neuron);
        setCurrentNeuron(neuron);

        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // if filter, find new fragments that might be affected
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.addNeuron(neuron);
                    updateFrags(updates);
                }

                fireNeuronCreated(neuron);
                fireNeuronSelected(neuron);
                // //activityLog.logCreateNeuron(workspace.getId(), neuron.getId());
            }
        });

        return neuron;
    }

    /**
     * Create a new copy of the given workspace, owned by the current user.
     * @param workspace = workspace object
     * @param name = name of new workspace
     * @throws Exception
     */
    public synchronized TmWorkspace copyWorkspace(TmWorkspace workspace, String name, String assignOwner) throws Exception {
        TmWorkspace workspaceCopy = tmDomainMgr.copyWorkspace(workspace, name, assignOwner);
        //activityLog.logCreateWorkspace(workspace.getId());
        return workspaceCopy;
    }
    
    /**
     * add a root annotation to the given neuron; this is an annotation without a parent
     *
     * @param neuron = neuron object
     * @param xyz = x, y, z location of new annotation
     * @throws Exception
     */
    public synchronized TmGeoAnnotation addRootAnnotation(final TmNeuronMetadata neuron, final Vec3 xyz) throws Exception {
        // the null in this call means "this is a root annotation" (would otherwise
        //  be the parent).  Updates to neuron's collections are done in the
        //  as well.
        final TmGeoAnnotation annotation = neuronManager.addGeometricAnnotation(
                neuron, neuron.getId(), xyz.x(), xyz.y(), xyz.z());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnnotationAdded(annotation);
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                    updateFrags(updates);
                    selectPoint(neuron.getId(), annotation.getId());
                }
                //activityLog.logEndOfOperation(getWsId(), xyz);
            }
        });

        return annotation;
    }

    /**
     * add a child annotation
     *
     * @param parentAnn = parent annotation object
     * @param xyz = location of new child annotation
     * @throws Exception
     */
    public synchronized TmGeoAnnotation addChildAnnotation(TmGeoAnnotation parentAnn, final Vec3 xyz) throws Exception {
        if (parentAnn == null) {
            return null;
        }

        addTimer.mark("start addChildAnn");
                 
        final TmNeuronMetadata neuron = getNeuronFromNeuronID(parentAnn.getNeuronId());
                
        final TmGeoAnnotation annotation = neuronManager.addGeometricAnnotation(
                neuron, parentAnn.getId(), xyz.x(), xyz.y(), xyz.z());
        annotation.setRadius(parentAnn.getRadius());

        log.info("Added annotation {} to neuron {}", annotation.getId(), neuron);
        
        // the parent may lose some predefined notes (finished end, possible branch)
        stripPredefNotes(neuron, parentAnn.getId());
        
        if (automatedTracingEnabled()) {
           // if (viewStateListener != null)
             //   viewStateListener.pathTraceRequested(annotation.getNeuronId(), annotation.getId());
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnnotationAdded(annotation);
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                    updateFrags(updates);
                    selectPoint(neuron.getId(), annotation.getId());
                   // SkeletonController.getInstance().setNextParent(annotation.getId());
                }
                //activityLog.logEndOfOperation(getWsId(), xyz);
            }
        });
        
        addTimer.mark("end addChildAnn");
        // reset timer state; we don't care about end > start
        addTimer.clearPreviousStepName();

        return annotation;
    }

    /**
     * move an existing annotation to a new location
     *
     * @param annotationID = ID of annotation to move
     * @param location = new location
     * @throws Exception
     */
    public synchronized void moveAnnotation(final Long neuronID, final Long annotationID, final Vec3 location) throws Exception {
        final TmNeuronMetadata neuron = this.getNeuronFromNeuronID(neuronID);
        final TmGeoAnnotation annotation = getGeoAnnotationFromID(neuron, annotationID);

        // find each connecting annotation; if there's a traced path to it,
        //  remove it (refresh annotation!)
        // at the same time, delete the paths out of the local neuron object, too
        TmGeoAnnotation parent = neuron.getParentOf(annotation);
        if (parent != null) {
            removeAnchoredPath(neuron, annotation, parent);
            neuron.getAnchoredPathMap().remove(new TmAnchoredPathEndpoints(annotation.getId(), parent.getId()));
        }
        for (TmGeoAnnotation neighbor: neuron.getChildrenOf(annotation)) {
            removeAnchoredPath(neuron, annotation, neighbor);
            neuron.getAnchoredPathMap().remove(new TmAnchoredPathEndpoints(annotation.getId(), neighbor.getId()));
        }

        // update local annotation object
        synchronized(annotation) {
            annotation.setX(location.getX());
            annotation.setY(location.getY());
            annotation.setZ(location.getZ());
        }

        try {
            // Update value in database.
            synchronized(neuron) {
                neuronManager.saveNeuronData(neuron);
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireAnnotationMoved(annotation);
                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                        updateFrags(updates);
                        selectPoint(neuron.getId(), annotation.getId());
                    }
                    //activityLog.logEndOfOperation(getWsId(), location);
                }
            });
        } 
        catch (Exception e) {
            // error means not persisted; however, in the process of moving,
            //  the marker's already been moved, to give interactive feedback
            //  to the user; so in case of error, tell the view to update to current
            //  position (pre-move)
            // this is unfortunately untested, because I couldn't think of an
            //  easy way to simulate or force a failure!
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireAnnotationNotMoved(annotation);
                    //activityLog.logEndOfOperation(getWsId(), location);
                }
            });
            throw e;
        }

        log.info("Moved annotation {} in neuron {} to {}", annotation.getId(), neuron.getId(), location);
        
        //final TmWorkspace workspace = modelManager.getCurrentWorkspace();

        if (automatedTracingEnabled()) {
            // trace to parent, and each child to this parent:
           // viewStateListener.pathTraceRequested(annotation.getNeuronId(), annotation.getId());
            for (TmGeoAnnotation child : neuron.getChildrenOf(annotation)) {
             //   viewStateListener.pathTraceRequested(child.getNeuronId(), child.getId());
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //activityLog.logEndOfOperation(getWsId(), location);
            }
        });
    }

    /**
     * change radius of an existing annotation
     *
     * @param annotationID = ID of annotation to move
     * @param radius = new radius, in units of micrometers
     * @throws Exception
     */
    public synchronized void updateAnnotationRadius(final Long neuronID, final Long annotationID, final float radius) throws Exception {
        final TmNeuronMetadata neuron = this.getNeuronFromNeuronID(neuronID);
        final TmGeoAnnotation annotation = getGeoAnnotationFromID(neuron, annotationID);

        // update local annotation object
        final Double oldRadius = annotation.getRadius();
        synchronized(annotation) {
            annotation.setRadius(new Double(radius));
        }

        try {
            // Update value in database.
            synchronized(neuron) {
                neuronManager.saveNeuronData(neuron);
            }
            
        } catch (Exception e) {
            // Rollback
            annotation.setRadius(oldRadius);
            throw e;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnnotationRadiusUpdated(annotation);
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                    updateFrags(updates);
                }
            }
        });
        
        log.info("Updated radius for annotation {} in neuron {}", annotation.getId(), neuron);

        if (automatedTracingEnabled()) {
            // trace to parent, and each child to this parent:
            //viewStateListener.pathTraceRequested(annotation.getNeuronId(), annotation.getId());
            for (TmGeoAnnotation child : neuron.getChildrenOf(annotation)) {
              //  viewStateListener.pathTraceRequested(child.getNeuronId(), child.getId());
            }
        }
    }

    /**
     * change radius for every annotation in a neuron
     *
     * @param neuronID = neuron ID
     * @param radius = radius, microns
     * @throws Exception
     */
    public synchronized void updateNeuronRadius(final Long neuronID, final float radius) throws Exception {

        final TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronID);
        Map<Long, Double> oldRadii = new HashMap<>();

        for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
            for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                oldRadii.put(ann.getId(), ann.getRadius());
                synchronized (ann) {
                    ann.setRadius(new Double(radius));
                }
            }
        }
        try {
            synchronized (neuron) {
                neuronManager.saveNeuronData(neuron);
            }
        } catch (Exception e) {
            // roll back
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                    synchronized (ann) {
                        ann.setRadius(oldRadii.get(ann.getId()));
                    }
                }
            }
            throw e;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronRadiusUpdated(neuron);
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                    updateFrags(updates);
                }
            }
        });

        log.info("Updated radius for neuron {}", neuronID);
    }

    /**
     * merge the neurite that has source Annotation into the neurite containing
     * targetAnnotation
     */
    public synchronized void mergeNeurite(
            final Long sourceNeuronID, final Long sourceAnnotationID, 
            final Long targetNeuronID, final Long targetAnnotationID) throws Exception {

        Stopwatch stopwatch = Stopwatch.createStarted();

        final TmNeuronMetadata targetNeuron = getNeuronFromNeuronID(targetNeuronID);
        final TmGeoAnnotation targetAnnotation = targetNeuron.getGeoAnnotationMap().get(targetAnnotationID);
        final TmGeoAnnotation sourceAnnotation = getGeoAnnotationFromID(sourceNeuronID, sourceAnnotationID);

        final TmNeuronMetadata sourceNeuron = 
                !sourceNeuronID.equals(targetNeuronID) ? 
                getNeuronFromNeuronID(sourceNeuronID) :  targetNeuron;

        // reroot source neurite to source ann
        if (!sourceAnnotation.isRoot()) {
            // log.info("Handling non-root case.");
            neuronManager.rerootNeurite(sourceNeuron, sourceAnnotation);
        }

        // if source neurite not in same neuron as dest neurite: move it; don't
        //  use annModel.moveNeurite() because we don't want those updates & signals yet
        if (!sourceNeuron.getId().equals(targetNeuron.getId())) {
            // log.info("Two different neurons.");
            neuronManager.moveNeurite(sourceAnnotation, sourceNeuron, targetNeuron);
        }


        // reparent source annotation to dest annotation:
        // log.info("Reparenting annotations.");
        neuronManager.reparentGeometricAnnotation(sourceAnnotation, targetAnnotationID, targetNeuron);
        
        log.info("Merged source annotation {} into target annotation {} in neuron {}", sourceAnnotationID, targetAnnotationID, targetNeuron);

        // Establish p/c linkage between target and source.
        // log.info("Parent/child linkages target and source.");
        sourceAnnotation.setParentId(targetAnnotationID);

        setCurrentNeuron(targetNeuron);

        // see note in addChildAnnotations re: predef notes
        // for merge, two linked annotations are affected; fortunately, the
        //  neuron has just been refreshed
        // log.info("Stripping predef notes.");
        final boolean notesChangedSource = stripPredefNotes(targetNeuron, sourceAnnotationID);
        final boolean notesChangedTarget = stripPredefNotes(targetNeuron, targetAnnotationID);
        
        // Save the target neurons; this has more side effects than one would like
        //  first, order matters; do source first so moved annotations are removed there
        //      before being added to the target; this prevents a double-delete in the
        //      spatial index
        //  second, you need to save the source neuron even if you plan to delete it,
        //      again so the annotation moves will be properly accounted for
        //  this is all needed to get around the fact that moving annotations
        //      from one neuron to another isn't atomic like it should be
        neuronManager.saveNeuronData(sourceNeuron);
        neuronManager.saveNeuronData(targetNeuron);

        // trace new path; must be done after neuron save, so the path tracer
        //  can grab the new neuron data; also note that the source annotation
        //  is now in the target neuron
        if (automatedTracingEnabled()) {
            // log.info("Tracing paths.");
           // viewStateListener.pathTraceRequested(targetNeuronID, sourceAnnotationID);
        }

        // If source neuron is now empty, delete it, otherwise save it.
        final boolean sourceDeleted = sourceNeuron.getGeoAnnotationMap().isEmpty();
        if (sourceDeleted) {
            neuronManager.deleteNeuron(currentWorkspace, sourceNeuron); 
            if (applyFilter) {
                NeuronUpdates updates = neuronFilter.deleteNeuron(sourceNeuron);
                updateFrags(updates);
            }
            log.info("Source neuron was deleted: "+sourceNeuron);
        }
        if (applyFilter) {
             NeuronUpdates updates = neuronFilter.updateNeuron(targetNeuron);
             updateFrags(updates);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    
                log.info("MERGE A: {}",stopwatch.elapsed().toMillis());
                    // temporary fix to set index properly
                    final List<TmNeuronMetadata> neuronList = new ArrayList<>();
                    neuronList.add(targetNeuron);
                    
                    if (notesChangedSource) {
                        fireNotesUpdated(sourceAnnotation);
                    }
                    if (notesChangedTarget) {
                        fireNotesUpdated(targetAnnotation);
                    }                   
                    if (sourceDeleted) {
                        fireNeuronDeleted(sourceNeuron);
                    }
                    else {
                        fireNeuronChanged(sourceNeuron);
                    }
                    fireNeuronChanged(targetNeuron);
                    
                log.info("MERGE B: {}",stopwatch.elapsed().toMillis());
                }
                finally {
                    endTransaction();                
                }
                log.info("TOTAL MERGE: {}",stopwatch.elapsed().toMillis());
                stopwatch.stop();
                //activityLog.logEndOfOperation(getWsId(), targetAnnotation);
            }
        });

    }

    /**
     * In order to avoid doing things twice (or more) unnecessarily, we stop any piecemeal UI updates from being made
     * and wait until the transaction to end before doing everything in bulk.
     */
    private void beginTransaction() {
     //   SkeletonController.getInstance().beginTransaction();
     //   FilteredAnnotationList.getInstance().beginTransaction();
        TransactionManager.getInstance().beginTransaction();
    }

    /**
     * Commit everything that changed into UI changes.
     */
    private void endTransaction() {
      //  SkeletonController.getInstance().endTransaction();
      //  FilteredAnnotationList.getInstance().endTransaction();
        TransactionManager.getInstance().endTransaction();
    }
    
    /**
     * move the neurite containing the input annotation to the given neuron
     */
    public synchronized void moveNeurite(final TmGeoAnnotation annotation, final TmNeuronMetadata destNeuron) throws Exception {
        if (eitherIsNull(annotation, destNeuron)) {
            return;
        }
        final TmNeuronMetadata sourceNeuron = getNeuronFromNeuronID(annotation.getNeuronId());
        neuronManager.moveNeurite(annotation, sourceNeuron, destNeuron);
        neuronManager.saveNeuronData(sourceNeuron);
        neuronManager.saveNeuronData(destNeuron);
        if (applyFilter) {
            NeuronUpdates updates = neuronFilter.updateNeuron(sourceNeuron);
            updates = neuronFilter.updateNeuron(destNeuron);
            updateFrags(updates);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    fireNeuronChanged(sourceNeuron);
                    fireNeuronChanged(destNeuron);
                    fireNeuronSelected(destNeuron);
                }
                finally {
                    endTransaction();
                    setSelectMode(true);
                }
                //activityLog.logEndOfOperation(getWsId(), annotation);
            }
        });
    }
    

    /**
     * this method deletes a link, which is defined as an annotation with
     * one parent and no more than one child (not a root, not a branch point)
     * (unless it's a root with no children)
     *
     * @param link = annotation object
     * @throws Exception
     */
    public synchronized void deleteLink(final TmGeoAnnotation link) throws Exception {
        if (link == null) {
            return;
        }

        // check it's not a branch..
        if (link.getChildIds().size() > 1) {
            return;
        }

        // ..or a root with children
        if (link.isRoot() && link.getChildIds().size() > 0) {
            return;
        }

        // check that we can find the neuron
        final TmNeuronMetadata neuron = getNeuronFromNeuronID(link.getNeuronId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            log.error("Unexpected null neuron during anchor deletion");
            return;
        }

        // begin the (long) deletion process
        // reparent the deleted node's child (if there is one) to the node's parent
        TmGeoAnnotation parent = neuron.getParentOf(link);
        TmGeoAnnotation child = null;
        if (link.getChildIds().size() == 1) {
            child = neuron.getChildrenOf(link).get(0);
            {
                // Amina saw a NullPointerException inside the reparentGeometricAnnotation call below.
                // Perhaps logging the trouble could help here
                if (child == null) {
                    log.info("Unexpected null child during anchor deletion");
                    return;
                }
            }
            neuronManager.reparentGeometricAnnotation(child, parent.getId(), neuron);

            // if segment to child had a traced path, remove it
            removeAnchoredPath(neuron, link, child);
        }

        // if segment to parent had a trace, remove it
        removeAnchoredPath(neuron, link, parent);

        // if the link had a note, delete it:
        if (neuron.getStructuredTextAnnotationMap().containsKey(link.getId())) {
            neuron.getStructuredTextAnnotationMap().remove(link.getId());
        }

        // if link had a child, remove link
        if (child != null) {
            link.getChildIds().remove(child.getId());
        }

        // remove link from its parent
        if (!link.isRoot()) {
            parent.getChildIds().remove(link.getId());
        }

        // ...and finally get rid of the link itself; then, we're done, and
        //  the neuron can be serialized
        neuron.getGeoAnnotationMap().remove(link.getId());
        if (link.isRoot()) {
            neuron.removeRootAnnotation(link);
        }
        
        // Async update
        neuronManager.saveNeuronData(neuron);

        log.info("Deleted link annotation {} in neuron {}", link.getId(),  neuron);
        
        // if we're tracing, retrace if there's a new connection
        if (automatedTracingEnabled() && child != null) {
            //viewStateListener.pathTraceRequested(child.getNeuronId(), child.getId());
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                beginTransaction();
                try {
                    // Need to delete the anchor to undraw it
                    fireAnnotationsDeleted(Arrays.asList(link));
                    // Also need to redraw the neurite, because we need the link from the reparenting to appear
                    fireNeuronChanged(neuron);

                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                        updateFrags(updates);
                    }
                }
                finally {
                    endTransaction();
                }
                //activityLog.logEndOfOperation(getWsId(), link);

            }
        });
    }

    /**
     * this method deletes an annotation and all of its children, and its
     * children's children, yea, unto every generation that liveth
     *
     * @param rootAnnotation = annotation to be deleted along with its descendents
     * @throws Exception
     */
    public synchronized void deleteSubTree(final TmGeoAnnotation rootAnnotation) throws Exception {
        if (rootAnnotation == null) {
            return;
        }

        final TmNeuronMetadata neuron = getNeuronFromNeuronID(rootAnnotation.getNeuronId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // grab the parent of the root before the root disappears:
        TmGeoAnnotation rootParent = neuron.getParentOf(rootAnnotation);

        final List<TmGeoAnnotation> notesChanged = new ArrayList<>();
        final List<TmGeoAnnotation> deleteList = neuron.getSubTreeList(rootAnnotation);
        TmStructuredTextAnnotation note;
        for (TmGeoAnnotation annotation: deleteList) {
            // for each annotation, delete any paths traced to its children;
            //  do before the deletion!
            for (TmGeoAnnotation child: neuron.getChildrenOf(annotation)) {
                removeAnchoredPath(neuron, annotation, child);
            }

            note = neuron.getStructuredTextAnnotationMap().get(annotation.getId());
            if (note != null) {
                // don't use removeNote(); it triggers updates we don't want yet
                neuron.getStructuredTextAnnotationMap().remove(annotation.getId());
                notesChanged.add(annotation);
            }
            neuron.getGeoAnnotationMap().remove(annotation.getId());
            if (annotation.isRoot()) {
                neuron.removeRootAnnotation(annotation);
            }
        }
        // for the root annotation, also delete any traced paths to the parent,
        // if it exists and eliminate the root annotation as a child of the
        // root-parent.
        if (rootParent != null) {
            removeAnchoredPath(neuron, rootAnnotation, rootParent);
            rootParent.getChildIds().remove(rootAnnotation.getId());
        }

        // Must serialize the neuron, after having made changes.
        neuronManager.saveNeuronData(neuron);

        log.info("Deleted sub tree rooted at {} in neuron {}", rootAnnotation.getId(),  neuron);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                beginTransaction();
                try {
                    for (TmGeoAnnotation ann : notesChanged) {
                        fireNotesUpdated(ann);
                    }
                    fireAnnotationsDeleted(deleteList);
                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                        updateFrags(updates);
                    }

                    fireNeuronSelected(neuron);
                }
                finally {
                    endTransaction();
                }
                //activityLog.logEndOfOperation(getWsId(), rootAnnotation);
            }
        });

    }

    /**
     * this method "splits" an annotation; it creates a new annotation on the line
     * between the input annotation and its parent, displaced a bit toward the parent;
     * if it's a root, it inserts in the direction of either its single child, or it's an error
     *
     * @param annotation = annotation to be split
     */
    public synchronized void splitAnnotation(final TmGeoAnnotation annotation) throws Exception {
        if (annotation == null) {
            return;
        }

        // ann1 is the child of ann2 in both cases; if reverse, place the new point
        //  near ann2 instead of ann1
        final TmNeuronMetadata neuron = getNeuronFromNeuronID(annotation.getNeuronId());
        TmGeoAnnotation annotation1;
        TmGeoAnnotation annotation2;
        boolean reverse;

        if (annotation.isRoot()) {
            // root case is special; if one child, split toward child; otherwise, error
            //  (with zero or many children, ambiguous where to put new annotation)
            if (annotation.getChildIds().size() != 1) {
                throw new Exception("cannot split root annotation with zero or many children");
            }
            annotation1 = neuron.getChildrenOf(annotation).get(0);
            annotation2 = annotation;
            reverse = true;
        } else {
            // regular point: split toward the parent
            annotation1 = annotation;
            annotation2 = neuron.getParentOf(annotation);
            reverse = false;
        }

        // heuristic: try for a default separation in pixels; we don't want to
        //  go bigger, but we may need to go smaller; if the two anchors are
        //  already really close together, don't go beyond the halfway point
        //  between them
        // (remember, our t=0 is the original point)
        ParametrizedLine pLine = new ParametrizedLine(
                new Vec3(annotation1.getX(), annotation1.getY(), annotation1.getZ()),
                new Vec3(annotation2.getX(), annotation2.getY(), annotation2.getZ())
        );
        Double t = pLine.parameterFromPathLength(SPLIT_ANCHOR_DISTANCE);
        if (t > 0.5) {
            t = 0.5;
        }
        if (reverse) {
            t = 1.0 - t;
        }
        Vec3 newPoint = pLine.getPoint(t);

        // create the new annotation, child of original parent
        final TmGeoAnnotation newAnnotation = neuronManager.addGeometricAnnotation(neuron,
                annotation2.getId(), newPoint.x(), newPoint.y(), newPoint.z());
        
        // set radius of new point to an intermediate value
        double newRadius = DefaultNeuron.radius;
        if (annotation2.getRadius() != null) {
            newRadius = annotation2.getRadius();
            if (annotation1.getRadius() != null) {
                double r1 = annotation1.getRadius();
                double r2 = annotation2.getRadius();
                newRadius = t * r2 + (1.0 - t) * r1;
            }
        }
        else if (annotation1.getRadius() != null) {
            newRadius = annotation1.getRadius();
        }
        newAnnotation.setRadius(newRadius);

        //  reparent existing annotation to new annotation
        neuronManager.reparentGeometricAnnotation(annotation1, newAnnotation.getId(), neuron);

        // if that segment had a trace, remove it
        removeAnchoredPath(neuron, annotation1, annotation2);
        neuronManager.saveNeuronData(neuron);

        log.info("Split at annotation {} in neuron {}", annotation.getId(),  neuron);
        
        // retrace
        if (automatedTracingEnabled()) {
          //  if (viewStateListener != null) {
            //    viewStateListener.pathTraceRequested(newAnnotation.getNeuronId(), newAnnotation.getId());
              //  viewStateListener.pathTraceRequested(annotation1.getNeuronId(), annotation1.getId());
            //}
        }

        final TmGeoAnnotation updateAnnotation = neuron.getGeoAnnotationMap().get(annotation1.getId());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                beginTransaction();
                try {
                    fireAnnotationAdded(newAnnotation);
                    fireAnnotationReparented(updateAnnotation, neuron.getId());
                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                        updateFrags(updates);
                        selectPoint(neuron.getId(), annotation.getId());
                    }
                }
                finally {
                    endTransaction();
                }
                //activityLog.logEndOfOperation(getWsId(), annotation);
            }
        });
    }

    /**
     * reroot a neurite at the input annotation; it becomes the top level parent, and
     * other annotations' relations are adjusted to compensate
     *
     * @param newRootID = ID of new root annotation for neurite
     * @throws Exception
     */
    public synchronized void rerootNeurite(Long neuronId, Long newRootID) throws Exception {
        // do it in the DAO layer
        final TmGeoAnnotation newRoot = getGeoAnnotationFromID(neuronId, newRootID);
        TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronId);
        neuronManager.rerootNeurite(neuron, newRoot);

        // see notes in addChildAnnotation re: the predef notes
        // in this case, the new root is the only annotation we need to check
        final boolean notesChangedFinal = stripPredefNotes(neuron, newRootID);
        
        neuronManager.saveNeuronData(neuron);

        log.info("Rerooted at annotation {} in neuron {}", newRootID,  neuron);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (notesChangedFinal) {
                    fireNotesUpdated(newRoot);
                }
                fireNeuronChanged(neuron);
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                    updateFrags(updates);
                }
                //activityLog.logEndOfOperation(getWsId(), newRoot);
            }
        });
    }

    /**
     * split a neurite at the input node; the node is detached from its parent, and it and
     * its children become a new neurite in the same neuron
     *
     * @param newRootID = ID of root of new neurite
     * @throws Exception
     */
    public synchronized void splitNeurite(final Long neuronID, final Long newRootID) throws Exception {
        final TmGeoAnnotation newRoot = getGeoAnnotationFromID(neuronID, newRootID);
        final TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronID);
        TmGeoAnnotation newRootParent = neuron.getParentOf(newRoot);
        removeAnchoredPath(neuron, newRoot, newRootParent);
        neuronManager.splitNeurite(neuron, newRoot);

        // update domain objects and database, and notify
        neuronManager.saveNeuronData(neuron);

        log.info("Split neuron at annotation {} in neuron {}", newRootID,  neuron);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                beginTransaction();
                try {
                    TmGeoAnnotation newRootAnnotation = neuron.getGeoAnnotationMap().get(newRootID);
                    if (newRootAnnotation == null) {
                        // Happens during Horta undo-merge-neurites. I'm Not sure why.
                        log.warn("Failed to find new annotation after splitNeurite");
                    }
                    else {
                        fireAnnotationReparented(newRootAnnotation, neuron.getId());
                    }
                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                        updateFrags(updates);
                    }
                    fireNeuronSelected(neuron);
                }
                finally {
                    endTransaction();
                }
                //activityLog.logEndOfOperation(getWsId(), newRoot);
            }
        });
    }

    public synchronized void addAnchoredPath(final Long neuronID, final TmAnchoredPathEndpoints endpoints, List<List<Integer>> points) throws Exception{

        // check we can find both endpoints in same neuron
        //  don't need to check that they are neighboring; UI gesture already enforces it
        TmNeuronMetadata neuron1 = getNeuronFromNeuronID(neuronID);
        if (neuron1==null) {
            // something's been deleted
            return;
        }

        // now verify that endpoints for path are still where they were when path
        //  was being drawn (ie, make sure user didn't move the endpoints in the meantime)
        // check that the first and last points in the list match the current locations of the
        //  annotations, in some order (despite stated convention, I have not found the point
        //  list to be in consistent order vis a vis the ordering of the annotation IDs)

        TmGeoAnnotation ann1 = neuron1.getGeoAnnotationMap().get(endpoints.getFirstAnnotationID());
        TmGeoAnnotation ann2 = neuron1.getGeoAnnotationMap().get(endpoints.getSecondAnnotationID());

        boolean order1 = annotationAtPoint(ann1, points.get(0)) && annotationAtPoint(ann2, points.get(points.size() - 1));
        boolean order2 = annotationAtPoint(ann2, points.get(0)) && annotationAtPoint(ann1, points.get(points.size() - 1));
        if (!order1 && !order2) {
            // something's been moved; we should log this?
            return;
        }


        // if a path between those endpoints exists, remove it first:
        if (neuron1.getAnchoredPathMap().containsKey(endpoints)) {
            removeAnchoredPath(neuron1, neuron1.getAnchoredPathMap().get(endpoints));
        }

        // transform point list and persist
        final TmAnchoredPath path = neuronManager.addAnchoredPath(neuron1, endpoints.getFirstAnnotationID(),
                endpoints.getSecondAnnotationID(), points);

        log.info("Added anchored path {} in neuron {}", path.getId(),  neuron1);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnchoredPathAdded(neuronID, path);
                if (applyFilter) {
                    NeuronUpdates updates = neuronFilter.updateNeuron(neuron1);
                    updateFrags(updates);
                }
                //activityLog.logAddAnchoredPath(modelManager.getCurrentWorkspace().getId(), path.getId());
            }
        });

    }

    /**
     * used in addAnchoredPath; is the annotation's location, truncated to integer, the
     * same as the list of integers?
     */
    private boolean annotationAtPoint(TmGeoAnnotation annotation, List<Integer> pointList) {
        return Math.abs( annotation.getX().intValue() - pointList.get(0) ) < 5 &&
                Math.abs( annotation.getY().intValue() - pointList.get(1) ) < 5 &&
                Math.abs( annotation.getZ().intValue() - pointList.get(2) ) < 5;
    }

    /**
     * remove an anchored path between two annotations, if one exists;
     * should only be called within NeuronManager, and it does not
     * persist neuron or do any other cleanup
     */
    private void removeAnchoredPath(final TmNeuronMetadata neuron, final TmAnchoredPath path) throws  Exception {
        // Remove the anchor path from its containing neuron
        neuron.getAnchoredPathMap().remove(path.getEndpoints());

        final ArrayList<TmAnchoredPath> pathList = new ArrayList<>();
        pathList.add(path);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnchoredPathsRemoved(neuron.getId(), pathList);
                //activityLog.logRemoveAnchoredPath(modelManager.getCurrentWorkspace().getId(), path.getId());
            }
        });
    }

    private void removeAnchoredPath(TmNeuronMetadata neuron, TmGeoAnnotation annotation1, TmGeoAnnotation annotation2)
            throws Exception {
        if (eitherIsNull(annotation1, annotation2)) {
            return;
        }

        // we assume second annotation is in same neuron; if it's not, there's no path
        //  to remove anyway
        TmAnchoredPathEndpoints endpoints = new TmAnchoredPathEndpoints(annotation1.getId(), annotation2.getId());
        if (neuron.getAnchoredPathMap().containsKey(endpoints)) {
            removeAnchoredPath(neuron, neuron.getAnchoredPathMap().get(endpoints));
        }
    }

    public String getNote(Long annotationID, TmNeuronMetadata neuron) {
        final TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(annotationID);
        if (textAnnotation != null) {
            JsonNode rootNode = textAnnotation.getData();
            JsonNode noteNode = rootNode.path("note");
            if (!noteNode.isMissingNode()) {
                return noteNode.asText();
            }
        }
        return "";
    }

    public String getNote(final Long neuronID, Long annotationID) {
        TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronID);
        return getNote(annotationID, neuron);
    }

    /**
     * add or update a note on a geometric annotation
     */
    public synchronized void setNote(final TmGeoAnnotation geoAnnotation, final String noteString) throws Exception {
        TmNeuronMetadata neuron = getNeuronFromNeuronID(geoAnnotation.getNeuronId());
        if (neuron == null) {
            throw new Exception("can't find neuron for annotation with ID " + geoAnnotation.getId());
        }

        TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(geoAnnotation.getId());
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "";
        if (textAnnotation != null) {
            // if you've got a structured text annotation already, use it; for now, you only get one
            JsonNode rootNode = textAnnotation.getData();
            if (noteString.length() > 0) {
                ((ObjectNode) rootNode).put("note", noteString);
                jsonString = mapper.writeValueAsString(rootNode);
                neuronManager.updateStructuredTextAnnotation(neuron, textAnnotation, jsonString);
            } else {
                // there is a note attached, but we want it gone; if it's the only thing there,
                //  delete the whole structured text annotation
                ((ObjectNode) rootNode).remove("note");
                if (rootNode.size() > 0) {
                    jsonString = mapper.writeValueAsString(rootNode);
                    neuronManager.updateStructuredTextAnnotation(neuron, textAnnotation, jsonString);
                } else {
                    // otherwise, there's something left, so persist it (note: as of this
                    //  writing, there aren't any other structured text annotations besides
                    //  note, but no need to get sloppy!)
                    neuronManager.deleteStructuredTextAnnotation(neuron, textAnnotation.getParentId());
                }
            }

        } else {
            // it doesn't exist; if input is also null, don't even bother
            if (noteString.length() > 0) {
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("note", noteString);

                jsonString = mapper.writeValueAsString(rootNode);
                textAnnotation = neuronManager.addStructuredTextAnnotation(neuron, geoAnnotation.getId(), jsonString);
            }
        }

        // update modification date in for the geo annotation whose text annotation
        geoAnnotation.updateModificationDate();

        // Send the data back to the server to save.
        neuronManager.saveNeuronData(neuron);

        log.info("Set note on annotation {} in neuron {}", geoAnnotation.getId(),  neuron);
        
        final TmWorkspace workspace = modelManager.getCurrentWorkspace();

        SwingUtilities.invokeLater(() -> {
            fireNotesUpdated(geoAnnotation);
            //activityLog.logSetNote(workspace.getId(), geoAnnotation.getId(), noteString);
        });
    }

    public synchronized void removeNote(final Long neuronID, final TmStructuredTextAnnotation textAnnotation) throws Exception {

        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        TmNeuronMetadata neuron = getNeuronFromNeuronID(neuronID);
        neuronManager.deleteStructuredTextAnnotation(neuron, textAnnotation.getParentId());
        final TmGeoAnnotation ann = getGeoAnnotationFromID(neuron, textAnnotation.getParentId());
        ann.updateModificationDate();
        neuronManager.saveNeuronData(neuron);

        log.info("Removed note on annotation {} in neuron {}", ann.getId(),  neuron);
        
        // updates
        SwingUtilities.invokeLater(() -> {
            fireNotesUpdated(ann);
            //activityLog.logRemoveNote(workspace.getId(), textAnnotation.getParentId());
        });
    }

    public boolean getNeuronVisibility(TmNeuronMetadata neuron) {
        if (neuron == null) {
            // doesn't really matter if the neuron doesn't exist
            return true;
        }
        Set<String> neuronTags = getUserNeuronTags(neuron);
        if (neuronTags!=null) {
            if (neuronTags.contains(NEURON_TAG_VISIBILITY)) {
                return false;
            }
        }
        return true;
    }

    /*public void setNeuronVisibility(TmNeuronMetadata neuron, boolean visibility) {
        NeuronStyle style = getNeuronStyle(neuron);
        if (style.isVisible() != visibility) {
            Map<TmNeuronMetadata,NeuronStyle> styleUpdater = new HashMap<>();
            style.setVisible(visibility);
            styleUpdater.put(neuron, style);
            if (visibility) {
                removeUserNeuronTag(NEURON_TAG_VISIBILITY, neuron);
            } else {
                addUserNeuronTag(NEURON_TAG_VISIBILITY, neuron);
            }
            SwingUtilities.invokeLater(() -> fireNeuronStylesChanged(styleUpdater));
        }
    }

    public void setNeuronVisibility(Collection<TmNeuronMetadata> bulkNeurons, boolean visibility) {
        Map<TmNeuronMetadata, NeuronStyle> styleUpdater = new HashMap<>();
        for (TmNeuronMetadata neuron : bulkNeurons) {
            NeuronStyle style = getNeuronStyle(neuron);
            if (style.isVisible() != visibility) {
                style.setVisible(visibility);
                styleUpdater.put(neuron, style);
                if (visibility) {
                    getAllTagMeta().removeUserTag(NEURON_TAG_VISIBILITY, neuron);
                } else {
                    getAllTagMeta().addUserTag(NEURON_TAG_VISIBILITY, neuron);
                }
            }
        }
        if (styleUpdater.size() > 0) {
            SwingUtilities.invokeLater(() -> fireNeuronStylesChanged(styleUpdater));
        }
    }
*/
    /**
     * change the style for a neuron; synchronized because it could be
     * called from multiple threads, and the update is not atomic
     */
  /*  public synchronized void setNeuronStyle(TmNeuronMetadata neuron, NeuronStyle style) throws Exception {
        neuron.setColor(style.getColor());
        setNeuronVisibility(neuron, style.isVisible());
        neuronManager.saveNeuronData(neuron);
        SwingUtilities.invokeLater(() -> fireNeuronStyleChanged(neuron, style));
        //activityLog.logSetStyle(modelManager.getCurrentWorkspace().getId(), neuron.getId());
    }

    public synchronized void setNeuronColors(List<TmNeuronMetadata> neuronList, Color color) throws Exception {
        
        BulkNeuronStyleUpdate bulkNeuronStyleUpdate = new BulkNeuronStyleUpdate();
        bulkNeuronStyleUpdate.setNeuronIds(DomainUtils.getIds(neuronList));
        bulkNeuronStyleUpdate.setColorHex(ModelTranslation.getColorHex(color));
        tmDomainMgr.updateNeuronStyles(bulkNeuronStyleUpdate);

        Map<TmNeuronMetadata, NeuronStyle> updateMap = new HashMap<>();
        for (TmNeuronMetadata neuron : neuronList) {
            neuron.setColor(color);
            updateMap.put(neuron, getNeuronStyle(neuron));
        }
        
        SwingUtilities.invokeLater(() -> fireNeuronStylesChanged(updateMap));
    }
*/
    /**
     * retrieve a neuron style for a neuron, whether stored or default
     */
  /*  public NeuronStyle getNeuronStyle(TmNeuronMetadata neuron) {
        boolean visibility = getNeuronVisibility(neuron);
        if (neuron.getColor() == null) {
            return NeuronStyle.getStyleForNeuron(neuron.getId(), visibility, false);
        }
        else {
            return new NeuronStyle(neuron.getColor(), visibility, false);
        }
    }
*/
    public boolean automatedRefinementEnabled() {
        return modelManager.getCurrentWorkspace().isAutoPointRefinement();
    }

    public boolean automatedTracingEnabled() {
        return modelManager.getCurrentWorkspace().isAutoTracing();
    }

    /**
     * examine the input annotation and remove any predefined notes which
     * are no longer valid
     */
    private synchronized boolean stripPredefNotes(TmNeuronMetadata neuron, Long annID) throws Exception {
        String noteText = getNote(annID, neuron);
        boolean modified = false;
        if (noteText.length() > 0) {
            List<PredefinedNote> predefList = PredefinedNote.findNotes(noteText);
            for (PredefinedNote predefNote: predefList) {
                if (!predefNote.isValid(neuron, annID)) {
                    // remove it!
                    noteText = noteText.replace(predefNote.getNoteText(), "");
                    modified = true;
                }
            }
            if (modified) {
                setNote(getGeoAnnotationFromID(neuron.getId(), annID), noteText);
                return true;
            }
        }
        return false;
    }

    /**
     * export the neurons in the input list into the given file, in swc format;
     * all neurons (and all their neurites!) are crammed into a single file
     */
    public void exportSWCData(File swcFile, int downsampleModulo, Collection<TmNeuronMetadata> neurons,
        boolean exportNotes, Progress progress) throws Exception {

        log.info("Exporting {} neurons to SWC file {}",neurons.size(),swcFile);
        progress.setStatus("Creating headers");

        // I need the neuron order to be deterministic:
        List<TmNeuronMetadata> neuronList = new ArrayList<>(neurons);

        Map<Long,List<String>> neuronHeaders = new HashMap<>();
        for (TmNeuronMetadata neuron: neuronList) {
            List<String> headers = neuronHeaders.get(neuron.getId());
            if (headers == null) {
                headers = new ArrayList<>();
                neuronHeaders.put(neuron.getId(), headers);
            }
            //NeuronStyle style = getNeuronStyle(neuron);
            //float[] color = style.getColorAsFloatArray();
            //headers.add(String.format(COLOR_FORMAT, color[0], color[1], color[2]));
            if (neuronList.size() > 1) {
                // Allow user to pick name as name of file, if saving individual neuron.
                // Do not save the internal name.
                headers.add(String.format(NAME_FORMAT, neuron.getName()));
            }
        }
        
        progress.setStatus("Exporting neuron files");

        // get swcdata via converter, then write; conversion from TmNeurons is done
        //  all at once so all neurons are off set from the same center of mass
        // First write one file per neuron.
        List<SWCData> swcDatas = swcDataConverter.fromTmNeuron(neuronList, neuronHeaders, downsampleModulo);
        // there's one swc and one note file per neuron, plus aggregate; note how
        //  we set progress; i only increments per neuron, but we show progress over
        //  all files
        int total = 1;
        if (exportNotes) {
            total = 2 * (swcDatas.size() + 1);
        } else {
            total = swcDatas.size() + 1;
        }
        if (swcDatas != null && !swcDatas.isEmpty()) {
            int i = 0;
            for (SWCData swcData: swcDatas) {
                progress.setStatus("Exporting neuron file " + (i + 1));
                if (swcDatas.size() == 1) {
                    swcData.write(swcFile, -1);
                }
                else {
                    swcData.write(swcFile, i);
                }
                progress.setProgress(2 * i, total);

                if (exportNotes) {
                    progress.setStatus("Exporting notes file " + (i + 1));
                    NoteExporter.exportNotes(swcData.getPath(), modelManager.getCurrentWorkspace().getId(), swcData.getNeuronCenter(),
                        neuronList.get(i), swcDataConverter);
                    progress.setProgress(2 * i + 1, total);
                }

                i++;
                // for reasons that are not clear but may involve modification of the workspace
                //  while a long export is happening (particularly now in the shared workspace era),
                //  the number of items processed can occasionally exceed the total, causing an
                //  error in the progress widget; adjust for that
                if (2 * i + 1 >= total) {
                    total = 2 * i + 1;
                }
            }
        }


        // Next write one file containing all neurons, if there are more than one.
        if (swcDatas != null  &&  swcDatas.size() > 1) {
            SWCData swcData = swcDataConverter.fromAllTmNeuron(neuronList, downsampleModulo);
            if (swcData != null) {
                swcData.write(swcFile);
                progress.setStatus("Exporting combined neuron file");
                //activityLog.logExportSWCFile(modelManager.getCurrentWorkspace().getId(), swcFile.getName());

                if (exportNotes) {
                    progress.setStatus("Exporting combined notes file");
                    NoteExporter.exportNotes(swcData.getPath(),  modelManager.getCurrentWorkspace().getId(), swcData.getNeuronCenter(),
                        neuronList, swcDataConverter);
                }
            }
        }

        progress.setProgress(total, total);
        progress.setStatus("Done");
    }

    public synchronized void importBulkSWCData(final File swcFile, TmWorkspace tmWorkspace) throws Exception {

        log.info("Importing neuron from SWC file {}",swcFile);
        
        // the constructor also triggers the parsing, but not the validation
        SWCData swcData = SWCData.read(swcFile);
        if (!swcData.isValid()) {
            throw new Exception(String.format("invalid SWC file %s; reason: %s",
                    swcFile.getName(), swcData.getInvalidReason()));
        }



        // create one neuron for the file; take name from the filename (strip extension)
        String neuronName = swcData.parseName();
        if (neuronName == null) {
            neuronName = swcFile.getName();
        }
        if (neuronName.endsWith(SWCData.STD_SWC_EXTENSION)) {
            neuronName = neuronName.substring(0, neuronName.length() - SWCData.STD_SWC_EXTENSION.length());
        }
        Consumer<Map<String,Object>> callback = this::finishBulkSWCData;
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("swc", swcData);
        parameters.put("file", swcFile);
        
        // Must create the neuron up front, because we need the id when adding the linked geometric annotations below.
        CompletableFuture<TmNeuronMetadata> future = neuronManager.createTiledMicroscopeNeuron(tmWorkspace, neuronName);
        if (future==null)
            return;
        try {
            TmNeuronMetadata updatedNeuron = future.get(5, TimeUnit.SECONDS);
            parameters.put("neuron", updatedNeuron);
            finishBulkSWCData(parameters);
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }

    public synchronized void finishBulkSWCData(Map<String,Object> neuronData) {
        TmNeuronMetadata neuron = (TmNeuronMetadata)neuronData.get("neuron");
        if (neuron==null)
            return;

        SWCData swcData = (SWCData)neuronData.get("swc");
        if (swcData==null)
            return;

        File swcFile = (File)neuronData.get("file");
        // note from CB, July 2013: Vaa3d can't handle large coordinates in swc files,
        //  so he added an OFFSET header and recentered on zero when exporting
        // therefore, if that header is present, respect it
        double[] externalOffset = swcData.parseOffset();
        // Bulk update in play.
        // and as long as we're doing brute force, we can update progress
        //  granularly (if we have a worker); start with 5% increments (1/20)
        int totalLength = swcData.getNodeList().size();
        int updateFrequency = totalLength / 20;
        if (updateFrequency == 0) {
            updateFrequency = 1;
        }

        Map<Integer, Integer> nodeParentLinkage = new HashMap<>();

        Map<Integer, TmGeoAnnotation> annotations = new HashMap<>();
        for (SWCNode node : swcData.getNodeList()) {
            // Internal points, as seen in annotations, are same as external
            // points in SWC: represented as voxels. --LLF
            double[] internalPoint = swcDataConverter.internalFromExternal(
                    new double[]{
                            node.getX() + externalOffset[0],
                            node.getY() + externalOffset[1],
                            node.getZ() + externalOffset[2],}
            );

            // Build an external, unblessed annotation.  Set the id to the index.
            Date now = new Date();
            TmGeoAnnotation unserializedAnnotation = new TmGeoAnnotation(
                    new Long(node.getIndex()), null, neuron.getId(),
                    internalPoint[0], internalPoint[1], internalPoint[2], node.getRadius(),
                    now, now
            );

            annotations.put(node.getIndex(), unserializedAnnotation);
            nodeParentLinkage.put(node.getIndex(), node.getParentIndex());
        }

        // Fire off the bulk update.  The "un-serialized" or
        // db-unknown annotations could be swapped for "blessed" versions.
        neuronManager.addLinkedGeometricAnnotationsInMemory(nodeParentLinkage, annotations, neuron);

        // Set neuron color
        float[] colorArr = swcData.parseColorFloats();
        if (colorArr != null) {
            Color color = new Color(colorArr[0], colorArr[1], colorArr[2]);
            neuron.setColor(color);
        }

        try {
            // need to save neuron now; notes have to be attached to the final
            //  annotation IDs, not the placeholders that exist before the save
            neuronManager.saveNeuronData(neuron);

            // check for corresponding notes file; if present, import notes
            // find file; read and parse it
            File notesFile = findNotesFile(swcFile);
            if (notesFile.exists()) {
                // read and parse
                Map<Vec3, String> notes = parseNotesFile(notesFile);

                // add notes to neuron; get a fresh copy that has updated ann IDs
                neuron = neuronManager.getNeuronById(neuron.getId());
                if (notes.size() > 0) {
                    ObjectMapper mapper = new ObjectMapper();

                    // unfortunately, the only way to associate the notes with the nodes
                    //  is through a brute-force search; we need to associate the locations
                    //  with the annotation ID, but those IDs are changed during the save,
                    //  and we can't track the mapping; the spatial index is built later
                    //  and asynchronously, so we don't have access to it now
                    // later testing: added a few random notes to a neuron with 28k nodes;
                    //  import took ~1s with or without notes
                    for (TmGeoAnnotation root : neuron.getRootAnnotations()) {
                        for (TmGeoAnnotation ann : neuron.getSubTreeList(root)) {
                            Vec3 loc = new Vec3(ann.getX(), ann.getY(), ann.getZ());
                            if (notes.containsKey(loc)) {
                                // fortunately, we only need the simplest case seen in setNotes():
                                ObjectNode node = mapper.createObjectNode();
                                node.put("note", notes.get(loc));
                                neuronManager.addStructuredTextAnnotation(neuron, ann.getId(), mapper.writeValueAsString(node));
                            }
                        }
                    }
                    // now save again, with the note data
                    neuronManager.saveNeuronData(neuron);

                    if (applyFilter) {
                        NeuronUpdates updates = neuronFilter.updateNeuron(neuron);
                        updateFrags(updates);
                    }
                    fireNeuronChanged(neuron);
                    fireNeuronSelected(neuron);
                }
            }

            postWorkspaceUpdate(neuron);
        } catch (Exception e) {
            log.info("Error saving neurons", e);
        }
    }

    public synchronized void postWorkspaceUpdate(TmNeuronMetadata neuron) {
        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        // update workspace; update and select new neuron; this will draw points as well
        SwingUtilities.invokeLater(() -> {
            fireWorkspaceLoaded(workspace);
            if (neuron!=null) {
                selectNeuron(neuron);
            }
        });
    }

    private File findNotesFile(File swcFile) {
        String notesBase = FilenameUtils.removeExtension(swcFile.getName());
        Path notePath = swcFile.toPath().getParent().resolve(notesBase + ".json");
        return notePath.toFile();
    }

    private Map<Vec3, String> parseNotesFile(File notesFile) {
        Map<Vec3, String> notes = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = mapper.readTree(notesFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        JsonNode offsetNode = rootNode.path("offset");
        JsonNode neuronsNode = rootNode.path("neurons");
        if (offsetNode.isMissingNode() || neuronsNode.isMissingNode()) {
            // trouble; bail out
            return notes;
        }

        double[] offset = {0.0, 0.0, 0.0};
        for (int i=0; i<3; i++) {
            offset[i] = offsetNode.get(i).asDouble();
        }

        for (JsonNode neuronNode: neuronsNode) {
            JsonNode notesNode = neuronNode.path("notes");
            if (notesNode.isMissingNode()) {
                // trouble; skip this node
                continue;
            }
            for (JsonNode noteNode: notesNode) {

                // from swc part, above:
                double[] point = swcDataConverter.internalFromExternal(
                        new double[]{
                                noteNode.get(0).asDouble() + offset[0],
                                noteNode.get(1).asDouble() + offset[1],
                                noteNode.get(2).asDouble() + offset[2],});
                Vec3 loc = new Vec3(point[0], point[1], point[2]);
                notes.put(loc, noteNode.get(3).asText());
            }
        }
        return notes;
    }

    // and now we have all the NeuronTagMap methods...in each case, it's a simple
    //  wrapper where for mutating calls, we save the map and fire appropriate updates

    public Set<String> getPredefinedNeuronTags() {
        return modelManager.getAllTagMeta().getPredefinedTags();
    }

    public Set<String> getAvailableNeuronTags() {
        Set<String> availableTags = new HashSet<>(getAllNeuronTags());
        availableTags.addAll(getPredefinedNeuronTags());
        return availableTags;
    }

    public Set<String> getNeuronTags(TmNeuronMetadata neuron) {
        return modelManager.getAllTagMeta().getTags(neuron);
    }

    public Set<String> getAllNeuronTags() {
        return modelManager.getAllTagMeta().getAllTags();
    }

    public void saveUserPreferences() throws Exception {
        // for now use the tag map as the user preferences... as preferences increase, generalize the structure
        FrameworkAccess.setRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MOUSELIGHT,
                this.modelManager.getCurrentSample().getId().toString(), modelManager.getAllTagMeta().getAllTagGroupMappings());
    }

    public void saveTagMeta(Map<String,Map<String,Object>> allTagMeta) throws Exception {
        currentTagMap.saveTagGroupMappings(allTagMeta);
        // persist this map as a user preference for now
        saveUserPreferences();
        
        // sync up with Horta
        //if (neuronSetAdapter.getMetaWorkspace()!=null) {
          //  neuronSetAdapter.getMetaWorkspace().setTagMetadata(currentTagMap);
       // }
    }
    
    public Map<String, Map<String,Object>> getTagGroupMappings() {
        return modelManager.getAllTagMeta().getAllTagGroupMappings();
    }
    
    public void saveUserTags() throws Exception {
       // empty because for now since don't want to save their hide preferences between sessions
    }
    

    public Set<TmNeuronMetadata> getNeuronsForTag(String tag) {
        return modelManager.getAllTagMeta().getNeurons(tag);
    }

    public boolean hasNeuronTag(TmNeuronMetadata neuron, String tag) {
        return modelManager.getAllTagMeta().hasTag(neuron, tag);
    }
    
    public void addNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception {
        addNeuronTag(tag, Arrays.asList(neuron));
    }

    public void addNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception {
        tmDomainMgr.bulkEditNeuronTags(neuronList, Arrays.asList(tag), true);
        for (TmNeuronMetadata neuron: neuronList) {
            currentTagMap.addTag(tag, neuron);
            neuron.getTags().add(tag);
        }
        SwingUtilities.invokeLater(() -> fireNeuronTagsChanged(neuronList));
    }
    
    private void addUserNeuronTag(String tag, TmNeuronMetadata neuron) {
        currentTagMap.addUserTag(tag, neuron);
        try {
            saveUserTags();
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }
    
    private void removeUserNeuronTag(String tag, TmNeuronMetadata neuron) {
        currentTagMap.removeUserTag(tag, neuron);
        try {
            saveUserTags();
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }
    
    private Set<String> getUserNeuronTags(TmNeuronMetadata neuron) {
        if (currentTagMap != null) {
            return currentTagMap.getUserTags().get(neuron.getId());
        } else {
            return new HashSet<>();
        }
    }

    public void removeNeuronTag(String tag, TmNeuronMetadata neuron) throws Exception {
        removeNeuronTag(tag, Arrays.asList(neuron));
    }

    public void removeNeuronTag(String tag, List<TmNeuronMetadata> neuronList) throws Exception {
        tmDomainMgr.bulkEditNeuronTags(neuronList, Arrays.asList(tag), false);
        for (TmNeuronMetadata neuron: neuronList) {
            modelManager.getAllTagMeta().removeTag(tag, neuron);
            neuron.getTags().remove(tag);
        }
        SwingUtilities.invokeLater(() -> fireNeuronTagsChanged(neuronList));
    }

    public void clearNeuronTags(TmNeuronMetadata neuron) throws Exception {
        tmDomainMgr.bulkEditNeuronTags(Arrays.asList(neuron), new ArrayList<>(neuron.getTags()), false);
        modelManager.getAllTagMeta().clearTags(neuron);
        neuron.getTags().clear();
        SwingUtilities.invokeLater(() -> fireNeuronTagsChanged(Arrays.asList(neuron)));
    }

    public List<File> breakOutByRoots(File infile) throws IOException {
        return new SWCData().breakOutByRoots(infile);
    }

    /**
     * tells the annotation mgr to pop up an error dialog; this is
     * not a great way to do this, but hopefully it's going to be rare
     */
    private void presentError(String message, String title) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
             //   AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
             //   annotationMgr.presentError(message, title);
            }
        });
    }


    // ----- notifications to listeners -----
    // ***** NOTE *****
    // all calls to all "fire" methods must be done in the UI thread!
    //  use SwingUtilities.invokeLater()!

    public void fireAnnotationNotMoved(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l: tmGeoAnnoModListeners) {
            l.annotationNotMoved(annotation);
        }
    }

    public void fireAnnotationMoved(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l: tmGeoAnnoModListeners) {
            l.annotationMoved(annotation);
        }
    }

    public void fireAnnotationRadiusUpdated(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l: tmGeoAnnoModListeners) {
            l.annotationRadiusUpdated(annotation);
        }
    }

    public void fireNeuronRadiusUpdated(TmNeuronMetadata neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
             l.neuronRadiusUpdated(neuron);
        }
    }

    private boolean eitherIsNull(Object object1, Object object2) {
        return object1 == null || object2 == null;
    }

    void fireAnnotationAdded(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l : tmGeoAnnoModListeners) {
            l.annotationAdded(annotation);
        }
    }

    void fireAnnotationsDeleted(List<TmGeoAnnotation> deleteList) {
        // undraw deleted annotation
        for (TmGeoAnnotationModListener l : tmGeoAnnoModListeners) {
            l.annotationsDeleted(deleteList);
        }
    }

    void fireAnnotationReparented(TmGeoAnnotation annotation, Long prevNeuronId) {
        for (TmGeoAnnotationModListener l : tmGeoAnnoModListeners) {
            l.annotationReparented(annotation, prevNeuronId);
        }
    }

    void fireAnchoredPathsRemoved(Long neuronID, List<TmAnchoredPath> deleteList) {
        // undraw deleted annotation
        for (TmAnchoredPathListener l : tmAnchoredPathListeners) {
            l.removeAnchoredPaths(neuronID, deleteList);
        }
    }

    void fireAnchoredPathAdded(Long neuronID, TmAnchoredPath path) {
        for (TmAnchoredPathListener l : tmAnchoredPathListeners) {
            l.addAnchoredPath(neuronID, path);
        }
    }

    void fireWorkspaceUnloaded(TmWorkspace workspace) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.workspaceUnloaded(workspace);
        }
    }
    
    void fireWorkspaceLoaded(TmWorkspace workspace) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.workspaceLoaded(workspace);
        }
    }
    
    void fireWorkspaceChanged(List<TmNeuronMetadata> neuronList) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronTagsChanged(neuronList);
        }
    }
    
    void fireBulkNeuronsChanged(List<TmNeuronMetadata> addList, List<TmNeuronMetadata> deleteList) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.bulkNeuronsChanged(addList, deleteList);
        }
    }

    public void fireSpatialIndexReady(TmWorkspace workspace) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.spatialIndexReady(workspace);
        }
    }

    void fireNeuronSpatialFilterUpdated(boolean enabled, NeuronSpatialFilter filter) {
        for (GlobalNeuronSpatialFilterListener l: neuronSpatialFilterListeners) {
            l.neuronSpatialFilterUpdated(enabled, filter);
        }
    }
        
    public void fireBackgroundNeuronCreated(TmNeuronMetadata neuron) {
        for (BackgroundAnnotationListener b: backgroundAnnotationListeners) {
            b.neuronModelCreated(neuron);
        }
    }

    public void fireBackgroundNeuronDeleted(TmNeuronMetadata neuron) {
        for (BackgroundAnnotationListener b: backgroundAnnotationListeners) {
            b.neuronModelDeleted(neuron);
        }
    }

    public void fireBackgroundNeuronChanged(TmNeuronMetadata neuron) {
        for (BackgroundAnnotationListener b: backgroundAnnotationListeners) {
            b.neuronModelChanged(neuron);
        }
    }
    
    public void fireBackgroundNeuronOwnershipChanged(TmNeuronMetadata neuron) {
        // don't mess with background updates to system neurons if the spatial filter is hiding them
        if (!applyFilter) {
            for (BackgroundAnnotationListener b: backgroundAnnotationListeners) {
                b.neuronOwnerChanged(neuron);
            }
        }
    }
    
    void fireNeuronCreated(TmNeuronMetadata neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronCreated(neuron);
        }
    }

    void fireNeuronDeleted(TmNeuronMetadata neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronDeleted(neuron);
        }
    }

    void fireNeuronChanged(TmNeuronMetadata neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronChanged(neuron);
        }
    }
    
    void fireNeuronRenamed(TmNeuronMetadata neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronRenamed(neuron);
        }
    }

    void fireNeuronsOwnerChanged(List<TmNeuronMetadata> neuronList) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronsOwnerChanged(neuronList);
        }
    }

    void fireNeuronSelected(TmNeuronMetadata neuron) {
        log.info("SELECT MODE IS {}",getSelectMode());
        if (!getSelectMode())
            return;
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronSelected(neuron);
        }
        if (neuron!=null) {
            selectionModel.select(neuron, true, true);
        }
    }

    /*void fireNeuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronStyleChanged(neuron, style);
        }
    }

    void fireNeuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronStylesChanged(neuronStyleMap);
        }et


     */

    void fireNeuronTagsChanged(List<TmNeuronMetadata> neuronList) {
        /// RESTORE FUNCTIONALITY HERE
    }

    void fireNotesUpdated(TmGeoAnnotation ann) {
     //   if (notesUpdateListener != null) {
       //     notesUpdateListener.notesUpdated(ann);
      //  }
    }

   /* public NeuronSet getNeuronSet() {
        return neuronSetAdapter;
    }*/

    public NeuronModel getNeuronManager() {
        return neuronManager;
    }
    
    public boolean getSelectMode() {
        return select;
    }

    public void setSelectMode(boolean select) {
        this.select = select;
    }

    private void updateFrags(NeuronUpdates updates) {
        List<TmNeuronMetadata> addList = new ArrayList<>();
        List<TmNeuronMetadata> deleteList = new ArrayList<>();
        Iterator<Long> addIter = updates.getAddedNeurons().iterator();
        while (addIter.hasNext()) {
            TmNeuronMetadata neuron = getNeuronFromNeuronID(addIter.next());
            if (neuron != null) {
                addList.add(neuron);
            }
        }
        Iterator<Long> delIter = updates.getDeletedNeurons().iterator();
        while (delIter.hasNext()) {
            TmNeuronMetadata neuron = getNeuronFromNeuronID(delIter.next());
            if (neuron != null) {
                deleteList.add(neuron);
            }
        }

        fireBulkNeuronsChanged(addList, deleteList);
    }

    public boolean isFilteringEnabled() {
        return applyFilter;
    }

    public void setNeuronFiltering(boolean filtering) {
        applyFilter = filtering;
        try {            
            fireWorkspaceLoaded(currentWorkspace);
            fireSpatialIndexReady(currentWorkspace);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fireNeuronSpatialFilterUpdated(applyFilter, getFilterStrategy());
    }

    public NeuronSpatialFilter getFilterStrategy() {
        return neuronFilter;
    }

    public void setFilterStrategy(NeuronSpatialFilter filterStrategy) {
        neuronFilter = filterStrategy;
        neuronFilter.initFilter(neuronManager.getNeurons());
        setNeuronFiltering(true);
    }

    public int getNumTotalNeurons() {
        if (isFilteringEnabled()) {
            if (neuronFilter != null) {
                return neuronFilter.getNumTotalNeurons();
            } else {
                return 0;
            }
        } else {
            return getNeuronManager().getNeurons().size();
        }
    }
}
