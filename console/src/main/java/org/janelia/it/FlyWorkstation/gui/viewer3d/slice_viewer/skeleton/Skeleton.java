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
		skeletonChangedSignal.emit();
	}

	public Anchor getNextParent() {
		return nextParent;
	}

	public Set<Anchor> getAnchors() {
		return anchors;
	}

	public boolean setNextParent(Anchor nextParent) {
		if (nextParent == null)
			return false;
		if (! anchors.contains(nextParent))
			return false; // exception?
		this.nextParent = nextParent;
		return true;
	}

	public void addAnchorAtXyz(Vec3 xyz) {
		addAnchor(new Anchor(xyz));
	}
}
