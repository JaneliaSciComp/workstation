/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.gui.large_volume_viewer;

/**
 * These are minima/maxima to frame up a designated 3d region in terms of
 * tile index units.
 * 
 * @author fosterl
 */
class TileBoundingBox {

    private int wMin;
    private int wMax;
    private int hMin;
    private int hMax;

    /**
     * @return the wMin
     */
    int getwMin() {
        return wMin;
    }

    /**
     * @param wMin the wMin to set
     */
    void setwMin(int wMin) {
        this.wMin = wMin;
    }

    /**
     * @return the wMax
     */
    int getwMax() {
        return wMax;
    }

    /**
     * @param wMax the wMax to set
     */
    void setwMax(int wMax) {
        this.wMax = wMax;
    }

    /**
     * @return the hMin
     */
    int gethMin() {
        return hMin;
    }

    /**
     * @param hMin the hMin to set
     */
    void sethMin(int hMin) {
        this.hMin = hMin;
    }

    /**
     * @return the hMax
     */
    int gethMax() {
        return hMax;
    }

    /**
     * @param hMax the hMax to set
     */
    void sethMax(int hMax) {
        this.hMax = hMax;
    }
}
