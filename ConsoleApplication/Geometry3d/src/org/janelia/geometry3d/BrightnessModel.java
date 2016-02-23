/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.geometry3d;

import java.util.Observer;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.Copyable;
import org.janelia.console.viewerapi.ObservableInterface;

/**
 *
 * @author Christopher Bruns
 */
public class BrightnessModel 
implements Copyable<BrightnessModel>, ObservableInterface
{
    private float minimum = 0; // range 0-1
    private float maximum = 1; // range 0-1
    private final ComposableObservable changeObservable = new ComposableObservable();

    public BrightnessModel() {}

    public BrightnessModel(BrightnessModel rhs) {
        copy(rhs);
    }

    @Override
    public final void copy(BrightnessModel rhs) {
        setMinimum(rhs.minimum);
        setMaximum(rhs.maximum);        
    }
    
    public float getMaximum() {
        return maximum;
    }

    public float getMinimum() {
        return minimum;
    }

    public final void setMinimum(float minimum) {
        if (minimum == this.minimum)
            return;
        // System.out.println("Min changed!");
        changeObservable.setChanged();
        this.minimum = minimum;
    }

    public final void setMaximum(float maximum) {
        if (maximum == this.maximum)
            return;
        changeObservable.setChanged();
        this.maximum = maximum;
    }

    @Override
    public void setChanged() {
        changeObservable.setChanged();
    }

    @Override
    public void notifyObservers() {
        changeObservable.notifyObservers();
    }

    @Override
    public void addObserver(Observer observer) {
        changeObservable.addObserver(observer);
    }

    @Override
    public void deleteObserver(Observer observer) {
        changeObservable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers() {
        changeObservable.deleteObservers();
    }

    @Override
    public boolean hasChanged()
    {
        return changeObservable.hasChanged();
    }
    
}
