package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.HistoryStack;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;

public class Skeleton {
	
	/**
	 * AnchorSeed holds enough data to nucleate a new Anchor.
	 * So I can send multiple values in a single argument for Signal1/Slot1.
	 * @author brunsc
	 *
	 */
	public class AnchorSeed {
		private Vec3 location;
		private Anchor parent;

		public AnchorSeed(Vec3 location, Anchor parent) {
			this.location = location;
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
	private Map<Long, Anchor> anchorsByGuid = new HashMap<Long, Anchor>();
	// TODO - anchor browsing history should maybe move farther back
	private HistoryStack<Anchor> anchorHistory = new HistoryStack<Anchor>();

	public Signal skeletonChangedSignal = new Signal();

	// API for synchronizing with back end database
	// after discussion with Don Olbris July 8, 2013
	// 
	public Signal1<AnchorSeed> addAnchorRequestedSignal = 
			new Signal1<AnchorSeed>();
	// Response from database
	public Slot1<TmGeoAnnotation> addAnchorSlot = new Slot1<TmGeoAnnotation>() {
		@Override
		public void execute(TmGeoAnnotation tga) {
			Vec3 location = new Vec3(tga.getX(), tga.getY(), tga.getZ());
			Anchor parentAnchor = anchorsByGuid.get(tga.getParentId());
			Anchor anchor = new Anchor(location, parentAnchor);
			anchor.setGuid(tga.getId());
			addAnchor(anchor);
		}
	};
	// Direct connection in case there is no database. Connect to addAnchorRequestedSignal in this case.
	// TODO - don't use this when workstation is functioning.
	public Slot1<AnchorSeed> addShortCircuitAnchorSlot = new Slot1<AnchorSeed>() {
		@Override
		public void execute(AnchorSeed seed) {
			addAnchor(new Anchor(seed.location, seed.parent));
		}
	};
	// AFTER anchor has already been added (not simply requested)
	public Signal1<Anchor> anchorAddedSignal = new Signal1<Anchor>();
	
	public Slot clearSlot = new Slot() {
		@Override
		public void execute() {
			clear();
		}
	};
	public Signal clearedSignal = new Signal();
	
	
	public Skeleton() {
		// TODO - don't make this connection when using workstation database
		// addAnchorRequestedSignal.connect(addShortCircuitAnchorSlot);
		//
		// Adding an anchor changes the skeleton
		anchorAddedSignal.connect(skeletonChangedSignal);
	}
	
	public Anchor addAnchor(Anchor anchor) {
		if (anchors.contains(anchor))
			return anchor;
		anchors.add(anchor);
		Long guid = anchor.getGuid();
		if (guid != null)
			anchorsByGuid.put(guid, anchor);
		anchor.anchorChangedSignal.connect(skeletonChangedSignal);
		anchorHistory.push(anchor);
		anchorAddedSignal.emit(anchor);
		return anchor;
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
		skeletonChangedSignal.emit();
		return true;
	}

	public Set<Anchor> getAnchors() {
		return anchors;
	}
	
	public HistoryStack<Anchor> getHistory() {
		return anchorHistory;
	}

}
