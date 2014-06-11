package org.janelia.it.workstation.gui.slice_viewer.skeleton;

import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.signal.Signal1;

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
	private Set<Anchor> neighbors = new LinkedHashSet<Anchor>();

    // the difference between these signals: one is triggered by
    //  user mouse actions, will trigger things happening due
    //  to the anchor's having been moved (eg, merges)
    // the "silent" version is triggered
    //  programmatically and won't cause anything other than
    //  the positioning and drawing of the anchor
	public Signal1<Anchor> anchorMovedSignal = new Signal1<Anchor>();
	public Signal1<Anchor> anchorMovedSilentSignal = new Signal1<Anchor>();

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

    /**
     * update the anchor location, but send the signal
     * on the "silent" channel that will not initiate
     * further actions; meant to be used when setting
     * an anchor location programmatically rather
     * than by the user
     */
	public void setLocationSilent(Vec3 location) {
		if (location.equals(this.location))
			return;
		this.location = location;
		anchorMovedSilentSignal.emit(this);
	}

	public void setAnchorType(Type anchorType) {
		this.anchorType = anchorType;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

}
