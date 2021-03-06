package org.janelia.workstation.gui.viewer3d.picking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A "Co/Dec" for identifiers to be pushed over to GPU.
 * 
 * @author fosterl
 */
public class IdCoder {
    public static final float HIBYTE_ENCODE_RANGE = 256.0f*256.0f*256.0f;
    public static final float MIDBYTE_ENCODE_RANGE = 256.0f*256.0f;
    public static final float BYTE_ENCODE_RANGE = 256.0f;
    public static final float ENCODE_RANGE = HIBYTE_ENCODE_RANGE + MIDBYTE_ENCODE_RANGE + BYTE_ENCODE_RANGE;
    public static final float RAW_RANGE_DIVISOR = ENCODE_RANGE - 0.5f;
    private Logger log = LoggerFactory.getLogger(IdCoder.class);
    
    private final static int ID_BREADTH = 4;  // Employ spacing to avoid clash.
    public IdCoder() {
        log.debug("ID Breadth={}.", ID_BREADTH);
    }
    
    public float[] encode(int id) {
        id += 1; // No zero identifiers!
        id *= ID_BREADTH;   // space out the identifiers, to lower prob of clash
        int lobyte = id & 255;
        int midbyte = (id >> 8) & 255;
        int hibyte = (id >> 16) & 255;

        return new float[]{lobyte / BYTE_ENCODE_RANGE, midbyte / BYTE_ENCODE_RANGE, hibyte / BYTE_ENCODE_RANGE};
    }
    
    public int decode(float colorVal) {
        return (int)(Math.round((colorVal * ENCODE_RANGE) / ID_BREADTH)) - 1;
    }
}
