package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.util.HashSet;
import java.util.Set;

public class Skeleton {
	private Set<Anchor> anchors = new HashSet<Anchor>();
	private Anchor nextParent = null;
	
	public void addAnchor(Anchor anchor) {
		if (anchors.contains(anchor))
			return;
		anchors.add(anchor);
		if (nextParent != null) {
			nextParent.addNeighbor(anchor);
		}
	}

	public Anchor getNextParent() {
		return nextParent;
	}

	public boolean setNextParent(Anchor nextParent) {
		if (nextParent == null)
			return false;
		if (! anchors.contains(nextParent))
			return false; // exception?
		this.nextParent = nextParent;
		return true;
	}
}
