/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer;

/**
 * This delineates some 3D region in screen coordinates.
 * 
 * @author fosterl
 */
public class ScreenBoundingBox {

    private double wFMin;
    private double wFMax;
    private double hFMin;
    private double hFMax;

    /**
     * @return the wFMin
     */
    public double getwFMin() {
        return wFMin;
    }

    /**
     * @param wFMin the wFMin to set
     */
    public void setwFMin(double wFMin) {
        this.wFMin = wFMin;
    }

    /**
     * @return the wFMax
     */
    public double getwFMax() {
        return wFMax;
    }

    /**
     * @param wFMax the wFMax to set
     */
    public void setwFMax(double wFMax) {
        this.wFMax = wFMax;
    }

    /**
     * @return the hFMin
     */
    public double gethFMin() {
        return hFMin;
    }

    /**
     * @param hFMin the hFMin to set
     */
    public void sethFMin(double hFMin) {
        this.hFMin = hFMin;
    }

    /**
     * @return the hFMax
     */
    public double gethFMax() {
        return hFMax;
    }

    /**
     * @param hFMax the hFMax to set
     */
    public void sethFMax(double hFMax) {
        this.hFMax = hFMax;
    }
}
