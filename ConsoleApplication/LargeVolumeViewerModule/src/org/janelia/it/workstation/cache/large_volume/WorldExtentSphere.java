/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.util.Set;

public class WorldExtentSphere implements GeometricNeighborhood {

    private Set<File> files;
    
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
}
