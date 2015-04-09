/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer;

/**
 * These are minima/maxima to frame up a designated 3d region in terms of
 * tile index units.
 * 
 * @author fosterl
 */
public class TileBoundingBox {

    private int wMin;
    private int wMax;
    private int hMin;
    private int hMax;

    /**
     * @return the wMin
     */
    public int getwMin() {
        return wMin;
    }

    /**
     * @param wMin the wMin to set
     */
    public void setwMin(int wMin) {
        this.wMin = wMin;
    }

    /**
     * @return the wMax
     */
    public int getwMax() {
        return wMax;
    }

    /**
     * @param wMax the wMax to set
     */
    public void setwMax(int wMax) {
        this.wMax = wMax;
    }

    /**
     * @return the hMin
     */
    public int gethMin() {
        return hMin;
    }

    /**
     * @param hMin the hMin to set
     */
    public void sethMin(int hMin) {
        this.hMin = hMin;
    }

    /**
     * @return the hMax
     */
    public int gethMax() {
        return hMax;
    }

    /**
     * @param hMax the hMax to set
     */
    public void sethMax(int hMax) {
        this.hMax = hMax;
    }
}
