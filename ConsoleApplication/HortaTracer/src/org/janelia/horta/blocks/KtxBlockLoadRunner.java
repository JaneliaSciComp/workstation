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

package org.janelia.horta.blocks;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.actors.TetVolumeMeshActor;
import org.janelia.horta.ktx.KtxData;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
public class KtxBlockLoadRunner 
extends ComposableObservable
implements Runnable
{
    public enum State {
        INITIAL,
        LOADING,
        INTERRUPTED,
        LOADED,
        FAILED,
    }
    
    private URL url;
    private InputStream inputStream;
    
    public State state = State.INITIAL;
    public TetVolumeMeshActor blockActor;
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public KtxBlockLoadRunner(URL url) {
        this.url = url;
    }
    
    public KtxBlockLoadRunner(InputStream stream) {
        this.inputStream = stream;
    }
    
    @Override
    public void run() 
    {
        if (inputStream != null)
            loadStream(inputStream);
        else
            loadUrl();
    }
    
    private void loadStream(InputStream stream) {
        long start = System.nanoTime();
        state = State.LOADING;
        KtxData ktxData = new KtxData();
        // Thank you Java 7 for try-with-resources
        try
        {
            ktxData.loadStreamInterruptably(stream);
        } catch (IOException ex) {
            state = State.FAILED;
            Exceptions.printStackTrace(ex);
            return;
        } catch (InterruptedException ex) {
            state = State.INTERRUPTED;
            return;
        }
        TetVolumeActor parentActor = TetVolumeActor.getInstance();
        blockActor = new TetVolumeMeshActor(ktxData, parentActor);
        state = State.LOADED;
        setChanged();
        long end = System.nanoTime();
        double elapsed = (end - start)/1.0e9;
        logger.info(String.format("Ktx tile load took %.3f seconds", elapsed));
        // notify listeners
        notifyObservers();
    }
    
    private void loadUrl() {
        // Thank you Java 7 for try-with-resources
        try (InputStream stream = new BufferedInputStream(url.openStream())) 
        {
            loadStream(stream);
        } catch (IOException ex) {
            state = State.FAILED;
            Exceptions.printStackTrace(ex);
        }
    }
    
}
