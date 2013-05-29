package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;

public class Skeleton {
	private Set<Anchor> anchors = new LinkedHashSet<Anchor>();
	private Anchor nextParent = null;

	public Signal skeletonChangedSignal = new Signal();
	
	public void addAnchor(Anchor anchor) {
		if (anchors.contains(anchor))
			return;
		anchors.add(anchor);
		if (nextParent != null) {
			nextParent.addNeighbor(anchor);
		}
		nextParent = anchor;
		anchor.anchorChangedSignal.connect(skeletonChangedSignal);
		skeletonChangedSignal.emit();
	}

	public Anchor getNextParent() {
		return nextParent;
	}

	public Set<Anchor> getAnchors() {
		return anchors;
	}

	public boolean setNextParent(Anchor nextParent) {
		if (nextParent == null) {
			this.nextParent = null; // Will start a new tree next time
			return true;
		}
		if (! anchors.contains(nextParent))
			return false; // exception?
		this.nextParent = nextParent;
		return true;
	}

	public void addAnchorAtXyz(Vec3 xyz) {
		addAnchor(new Anchor(xyz));
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
		skeletonChangedSignal.emit();
		return true;
	}
}
