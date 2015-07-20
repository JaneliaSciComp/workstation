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
    private Logger log = LoggerFactory.getLogger(IdCoder.class);
    public static final float ENCODE_RANGE = 256f;
    
    private int idRange;
    private float idBreadth;
    public IdCoder(int idRange) {
        this.idRange = idRange;
        this.idBreadth = ENCODE_RANGE / idRange;
        log.info("ID Range={}.  ID Breadth={}.", idRange, idBreadth);
    }
    
    public float[] encode(int i) {
        //@todo
        //Temporary: need to do proper 3-byte encoding!!
        return new float[]{(idBreadth * (i + 1)) / ENCODE_RANGE, 0, 0};
    }
    
    public int decode(float colorVal) {
        return (int)((colorVal * ENCODE_RANGE) / idBreadth) - 1;
    }
}
