/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.viewer3d.picking; 

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
    
    private int idRange;
    private int idBreadth;
    public IdCoder(int idRange) {
        this.idRange = idRange;
        this.idBreadth = 4;
        log.info("ID Range={}.  ID Breadth={}.", idRange, idBreadth);
    }
    
    public float[] encode(int id) {
        id += 1; // No zero identifiers!
        id *= idBreadth;   // space out the identifiers, to lower prob of clash
        int lobyte = id & 255;
        int midbyte = (id >> 8) & 255;
        int hibyte = (id >> 16) & 255;

        return new float[]{lobyte / BYTE_ENCODE_RANGE, midbyte / BYTE_ENCODE_RANGE, hibyte / BYTE_ENCODE_RANGE};
    }
    
    public int decode(float colorVal) {
        return (int)(Math.round((colorVal * ENCODE_RANGE) / idBreadth)) - 1;
    }
}
