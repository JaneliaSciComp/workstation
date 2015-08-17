/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Comparator of files, based on another, internally-stored mapping of same
 * files, to their focus proximity as distance, in microns.
 * @author fosterl
 */
public class FocusProximityComparator implements Comparator<File> {

    private final Map<File,Double> proximityMap = new HashMap<>();
    
    public FocusProximityComparator() {        
    }
    
    public void addFile(File file, Double proximity) {
        proximityMap.put(file, proximity);
    }
    
    /**
     * This will provide a sort-order that favors proximity towards start of
     * list.
     * 
     * @param o1 a file from the known map.
     * @param o2 a 2nd file from known map.
     * @return proximity-based comparison.
     */
    @Override
    public int compare(File o1, File o2) {
        Double prox1 = proximityMap.get(o1);
        Double prox2 = proximityMap.get(o2);
        if (prox1 == null  ||  prox2 == null) {
            throw new IllegalArgumentException("Use this comparator only on files whose proximities have been added.");
        }
        return prox1 == prox2 ? 0 : prox1 < prox2 ? -1 : 1;
    }

}
