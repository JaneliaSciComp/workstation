/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.components.model;

/**
 * Implement this to make data for the position/status panel.
 *
 * @author fosterl
 */
public interface PositionalStatusModel {
    public enum Status {
        Unfilled, InProgress, Filled, OutOfRange
    }
    
    double[] getPosition();
    int[] getTileXyz();
    Status getStatus();
    void addListener(PositionalStatusListener listener);
    void removeListener(PositionalStatusListener listener);
}
