package org.janelia.it.workstation.gui.browser.nodes;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IdGenerator {
    
    private static long counter = 0;

    public static long getNextId() {
        return counter++;
    }
    
}
