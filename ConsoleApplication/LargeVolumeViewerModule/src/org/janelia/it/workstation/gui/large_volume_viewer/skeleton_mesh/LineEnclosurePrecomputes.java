/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracking the cpu-intensive computed values used in line enclosure factory.
 * Also makes it possible to clean these up at appropriate junctures.
 *
 * @author fosterl
 */
public class LineEnclosurePrecomputes {
    // These caches should remain in effect for any time this class is in use.
    private static final Map<Integer, Matrix> aboutZToMatrix = new HashMap<>();
    private static final Map<String, Matrix> aboutXAboutYToMatrix = new HashMap<>();
    private static final Map<String, List<double[][]>> pointsToCaps = new HashMap<>();
    
    /**
     * Blow away anything that is specific to a workspace.
     */
    public static void clearWorkspaceRelevant() {
        // The end caps will very likely not overlap between multiple different
        // samples, because they are tied to locations.  Here, they may be cleared.
        pointsToCaps.clear();
    }

    public static Matrix getAboutZMatrix( Integer key ) {
        return aboutZToMatrix.get(key);
    }
    
    public static void putAboutZMatrix( Integer key, Matrix matrix ) {
        aboutZToMatrix.put( key, matrix );
    }
    
    public static Matrix getAboutXAboutYMatrix( String key ) {
        return aboutXAboutYToMatrix.get( key );
    }
    
    public static void putAboutXAboutYMatrix(String key, Matrix matrix) {
        aboutXAboutYToMatrix.put(key, matrix);
    }

    public static List<double[][]> getEndCaps( String key ) {
        return pointsToCaps.get( key );
    }
    
    public static void putEndCaps( String key, List<double[][]> endCaps ) {
        pointsToCaps.put( key, endCaps);
    }
}
