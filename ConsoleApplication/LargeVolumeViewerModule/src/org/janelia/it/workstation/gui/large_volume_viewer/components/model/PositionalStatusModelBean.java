/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.components.model;

import java.util.Collection;
import java.util.ArrayList;

/**
 * This is a simple, bean-oriented version of the model.
 *
 * @author fosterl
 */
public class PositionalStatusModelBean implements PositionalStatusModel {

    private double[] position;
    private int[] tileXyz;
    private Status status = Status.Unfilled;
    
    private Collection<PositionalStatusListener> listeners = new ArrayList<>();

    public PositionalStatusModelBean( double[] position ) {
        this.position = position;
    }
    
    public void setPosition( double[] position ) {
        this.position = position;
        fireUpdate();
    }
    
    @Override
    public double[] getPosition() {
        return position;
    }
    
    public void setStatus(Status status) {
        this.status = status;
        fireUpdate();
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setTileXyz(int[] tileXyz) {
        this.tileXyz = tileXyz;
    }
    
    @Override
    public int[] getTileXyz() {
        return tileXyz;
    }

    @Override
    public void addListener(PositionalStatusListener listener) {
        listeners.add( listener );
    }

    @Override
    public void removeListener(PositionalStatusListener listener) {
        listeners.remove(listener);
    }
    
    private void fireUpdate() {
        for (PositionalStatusListener listener: listeners) {
            listener.update(this);
        }
    }

}
