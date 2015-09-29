package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

// workstation imports

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;

import com.google.common.base.Stopwatch;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.geom.ParametrizedLine;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.LoadTimer;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.janelia.it.workstation.shared.util.SWCDataConverter;
import org.janelia.it.workstation.shared.util.SWCNode;
import org.janelia.it.workstation.shared.util.SWCData;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.NotesUpdateListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmAnchoredPathListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.TmGeoAnnotationModListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ViewStateListener;


public class AnnotationModel
/*

this class is responsible for handling requests from the AnnotationManager.  those
requests are ready-to-execute; the AnnotationManager has taken care of validation.
the requests may map one-to-one onto atomic operations in the TiledMicroscopeDAO,
or they may package up several of those actions.

public methods in this class that throw exceptions are the ones that involve db calls, and
they should all be called from worker threads.  the others, typically getters of
various info, do not.  private methods don't necessarily follow that pattern.

a note on entities: in general, we want to only work with the domain objects in 
this class.  use "updateCurrentWorkspace(AndNeuron)" to keep the local copies of
those objects in sync with the back end.  other components will limit themselves
to accessing those two objects in general.

it turns out that it's expensive to update the domain objects from the db when
there's a lot of data, so we started updating the objects locally instead;
after more complicated operations (typically rarer), we'll still update from db

this class does not interact directly with the UI.  it observes 
UI elements that select, and its events are connected with a variety of UI
elements that need to respond to changing data.  this sometimes makes it hard 
to know whether methods are called in the Java EDT (event thread) or not, and 
that's important for knowing how you have to call the UI updates.  the answer 
is that all the calls that hit the db go through modelMgr and could throw 
exceptions.  so any call that calls modelMgr must catch Exceptions, and 
therefore it should do its updates on the EDT, because it's probably being
called from a  SimpleWorker thread.
*/
{
    public static final String STD_SWC_EXTENSION = ".swc";
    private static final String COLOR_FORMAT = "# COLOR %f,%f,%f";
    private static final String NAME_FORMAT = "# NAME %s";

    private ModelMgr modelMgr;
    private SessionMgr sessionMgr;
    private SWCDataConverter swcDataConverter;

    private TmWorkspace currentWorkspace;
    private TmNeuron currentNeuron;
    
    private ViewStateListener viewStateListener;
    private NotesUpdateListener notesUpdateListener;
    
    private FilteredAnnotationModel filteredAnnotationModel;

    private Collection<TmGeoAnnotationModListener> tmGeoAnnoModListeners = new ArrayList<>();
    private Collection<TmAnchoredPathListener> tmAnchoredPathListeners = new ArrayList<>();
    private Collection<GlobalAnnotationListener> globalAnnotationListeners = new ArrayList<>();

    private LoadTimer addTimer = new LoadTimer();

    // ----- constants
    // name of entity that holds our workspaces
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";

    // how far away to try to put split anchors (pixels)
    private static final Double SPLIT_ANCHOR_DISTANCE = 60.0;


    public AnnotationModel() {
        modelMgr = ModelMgr.getModelMgr();
        sessionMgr = SessionMgr.getSessionMgr();
        filteredAnnotationModel = new FilteredAnnotationModel();

        // Report performance statistics when program closes
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                addTimer.report();
            }
        });

    }
    
    public FilteredAnnotationModel getFilteredAnnotationModel() {
        return filteredAnnotationModel;
    }

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
    
    // current workspace methods
    public TmWorkspace getCurrentWorkspace() {
        return currentWorkspace;
    }

    private void updateCurrentWorkspace() {
        try {
            currentWorkspace = modelMgr.loadWorkspace(currentWorkspace.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCurrentWorkspaceAndNeuron() {
        updateCurrentWorkspace();
        // we can refresh the neuron object from the workspace:
        if (getCurrentNeuron() != null) {
            currentNeuron = getNeuronFromNeuronID(getCurrentNeuron().getId());
        }
    }

    public void loadWorkspace(TmWorkspace workspace) {
        if (workspace != null) {
            currentWorkspace = workspace;
        } else {
            currentWorkspace = null;
        }
        setCurrentNeuron(null);

        final TmWorkspace updateWorkspace = getCurrentWorkspace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireWorkspaceLoaded(updateWorkspace);
                fireNeuronSelected(null);
            }
        });

    }

    public void setSWCDataConverter( SWCDataConverter converter ) {
        this.swcDataConverter = converter;
    }
    
    // preferences stuff
    public void setPreference(String key, String value) {
        if (currentWorkspace != null) {
            try {
                // Push to database.
                modelMgr.createOrUpdateWorkspacePreference(currentWorkspace.getId(),
                        key, value);
                // Push to memory as well.
                currentWorkspace.getPreferences().setProperty(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getPreference(String key) {
        String rtnVal = null;
        if (currentWorkspace != null) {
            try {
                rtnVal = currentWorkspace.getPreferences().getProperty(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rtnVal;
    }

    // current neuron methods
    public TmNeuron getCurrentNeuron() {
        return currentNeuron;
    }

    // this method sets the current neuron but does not
    //  fire an event to update the UI
    private void setCurrentNeuron(TmNeuron neuron) {
        // be sure we're using the neuron object from the current workspace
        if (neuron != null) {
            currentNeuron = getNeuronFromNeuronID(neuron.getId());
        } else {
            currentNeuron = null;
        }
    }

    // this method sets the current neuron *and*
    //  updates the UI; null neuron means deselect
    public void selectNeuron(TmNeuron neuron) {
        if (neuron != null && getCurrentNeuron() != null && neuron.getId().equals(getCurrentNeuron().getId())) {
            return;
        }
        setCurrentNeuron(neuron);
        fireNeuronSelected(neuron);
    }

    // convenience methods
    /**
     * given the ID of an annotation, return an object wrapping it (or null)
     */
    public TmGeoAnnotation getGeoAnnotationFromID(Long annotationID) {
        if (getCurrentWorkspace() == null) {
            return null;
        }

        TmNeuron foundNeuron = getNeuronFromAnnotationID(annotationID);
        if (foundNeuron != null) {
            return foundNeuron.getGeoAnnotationMap().get(annotationID);
        } else {
            return null;
        }
    }

    /**
     * given the ID of an annotation, return the neuron that contains it (or null) 
     */
    public TmNeuron getNeuronFromAnnotationID(Long annotationID) {
        if (getCurrentWorkspace() == null) {
            return null;
        }

        TmNeuron foundNeuron = null;
        for (TmNeuron neuron: getCurrentWorkspace().getNeuronList()) {
            if (neuron.getGeoAnnotationMap().containsKey(annotationID)) {
                foundNeuron = neuron;
                break;
            }
        }
        return foundNeuron;
    }

    public TmNeuron getNeuronFromNeuronID(Long neuronID) {
        TmNeuron foundNeuron = null;
        for (TmNeuron neuron: getCurrentWorkspace().getNeuronList()) {
            if (neuron.getId().equals(neuronID)) {
                foundNeuron = neuron;
                break;
            }
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

        TmNeuron neuron = getNeuronFromAnnotationID(annotation.getId());
        TmGeoAnnotation current = annotation;
        TmGeoAnnotation parent = neuron.getParentOf(current);
        while (parent !=null) {
            current = parent;
            parent = neuron.getParentOf(current);
        }
        return current;
    }

    /**
     * find the annotation closest to the input location, excluding
     * the input annotation (null = don't exclude any)
     */
    public TmGeoAnnotation getClosestAnnotation(Vec3 location, TmGeoAnnotation excludedAnnotation) {

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        TmGeoAnnotation closest = null;
        // I hate magic numbers as much as the next programmer, but surely this
        //  is big enough, right?
        double closestSquaredDist = 1.0e99;

        double dx, dy, dz, dist;

        // our valid IDs are positive, so this will never match
        Long excludedAnnotationID;
        if (excludedAnnotation == null) {
            excludedAnnotationID = -1L;
        } else {
            excludedAnnotationID = excludedAnnotation.getId();
        }
        for (TmNeuron neuron: getCurrentWorkspace().getNeuronList()) {
            for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
                for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                    if (excludedAnnotationID.equals(ann.getId())) {
                        continue;
                    }
                    dx = ann.getX() - x;
                    dy = ann.getY() - y;
                    dz = ann.getZ() - z;
                    dist = dx * dx + dy * dy + dz * dz;
                    if (dist < closestSquaredDist) {
                        closestSquaredDist = dist;
                        closest = ann;
                    }
                }
            }
        }
        return closest;
    }

    /**
     * create a neuron in the current workspace
     *
     * @param name = name of neuron
     * @throws Exception
     */
    public void createNeuron(String name) throws Exception {
        final TmNeuron neuron = modelMgr.createTiledMicroscopeNeuron(getCurrentWorkspace().getId(), name);

        updateCurrentWorkspace();
        setCurrentNeuron(neuron);

        final TmWorkspace workspace = getCurrentWorkspace();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireWorkspaceLoaded(workspace);
                fireNeuronSelected(neuron);
            }
        });

    }

    /**
     * rename the given neuron
     */
    public void renameCurrentNeuron(String name) throws Exception {
        // rename
        Long currentNeuronID = getCurrentNeuron().getId();
        Entity neuronEntity = modelMgr.getEntityById(currentNeuronID);
        modelMgr.renameEntity(neuronEntity, name);

        // update & notify
        final TmNeuron neuron = getNeuronFromNeuronID(currentNeuronID);
        neuron.setName(name);

        final TmWorkspace workspace = getCurrentWorkspace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireWorkspaceLoaded(workspace);
                fireNeuronSelected(neuron);
            }
        });
    }

    public void deleteCurrentNeuron() throws Exception {
        if (getCurrentNeuron() == null) {
            return;
        }

        // keep a copy so we know what visuals to remove:
        TmNeuron deletedNeuron = getCurrentNeuron();
        setCurrentNeuron(null);

        // delete
        modelMgr.deleteEntityTree(deletedNeuron.getId());

        // updates
        final TmWorkspace workspace = getCurrentWorkspace();
        workspace.getNeuronList().remove(deletedNeuron);

        final ArrayList<TmGeoAnnotation> tempAnnotationList = new ArrayList<>(deletedNeuron.getGeoAnnotationMap().values());
        final ArrayList<TmAnchoredPath> tempPathList = new ArrayList<>(deletedNeuron.getAnchoredPathMap().values());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronSelected(null);
                fireAnnotationsDeleted(tempAnnotationList);
                fireAnchoredPathsRemoved(tempPathList);
                fireWorkspaceLoaded(workspace);
            }
        });
    }

    /**
     * @param parentEntity = parent folder in hierarchy
     * @param brainSampleID = tiled microscope sample ID
     * @param name = name of new workspace
     * @throws Exception
     */
    public void createWorkspace(Entity parentEntity, Long brainSampleID, String name) throws Exception {
        TmWorkspace workspace = modelMgr.createTiledMicroscopeWorkspace(parentEntity.getId(),
                brainSampleID, name, sessionMgr.getSubject().getKey());
        loadWorkspace(workspace);
    }

    /**
     * we save our workspaces in a pre-defined area; this method returns it, creating if needed
     *
     * @return workspace folder entity
     * @throws Exception
     */
    public Entity getOrCreateWorkspacesFolder() throws Exception {
        Entity workspaceRootEntity = ModelMgr.getModelMgr().getOwnedCommonRootByName(WORKSPACES_FOLDER_NAME);
        if (workspaceRootEntity == null) {
            workspaceRootEntity = modelMgr.createCommonRoot(WORKSPACES_FOLDER_NAME);
        }
        return workspaceRootEntity;
    }

    /**
     * add a root annotation to the given neuron; this is an annotation without a parent
     *
     * @param neuron = neuron object
     * @param xyz = x, y, z location of new annotation
     * @throws Exception
     */
    public void addRootAnnotation(final TmNeuron neuron, Vec3 xyz) throws Exception {
        // the null  in this call means "this is a root annotation" (would otherwise
        //  be the parent)
        final TmGeoAnnotation annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                null, 0, xyz.x(), xyz.y(), xyz.z(), "");

        // update neuron locally instead of getting from db again
        neuron.getRootAnnotations().add(annotation);
        neuron.getGeoAnnotationMap().put(annotation.getId(), annotation);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronSelected(neuron);
                fireAnnotationAdded(annotation);
            }
        });
    }

    /**
     * add a child annotation
     *
     * @param parentAnn = parent annotation object
     * @param xyz = location of new child annotation
     * @throws Exception
     */
    public void addChildAnnotation(TmGeoAnnotation parentAnn, Vec3 xyz) throws Exception {
        if (parentAnn == null) {
            return;
        }

        addTimer.mark("start addChildAnn");
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        System.out.println("entering addChildAnnotation: " + stopwatch);

        final TmNeuron neuron = getNeuronFromAnnotationID(parentAnn.getId());
        System.out.println("adding annotation (calling modelMgr): " + stopwatch);
        final TmGeoAnnotation annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                parentAnn.getId(), 0, xyz.x(), xyz.y(), xyz.z(), "");

        System.out.println("updating workspace: " + stopwatch);

        // update the neuron locally so we don't have to update from db
        TmGeoAnnotation parent = getGeoAnnotationFromID(annotation.getParentId());
        parent.addChild(annotation);
        neuron.getGeoAnnotationMap().put(annotation.getId(), annotation);


        // the parent may lose some predefined notes (finished end, possible branch)
        System.out.println("stripping predef notes: " + stopwatch);
        stripPredefNotes(getNeuronFromAnnotationID(parentAnn.getId()), parentAnn.getId());

        if (automatedTracingEnabled()) {
            System.out.println("updating traced path: " + stopwatch);
            if (viewStateListener != null)
                viewStateListener.pathTraceRequested(annotation.getId());
        }

        System.out.println("dispatching update events: " + stopwatch);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNeuronSelected(neuron);
                fireAnnotationAdded(annotation);
            }
        });

        System.out.println("leaving addChildAnnotation: " + stopwatch);
        stopwatch.stop();
        addTimer.mark("end addChildAnn");
        // reset timer state; we don't care about end > start
        addTimer.setPreviousStepName("start addChildAnn");
    }

    /**
     * move an existing annotation to a new location
     *
     * @param annotationID = ID of annotation to move
     * @param location = new location
     * @throws Exception
     */
    public void moveAnnotation(final Long annotationID, Vec3 location) throws Exception {
        TmGeoAnnotation annotation = getGeoAnnotationFromID(annotationID);
        try {
            modelMgr.updateGeometricAnnotation(annotation, annotation.getIndex(),
                location.getX(), location.getY(), location.getZ(),
                annotation.getComment());
        } catch (Exception e) {
            // error means not persisted; however, in the process of moving,
            //  the marker's already been moved, to give interactive feedback
            //  to the user; so in case of error, tell the view to update to current
            //  position (pre-move)
            // this is unfortunately untested, because I couldn't think of an
            //  easy way to simulate or force a failure!
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireAnnotationNotMoved(annotationID);
                }
            });
            throw e;
        }

        // find each connecting annotation; if there's a traced path to it,
        //  remove it (refresh annotation!)
        // at the same time, delete the paths out of the local neuron object, too
        final TmNeuron neuron = getNeuronFromAnnotationID(annotationID);

        TmGeoAnnotation parent = neuron.getParentOf(annotation);
        if (parent != null) {
            removeAnchoredPath(annotation, parent);
            neuron.getAnchoredPathMap().remove(new TmAnchoredPathEndpoints(annotation.getId(), parent.getId()));
        }
        for (TmGeoAnnotation neighbor: neuron.getChildrenOf(annotation)) {
            removeAnchoredPath(annotation, neighbor);
            neuron.getAnchoredPathMap().remove(new TmAnchoredPathEndpoints(annotation.getId(), neighbor.getId()));
        }

        // update local annotation object
        annotation.setX(location.getX());
        annotation.setY(location.getY());
        annotation.setZ(location.getZ());


        final TmWorkspace workspace = getCurrentWorkspace();

        if (automatedTracingEnabled()) {
            // trace to parent, and each child to this parent:
            viewStateListener.pathTraceRequested(annotation.getId());
            for (TmGeoAnnotation child : neuron.getChildrenOf(annotation)) {
                viewStateListener.pathTraceRequested(child.getId());
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);

                if (getCurrentNeuron() != null) {
                    if (neuron.getId().equals(getCurrentNeuron().getId())) {
                        fireNeuronSelected(getCurrentNeuron());
                    }
                }
            }
        });
    }

    /**
     * merge the neurite that has source Annotation into the neurite containing
     * targetAnnotation
     */
    public void mergeNeurite(final Long sourceAnnotationID, final Long targetAnnotationID) throws Exception {

        TmGeoAnnotation sourceAnnotation = getGeoAnnotationFromID(sourceAnnotationID);
        TmNeuron sourceNeuron = getNeuronFromAnnotationID(sourceAnnotationID);

        // reroot source neurite to source ann
        modelMgr.rerootNeurite(sourceNeuron, sourceAnnotation);

        // reload the things we just changed:
        updateCurrentWorkspace();
        sourceAnnotation = getGeoAnnotationFromID(sourceAnnotationID);
        sourceNeuron = getNeuronFromAnnotationID(sourceAnnotationID);
        // Remove traced paths.
        for (TmGeoAnnotation child: sourceNeuron.getChildrenOf(sourceAnnotation)) {
            removeAnchoredPath(child, sourceAnnotation);
        }

        // if source neurite not in same neuron as dest neurite: move it; don't
        //  use annModel.moveNeurite() because we don't want those updates & signals yet
        TmNeuron targetNeuron = getNeuronFromAnnotationID(targetAnnotationID);
        if (!sourceNeuron.getId().equals(targetNeuron.getId())) {
            modelMgr.moveNeurite(sourceAnnotation, targetNeuron);
        }

        // Refresh domain objects that we've changed and will use again.
        updateCurrentWorkspace();
        sourceAnnotation = getGeoAnnotationFromID(sourceAnnotationID);
        targetNeuron = getNeuronFromAnnotationID(targetAnnotationID);


        // Reparent all source annotation's children to dest ann
        for (TmGeoAnnotation child: sourceNeuron.getChildrenOf(sourceAnnotation)) {
            modelMgr.reparentGeometricAnnotation(child, targetAnnotationID, targetNeuron);
        }

        // if the source ann has a note, move it to or append it to the target ann:
        TmStructuredTextAnnotation sourceNote = sourceNeuron.getStructuredTextAnnotationMap().get(sourceAnnotationID);
        if (sourceNote != null) {
            TmStructuredTextAnnotation targetNote = targetNeuron.getStructuredTextAnnotationMap().get(targetAnnotationID);
            String sourceNoteText = new String("");
            JsonNode rootNode = sourceNote.getData();
            JsonNode noteNode = rootNode.path("note");
            if (!noteNode.isMissingNode()) {
                sourceNoteText = noteNode.asText();
            }
            if (sourceNoteText.length() > 0) {
                if (targetNote == null) {
                    // add old note to target
                    setNote(getGeoAnnotationFromID(targetAnnotationID), sourceNoteText);
                } else {
                    // merge notes
                    String targetNoteText = new String("");
                    rootNode = targetNote.getData();
                    noteNode = rootNode.path("note");
                    if (!noteNode.isMissingNode()) {
                        targetNoteText = noteNode.asText();
                    }
                    if (targetNoteText.length() > 0) {
                        setNote(getGeoAnnotationFromID(targetAnnotationID),
                            String.format("%s %s", targetNoteText, sourceNoteText));
                    }
                }
            }
        }

        // finally, delete source ann
        if (sourceNote != null) {
            modelMgr.deleteStructuredTextAnnotation(sourceNote.getId());
        }
        // we need a copy of this annotation from before it's deleted for a later update:
        final List<TmGeoAnnotation> deleteList = new ArrayList<>();
        deleteList.add(sourceAnnotation);

        modelMgr.deleteGeometricAnnotation(sourceAnnotationID);

        // update objects *again*, last time:
        updateCurrentWorkspace();
        final TmWorkspace workspace = getCurrentWorkspace();
        final TmNeuron updateTargetNeuron = getNeuronFromAnnotationID(targetAnnotationID);
        setCurrentNeuron(updateTargetNeuron);

        final TmGeoAnnotation targetAnnotation = getGeoAnnotationFromID(targetAnnotationID);
        for (TmGeoAnnotation child : updateTargetNeuron.getChildrenOf(targetAnnotation)) {
            if (automatedTracingEnabled()) {
                viewStateListener.pathTraceRequested(child.getId());
            }
        }

        // see note in addChildAnnotations re: predef notes
        // for merge, the target annotation is the one affected; fortunately, the
        //  neuron has just been refreshed
        stripPredefNotes(updateTargetNeuron, targetAnnotationID);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);
                fireNeuronSelected(updateTargetNeuron);
                fireWorkspaceLoaded(workspace);
                for (TmGeoAnnotation child : updateTargetNeuron.getChildrenOf(targetAnnotation)) {
                    fireAnnotationReparented(child);
                }
                fireAnnotationsDeleted(deleteList);
            }
        });

    }

    /**
     * move the neurite containing the input annotation to the given neuron
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron neuron) throws Exception {
        if (annotation == null || neuron == null) {
            return;
        }
        modelMgr.moveNeurite(annotation, neuron);

        // updates
        updateCurrentWorkspaceAndNeuron();
        final TmWorkspace workspace = getCurrentWorkspace();
        final TmNeuron currentNeuron = getCurrentNeuron();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);
                fireNeuronSelected(currentNeuron);
                fireWorkspaceLoaded(workspace);
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
    public void deleteLink(final TmGeoAnnotation link) throws Exception {
        if (link == null) {
            return;
        }

        // check it's not a branch or a root with children
        if (link.getChildIds().size() > 1) {
            return;
        }

        if (link.isRoot() && link.getChildIds().size() > 0) {
            return;
        }

        // check we can find it
        // again, not clear if this should be an exception or not?
        TmNeuron neuron = getNeuronFromAnnotationID(link.getId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // delete it; reparent its child (if any) to its parent
        TmGeoAnnotation parent = neuron.getParentOf(link);
        TmGeoAnnotation child = null;
        if (link.getChildIds().size() == 1) {
            child = neuron.getChildrenOf(link).get(0);
            modelMgr.reparentGeometricAnnotation(child, parent.getId(), neuron);

            // if segment to child had a trace, remove it
            removeAnchoredPath(link, child);
        }

        // delete the deleted annotation that is to be deleted (and its note, too):
        TmStructuredTextAnnotation note = neuron.getStructuredTextAnnotationMap().get(link.getId());
        if (note != null) {
            // don't use removeNote(); it triggers updates we don't want yet
            modelMgr.deleteStructuredTextAnnotation(note.getId());
        }
        modelMgr.deleteGeometricAnnotation(link.getId());

        // if segment to parent had a trace, remove it
        removeAnchoredPath(link, parent);

        updateCurrentWorkspaceAndNeuron();
        final TmWorkspace workspace = getCurrentWorkspace();

        final TmGeoAnnotation updateChild;
        if (child != null) {
            neuron = getNeuronFromAnnotationID(child.getId());
            updateChild = neuron.getGeoAnnotationMap().get(child.getId());
        } else {
            updateChild = null;
        }

        // if we're tracing, retrace if there's a new connection
        if (automatedTracingEnabled() && child != null) {
            viewStateListener.pathTraceRequested(child.getId());
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);

                // need to delete an anchor (which does undraw it); but then
                //  need to redraw the neurite, because we need the link
                //  from the reparenting to appear
                List<TmGeoAnnotation> deleteList = new ArrayList<>(1);
                deleteList.add(link);
                fireAnnotationsDeleted(deleteList);

                if (updateChild != null) {
                    fireAnnotationReparented(updateChild);
                }

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
    public void deleteSubTree(TmGeoAnnotation rootAnnotation) throws Exception {
        if (rootAnnotation == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotationID(rootAnnotation.getId());
        if (neuron == null) {
            // should this be an error?  it's a sign that the annotation has already
            //  been deleted, or something else that shouldn't happen
            return;
        }

        // grab the parent of the root before the root disappears:
        TmGeoAnnotation rootParent = neuron.getParentOf(rootAnnotation);

        final List<TmGeoAnnotation> deleteList = neuron.getSubTreeList(rootAnnotation);
        TmStructuredTextAnnotation note;
        for (TmGeoAnnotation annotation: deleteList) {
            // for each annotation, delete any paths traced to its children;
            //  do before the deletion!
            for (TmGeoAnnotation child: neuron.getChildrenOf(annotation)) {
                removeAnchoredPath(annotation, child);
            }

            note = neuron.getStructuredTextAnnotationMap().get(annotation.getId());
            if (note != null) {
                // don't use removeNote(); it triggers updates we don't want yet
                modelMgr.deleteStructuredTextAnnotation(note.getId());
            }
            modelMgr.deleteGeometricAnnotation(annotation.getId());
        }
        // for the root annotation, also delete any traced paths to the parent, if it exists
        if (rootParent != null) {
            removeAnchoredPath(rootAnnotation, rootParent);
        }


        updateCurrentWorkspaceAndNeuron();
        final TmWorkspace workspace = getCurrentWorkspace();
        final TmNeuron updateNeuron = getCurrentNeuron();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);
                fireNeuronSelected(updateNeuron);
                fireAnnotationsDeleted(deleteList);
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
    public void splitAnnotation(TmGeoAnnotation annotation) throws Exception {
        if (annotation == null) {
            return;
        }

        TmNeuron neuron = getNeuronFromAnnotationID(annotation.getId());

        // ann1 is the child of ann2 in both cases; if reverse, place the new point
        //  near ann2 instead of ann1
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

        // create the new annotation, child of original parent; then
        //  reparent existing annotation to new annotation
        final TmGeoAnnotation newAnnotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                annotation2.getId(), 0, newPoint.x(), newPoint.y(), newPoint.z(), "");

        //refresh neuron, then again, for updates
        neuron = modelMgr.loadNeuron(neuron.getId());
        modelMgr.reparentGeometricAnnotation(annotation1, newAnnotation.getId(), neuron);
        neuron = modelMgr.loadNeuron(neuron.getId());

        // if that segment had a trace, remove it
        removeAnchoredPath(annotation1, annotation2);

        // updates:
        updateCurrentWorkspaceAndNeuron();

        // retrace
        if (automatedTracingEnabled()) {
            if (viewStateListener != null) {
                viewStateListener.pathTraceRequested(newAnnotation.getId());
                viewStateListener.pathTraceRequested(annotation1.getId());
            }
        }

        final TmGeoAnnotation updateAnnotation = neuron.getGeoAnnotationMap().get(annotation1.getId());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnnotationAdded(newAnnotation);
                fireAnnotationReparented(updateAnnotation);
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
    public void rerootNeurite(Long newRootID) throws Exception {
        // do it in the DAO layer
        TmGeoAnnotation newRoot = getGeoAnnotationFromID(newRootID);
        TmNeuron neuron = getNeuronFromAnnotationID(newRoot.getId());
        modelMgr.rerootNeurite(neuron, newRoot);

        // notify, etc.; don't need to redraw anything, but the neurite list etc. need to be reloaded
        updateCurrentWorkspaceAndNeuron();

        // see notes in addChildAnnotation re: the predef notes
        // in this case, the new root is the only annotation we need to check
        stripPredefNotes(getNeuronFromAnnotationID(newRootID), newRootID);

        if (neuron.getId().equals(getCurrentNeuron().getId())){
            final TmNeuron updateNeuron = getCurrentNeuron();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireNeuronSelected(updateNeuron);
                }
            });
        }
    }

    /**
     * split a neurite at the input node; the node is detached from its parent, and it and
     * its children become a new neurite in the same neuron
     *
     * @param newRootID = ID of root of new neurite
     * @throws Exception
     */
    public void splitNeurite(final Long newRootID) throws Exception {
        TmGeoAnnotation newRoot = getGeoAnnotationFromID(newRootID);
        TmNeuron neuron = getNeuronFromAnnotationID(newRootID);
        TmGeoAnnotation newRootParent = neuron.getParentOf(newRoot);
        removeAnchoredPath(newRoot, newRootParent);
        modelMgr.splitNeurite(neuron, newRoot);

        // update and notify
        updateCurrentWorkspaceAndNeuron();
        final TmNeuron updateNeuron = getNeuronFromAnnotationID(newRootID);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnnotationReparented(updateNeuron.getGeoAnnotationMap().get(newRootID));
                fireNeuronSelected(updateNeuron);
            }
        });
    }

    public void addAnchoredPath(TmAnchoredPathEndpoints endpoints, List<List<Integer>> points) throws Exception{

        // check we can find both endpoints in same neuron
        //  don't need to check that they are neighboring; UI gesture already enforces it
        TmNeuron neuron1 = getNeuronFromAnnotationID(endpoints.getAnnotationID1());
        TmNeuron neuron2 = getNeuronFromAnnotationID(endpoints.getAnnotationID2());
        if (neuron1 == null || neuron2 == null) {
            // something's been deleted
            return;
        }
        if (!neuron1.getId().equals(neuron2.getId())) {
            throw new Exception("anchored path annotations are in different neurons");
        }

        // now verify that endpoints for path are still where they were when path
        //  was being drawn (ie, make sure user didn't move the endpoints in the meantime)
        // check that the first and last points in the list match the current locations of the
        //  annotations, in some order (despite stated convention, I have not found the point
        //  list to be in consistent order vis a vis the ordering of the annotation IDs)

        TmGeoAnnotation ann1 = neuron1.getGeoAnnotationMap().get(endpoints.getAnnotationID1());
        TmGeoAnnotation ann2 = neuron2.getGeoAnnotationMap().get(endpoints.getAnnotationID2());

        boolean order1 = annotationAtPoint(ann1, points.get(0)) && annotationAtPoint(ann2, points.get(points.size() - 1));
        boolean order2 = annotationAtPoint(ann2, points.get(0)) && annotationAtPoint(ann1, points.get(points.size() - 1));
        if (!order1 && !order2) {
            // something's been moved; we should log this?
            return;
        }


        // if a path between those endpoints exists, remove it first:
        if (neuron1.getAnchoredPathMap().containsKey(endpoints)) {
            removeAnchoredPath(neuron1.getAnchoredPathMap().get(endpoints));
        }

        // transform point list and persist
        final TmAnchoredPath path = modelMgr.addAnchoredPath(neuron1.getId(), endpoints.getAnnotationID1(),
                endpoints.getAnnotationID2(), points);

        // update local domain object
        TmNeuron neuron = getNeuronFromAnnotationID(ann1.getId());
        neuron.getAnchoredPathMap().put(endpoints, path);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnchoredPathAdded(path);
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
     * should only be called within AnnotationModel, and it does not
     * update neurons or do any other cleanup
     */
    private void removeAnchoredPath(TmAnchoredPath path) throws  Exception {
        modelMgr.deleteAnchoredPath(path.getId());

        final ArrayList<TmAnchoredPath> pathList = new ArrayList<>();
        pathList.add(path);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireAnchoredPathsRemoved(pathList);
            }
        });
    }

    /**
     * remove an anchored path between two annotations, if one exists;
     * should only be called within AnnotationModel, and it does not
     * update neurons or do any other cleanup
     */
    private void removeAnchoredPath(TmGeoAnnotation annotation1, TmGeoAnnotation annotation2)
        throws Exception {
        if (annotation1 == null || annotation2 == null) {
            return;
        }

        // we assume second annotation is in same neuron; if it's not, there's no path
        //  to remove anyway
        TmNeuron neuron1 = getNeuronFromAnnotationID(annotation1.getId());
        TmAnchoredPathEndpoints endpoints = new TmAnchoredPathEndpoints(annotation1.getId(),
                annotation2.getId());
        if (neuron1.getAnchoredPathMap().containsKey(endpoints)) {
            removeAnchoredPath(neuron1.getAnchoredPathMap().get(endpoints));
        }        
    }

    public String getNote(Long annotationID) {
        TmNeuron neuron = getNeuronFromAnnotationID(annotationID);
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

    /**
     * add or update a note on a geometric annotation
     */
    public synchronized void setNote(TmGeoAnnotation geoAnnotation, String noteString) throws Exception {
        TmNeuron neuron = getNeuronFromAnnotationID(geoAnnotation.getId());
        if (neuron == null) {
            throw new Exception("can't find neuron for annotation with ID " + geoAnnotation.getId());
        }

        TmStructuredTextAnnotation textAnnotation = neuron.getStructuredTextAnnotationMap().get(geoAnnotation.getId());
        ObjectMapper mapper = new ObjectMapper();
        if (textAnnotation != null) {
            // if you've got a structured text annotation already, use it; for now, you only get one
            JsonNode rootNode = textAnnotation.getData();
            if (noteString.length() > 0) {
                ((ObjectNode) rootNode).put("note", noteString);
                modelMgr.updateStructuredTextAnnotation(textAnnotation, mapper.writeValueAsString(rootNode));
            } else {
                // there is a note attached, but we want it gone; if it's the only thing there,
                //  delete the whole structured text annotation
                ((ObjectNode) rootNode).remove("note");
                if (rootNode.size() > 0) {
                    modelMgr.updateStructuredTextAnnotation(textAnnotation, mapper.writeValueAsString(rootNode));
                } else {
                    // otherwise, there's something left, so persist it (note: as of this
                    //  writing, there aren't any other structured text annotations besides
                    //  note, but no need to get sloppy!)
                    modelMgr.deleteStructuredTextAnnotation(textAnnotation.getId());
                }
            }

        } else {
            // it doesn't exist; if input is also null, don't even bother
            if (noteString.length() > 0) {
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("note", noteString);

                textAnnotation = modelMgr.addStructuredTextAnnotation(neuron.getId(), geoAnnotation.getId(),
                        TmStructuredTextAnnotation.GEOMETRIC_ANNOTATION, TmStructuredTextAnnotation.FORMAT_VERSION,
                        mapper.writeValueAsString(rootNode));
            }
        }

        // update domain object; similar logic to above
        // if it's empty string, delete note object from neuron
        if (noteString.length() == 0) {
            if (neuron.getStructuredTextAnnotationMap().containsKey(geoAnnotation.getId())) {
                neuron.getStructuredTextAnnotationMap().remove(geoAnnotation.getId());
            }
        } else {
            // if it's not empty and in neuron, update string in note object
            // if it's not empty and not in neuron, add new note object to neuron
            if (neuron.getStructuredTextAnnotationMap().containsKey(geoAnnotation.getId())) {
                neuron.getStructuredTextAnnotationMap().get(geoAnnotation.getId()).setDataString(noteString);
            } else {
                neuron.getStructuredTextAnnotationMap().put(geoAnnotation.getId(), textAnnotation);
            }
        }


        final TmWorkspace workspace = getCurrentWorkspace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);
            }
        });

    }

    public void removeNote(TmStructuredTextAnnotation textAnnotation) throws Exception {
        modelMgr.deleteStructuredTextAnnotation(textAnnotation.getId());

        // updates
        final TmWorkspace workspace = getCurrentWorkspace();
        TmNeuron neuron = getNeuronFromAnnotationID(textAnnotation.getParentId());
        neuron.getStructuredTextAnnotationMap().remove(textAnnotation.getParentId());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireNotesUpdated(workspace);
            }
        });
    }

    /**
     * change the style for a neuron; synchronized because it could be
     * called from multiple threads, and the update is not atomic
     */
    public synchronized void setNeuronStyle(TmNeuron neuron, NeuronStyle style) throws IOException {
        Map<Long, NeuronStyle> neuronStyleMap = getNeuronStyleMap();
        neuronStyleMap.put(neuron.getId(), style);
        setNeuronStyleMap(neuronStyleMap);

        // fire change to listeners
        fireNeuronStyleChanged(neuron, style);
    }

    /**
     * retrieve the neuron ID: NeuronStyle map from preferences
     */
    public Map<Long, NeuronStyle> getNeuronStyleMap() {
        Map<Long, NeuronStyle> neuronStyleMap = new HashMap<>();

        String stylePref = getCurrentWorkspace().getPreferences().getProperty(AnnotationsConstants.PREF_ANNOTATION_NEURON_STYLES);
        if (stylePref == null) {
            // no such preference
            return neuronStyleMap;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = null;
        try {
            rootNode = (ObjectNode) mapper.readTree(stylePref);
        } catch (IOException e) {
            // can't parse, return the empty map
            // debated what to do here, but this should never happen,
            //  so I'll be lazy about it
            return neuronStyleMap;
        }

        // I can't believe you can't use a regular for loop for this...
        Iterator<Map.Entry<String, JsonNode>> nodeIterator = rootNode.fields();
        while (nodeIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = nodeIterator.next();
            neuronStyleMap.put(Long.parseLong(entry.getKey()), NeuronStyle.fromJSON((ObjectNode) entry.getValue()));
        }

        return  neuronStyleMap;
    }

    /**
     * retrieve a neuron style for a neuron, whether stored or default
     */
    public NeuronStyle getNeuronStyle(TmNeuron neuron) {
        Map<Long, NeuronStyle> neuronStyleMap = getNeuronStyleMap();
        if (neuronStyleMap.containsKey(neuron.getId())) {
            return neuronStyleMap.get(neuron.getId());
        } else {
            return NeuronStyle.getStyleForNeuron(neuron.getId());
        }
    }

    /**
     * store the neuron ID to NeuronStyle map in the preferences, overwriting
     * previous entry; private because we want it only being access via the
     * synchronized method setNeuronStyle in this class
     */
    private void setNeuronStyleMap(Map<Long, NeuronStyle> neuronStyleMap) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        for (Long neuronID: neuronStyleMap.keySet()) {
            rootNode.put(neuronID.toString(), neuronStyleMap.get(neuronID).asJSON());
        }
        setPreference(AnnotationsConstants.PREF_ANNOTATION_NEURON_STYLES, rootNode.toString());

        // no updates here!
    }

    public boolean automatedRefinementEnabled() {
        String automaticRefinementPref = getCurrentWorkspace().getPreferences().getProperty(AnnotationsConstants.PREF_AUTOMATIC_POINT_REFINEMENT);
        if (automaticRefinementPref != null) {
            return Boolean.parseBoolean(automaticRefinementPref);
        } else {
            return false;
        }
    }

    public boolean automatedTracingEnabled() {
        String automaticTracingPref = getCurrentWorkspace().getPreferences().getProperty(AnnotationsConstants.PREF_AUTOMATIC_TRACING);
        if (automaticTracingPref != null) {
            return Boolean.parseBoolean(automaticTracingPref);
        } else {
            return false;
        }
    }

    /**
     * examine the input annotation and remove any predefined notes which
     * are no longer valid
     */
    private void stripPredefNotes(TmNeuron neuron, Long annID) throws Exception {
        String noteText = getNote(annID);
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
                setNote(getGeoAnnotationFromID(annID), noteText);
            }
        }

    }

    /**
     * export the neurons in the input list into the given file, in swc format;
     * all neurons (and all their neurites!) are crammed into a single file
     */
    public void exportSWCData(File swcFile, List<Long> neuronIDList, int downsampleModulo) throws Exception {

        // get fresh neuron objects from ID list
        Map<Long,List<String>> neuronHeaders = new HashMap<>();
        ArrayList<TmNeuron> neuronList = new ArrayList<>();
        for (Long ID: neuronIDList) {
            if (ID != null) {
                TmNeuron foundNeuron = null;
                for (TmNeuron neuron: getCurrentWorkspace().getNeuronList()) {
                    if (neuron.getId().equals(ID)) {
                        foundNeuron = neuron;
                        List<String> headers = neuronHeaders.get(ID);
                        if (headers == null) {
                            headers = new ArrayList<>();
                            neuronHeaders.put(ID, headers);
                        }
                        NeuronStyle style = getNeuronStyle(neuron);
                        float[] color = style.getColorAsFloatArray();
                        headers.add(String.format(COLOR_FORMAT, color[0], color[1], color[2]));                        
                        if (neuronIDList.size() > 1) {
                            // Allow user to pick name as name of file, if saving individual neuron.
                            // Do not save the internal name.
                            headers.add(String.format(NAME_FORMAT, neuron.getName()));                        
                        }
                    }
                }
                if (foundNeuron != null) {
                    neuronList.add(foundNeuron);
                }
            }
        }
        
        // get swcdata via converter, then write        
        // First write one file per neuron.
        List<SWCData> swcDatas = swcDataConverter.fromTmNeuron(neuronList, neuronHeaders, downsampleModulo);
        if (swcDatas != null  &&  !swcDatas.isEmpty()) {
            int i = 0;
            for (SWCData swcData: swcDatas) {
                if (swcDatas.size() == 1) {
                    swcData.write(swcFile, -1);
                }
                else {
                    swcData.write(swcFile, i);
                }
                i++;
            }
        }
        // Next write one file containing all neurons, if there are more than one.
        if (swcDatas != null  &&  swcDatas.size() > 1) {
            SWCData swcData = swcDataConverter.fromAllTmNeuron(neuronList, downsampleModulo);
            if (swcData != null) {
                swcData.write(swcFile);
            }
        }
    }

    public void importSWCData(File swcFile) throws Exception {
        importSWCData(swcFile, null);
    }

    public void importSWCData(File swcFile, SimpleWorker worker) throws Exception {

        // the constructor also triggers the parsing, but not the validation
        SWCData swcData = SWCData.read(swcFile);
        if (!swcData.isValid()) {
            throw new Exception(String.format("invalid SWC file %s; reason: %s",
                    swcFile.getName(), swcData.getInvalidReason()));
        }

        // note from CB, July 2013: Vaa3d can't handle large coordinates in swc files,
        //  so he added an OFFSET header and recentered on zero when exporting
        // therefore, if that header is present, respect it
        double[] externalOffset = swcData.parseOffset();        

        // create one neuron for the file; take name from the filename (strip extension)
        String neuronName = swcData.parseName();
        if (neuronName == null)
            neuronName = swcFile.getName();
        if (neuronName.endsWith(STD_SWC_EXTENSION)) {
            neuronName = neuronName.substring(0, neuronName.length() - STD_SWC_EXTENSION.length());
        }
        final TmNeuron neuron = modelMgr.createTiledMicroscopeNeuron(getCurrentWorkspace().getId(), neuronName);

        // let's go with brute force for now; loop over nodes and
        //  insert into db sequentially, since we have no bulk update
        //  for annotations right now

        // and as long as we're doing brute force, we can update progress
        //  granularly (if we have a worker); start with 5% increments (1/20)
        int totalLength = swcData.getNodeList().size();
        int updateFrequency = totalLength / 20;
        if (updateFrequency == 0) {
            updateFrequency = 1;
        }
        if (worker != null) {
            worker.setProgress(0L, totalLength);
        }

        Map<Integer, TmGeoAnnotation> annotations = new HashMap<>();
        TmGeoAnnotation annotation;
        for (SWCNode node: swcData.getNodeList()) {
            // Internal points, as seen in annotations, are same as external
            // points in SWC: represented as voxels. --LLF
            double[] internalPoint = swcDataConverter.internalFromExternal(
                new double[] {
                        node.getX() + externalOffset[0],
                        node.getY() + externalOffset[1],
                        node.getZ() + externalOffset[2],
                }
            );
            if (node.getParentIndex() == -1) {
                annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                    null, 0, 
                    internalPoint[0], internalPoint[1], internalPoint[2],
                    "");
            } else {
                annotation = modelMgr.addGeometricAnnotation(neuron.getId(),
                    annotations.get(node.getParentIndex()).getId(),
                    0, 
                    internalPoint[0], internalPoint[1], internalPoint[2],
                    "");
            }
            annotations.put(node.getIndex(), annotation);
            if (worker != null && (node.getIndex() % updateFrequency) == 0) {
                worker.setProgress(node.getIndex(), totalLength);
            }
        }
        updateNeuronColor(swcData, neuron);
        
        // update workspace; update and select new neuron; this will draw points as well
        updateCurrentWorkspace();
        final TmWorkspace workspace = getCurrentWorkspace();
        setCurrentNeuron(neuron);
        final TmNeuron updateNeuron = getCurrentNeuron();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireWorkspaceLoaded(workspace);
                fireNeuronSelected(updateNeuron);                
            }
        });


        // normally if automatic tracing is enabled, we'd do that here, but
        //  for bulk import, I don't think this is a good idea right now,
        //  as we have no mechanism for managing the jobs

    }

    public void fireAnnotationNotMoved(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l: tmGeoAnnoModListeners) {
            l.annotationNotMoved(annotation);
        }
    }

    private void updateNeuronColor(SWCData swcData, final TmNeuron neuron) throws IOException {
        float[] colorArr = swcData.parseColorFloats();
        if (colorArr != null) {
            NeuronStyle style = new NeuronStyle(
                    new Color(colorArr[0], colorArr[1], colorArr[2]), true
            );
            setNeuronStyle(neuron, style);
        }
    }

    private void fireAnnotationNotMoved(Long annotationID) {
        fireAnnotationNotMoved(getGeoAnnotationFromID(annotationID));
    }

    private void fireAnnotationAdded(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l : tmGeoAnnoModListeners) {
            l.annotationAdded(annotation);
        }
    }

    private void fireAnnotationsDeleted(List<TmGeoAnnotation> deleteList) {
        // undraw deleted annotation
        for (TmGeoAnnotationModListener l : tmGeoAnnoModListeners) {
            l.annotationsDeleted(deleteList);
        }
    }
    
    private void fireAnnotationReparented(TmGeoAnnotation annotation) {
        for (TmGeoAnnotationModListener l : tmGeoAnnoModListeners) {
            l.annotationReparented(annotation);
        }
    }
    
    private void fireAnchoredPathsRemoved(List<TmAnchoredPath> deleteList) {
        // undraw deleted annotation
        for (TmAnchoredPathListener l : tmAnchoredPathListeners) {
            l.removeAnchoredPaths(deleteList);
        }
    }
    
    private void fireAnchoredPathAdded(TmAnchoredPath path) {
        for (TmAnchoredPathListener l : tmAnchoredPathListeners) {
            l.addAnchoredPath(path);
        }
    }
    
    private void fireWorkspaceLoaded(TmWorkspace workspace) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.workspaceLoaded(workspace);
        }
    }

    private void fireNeuronSelected(TmNeuron neuron) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronSelected(neuron);
        }
    }

    private void fireNeuronStyleChanged(TmNeuron neuron, NeuronStyle style) {
        for (GlobalAnnotationListener l: globalAnnotationListeners) {
            l.neuronStyleChanged(neuron, style);
        }
    }
    
    private void fireNotesUpdated(TmWorkspace workspace) {
        if (notesUpdateListener != null) {
            notesUpdateListener.notesUpdated(workspace);
        }
    }

}
