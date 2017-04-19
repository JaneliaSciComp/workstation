package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.IdSource;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPath;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnnotationObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.TileFormatProvider;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
import org.janelia.it.jacs.shared.utils.GeomUtils;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionSupport;
import org.janelia.it.workstation.gui.large_volume_viewer.LoadTimer;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwBranch;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwDecision;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwGraphStatus;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NotesUpdateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmAnchoredPathListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmGeoAnnotationModListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Annotation model which masquerades a directed annotation session as TM objects, so that they
 * can be loaded into LVV/Horta through the existing mechanisms. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DirectedSessionAnnotationModel implements DomainObjectSelectionSupport {

    private static final Logger log = LoggerFactory.getLogger(DirectedSessionAnnotationModel.class);

    protected final LoadTimer addTimer = new LoadTimer();
    protected final ActivityLogHelper activityLog = ActivityLogHelper.getInstance();
    protected final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    protected final TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
    protected final FilteredAnnotationModel filteredAnnotationModel = new FilteredAnnotationModel();
    
    protected final TileFormatProvider tileFormatProvider;
    
    protected TmSample currentSample;
    protected TmDirectedSession currentSession;
    protected TmNeuronMetadata currentNeuron;
    protected Map<Long, TmNeuronMetadata> neuronMap = new LinkedHashMap<>();

    protected SWCDataConverter swcDataConverter;
    protected ViewStateListener viewStateListener;
    protected NotesUpdateListener notesUpdateListener;
    protected final Collection<TmGeoAnnotationModListener> tmGeoAnnoModListeners = new ArrayList<>();
    protected final Collection<TmAnchoredPathListener> tmAnchoredPathListeners = new ArrayList<>();
    protected final Collection<GlobalAnnotationListener> globalAnnotationListeners = new ArrayList<>();

    private DtwDecision decision;

    public DirectedSessionAnnotationModel(TileFormatProvider tileFormatProvider) {
        this.tileFormatProvider = tileFormatProvider;
        log.info("Creating new BasicAnnotationModel: {}", this);
    }
    
    public synchronized void clear() {
        log.info("Clearing annotation model");
        currentSession = null;
        currentSample = null;
        setCurrentNeuron(null);
        fireAnnotationsUnloaded(currentSession);
    }
    
    public synchronized void setSample(final TmSample sample) {
        this.currentSample = sample;
    }
    
    public synchronized void loadSample(final TmSample sample) throws Exception {
        if (sample == null) {
            throw new IllegalArgumentException("Cannot load null sample");
        }
        log.info("Loading sample {}", sample.getId());
        currentSession = null;
        currentSample = sample;
    }
    
    public synchronized void loadSession(final TmDirectedSession session) throws Exception {
        if (session == null) {
            throw new IllegalArgumentException("Cannot load null session");
        }
        log.info("Loading session {}", session.getId());
        currentSession = session;
        currentSample = tmDomainMgr.getSample(session);
        
        // Clear neuron selection
        log.info("Clearing current neuron for workspace {}", session.getId());
        setCurrentNeuron(null);   
    }

    public void loadComplete() throws Exception { 
        final TmDirectedSession session = getCurrentSession();
        
        // Update TC, in case the load bypassed it
        LargeVolumeViewerTopComponent.getInstance().setCurrent(session==null ? getCurrentSample() : session);

        //fireAnnotationsLoaded(session);
        
        // Load first decision
        loadNextDecision();

        // Tell viewers to update
        fireAnnotationsLoaded(session);
        fireNeuronSelected(null);
        
        if (session!=null) {
            activityLog.logLoadDirectedSession(session.getId());
        }
    }
    
    private DtwDecision loadNextDecision() throws Exception {
        final TmDirectedSession session = getCurrentSession();
        
        neuronMap.clear();
        
        decision = tmDomainMgr.getNextDecision(session);
        log.info("Got next decision (id={}) with order date {}", decision.getId(), decision.getOrderDate());

        int meanPointsPerNeuron = 1000;
        int neuronCount = decision.getBranches().size();
        IdSource idSource = new IdSource((int)(neuronCount*meanPointsPerNeuron*2));
        String owner = session.getOwnerKey();

        TmWorkspace dummyWorkspace = new TmWorkspace(); 
        dummyWorkspace.setId(idSource.next());
        dummyWorkspace.setName(session.getName());
        dummyWorkspace.setOwnerKey(owner);
        dummyWorkspace.setReaders(Sets.newHashSet(owner));
        dummyWorkspace.setWriters(Sets.newHashSet(owner));
        dummyWorkspace.setSampleRef(session.getSampleRef());
             
        for (DtwBranch dtwBranch : decision.getBranches()) {

            // Create neuron for each branch
            Long neuronId = idSource.next();
            
            TmNeuronMetadata metadata = new TmNeuronMetadata(dummyWorkspace, "Branch_"+dtwBranch.getId());
            metadata.setId(neuronId);
            metadata.setOwnerKey(owner);
            metadata.setReaders(Sets.newHashSet(owner));
            metadata.setWriters(Sets.newHashSet(owner));
            
            Map<Long, TmGeoAnnotation> map = metadata.getGeoAnnotationMap();
            TmGeoAnnotation prev = null;
            
            for (String location : dtwBranch.getNodes()) {

                TmGeoAnnotation annotation = new TmGeoAnnotation();
                annotation.setId(idSource.next());
                annotation.setNeuronId(neuronId);
                annotation.setCreationDate(new Date());
                populateAnnotationLocation(annotation, location);

                if (prev==null) {
                    annotation.setParentId(neuronId);
                    metadata.addRootAnnotation(annotation);
                }
                else {
                    annotation.setParentId(prev.getId());
                    prev.addChild(annotation);
                }

                map.put(annotation.getId(), annotation);
                prev = annotation;
                
            }

            log.info("Loaded {} locations for branch {}", map.size(), metadata.getName());
            neuronMap.put(metadata.getId(), metadata);
        }
        
        log.info("Loaded {} branches as neurons", neuronMap.size());
        
        return decision;
    }

    public DtwDecision getNextDecision() {
        return decision;
    }
    
    public DtwDecision nextDecisionRequested() throws Exception {
        final TmDirectedSession session = getCurrentSession();
        
        DtwDecision decision = loadNextDecision();
        
        fireAnnotationsLoaded(session);
        
        return decision;
    }
    
    public void makeDecision(int choiceIndex) throws Exception {
        decision.setChoiceIndex(choiceIndex);
        tmDomainMgr.saveDecision(decision);
    }
    
    public void startGraphUpdate() throws Exception {
        tmDomainMgr.startGraphUpdate(currentSession);
    }

    public DtwGraphStatus getGraphStatus() throws Exception {
        return tmDomainMgr.getGraphStatus(currentSession);
    }
    
    private void populateAnnotationLocation(TmGeoAnnotation annotation, String location) {
        Vec3 vec = GeomUtils.parseVec3(location);
        annotation.setX(vec.x());
        annotation.setY(vec.y());
        annotation.setZ(vec.z());
    }
    
    public Map<Long, TmNeuronMetadata> getNeuronMap() {
        return neuronMap;
    }

    public TmSample getCurrentSample() {
        return currentSample;
    }
    
    public TmDirectedSession getCurrentSession() {
        return currentSession;
    }

    public void saveCurrentSession() throws Exception {
        saveSession(currentSession);
    }

    public void saveSession(TmDirectedSession session) throws Exception {
        tmDomainMgr.save(session);
    }
    
    public boolean editsAllowed() {
        if (getCurrentSession()==null) return false;
        return ClientDomainUtils.hasWriteAccess(getCurrentSession());
    }
    
    @Override
    public DomainObjectSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    public Collection<TmNeuronMetadata> getNeuronList() {
        return neuronMap.values();
    }

    // current neuron methods
    public TmNeuronMetadata getCurrentNeuron() {
        log.trace("getCurrentNeuron = {}",currentNeuron);
        return currentNeuron;
    }

    // this method sets the current neuron but does not fire an event to update the UI
    protected synchronized void setCurrentNeuron(TmNeuronMetadata neuron) {
        log.trace("setCurrentNeuron({})",neuron);
        // be sure we're using the neuron object from the current workspace
        if (neuron != null) {
            this.currentNeuron = getNeuronFromNeuronID(neuron.getId());
        } 
        else {
            this.currentNeuron = null;
        }
    }

    // this method sets the current neuron *and* updates the UI; null neuron means deselect
    public void selectNeuron(TmNeuronMetadata neuron) {
        log.info("selectNeuron({})",neuron);
        if (neuron != null && getCurrentNeuron() != null && neuron.getId().equals(getCurrentNeuron().getId())) {
            return;
        }
        setCurrentNeuron(neuron); // synchronized on this BasicAnnotationModel here
        fireNeuronSelected(neuron);
        if (getCurrentSession()!=null && neuron!=null) {
            activityLog.logSelectNeuron(getCurrentSession().getId(), neuron.getId());
        }
    }

    public TmNeuronMetadata getNeuronFromNeuronID(Long neuronID) {
        TmNeuronMetadata foundNeuron = neuronMap.get(neuronID);
        if (foundNeuron == null) {
            // This happens, for example, when a new workspace is loaded and we try to find the previous nextParent anchor.
            log.warn("There is no neuron with id: {}", neuronID);
        }
        log.debug("getNeuronFromNeuronID({}) = {}",neuronID,foundNeuron);
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

    public FilteredAnnotationModel getFilteredAnnotationModel() {
        return filteredAnnotationModel;
    }
    
    //----------------------------------------------------------------------------------------------------------
    // Listener methods
    //----------------------------------------------------------------------------------------------------------
    
    public void addTmGeoAnnotationModListener(TmGeoAnnotationModListener listener) {
        tmGeoAnnoModListeners.add(listener);
    }

    public void removeTmGeoAnnotationModListener(TmGeoAnnotationModListener listener) {
        tmGeoAnnoModListeners.remove(listener);
    }

    public void addTmAnchoredPathListener(TmAnchoredPathListener listener) {
        tmAnchoredPathListeners.add(listener);
    }

    public void removeTmAnchoredPathListener(TmAnchoredPathListener listener) {
        tmAnchoredPathListeners.remove(listener);
    }

    public void addGlobalAnnotationListener(GlobalAnnotationListener listener) {
        globalAnnotationListeners.add(listener);
    }

    public void removeGlobalAnnotationListener(GlobalAnnotationListener listener) {
        globalAnnotationListeners.remove(listener);
    }

    public void setViewStateListener(ViewStateListener listener) {
        this.viewStateListener = listener;
    }

    public void setNotesUpdateListener(NotesUpdateListener notesUpdateListener) {
        this.notesUpdateListener = notesUpdateListener;
    }


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

    void fireAnnotationsUnloaded(TmAnnotationObject annotationObject) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.annotationsUnloaded(annotationObject);
        }
    }
    
    void fireAnnotationsLoaded(TmAnnotationObject annotationObject) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.annotationsLoaded(annotationObject);
        }
    }

    public void fireSpatialIndexReady(TmAnnotationObject annotationObject) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.spatialIndexReady(annotationObject);
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

    void fireNeuronSelected(TmNeuronMetadata neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronSelected(neuron);
        }
        if (neuron!=null) {
            selectionModel.select(neuron, true, true);
        }
    }

    void fireNeuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronStyleChanged(neuron, style);
        }
    }

    void fireNeuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronStylesChanged(neuronStyleMap);
        }
    }

    void fireNeuronTagsChanged(List<TmNeuronMetadata> neuronList) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronTagsChanged(neuronList);
        }
    }

    void fireNotesUpdated(TmGeoAnnotation ann) {
        if (notesUpdateListener != null) {
            notesUpdateListener.notesUpdated(ann);
        }
    }

    public void setSWCDataConverter(SWCDataConverter converter) {
        this.swcDataConverter = converter;
    }
}
