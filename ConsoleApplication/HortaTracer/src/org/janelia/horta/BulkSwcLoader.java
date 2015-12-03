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

package org.janelia.horta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.horta.nodes.BasicNeuronModel;
import org.openide.util.Exceptions;

/**
 * BulkSwcLoader is used to bulk load SWC neuron models in separate threads.
 * @author Christopher Bruns
 * TODO: create workspace if none
 * TODO: populate that workspace
 */
public class BulkSwcLoader
{
    private final ExecutorService pool = Executors.newFixedThreadPool(10);
    private AtomicInteger totalQueued = new AtomicInteger();
    private AtomicInteger totalLoaded = new AtomicInteger();
    private final NeuronSet neuronSet;
    
    public BulkSwcLoader(NeuronSet neuronSet) {
        this.neuronSet = neuronSet;
    }
    
    // Load a named stream, such as from a tar file
    public void addSwcStream(final InputStream is, final String fileName) {
        if (pool.isShutdown()) return;
        Runnable loadJob = new Runnable() {
            @Override
            public void run()
            {
                try {
                    addNeuron(new BasicNeuronModel(is, fileName));
                    is.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        totalQueued.incrementAndGet();
        pool.submit(loadJob);
    }

    // Load a file from disk
    public void addSwcFile(final File swcFile) {
        if (pool.isShutdown()) return;
        Runnable loadJob = new Runnable() {
            @Override
            public void run()
            {
                try {
                    addNeuron(new BasicNeuronModel(swcFile));
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        totalQueued.incrementAndGet();
        pool.submit(loadJob);
    }
    
    private void addNeuron(NeuronModel neuron) {
        synchronized(neuronSet) {
            neuronSet.add(neuron);
        }
        totalLoaded.incrementAndGet();        
    }
    
    public void shutdown(final Runnable onCompletion) {
        pool.shutdown();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    pool.awaitTermination(120, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                onCompletion.run();
            }
        });
        thread.start();
    }
    
}
