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

package org.janelia.horta.loader;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FilenameUtils;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.horta.nodes.BasicNeuronModel;
import org.janelia.horta.render.NeuronMPRenderer;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class HortaSwcLoader implements FileTypeLoader
{
    // We sometimes want to quickly load so many neuron models,
    // So load them asynchronously in new threads.
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    
    private final NeuronSet neuronSet;
    private final NeuronMPRenderer renderer;
    
    public HortaSwcLoader(NeuronSet neuronSet, NeuronMPRenderer renderer) {
        this.neuronSet = neuronSet;
        this.renderer = renderer;
    }
    
    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("SWC"))
            return true;
        return false;
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException
    {
        pool.submit(new Runnable() {
            @Override
            public void run()
            {
                try {
                    NeuronModel neuron = new BasicNeuronModel(source.getInputStream(), source.getFileName(), neuronSet);
                    synchronized(neuronSet) {
                        if (neuronSet.add(neuron))
                            neuronSet.getMembershipChangeObservable().setChanged();
                    }
                    renderer.addNeuronActors(neuron);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        return true;
    }

    public void runAfterLoad(final Runnable onComplete)
    {
        final ExecutorService closedPool = pool;
        synchronized(this) {
            pool = Executors.newFixedThreadPool(10);
        }
        closedPool.shutdown();
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run()
            {
                try {
                    closedPool.awaitTermination(120, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                onComplete.run();
            }
            
        });
        thread.start();
    }
    
}
