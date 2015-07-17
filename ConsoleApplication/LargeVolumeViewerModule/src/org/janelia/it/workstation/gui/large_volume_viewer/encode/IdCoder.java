/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.encode; 

/**
 * A "Co/Dec" for identifiers to be pushed over to GPU.
 * 
 * @author fosterl
 */
public class IdCoder {
    
    public static final float ENCODE_RANGE = 1000000f;
    
    private int idRange;
    private float idBreadth;
    public IdCoder(int idRange) {
        this.idRange = idRange;
        this.idBreadth = ENCODE_RANGE / idRange;
    }
    
    public float encode(int i) {
        return (idBreadth * (i + 1)) / ENCODE_RANGE;
    }
    
    public int decode(float colorVal) {
        return (int)((colorVal * ENCODE_RANGE) / idBreadth) - 1;
    }
}
