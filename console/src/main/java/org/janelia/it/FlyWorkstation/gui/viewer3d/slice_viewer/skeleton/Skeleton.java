package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;

public class Skeleton {
	private Set<Anchor> anchors = new LinkedHashSet<Anchor>();

	public Signal skeletonChangedSignal = new Signal();
	
	public Anchor addAnchor(Anchor anchor) {
		if (anchors.contains(anchor))
			return anchor;
		anchors.add(anchor);
		anchor.anchorChangedSignal.connect(skeletonChangedSignal);
		skeletonChangedSignal.emit();
		return anchor;
	}

	public Set<Anchor> getAnchors() {
		return anchors;
	}

	public Anchor addAnchorAtXyz(Vec3 xyz, Anchor parent) {
		return addAnchor(new Anchor(xyz, parent));
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
