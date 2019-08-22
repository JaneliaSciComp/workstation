/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.gui.large_volume_viewer;

/**
 * This delineates some 2D region in micrometers.  Convenience holder
 * object for working with 2D pieces, when the third axis is handled separately.
 * LLF
 * 
 * @author fosterl
 */
class ViewBoundingBox {

    private double wFMin;
    private double wFMax;
    private double hFMin;
    private double hFMax;

    /**
     * @return the wFMin
     */
    double getwFMin() {
        return wFMin;
    }

    /**
     * @param wFMin the wFMin to set
     */
    void setwFMin(double wFMin) {
        this.wFMin = wFMin;
    }

    /**
     * @return the wFMax
     */
    double getwFMax() {
        return wFMax;
    }

    /**
     * @param wFMax the wFMax to set
     */
    void setwFMax(double wFMax) {
        this.wFMax = wFMax;
    }

    /**
     * @return the hFMin
     */
    double gethFMin() {
        return hFMin;
    }

    /**
     * @param hFMin the hFMin to set
     */
    void sethFMin(double hFMin) {
        this.hFMin = hFMin;
    }

    /**
     * @return the hFMax
     */
    double gethFMax() {
        return hFMax;
    }

    /**
     * @param hFMax the hFMax to set
     */
    void sethFMax(double hFMax) {
        this.hFMax = hFMax;
    }
    
    public String toString() {
        return String.format(
                "ScreenBoundingBox[ wFMin=%s, wFMax=%s, hFMin=%s, hFMax=%s ]",
                wFMin, wFMax, hFMin, hFMax 
        );
    }
}
