package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Signal1;

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
	
	private Long janeliaWorkstationGuid;
	private Vec3 location;
	private Type anchorType = Type.UNDEFINED;
	private double radius = 1.0;
	// No explicit edge objects, just symmetric neighbor references
	private Set<Anchor> neighbors = new HashSet<Anchor>();
	
	public Signal1<Anchor> anchorMovedSignal = new Signal1<Anchor>();
	
	public Anchor(Vec3 location, Anchor parent) {
		this.location = location;
		addNeighbor(parent);
	}
	
	public boolean addNeighbor(Anchor neighbor) {
		if (neighbor == null)
			return false;
		if (neighbor == this)
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

	public Long getGuid() {
		return this.janeliaWorkstationGuid;
	}
	
	public void setGuid(Long id) {
		this.janeliaWorkstationGuid = id;
	}

	public double getRadius() {
		return radius;
	}

	public Set<Anchor> getNeighbors() {
		return neighbors;
	}

	public void setLocation(Vec3 location) {
		if (location.equals(this.location))
			return;
		this.location = location;
		anchorMovedSignal.emit(this);
	}

	public void setAnchorType(Type anchorType) {
		this.anchorType = anchorType;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

}
