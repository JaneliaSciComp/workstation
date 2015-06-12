/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.workstation.tracing.VoxelPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes the burden of managing certain model-like data, off the actors.
 *
 * @author fosterl
 */
public class SkeletonSegmentManager {
    
    private Skeleton skeleton;
    private Map<Anchor, Integer> neuronAnchorIndices = new HashMap<>();
    private Map<Long, Map<SegmentIndex, TracedPathActor>> neuronTracedSegments = new HashMap<>();
    private Map<Long, List<Integer>> lineIndices;
    private SkeletonActorStateUpdater updater;    
    
    private static final Logger log = LoggerFactory.getLogger( SkeletonSegmentManager.class );
    
    public SkeletonSegmentManager(Skeleton skeleton, SkeletonActorStateUpdater updater) {
        this(skeleton);
        this.updater = updater;
    }
    
    public SkeletonSegmentManager(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public void updateTracedPaths() {
        // Update Traced path actors

        // first, a short-circuit; if there are no anchors, the whole
        //  skeleton was cleared, and we can clear our traced segments as well;
        //  this is necessary because unlike in the old not-per-neuron way of
        //  doing things, we would normally need some info from anchors that
        //  just isn't there when the whole skeleton is cleared
        if (skeleton.getAnchors().size() == 0) {
            getNeuronTracedSegments().clear();
            return;
        }

        Set<SegmentIndex> foundSegments = new HashSet<>();
        Collection<AnchoredVoxelPath> skeletonSegments = skeleton.getTracedSegments();
        // log.info("Skeleton has " + skeletonSegments.size() + " traced segments");
        for (AnchoredVoxelPath segment : skeletonSegments) {
            SegmentIndex ix = segment.getSegmentIndex();

            // need neuron ID; get it from the anchor at either end of the
            //  traced path; if there isn't an anchor, just move on--that
            //  path is also gone (happens when neurons deleted, merged)
            Anchor pathAnchor = skeleton.getAnchorByID(ix.getAnchor1Guid());
            if (pathAnchor == null) {
                continue;
            }
            Long neuronID = pathAnchor.getNeuronID();

            foundSegments.add(ix);
            if (getNeuronTracedSegments().containsKey(neuronID)) {
                if (getNeuronTracedSegments().get(neuronID).containsKey(ix)) {
                    // Is the old traced segment still valid?
                    AnchoredVoxelPath oldSegment = getNeuronTracedSegments().get(neuronID).get(ix).getSegment();
                    List<VoxelPosition> p0 = oldSegment.getPath();
                    List<VoxelPosition> p1 = segment.getPath();
                    boolean looksTheSame = true;
                    if (p0.size() != p1.size()) // same size?
                    {
                        looksTheSame = false;
                    } else if (p0.get(0) != p1.get(0)) // same first voxel?
                    {
                        looksTheSame = false;
                    } else if (p0.get(p0.size() - 1) != p1.get(p1.size() - 1)) // same final voxel?
                    {
                        looksTheSame = false;
                    }
                    if (looksTheSame) {
                        continue; // already have this segment, no need to recompute!
                    } else {
                        getNeuronTracedSegments().get(neuronID).remove(ix);
                    }
                }
            } else {
                // haven't seen this neuron yet
                getNeuronTracedSegments().put(neuronID, new ConcurrentHashMap<SegmentIndex, TracedPathActor>());
            }
            TracedPathActor actor = new TracedPathActor(segment, skeleton.getTileFormat());

            getNeuronTracedSegments().get(neuronID).put(actor.getSegmentIndex(), actor);

            // not sure why this is in the loop instead of out of it!
            //  all it does is trigger a repaint; I suppose it's better to
            //  paint after every path added, so they pop in as they are
            //  ready; paint can't be that expensive, can it?
            if (updater != null) {
                updater.update();
            }
        }

        // carefully iterate over segments and prune the obsolete ones
        for (Long neuronID : getNeuronTracedSegments().keySet()) {
            Set<SegmentIndex> neuronSegmentIndices = new HashSet<>(getNeuronTracedSegments().get(neuronID).keySet());
            for (SegmentIndex ix : neuronSegmentIndices) {
                if (!foundSegments.contains(ix)) {
                    log.info("Removing orphan segment");
                    getNeuronTracedSegments().get(neuronID).remove(ix);
                }
            }
        }
    }

    public int getIndexForAnchor(Anchor anchor) {
        if (anchor == null) {
            return -1;
        }
        if (neuronAnchorIndices.containsKey(anchor)) {
            return neuronAnchorIndices.get(anchor);
        }
        return -1;
    }
    
    public void putIndexForAnchor(Anchor anchor, Integer vertexIndex) {
        neuronAnchorIndices.put(anchor, vertexIndex);
    }
    
    public void clearAnchorIndices() {
        neuronAnchorIndices.clear();
    }

    public void updateLines() {
        // iterate through anchors and record lines where there are no traced
        //  paths; then copy the line indices you get into an array
        // note: I believe this works because we process the points and
        //  lines in exactly the same order (the order skeleton.getAnchors()
        //  returns them in)

        Map<Long, List<Integer>> tempLineIndices = new HashMap<>();
        for (Anchor anchor : skeleton.getAnchors()) {
            int i1 = getIndexForAnchor(anchor);
            if (i1 < 0) {
                continue;
            }
            for (Anchor neighbor : anchor.getNeighbors()) {
                int i2 = getIndexForAnchor(neighbor);
                if (i2 < 0) {
                    continue;
                }
                if (i1 >= i2) {
                    continue; // only use ascending pairs, for uniqueness
                }
                SegmentIndex segmentIndex = new SegmentIndex(anchor.getGuid(), neighbor.getGuid());
                // if neuron has any paths, check and don't draw line
                //  where there's already a traced segment
                if (getNeuronTracedSegments().containsKey(anchor.getNeuronID())
                        && getNeuronTracedSegments().get(anchor.getNeuronID()).containsKey(segmentIndex)) {
                    continue;
                }
                if (!tempLineIndices.containsKey(anchor.getNeuronID())) {
                    tempLineIndices.put(anchor.getNeuronID(), new Vector<Integer>());
                }
                tempLineIndices.get(anchor.getNeuronID()).add(i1);
                tempLineIndices.get(anchor.getNeuronID()).add(i2);
            }
        }
        
        lineIndices = tempLineIndices;

    }

    /**
     * @return the lineIndices
     */
    public Map<Long, List<Integer>> getLineIndices() {
        return lineIndices;
    }

    /**
     * @return the neuronTracedSegments
     */
    public Map<Long, Map<SegmentIndex, TracedPathActor>> getNeuronTracedSegments() {
        return neuronTracedSegments;
    }

}
