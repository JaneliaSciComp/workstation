package org.janelia.workstation.controller.model.annotations.neuron;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.model.util.TmNeuronUtils;
import org.janelia.workstation.controller.model.IdSource;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.model.domain.tiledMicroscope.BoundingBox3d;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NeuronModel {

    private static final Logger LOG = LoggerFactory.getLogger(NeuronModel.class);
    private final NeuronModelAdapter neuronModelAdapter = new NeuronModelAdapter();
    private final IdSource idSource = new IdSource();
    private Map<Long, TmNeuronMetadata> neuronMap;
    private Set<BoundingBox3d> boundingBoxes;
    static NeuronModel modelInstance;

    static public NeuronModel getInstance() {
        if (modelInstance==null) {
            modelInstance = new NeuronModel();
        }
        return modelInstance;
    }

    public NeuronModel() {
        neuronMap = new ConcurrentHashMap<>();
        boundingBoxes = new HashSet<BoundingBox3d>();
    }

    public Collection<TmNeuronMetadata> getNeurons() {
        return neuronMap.values();
    }

    public Collection<BoundingBox3d> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void createBoundingBoxes(Long workspaceId, List<BoundingBox3d> boundingBoxes) {
        neuronModelAdapter.createBoundingBoxes(workspaceId, boundingBoxes);
        boundingBoxes.addAll(boundingBoxes);
    }

    public void retrieveFragments (List<Long> fragSet, TmWorkspace workspace) {
        List<TmNeuronMetadata> frags = neuronModelAdapter.loadFragments(fragSet, workspace);
        for (TmNeuronMetadata frag: frags) {
            frag.initNeuronData();
            neuronMap.put(frag.getId(), frag);
        }
    }

    public void addNeuron(TmNeuronMetadata neuron) {
        neuronMap.put(neuron.getId(), neuron);
    }

    public TmNeuronMetadata removeNeuron(TmNeuronMetadata neuron) {
        return neuronMap.remove(neuron.getId());
    }

    public TmNeuronMetadata getNeuronById(Long id) {
        if (id != null) {
            return neuronMap.get(id);
        } else {
            return null;
        }
    }

    public void clearMap() {
        neuronMap = new HashMap<>();
    }

    /**
     * Add an anchored path to an existing neuron.
     *
     * @todo may need to add create, update dates + ownerKey
     * @param tmNeuronMetadata add path to this.
     * @return the anchored path thus created.
     * @throws Exception
     */
    public TmAnchoredPath addAnchoredPath(TmNeuronMetadata tmNeuronMetadata, Long annotationID1, Long annotationID2,
                                          List<List<Integer>> pointlist) throws Exception {
        if (!tmNeuronMetadata.getGeoAnnotationMap().containsKey(annotationID1)
                || !tmNeuronMetadata.getGeoAnnotationMap().containsKey(annotationID2)) {
            throw new Exception("both annotations must be in neuron");
        }
        final TmAnchoredPathEndpoints key = new TmAnchoredPathEndpoints(annotationID1, annotationID2);
        final TmAnchoredPath value = new TmAnchoredPath( idSource.next(), key, pointlist );
        tmNeuronMetadata.getAnchoredPathMap().put(key,value);
        saveNeuronData(tmNeuronMetadata);
        return value;
    }

    /**
     * Geometric annotations are essentially points in the growing neuron.
     *
     * @todo may need to add create, update dates + ownerKey
     * @param tmNeuronMetadata will receive this point.
     * @param parentAnnotationId linkage.
     * @param x cords...
     * @param y
     * @param z
     * @return the completed point-rep.
     * @throws Exception
     */
    public TmGeoAnnotation addGeometricAnnotation(TmNeuronMetadata tmNeuronMetadata,
                                                  Long parentAnnotationId,
                                                  double x, double y, double z) throws Exception {
        TmGeoAnnotation rtnVal = new TmGeoAnnotation();
        rtnVal.setX(x);
        rtnVal.setY(y);
        rtnVal.setZ(z);
        rtnVal.setCreationDate(new Date());
        rtnVal.setNeuronId(tmNeuronMetadata.getId());
        rtnVal.setParentId(parentAnnotationId);
        rtnVal.setId(idSource.next());

        // If non-root, add this as a child of its parent, and set its radius, too
        if (parentAnnotationId != null) {
            TmGeoAnnotation parent = tmNeuronMetadata.getGeoAnnotationMap().get(parentAnnotationId);
            // Parent might be the neuron itself, if this is a root.
            // Otherwise, ensure the inter-annotation linkage.
            if (parent != null) {
                parent.addChild(rtnVal);
                rtnVal.setRadius(parent.getRadius());
            }
        }
        // Handle root geo annotations.
        if (parentAnnotationId == null  ||  parentAnnotationId == tmNeuronMetadata.getId()) {
            tmNeuronMetadata.addRootAnnotation(rtnVal);
        }

        tmNeuronMetadata.getGeoAnnotationMap().put(rtnVal.getId(), rtnVal);

        saveNeuronData(tmNeuronMetadata);
        // Ensure that the geo-annotation known to the neuron, after the
        // save, is the one we return.  Get the other one out of circulation.
        rtnVal = tmNeuronMetadata.getGeoAnnotationMap().get(rtnVal.getId());
        return rtnVal;
    }

    /**
     * Given a collection of annotations, under a common neuron, make
     * annotations for each in the database, preserving the linkages implied in
     * the "value" target of the map provided.
     *
     * @param annotations map of node offset id vs "unserialized" annotation.
     * @param nodeParentLinkage map of node offset id vs parent node offset id.
     */
    public void addLinkedGeometricAnnotationsInMemory(Map<Integer, Integer> nodeParentLinkage,
                                                      Map<Integer, TmGeoAnnotation> annotations,
                                                      TmNeuronMetadata tmNeuronMetadata) {
        TmNeuronUtils.addLinkedGeometricAnnotationsInMemory(nodeParentLinkage, annotations, tmNeuronMetadata, () -> idSource.next());
    }

    public TmStructuredTextAnnotation addStructuredTextAnnotation(TmNeuronMetadata neuron, Long parentID, String data) throws Exception {
        // parent must not already have a structured text annotation
        if (neuron.getStructuredTextAnnotationMap().containsKey(parentID)) {
            throw new Exception("parent ID already has a structured text annotation; use update, not add");
        }
        TmStructuredTextAnnotation annotation = new TmStructuredTextAnnotation(idSource.next(), parentID, data);

        neuron.getStructuredTextAnnotationMap().put( parentID, annotation );

        return annotation;
    }

    public CompletableFuture<TmNeuronMetadata> deleteNeuron(TmWorkspace tmWorkspace, TmNeuronMetadata tmNeuronMetadata) {
        TmNeuronMetadata oldValue = removeNeuron(tmNeuronMetadata);
        if (oldValue!=null) {
            // Need to signal to DB that this entitydata must be deleted.
            return neuronModelAdapter.removeNeuron(tmNeuronMetadata);
        }
        else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public void deleteStructuredTextAnnotation(TmNeuronMetadata neuron, long annotationId) {
        neuron.getStructuredTextAnnotationMap().remove(annotationId);
    }

    /**
     * The workspace's neurons will not have been loaded with the workspace,
     * because we wish to be able to load them from any client, including
     * one which happens to be on the server.
     */
    public void loadWorkspaceNeurons(TmWorkspace workspace) throws Exception {
        neuronMap.clear();
        // addNeurons() must be done serially, so flatten the stream:
        for (TmNeuronMetadata n: neuronModelAdapter.loadNeurons(workspace).collect(Collectors.toList())) {
            addNeuron(n);
        }
        LOG.info("loadWorkspaceNeurons() loaded {} neurons", neuronMap.size());
    }

    /**
     * Fetches the precomputed bounding boxes from a workspace so a scalable index can be built to selectively
     * download the fragments in the vicinity.
     * TO DO: To speed up development, I'm using the existing spatial filter and just using the corners of the bounding box
     * (6 3D points) to find close neurons.  To scale up a bit, this should really be a 3D spatial engine using r*trees
     * for full 3D annotation support.
     */
    public void loadWorkspaceBoundingBoxes (TmWorkspace workspace) throws Exception {
        LOG.info("loadWorkspaceNeurons() loaded {} neurons", neuronMap.size());
        boundingBoxes = new HashSet<BoundingBox3d>(neuronModelAdapter.loadBoundingBoxes(workspace));
    }

    /**
     * Moves the annotation and its tree from source to destination neuron.
     * Does not refresh from database, or save to database.
     *
     * @param annotation this will be moved.
     * @param oldTmNeuronMetadata this is the current (source) container of the annotation.
     * @param newTmNeuronMetadata this is the destination container of the annotation.
     * @throws Exception thrown by called methods.
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuronMetadata oldTmNeuronMetadata, TmNeuronMetadata newTmNeuronMetadata) throws Exception {
        long newNeuronId = newTmNeuronMetadata.getId();

        // already same neuron?  done!
        if (oldTmNeuronMetadata.getId().equals(newNeuronId)) {
            return;
        }

        // Find the root annotation.  Ultimate parent of the annotation.
        TmGeoAnnotation rootAnnotation = annotation;
        while (!rootAnnotation.isRoot()) {
            rootAnnotation = oldTmNeuronMetadata.getParentOf(rootAnnotation);
        }

        // Move all the geo-annotations from the old to the new neuron.
        Map<Long,TmGeoAnnotation> movedAnnotationIDs = new HashMap<>();
        final Map<Long, TmStructuredTextAnnotation> oldStructuredTextAnnotationMap = oldTmNeuronMetadata.getStructuredTextAnnotationMap();
        final Map<Long, TmStructuredTextAnnotation> newStructuredTextAnnotationMap = newTmNeuronMetadata.getStructuredTextAnnotationMap();
        for (TmGeoAnnotation ann: oldTmNeuronMetadata.getSubTreeList(rootAnnotation)) {
            movedAnnotationIDs.put(ann.getId(), ann);
            ann.setNeuronId(newNeuronId);
            newTmNeuronMetadata.getGeoAnnotationMap().put(ann.getId(), ann);
            // move any TmStructuredTextAnnotations as well:
            if (oldStructuredTextAnnotationMap.containsKey(ann.getId())) {
                TmStructuredTextAnnotation note = oldStructuredTextAnnotationMap.get(ann.getId());
                oldStructuredTextAnnotationMap.remove(ann.getId());
                newStructuredTextAnnotationMap.put(ann.getId(), note);
            }

        }

        // loop over anchored paths; if endpoints are in set of moved annotations,
        //  move the path as well
        Map<TmAnchoredPathEndpoints, TmAnchoredPath> oldNeuronAnchoredPathMap = oldTmNeuronMetadata.getAnchoredPathMap();
        Map<TmAnchoredPathEndpoints,TmAnchoredPath> newNeuronAnchoredPathMap = newTmNeuronMetadata.getAnchoredPathMap();

        // the usual "remove items from collection you're iterating over" idiom doesn't
        //  work here because the Map is really a Mongo-backed thing with weird internal
        //  state; so, do it explicitly in multiple loops:

        Set<TmAnchoredPathEndpoints> moveSet = new HashSet<>();
        for (TmAnchoredPathEndpoints endpoints: oldNeuronAnchoredPathMap.keySet()) {
            if (movedAnnotationIDs.containsKey(endpoints.getFirstAnnotationID())) {
                moveSet.add(endpoints);
            }
        }

        for (TmAnchoredPathEndpoints endpoints: moveSet) {
            TmAnchoredPath anchoredPath = oldNeuronAnchoredPathMap.get(endpoints);
            newNeuronAnchoredPathMap.put(endpoints, anchoredPath);
        }


        // Need to remove all these annotations from the old map, after
        // iteration through the map above, to avoid concurrent modification.
        for (Long movedAnnotationID: movedAnnotationIDs.keySet()) {
            oldTmNeuronMetadata.getGeoAnnotationMap().remove(movedAnnotationID);
        }

        // if it's the root, also change its parent annotation to the new neuron
        rootAnnotation.setParentId(newTmNeuronMetadata.getId());
        oldTmNeuronMetadata.removeRootAnnotation(rootAnnotation);
        newTmNeuronMetadata.addRootAnnotation(rootAnnotation);
    }

    /**
     * Change the parentage of the annotation to the new annotation ID.  This
     * operation is partial.  It can be applied to annotations which are
     * incomplete.  Therefore, this operation does not carry out exchange with
     * the database.
     *
     * @param annotation this gets different parent.
     * @param newParentAnnotationID this becomes the new parent.
     * @param tmNeuronMetadata the annotation and new parent must be under this neuron.
     * @throws Exception thrown if condition(s) above not met.
     */
    public void reparentGeometricAnnotation(TmGeoAnnotation annotation,
                                            Long newParentAnnotationID,
                                            TmNeuronMetadata tmNeuronMetadata) throws Exception {

        // verify that both annotations are in the input neuron
        if (!tmNeuronMetadata.getGeoAnnotationMap().containsKey(annotation.getId())) {
            throw new Exception("input neuron doesn't contain child annotation " + annotation.getId());
        }
        if (!tmNeuronMetadata.getGeoAnnotationMap().containsKey(newParentAnnotationID)) {
            throw new Exception("input neuron doesn't contain new parent annotation " + newParentAnnotationID);
        }

        // is it already the parent?
        if (annotation.getParentId().equals(newParentAnnotationID)) {
            return;
        }

        // Ensure: using the very same copy that is known to the neuron.
        annotation = tmNeuronMetadata.getGeoAnnotationMap().get(annotation.getId());

        // do NOT create cycles! new parent cannot be in original annotation's subtree:
        for (TmGeoAnnotation testAnnotation : tmNeuronMetadata.getSubTreeList(annotation)) {
            if (newParentAnnotationID.equals(testAnnotation.getId())) {
                return;
            }
        }

        // The reparented annotation will no longer be a root.
        if (annotation.isRoot()) {
            tmNeuronMetadata.removeRootAnnotation(annotation);
        }

        // Change child/down linkage.
        TmGeoAnnotation parentAnnotation = tmNeuronMetadata.getParentOf(annotation);
        if (parentAnnotation != null) {
            parentAnnotation.getChildIds().remove(annotation.getId());
        }
        // Change parent ID.
        annotation.setParentId(newParentAnnotationID);
        TmGeoAnnotation newParentAnnotation = tmNeuronMetadata.getGeoAnnotationMap().get(newParentAnnotationID);
        // Belt-and-suspenders: above tests that the map has this ID.
        if (newParentAnnotation != null) {
            newParentAnnotation.getChildIds().add(annotation.getId());
        }
    }

    public CompletableFuture<TmNeuronMetadata> createNeuron(TmNeuronMetadata neuron) throws Exception {
        return neuronModelAdapter.createNeuron(neuron);
    }

    /**
     * ownership change request; this version expects to happen immediately (user already
     * has authority to change the owner (they own it or it's a common neuron)
     */
    public CompletableFuture<TmNeuronMetadata> changeOwnership(TmNeuronMetadata neuron, String userKey) throws Exception {
        saveHistoricalNeuron(neuron);
        return neuronModelAdapter.changeOwnership(neuron, userKey);
    }

    /**
     * This can be a partial operation.  Therefore, it will not carry out any
     * communication with the databse.
     *
     * @param tmNeuronMetadata gets a new root.
     * @param newRoot becomes a root, and added to neuron.
     * @throws Exception
     */
    public void rerootNeurite(TmNeuronMetadata tmNeuronMetadata, TmGeoAnnotation newRoot) throws Exception {
        if (newRoot == null || tmNeuronMetadata == null) {
            return;
        }

        if (!tmNeuronMetadata.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    tmNeuronMetadata.getId(), newRoot.getId()));
        }

        // is it already a root?
        if (newRoot.isRoot()) {
            return;
        }

        // from input, follow parents up to current root, keeping them all
        List<TmGeoAnnotation> parentList = new ArrayList<>();
        TmGeoAnnotation testAnnotation = newRoot;
        while (!testAnnotation.isRoot()) {
            parentList.add(testAnnotation);
            testAnnotation = tmNeuronMetadata.getParentOf(testAnnotation);
        }
        parentList.add(testAnnotation);
        Long neuronId = tmNeuronMetadata.getId();
        testAnnotation.setNeuronId(neuronId);

        // reparent intervening annotations; skip the first item, which is the
        //  new root (which we've already dealt with)
        LOG.info("Inverting child/parent relationships.");
        for (int i = 1; i < parentList.size(); i++) {
            // change the parent ID/add as child, and save
            TmGeoAnnotation ann = parentList.get(i);

            // Remove from old parent.
            TmGeoAnnotation oldParentAnnotation = tmNeuronMetadata.getParentOf(ann);
            if (oldParentAnnotation != null) {
                oldParentAnnotation.getChildIds().remove(ann.getId());
            }
            else if (ann.isRoot()) {
                tmNeuronMetadata.removeRootAnnotation(ann);
            }

            Long newParentAnnotationID = parentList.get(i - 1).getId();
            ann.setParentId(newParentAnnotationID);
            // If it is now the parent, it cannot any longer be a child.
            ann.getChildIds().remove(newParentAnnotationID);
            ann.setNeuronId(neuronId);

            // Add to new parent.
            TmGeoAnnotation newParentAnnotation = tmNeuronMetadata.getGeoAnnotationMap().get(newParentAnnotationID);
            newParentAnnotation.addChild(ann);
        }

        // Finally, make this thing a root.
        makeRoot(tmNeuronMetadata, newRoot);
    }

    /** Encapsulating all steps linking an annotation as a root. */
    private void makeRoot(TmNeuronMetadata tmNeuronMetadata, TmGeoAnnotation newRoot) {
        if (tmNeuronMetadata.getId() == null) {
            LOG.warn("Attempting to set null parent from null neuron-id.");
        }
        newRoot.setParentId(tmNeuronMetadata.getId());
        if (! tmNeuronMetadata.containsRootAnnotation( newRoot )) {
            tmNeuronMetadata.addRootAnnotation( newRoot );
        }
    }

    public CompletableFuture<TmNeuronMetadata> saveNeuronData(TmNeuronMetadata neuron) throws Exception {
        saveHistoricalNeuron(neuron);
        return neuronModelAdapter.saveNeuron(neuron).exceptionally((e) -> {
            FrameworkAccess.handleException(e);
            return null;
        });
    }

    private void saveHistoricalNeuron(TmNeuronMetadata neuron) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        byte[] neuronData = mapper.writeValueAsBytes(neuron);
        TmHistoricalEvent event = new TmHistoricalEvent();
        Map<Long,byte[]> map = new HashMap<>();
        map.put(neuron.getId(), neuronData);
        event.setNeurons(map);
        event.setTimestamp(new Date());
        event.setType(TmHistoricalEvent.EVENT_TYPE.NEURON_UPDATE);
        TmModelManager.getInstance().getNeuronHistory().addHistoricalEvent(event);
    }

    public CompletableFuture<TmNeuronMetadata> restoreNeuronFromHistory(TmNeuronMetadata neuron) throws Exception {

        // restore from history
        TmNeuronMetadata oldNeuron = neuronMap.get(neuron.getId());
        oldNeuron.setNeuronData(neuron.getNeuronData());
        oldNeuron.setColor(neuron.getColor());
        oldNeuron.setName(neuron.getName());

        return neuronModelAdapter.saveNeuron(neuron);
    }

    public void refreshNeuronFromShared (TmNeuronMetadata neuron) throws Exception {
        TmNeuronMetadata oldNeuron = neuronMap.get(neuron.getId());
        oldNeuron.setNeuronData(neuron.getNeuronData());
        oldNeuron.initNeuronData();
        oldNeuron.setColor(neuron.getColor());
        oldNeuron.setName(neuron.getName());
    }

    public void splitNeurite(TmNeuronMetadata tmNeuronMetadata, TmGeoAnnotation newRoot) throws Exception {
        if (newRoot == null || tmNeuronMetadata == null)

        if (!tmNeuronMetadata.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    tmNeuronMetadata.getId(), newRoot.getId()));
        }

        // is it already a root?  then you can't split it (should have been
        //  checked before it gets here)
        if (newRoot.isRoot()) {
            return;
        }

        // Remove this new root as a child of its parent.
        TmGeoAnnotation oldParent = tmNeuronMetadata.getParentOf(newRoot);
        if (oldParent != null) {
            oldParent.getChildIds().remove(newRoot.getId());
        }
        // Ensure neuron knows this root; reset its parent
        //  to the neuron (as one does for a root).
        if (tmNeuronMetadata.getId() == null) {
            LOG.error("Neuron ID= null.  Setting annotation as root will fail.");
        }
        makeRoot(tmNeuronMetadata, newRoot);
    }

    public void updateStructuredTextAnnotation(TmNeuronMetadata neuron, TmStructuredTextAnnotation annotation, String jsonString) throws Exception {
        // note for the future: this method and the delete one below ought to be just
        //  in-lined in NeuronManager; maybe the add method above, too
        annotation.setDataString(jsonString);
        neuron.getStructuredTextAnnotationMap().put(annotation.getParentId(), annotation);
    }

    public int getPendingOperations() {
        return neuronModelAdapter.getPendingOperations();
    }
}
