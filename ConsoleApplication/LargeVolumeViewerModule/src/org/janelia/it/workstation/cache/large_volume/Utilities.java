package org.janelia.it.workstation.cache.large_volume;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various utils regarding caching.
 *
 * @author fosterl
 */
public class Utilities {
    private static final Logger log = LoggerFactory.getLogger(Utilities.class);
    public static void zeroScan(byte[] rtnVal, String id, String label) {
        if (rtnVal == null) {
            log.info("{} [{}] null storage to check.", label, id);
            return;
        }
        boolean foundNonZero = false;
        for (int i = 0; i < rtnVal.length; i++) {
            if (rtnVal[i] != 0) {
                foundNonZero = true;
                log.info("{} [{}] contains at least one non-zero byte.", label, id);
                break;
            }
        }
        if (!foundNonZero) {
            log.error("{} [{}] contains all zeros.", label, id);
        }
    }
}
