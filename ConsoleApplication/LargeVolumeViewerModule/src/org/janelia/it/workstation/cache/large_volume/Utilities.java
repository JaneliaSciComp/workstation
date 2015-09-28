package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various utils regarding caching.
 * 
 * @author fosterl
 */
public class Utilities {
    private static final Logger log = LoggerFactory.getLogger(Utilities.class);
    /**
     * See if this buffer is completely filled with zeros.
     * 
     * @return T=non-zeros found.  False otherwise.
     * @param buffer scan this
     * @param id name for scrutiny
     * @param label where the scan is called from
     */
    public static boolean zeroScan(byte[] buffer, String id, String label) {
        if (buffer == null) {
            log.debug("{} [{}] null storage to check.", label, id);
            return false;
        }
        boolean foundNonZero = false;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != 0) {
                foundNonZero = true;
                log.debug("{} [{}] contains at least one non-zero byte.", label, id);
                break;
            }
        }
        if (!foundNonZero) {
            log.error("{} [{}] contains all zeros.", label, id);
        }
        return foundNonZero;
    }

    public static String trimToOctreePath(File id) {
        return trimToOctreePath(id.getAbsolutePath());
    }
    
    public static String trimToOctreePath(String id) {
        int endPoint = id.lastIndexOf("/");
        int startPoint = 0;
        boolean foundAlpha = false;
        while (!foundAlpha) {
            endPoint--;
            if (Character.isAlphabetic(id.charAt(endPoint))) {
                foundAlpha = true;
                startPoint = endPoint;
                startPoint = id.indexOf("/", startPoint);
            }
        }
        return id.substring(startPoint);
    }
}
