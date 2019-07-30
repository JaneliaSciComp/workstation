package org.janelia.workstation.core.util;

/**
 * Utility functions for color depth search.
 *
 * TODO: extract this from the Core module
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthUtils {

    /**
     * Which alignment spaces should be shown to users for color depth search?
     * @param alignmentSpace
     * @return
     */
    public static boolean isAlignmentSpaceVisible(String alignmentSpace) {
        return ("JFRC2010_20x".equals(alignmentSpace) // legacy brain alignments
                || "FemaleVNCSymmetric2017_20x".equals(alignmentSpace) // legacy VNC alignments
                || alignmentSpace.startsWith("JRC2018_VNC") // VNC alignments
                || alignmentSpace.endsWith("20x_HR")); // Brain alignments
    }
}
