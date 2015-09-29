/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;

public class WorldExtentSphere implements GeometricNeighborhood {

    private static final AtomicInteger _s_instanceCounter = new AtomicInteger(0);
    private Set<File> files;
    private Double zoom;
    private double[] focus;
    private int[] tileExtents;
    private Map<String, PositionalStatusModel> models;
    
    // Keep an id based on instances constructed.
    private int instanceId = _s_instanceCounter.addAndGet(1);
    
	/**
	 * @see GeometricNeighborhood#getFiles()
	 */
    @Override
	public Set<File> getFiles() {
		return files;
	}
    
    public void setFiles(Set<File> files) {
        this.files = files;
    }

    /** Need to support comparison to decide whether to populate or not. */
    @Override
    public boolean equals(Object o) {
        boolean isEq = false;
        if (o != null  &&  o instanceof GeometricNeighborhood) {
            GeometricNeighborhood otherHood = (GeometricNeighborhood)o;
            isEq = files.equals(otherHood.getFiles());
        }
        return isEq;
    }

    /** @see #equals(java.lang.Object) */
    @Override
    public int hashCode() {
        return files.hashCode();
    }

    @Override
    public Double getZoom() {
        return zoom;
    }

    @Override
    public double[] getFocus() {
        return focus;
    }
    
    public void setZoom(Double zoom) {
        this.zoom = zoom;
    }
    
    public void setFocus(double[] focus) {
        this.focus = focus;
    }

    @Override
    public int getId() {
        return instanceId;
    }

    public void setPositionalModels( Map<String, PositionalStatusModel> models ) {
        this.models = models;
    }
    
    @Override
    public Map<String, PositionalStatusModel> getPositionalModels() {
        return models;
    }
    
    public void setTileExtents(int[] minTiles, int[] maxTiles) {
        this.tileExtents = new int[ minTiles.length ];
        for (int i = 0; i < tileExtents.length; i++) {
            tileExtents[i] = maxTiles[i] - minTiles[i];
        }
    }
    
    @Override
    public int[] getTileExtents() {
        return tileExtents;
    }
}
