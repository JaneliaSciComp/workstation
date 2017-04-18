package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnchoredPath;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmAnnotationObject;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionSupport;
import org.janelia.it.workstation.gui.large_volume_viewer.LoadTimer;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NotesUpdateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmAnchoredPathListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmGeoAnnotationModListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotation model which masquerades a directed annotation session as TM objects, so that they
 * can be loaded into LVV/Horta through the existing mechanisms. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionAnnotationModel implements DomainObjectSelectionSupport {

    private static final Logger log = LoggerFactory.getLogger(SessionAnnotationModel.class);

    protected final ActivityLogHelper activityLog = ActivityLogHelper.getInstance();
    protected final DomainObjectSelectionModel selectionModel = new DomainObjectSelectionModel();
    protected final LoadTimer addTimer = new LoadTimer();
    protected final TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();

    protected TmSample currentSample;
    protected TmDirectedSession currentSession;
    protected TmNeuronMetadata currentNeuron;

    protected ViewStateListener viewStateListener;
    protected NotesUpdateListener notesUpdateListener;
    protected final Collection<TmGeoAnnotationModListener> tmGeoAnnoModListeners = new ArrayList<>();
    protected final Collection<TmAnchoredPathListener> tmAnchoredPathListeners = new ArrayList<>();
    protected final Collection<GlobalAnnotationListener> globalAnnotationListeners = new ArrayList<>();

    public SessionAnnotationModel() {
    }

    /**
     * In order to avoid doing things twice (or more) unnecessarily, we stop any piecemeal UI updates from being made 
     * and wait until the transaction to end before doing everything in bulk.
     */
    private void beginTransaction() {
        SkeletonController.getInstance().beginTransaction();
        FilteredAnnotationList.getInstance().beginTransaction();
    }

    /**
     * Commit everything that changed into UI changes.
     */
    private void endTransaction() {
        SkeletonController.getInstance().endTransaction();
        FilteredAnnotationList.getInstance().endTransaction();
    }
    
    public synchronized void clear() {
        log.info("Clearing annotation model");
        currentSession = null;
        currentSample = null;
        setCurrentNeuron(null);
        fireWorkspaceUnloaded(currentSession);
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

        // Neurons need to be loaded en masse from raw data from server.
        log.info("Loading neurons for workspace {}", session.getId());
        
        if (true) throw new UnsupportedOperationException();
       
        //neuronManager.loadWorkspaceNeurons(session);
        
        // Clear neuron selection
        log.info("Clearing current neuron for workspace {}", session.getId());
        setCurrentNeuron(null);   
    }

    public void loadComplete() { 
        final TmDirectedSession session = getCurrentSession();
        // Update TC, in case the load bypassed it
        LargeVolumeViewerTopComponent.getInstance().setCurrent(session==null ? getCurrentSample() : session);    
        fireWorkspaceLoaded(session);
        fireNeuronSelected(null);
        if (session!=null) {
            activityLog.logLoadDirectedSession(session.getId());
        }
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
        throw new UnsupportedOperationException();
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
        setCurrentNeuron(neuron); // synchronized on this AnnotationModel here
        fireNeuronSelected(neuron);
        if (getCurrentSession()!=null && neuron!=null) {
            activityLog.logSelectNeuron(getCurrentSession().getId(), neuron.getId());
        }
    }

    public TmNeuronMetadata getNeuronFromNeuronID(Long neuronID) {
        throw new UnsupportedOperationException();
//        TmNeuronMetadata foundNeuron = neuronManager.getNeuronById(neuronID);
//        if (foundNeuron == null) {
//            // This happens, for example, when a new workspace is loaded and we try to find the previous nextParent anchor.
//            log.warn("There is no neuron with id: {}", neuronID);
//        }
//        log.debug("getNeuronFromNeuronID({}) = {}",neuronID,foundNeuron);
//        return foundNeuron;
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

    void fireWorkspaceUnloaded(TmAnnotationObject annotationObject) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.annotationsUnloaded(annotationObject);
        }
    }
    
    void fireWorkspaceLoaded(TmAnnotationObject annotationObject) {
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
}
