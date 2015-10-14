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

package org.janelia.horta.nodes;

import java.awt.Color;
import java.util.Observer;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.geometry3d.Vantage;
import org.janelia.horta.modelapi.HortaWorkspace;
import org.janelia.console.viewerapi.model.ReconstructionCollection;

/**
 * TODO: - in future, set Observable interface on workspace subcomponents
 * @author Christopher Bruns
 */
public class BasicHortaWorkspace implements HortaWorkspace
{
    private final ReconstructionCollection neurons = new BasicReconstructionCollection();
    private final Vantage vantage;
    private final ComposableObservable changeObservable = new ComposableObservable();
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.1f, 1f);

    public BasicHortaWorkspace(Vantage vantage) {
        this.vantage = vantage;
    }
    
    @Override
    public ReconstructionCollection getNeurons()
    {
        return neurons;
    }

    @Override
    public Vantage getVantage()
    {
        return vantage;
    }

    @Override
    public void setChanged()
    {
        changeObservable.setChanged();
    }

    @Override
    public void notifyObservers()
    {
        changeObservable.notifyObservers();
    }

    @Override
    public void addObserver(Observer observer)
    {
        changeObservable.addObserver(observer);
    }

    @Override
    public void deleteObserver(Observer observer)
    {
        changeObservable.deleteObserver(observer);
    }

    @Override
    public void deleteObservers()
    {
        changeObservable.deleteObservers();
    }

    @Override
    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(Color color)
    {
        if (backgroundColor.equals(color)) return;
        backgroundColor = color;
        setChanged();
    }
}
