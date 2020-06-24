package org.janelia.workstation.gui.large_volume_viewer.skeleton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.gui.large_volume_viewer.tracing.AnchoredVoxelPath;
import org.janelia.workstation.gui.large_volume_viewer.tracing.SegmentIndex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Skeleton {
    
    public static final String SKELETON_LOOKUP_PATH = "Skeleton/Node";
    
	@SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Skeleton.class);

    private class VersionedLinkedHashSet<E> extends LinkedHashSet<E> {
        private int version=0;

        public synchronized int getVersion() { return version; }

        public synchronized void incrementVersion() { version++; }

        @Override
        public boolean add(E e) {
            if (super.add(e)) {
                version++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean remove(Object o) {
            if (super.remove(o)) {
                version++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void clear() {
            version++;
            super.clear();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            if (super.removeAll(c)) {
                version++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            if (super.addAll(c)) {
                version++;
                return true;
            } else {
                return false;
            }
        }

    }

	/**
	 * AnchorSeed holds enough data to nucleate a new Anchor.
	 * So I can send multiple values in a single argument to events.
	 * @author brunsc
	 *
	 */
	public class AnchorSeed {
		private Vec3 location;
		private Anchor parent;

		public AnchorSeed(Vec3 locationInMicrometers, Anchor parent) {
            TileFormat.VoxelXyz locationInVoxels = 
                tileFormat.voxelXyzForMicrometerXyz(
                    new TileFormat.MicrometerXyz(
                            locationInMicrometers.getX(),
                            locationInMicrometers.getY(),
                            locationInMicrometers.getZ()
                    )
            );
			this.location = new Vec3( 
                    locationInVoxels.getX(), 
                    locationInVoxels.getY(), 
                    locationInVoxels.getZ() 
            );
			this.parent = parent;
		}
		
		public Long getParentGuid() {
			if (parent == null)
				return null;
			return parent.getGuid();
		}
		
		public Vec3 getLocation() {
			return location;
		}
	};
	
    private SkeletonController controller;
    private TileFormat tileFormat;

	private final VersionedLinkedHashSet<Anchor> _anchors = new VersionedLinkedHashSet<>();
    private final Set<Anchor> anchors = Collections.synchronizedSet(_anchors);
    private Anchor nextParent;
    private Anchor hoverAnchor;
	
	private final Map<SegmentIndex, AnchoredVoxelPath> tracedSegments = new ConcurrentHashMap<>();
	
	private final Map<Long, Anchor> anchorsByGuid = new HashMap<>();
	// TODO - anchor browsing history should maybe move farther back
//	private final HistoryStack<Anchor> anchorHistory = new HistoryStack<>();

    public void setController(SkeletonController controller) {
        this.controller = controller;
    }

    public void incrementAnchorVersion() { _anchors.incrementVersion(); }
    
	public Anchor addAnchor(Anchor anchor) {
		if (anchors.contains(anchor))
			return anchor;
		anchors.add(anchor);
		Long guid = anchor.getGuid();
		if (guid != null)
			anchorsByGuid.put(guid, anchor);
//		anchorHistory.push(anchor);
		return anchor;
	}

    public void addAnchors(List<Anchor> anchorList) {
        for (Anchor anchor: anchorList) {
            addAnchor(anchor);
        }
    }

    /**
     * @param tileFormat the tileFormat to set
     */
    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat = tileFormat;
    }
    
    public TileFormat getTileFormat() {
        return tileFormat;
    }

	public void addAnchorAtXyz(Vec3 xyz, Anchor parent) {
        controller.anchorAdded(new AnchorSeed(xyz, parent));
	}
    
    /**
     * Externally drive focus, given a target anchor.
     * 
     * @param annotationID look this up for loc.
     */
    public Vec3 setFocusByAnchorID( long annotationID ) {
        Anchor focusAnchor = getAnchorByID(annotationID);
        Vec3 location = null;
        if (focusAnchor != null) {
            location = focusAnchor.getLocation();
            controller.setLVVFocus( location );
        }
        return location;
    }

	public boolean connect(Anchor anchor1, Anchor anchor2) {
		if (! anchors.contains(anchor1))
			return false;
		if (! anchors.contains(anchor2))
			return false;
		if (! anchor1.addNeighbor(anchor2))
			return false;
        _anchors.incrementVersion();
        controller.skeletonChanged();
		return true;
	}
    
    /**
     * @return the nextParent
     */
    public Anchor getNextParent() {
        return nextParent;
    }

    /**
     * @param nextParent the nextParent to set
     */
    public void setNextParent(Anchor nextParent) {
        this.nextParent = nextParent;
    }

    /**
     * @return the hoverAnchor
     */
    public Anchor getHoverAnchor() {
        return hoverAnchor;
    }

    /**
     * @param hoverAnchor the hoverAnchor to set
     */
    public void setHoverAnchor(Anchor hoverAnchor) {
        this.hoverAnchor = hoverAnchor;
    }

    public void moveNeuriteRequest(Anchor anchor) {
        controller.moveNeuriteRequested(anchor);
    }

    public void smartMergeNeuriteRequest(Anchor clickedAnchor) {
        controller.smartMergeNeuriteRequested(clickedAnchor, getNextParent());
    }

	public boolean delete(Anchor anchor) {
		if (! anchors.contains(anchor))
			return false;
		if (anchor == null)
			return false;
		for (Anchor n : anchor.getNeighbors()) {
			n.getNeighbors().remove(anchor);
		}
		anchor.getNeighbors().clear();
		anchors.remove(anchor);
		Long guid = anchor.getGuid();
		if (guid != null)
			anchorsByGuid.remove(guid);
		//
//		anchorHistory.remove(anchor);
		return true;
	}

    /** Relay from external callers. */
    public void skeletonChanged() {
        controller.skeletonChanged();
    }

    public List<Anchor> addTmGeoAnchors(List<TmGeoAnnotation> annotationList) {
        List<Anchor> anchorList = new ArrayList<>();
        Map<String, Anchor> tempAnchorsByGuid = new HashMap<>();
        for (TmGeoAnnotation ann : annotationList) {
            Vec3 location = new Vec3(ann.getX(), ann.getY(), ann.getZ());
            Anchor anchor = new Anchor(location, null, ann.getNeuronId(), tileFormat);
            anchor.setGuid(ann.getId());
            tempAnchorsByGuid.put(ann.getNeuronId()+"-"+ann.getId(), anchor);
        }
        for (TmGeoAnnotation ann : annotationList) {
            Anchor anchor = tempAnchorsByGuid.get(ann.getNeuronId()+"-"+ann.getId());
            Anchor parentAnchor = tempAnchorsByGuid.get(ann.getNeuronId()+"-"+ann.getParentId());
            anchor.addNeighbor(parentAnchor);
            // only check the current batch of anchors for the parent, not the cache;
            //  we need to work with the latest anchor objects, not
            //  retrieve older objects that aren't connected right;
            //  currently parents are guaranteed to precede children in
            //  the input list (true as of July 2017); also true in July
            //  2017: list will include only complete neurons (ie, all
            //  annotations in one neuron); this is fragile! in the future,
            //  should probably handle situations where we aren't getting
            //  the full neuron (so do need to check the cache), and are
            //  possibly getting parents out of order (so ugh)

            anchorList.add(anchor);
        }

        addAnchors(anchorList);
        return anchorList;
    }
    
    public void deleteTmGeoAnchor(TmGeoAnnotation tga) {
        Anchor anchor = anchorsByGuid.get(tga.getId());
        if (anchor == null) {
            return;
        }
        delete(anchor);
    }
    
    public void reparentTmGeoAnchor(TmGeoAnnotation annotation) {
        Anchor anchor = anchorsByGuid.get(annotation.getId());
        
        HashSet<Long> annotationNeighbors = new HashSet<Long>(annotation.getChildIds().size() + 1);
        for (Long childId : annotation.getChildIds()) {
            annotationNeighbors.add(childId);
        }
        // might be reparented to have no parent:
        if (!annotation.isRoot()) {
            annotationNeighbors.add(annotation.getParentId());
        }

        // Update neuron id, in case the anchor was moved to another neuron
        anchor.setNeuronID(annotation.getNeuronId());
        
        updateNeighbors(anchor, annotationNeighbors);
    }
    
    public void moveTmGeoAnchorBack(TmGeoAnnotation tga) {
        Anchor anchor = anchorsByGuid.get(tga.getId());
        if (anchor == null) {
            return;
        }
        final Vec3 voxelVec3 = new Vec3(tga.getX(), tga.getY(), tga.getZ());
        anchor.setLocationSilent(tileFormat.micronVec3ForVoxelVec3Centered(voxelVec3));
    }
    
    public void moveTmGeoAnchor(TmGeoAnnotation tga) {
        Anchor anchor = anchorsByGuid.get(tga.getId());
        if (anchor == null) {
            return;
        }
        final Vec3 voxelVec3 = new Vec3(tga.getX(), tga.getY(), tga.getZ());
        // "silent" because we don't want to trigger the whole "move or merge?" dialog,
        // especially when triggered from a Horta/NeuronModelAdapter move
        anchor.setLocationSilent(tileFormat.micronVec3ForVoxelVec3Centered(voxelVec3));
    }
    
	public void clear() {
		if (anchors.size() == 0) {
			return; // no change
		}
		anchors.clear();
		anchorsByGuid.clear();
//		anchorHistory.clear();
	}
	
    /** given an anchor, update its neighbors to match the input set of
     * IDs; intended to keep an anchor's hierarchy in sync with the
     * annotations in the db; note that the update is bidirectional,
     * since neighbors are bidirectional; that is, it will update the
     * anchor at the other end of the neighbor relationship, too
     *
     * @param anchor
     * @param newNeighborIDs
     */
    public void updateNeighbors(Anchor anchor, Set<Long> newNeighborIDs) {

        HashSet<Long> currentNeighborIDs = new HashSet<Long>(anchor.getNeighbors().size());
        for (Anchor a: anchor.getNeighbors()) {
            currentNeighborIDs.add(a.getGuid());
        }

        // add anchors that are in new but not current:
        // intersection is in-place, so create a copy before proceeding
        HashSet<Long> toBeAdded = new HashSet<Long>(newNeighborIDs);
        toBeAdded.removeAll(currentNeighborIDs);
        for (Long id: toBeAdded) {
            Anchor addAnchor = anchorsByGuid.get(id);
            anchor.addNeighbor(addAnchor);
            addAnchor.addNeighbor(anchor);
        }

        // remove anchors that are in current but not in new
        HashSet<Long> toBeRemoved = new HashSet<Long>(currentNeighborIDs);
        toBeRemoved.removeAll(newNeighborIDs);
        for (Long id: toBeRemoved) {
            Anchor removeAnchor = anchorsByGuid.get(id);
            // hmm, Anchor class doesn't have remove method
            anchor.getNeighbors().remove(removeAnchor);
            removeAnchor.getNeighbors().remove(anchor);
        }
        _anchors.incrementVersion();
    }

    /**
     * Returns a synchronized set of anchors. To safely iterate over this 
     * set, synchronize on it first.
     */
	public Set<Anchor> getAnchors() {
		return anchors;
	}

    public Anchor getAnchorByID(Long anchorID) {
        return anchorsByGuid.get(anchorID);
    }

//	public HistoryStack<Anchor> getHistory() {
//		return anchorHistory;
//	}

    /**
     * request trace path to parent
     */
    public void traceAnchorConnection(Anchor anchor) {
        if (anchor == null)
            return;
        if (anchor.getNeighbors().size() < 1)
            // no parent
            return;

        controller.pathTraceRequested(anchor.getNeuronID(), anchor.getGuid());
    }

	public void addTracedSegment(AnchoredVoxelPath path)
	{
	    SegmentIndex ix = path.getSegmentIndex();
		tracedSegments.put(ix, path);
		// log.info("tracedSegments.size() [300] = "+tracedSegments.size());
	}

    public void addTracedSegments(List<AnchoredVoxelPath> pathList) {
        for (AnchoredVoxelPath path: pathList) {
            SegmentIndex ix = path.getSegmentIndex();
            tracedSegments.put(ix, path);
        }
    }

    public void removeTracedSegment(AnchoredVoxelPath path) {
        SegmentIndex ix = path.getSegmentIndex();
        tracedSegments.remove(ix);
    }

    public void removeTracedSegments(Long neuronID) {
        List<SegmentIndex> toDelete = new ArrayList<>();
        for(AnchoredVoxelPath path : tracedSegments.values()) {
            if (path.getNeuronID().equals(neuronID)) {
                toDelete.add(path.getSegmentIndex());
            }
        }
        for (SegmentIndex segmentIndex : toDelete) {
            tracedSegments.remove(segmentIndex);
        }
    }   
    
	public Collection<AnchoredVoxelPath> getTracedSegments() {
		// log.info("tracedSegments.size() [305] = "+tracedSegments.size());
		Collection<AnchoredVoxelPath> result = tracedSegments.values();
		// log.info("tracedSegments.values().size() [307] = "+result.size());
		return result;
	}

    public int getAnchorSetVersion() {
        return _anchors.getVersion();
    }

}
