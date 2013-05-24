package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

public class Anchor {
	public enum Type {
		UNDEFINED,
		SOMA,
		AXON,
		DENDRITE,
		APICAL_DENDRITE,
		FORK_POINT,
		END_POINT,
		CUSTOM
	};
	
	private Vec3 location;
	private Type anchorType = Type.UNDEFINED;
	private double radius = 1.0;
	// No explicit edge objects, just symmetric neighbor references
	private Set<Anchor> neighbors = new HashSet<Anchor>();
	
	public Anchor(Vec3 location) {
		this.location = location;
	}
	
	public boolean addNeighbor(Anchor neighbor) {
		if (neighbor == null)
			return false;
		if (neighbors.contains(neighbor))
			return false;
		neighbors.add(neighbor);
		neighbor.addNeighbor(this); // ensure reciprocity
		return true;
	}

	public Vec3 getLocation() {
		return location;
	}

	public Type getAnchorType() {
		return anchorType;
	}

	public double getRadius() {
		return radius;
	}

	public Set<Anchor> getNeighbors() {
		return neighbors;
	}

	public void setLocation(Vec3 location) {
		this.location = location;
	}

	public void setAnchorType(Type anchorType) {
		this.anchorType = anchorType;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

}
