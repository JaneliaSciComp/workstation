package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.HistoryStack;
import org.janelia.it.workstation.signal.Signal;
import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot;
import org.janelia.it.workstation.signal.Slot1;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.SegmentIndex;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.AnchorListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.AnchoredVoxelPathListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Skeleton implements AnchoredVoxelPathListener {
	@SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Skeleton.class);

    //---------------------------------IMPLEMENTS AnchoredVoxelPathListener
    @Override
    public void addAnchoredVoxelPath(AnchoredVoxelPath path) {
        addTracedSegment(path);
    }

    @Override
    public void addAnchoredVoxelPaths(List<AnchoredVoxelPath> paths) {
        addTracedSegments(paths);
    }

    @Override
    public void removeAnchoredVoxelPath(AnchoredVoxelPath path) {
        removeTracedSegment(path);
    }
	
    private TileFormat tileFormat;

	/**
	 * AnchorSeed holds enough data to nucleate a new Anchor.
	 * So I can send multiple values in a single argument for Signal1/Slot1.
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
	
	private Set<Anchor> anchors = new LinkedHashSet<Anchor>();
    private Collection<AnchorListener> anchorListeners = new ArrayList<>();
	
	private Map<SegmentIndex, AnchoredVoxelPath> tracedSegments =
			new ConcurrentHashMap<SegmentIndex, AnchoredVoxelPath>();
	
	private Map<Long, Anchor> anchorsByGuid = new HashMap<Long, Anchor>();
	// TODO - anchor browsing history should maybe move farther back
	private HistoryStack<Anchor> anchorHistory = new HistoryStack<Anchor>();

	public Signal skeletonChangedSignal = new Signal();
	public Signal1<Long> pathTraceRequestedSignal = new Signal1<Long>();

    public void addAnchorListener(AnchorListener listener) {
        anchorListeners.add(listener);
    }
    
    public void removeAnchorListener(AnchorListener listener) {
        anchorListeners.remove(listener);
    }
    
	// API for synchronizing with back end database
	// after discussion with Don Olbris July 8, 2013
	// 
	///// ADD
	public Signal1<AnchorSeed> addAnchorRequestedSignal = 
			new Signal1<AnchorSeed>();
	// Response from database
	public Slot1<TmGeoAnnotation> addAnchorSlot = new Slot1<TmGeoAnnotation>() {
		@Override
		public void execute(TmGeoAnnotation tga) {
			Vec3 location = new Vec3(tga.getX(), tga.getY(), tga.getZ());
			Anchor parentAnchor = anchorsByGuid.get(tga.getParentId());
			Anchor anchor = new Anchor(location, parentAnchor, tileFormat);
			anchor.setGuid(tga.getId());
			addAnchor(anchor);
		}
	};
    /**
     * add multiple anchors at once; in the input list, every annotation must
     * appear later in the list than its parent, or the connecting lines won't
     * be drawn correctly
     */
    public Slot1<List<TmGeoAnnotation>> addAnchorsSlot = new Slot1<List<TmGeoAnnotation>>() {
        @Override
        public void execute(List<TmGeoAnnotation> annotationList) {
            List<Anchor> anchorList = new ArrayList<Anchor>();
            Map<Long, Anchor> tempAnchorsByGuid = new HashMap<Long, Anchor>();
            for (TmGeoAnnotation ann: annotationList) {
                Vec3 location = new Vec3(ann.getX(), ann.getY(), ann.getZ());

                // anchorsByGuid isn't populated until the next call, so we
                //  need to check for parents in the batch we're adding as
                //  well; this is a bit redundant, but it maintatins the
                //  separation; this slot only prepares a list, it doesn't
                //  write to any of Skeleton's data structures
                Anchor parentAnchor = anchorsByGuid.get(ann.getParentId());
                if (parentAnchor == null) {
                    // check our batch, too
                    parentAnchor = tempAnchorsByGuid.get(ann.getParentId());
                }

                Anchor anchor = new Anchor(location, parentAnchor, tileFormat);
                anchor.setGuid(ann.getId());
                tempAnchorsByGuid.put(anchor.getGuid(), anchor);
                anchorList.add(anchor);
            }
            addAnchors(anchorList);
        }
    };
	// AFTER anchor has already been added (not simply requested)
	public Signal1<Anchor> anchorAddedSignal = new Signal1<Anchor>();

	///// DELETE
	// Anchor deletion
	public Slot1<TmGeoAnnotation> deleteAnchorSlot = new Slot1<TmGeoAnnotation>() {
		@Override
		public void execute(TmGeoAnnotation tga) {
			Anchor anchor = anchorsByGuid.get(tga.getId());
			if (anchor == null)
				return;
			delete(anchor);
		}
	};
    public Slot1<TmGeoAnnotation> reparentAnchorSlot = new Slot1<TmGeoAnnotation>() {
        @Override
        public void execute(TmGeoAnnotation annotation) {
            Anchor anchor = anchorsByGuid.get(annotation.getId());
            HashSet<Long> annotationNeighbors = new HashSet<Long>(annotation.getChildIds().size() + 1);
            for (Long childId: annotation.getChildIds()) {
                annotationNeighbors.add(childId);
            }
            // might be reparented to have no parent:
            if (!annotation.isRoot()) {
                annotationNeighbors.add(annotation.getParentId());
            }

            updateNeighbors(anchor, annotationNeighbors);
        }
    };
	public Slot1<Anchor> deleteAnchorShortCircuitSlot = new Slot1<Anchor>() {
		@Override
		public void execute(Anchor anchor) {delete(anchor);}
	};
	public Signal1<Anchor> anchorDeletedSignal = new Signal1<Anchor>();
    public Signal1<Anchor> anchorReparentedSignal = new Signal1<Anchor>();
    public Signal1<Anchor> anchorNeighborsUpdatedSignal = new Signal1<Anchor>();

	///// CLEAR
	public Slot clearSlot = new Slot() {
		@Override
		public void execute() {
			clear();
		}
	};
	public Signal clearedSignal = new Signal();

	///// MOVE
	public Signal1<Anchor> anchorMovedSignal = new Signal1<Anchor>();
	public Signal1<Anchor> anchorMovedSilentSignal = new Signal1<Anchor>();
	public Slot1<TmGeoAnnotation> moveAnchorBackSlot = new Slot1<TmGeoAnnotation>() {
		@Override
		public void execute(TmGeoAnnotation tga) {
			Anchor anchor = anchorsByGuid.get(tga.getId());
			if (anchor == null)
				return;
            final Vec3 voxelVec3 = new Vec3(tga.getX(), tga.getY(), tga.getZ());
			anchor.setLocationSilent(tileFormat.micronVec3ForVoxelVec3Centered(voxelVec3));
		}
	};

//    public Slot1<AnchoredVoxelPath> addAnchoredPathSlot = new Slot1<AnchoredVoxelPath>() {
//        @Override
//        public void execute(AnchoredVoxelPath path) {
//            addTracedSegment(path);
//        }
//    };
//
//    public Slot1<List<AnchoredVoxelPath>> addAnchoredPathsSlot = new Slot1<List<AnchoredVoxelPath>>() {
//        @Override
//        public void execute(List<AnchoredVoxelPath> pathList) {
//            addTracedSegments(pathList);
//        }
//    };
//
//    public Slot1<AnchoredVoxelPath> removeAnchoredPathSlot = new Slot1<AnchoredVoxelPath>() {
//        @Override
//        public void execute(AnchoredVoxelPath path) {
//            removeTracedSegment(path);
//        }
//    };

	public Skeleton() {
		// Don't make this connection when using workstation database
		// addAnchorRequestedSignal.connect(addShortCircuitAnchorSlot);
		// subtreeDeleteRequestedSignal.connect(deleteAnchorShortCircuitSlot); // TODO remove
		//
		// once anchor changes are persisted in db, we get signals:
		anchorAddedSignal.connect(skeletonChangedSignal);
		anchorDeletedSignal.connect(skeletonChangedSignal);
        anchorReparentedSignal.connect(skeletonChangedSignal);
        anchorNeighborsUpdatedSignal.connect(skeletonChangedSignal);
		anchorMovedSignal.connect(skeletonChangedSignal);
		anchorMovedSilentSignal.connect(skeletonChangedSignal);
		// log.info("Skeleton constructor");
	}
	
	public Anchor addAnchor(Anchor anchor) {
		if (anchors.contains(anchor))
			return anchor;
		anchors.add(anchor);
		Long guid = anchor.getGuid();
		if (guid != null)
			anchorsByGuid.put(guid, anchor);
		anchor.anchorMovedSignal.disconnect(this.anchorMovedSignal);
		anchor.anchorMovedSignal.connect(this.anchorMovedSignal);
		anchor.anchorMovedSilentSignal.disconnect(this.anchorMovedSilentSignal);
		anchor.anchorMovedSilentSignal.connect(this.anchorMovedSilentSignal);
		anchorHistory.push(anchor);
		anchorAddedSignal.emit(anchor);
		return anchor;
	}

    /**
     * @param tileFormat the tileFormat to set
     */
    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat = tileFormat;
    }
	
	public void addAnchors(List<Anchor> anchorList) {
        for (Anchor anchor: anchorList) {
            if (anchors.contains(anchor))
                continue;
            anchors.add(anchor);
            Long guid = anchor.getGuid();
            if (guid != null)
                anchorsByGuid.put(guid, anchor);
            anchor.anchorMovedSignal.disconnect(this.anchorMovedSignal);
            anchor.anchorMovedSignal.connect(this.anchorMovedSignal);
            anchor.anchorMovedSilentSignal.disconnect(this.anchorMovedSilentSignal);
            anchor.anchorMovedSilentSignal.connect(this.anchorMovedSilentSignal);
            anchorHistory.push(anchor);
        }
        // this is a bit of a cheat; I send one anchor knowing that it's
        //  never used--the whole skeleton gets updated, and that's what
        //  I want since I've potentially added many anchors
        anchorAddedSignal.emit(anchorList.get(0));
	}

	public void addAnchorAtXyz(Vec3 xyz, Anchor parent) {        
		addAnchorRequestedSignal.emit(new AnchorSeed(xyz, parent));
	}

	public void clear() {
		if (anchors.size() == 0) {
			return; // no change
		}
		anchors.clear();
		anchorsByGuid.clear();
		anchorHistory.clear();
		clearedSignal.emit();
		skeletonChangedSignal.emit();
	}
	
	public boolean connect(Anchor anchor1, Anchor anchor2) {
		if (! anchors.contains(anchor1))
			return false;
		if (! anchors.contains(anchor2))
			return false;
		if (! anchor1.addNeighbor(anchor2))
			return false;
		skeletonChangedSignal.emit();
		return true;
	}

    public void deleteLinkRequest(Anchor anchor) {
        for (AnchorListener l: anchorListeners) {
            l.deleteLinkRequested(anchor);
        }
    }

    public void deleteSubtreeRequest(Anchor anchor){
        for (AnchorListener l: anchorListeners) {
            l.deleteSubtreeRequested(anchor);
        }
    }

    public void splitAnchorRequest(Anchor anchor) {
        for (AnchorListener l: anchorListeners) {
            l.splitAnchorRequested(anchor);
        }
    }

    public void rerootNeuriteRequest(Anchor anchor) {
        for (AnchorListener l: anchorListeners) {
            l.rerootNeuriteRequested(anchor);
        }
    }

    public void addEditNoteRequest(Anchor anchor) {
        for (AnchorListener l: anchorListeners) {
            l.addEditNoteRequested(anchor);
        }      
    }

    public void splitNeuriteRequest(Anchor anchor) {
        for (AnchorListener l: anchorListeners) {
            l.splitNeuriteRequested(anchor);
        }
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
		anchorHistory.remove(anchor);
		//
		anchor.anchorMovedSignal.disconnect(this.anchorMovedSignal);
		anchor.anchorMovedSilentSignal.disconnect(this.anchorMovedSilentSignal);
		anchorDeletedSignal.emit(anchor);
		return true;
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

        anchorNeighborsUpdatedSignal.emit(anchor);

    }

	public Set<Anchor> getAnchors() {
		return anchors;
	}
	
	public HistoryStack<Anchor> getHistory() {
		return anchorHistory;
	}

    /**
     * request trace path to parent
     */
    public void traceAnchorConnection(Anchor anchor) {
        if (anchor == null)
            return;
        if (anchor.getNeighbors().size() < 1)
            // no parent
            return;

        pathTraceRequestedSignal.emit(anchor.getGuid());
    }

	public void addTracedSegment(AnchoredVoxelPath path)
	{
	    SegmentIndex ix = path.getSegmentIndex();
		tracedSegments.put(ix, path);
		// log.info("tracedSegments.size() [300] = "+tracedSegments.size());
		skeletonChangedSignal.emit();
	}

    public void addTracedSegments(List<AnchoredVoxelPath> pathList) {
        for (AnchoredVoxelPath path: pathList) {
            SegmentIndex ix = path.getSegmentIndex();
            tracedSegments.put(ix, path);
        }
        skeletonChangedSignal.emit();
    }

    public void removeTracedSegment(AnchoredVoxelPath path) {
        SegmentIndex ix = path.getSegmentIndex();
        tracedSegments.remove(ix);
        skeletonChangedSignal.emit();
    }

	public Collection<AnchoredVoxelPath> getTracedSegments() {
		// log.info("tracedSegments.size() [305] = "+tracedSegments.size());
		Collection<AnchoredVoxelPath> result = tracedSegments.values();
		// log.info("tracedSegments.values().size() [307] = "+result.size());
		return result;
	}

}
