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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeuronManager helps synchronized neuron models between Horta and Large Volume Viewer
 * @author Christopher Bruns
 */
public class NeuronManager implements LookupListener
{
    // Use Lookup to access neuron models from LVV
    // Based on tutorial at https://platform.netbeans.org/tutorials/74/nbm-selection-1.html
    private Lookup.Result<NeuronSet> neuronsLookupResult = null;
    private final Set<NeuronSet> currentNeuronLists = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public NeuronManager() {
        // TODO - link to workspace...
    }
    
    // When Horta TopComponent opens
    public void onOpened() {
        neuronsLookupResult = Utilities.actionsGlobalContext().lookupResult(NeuronSet.class);
        neuronsLookupResult.addLookupListener(this);
        checkNeuronLookup();
    }
    
    // When Horta TopComponent closes
    public void onClosed() {
        neuronsLookupResult.removeLookupListener(this);
    }
    
    // When the contents of the NeuronSet Lookup changes
    @Override
    public void resultChanged(LookupEvent le)
    {
        checkNeuronLookup();
    }
    
    // Respond to changes in NeuronSet Lookup
    private void checkNeuronLookup() {
        Collection<? extends NeuronSet> allNeuronLists = neuronsLookupResult.allInstances();
        if (! allNeuronLists.isEmpty()) {
            logger.info("Neuron Lookup found!");
            for (NeuronSet neuronList : allNeuronLists) {
                if (! currentNeuronLists.contains(neuronList)) {
                    logger.info("Found new neuron list!");
                    currentNeuronLists.add(neuronList);
                    // TODO - process the new neuron list
                }
            }
        }
        else {
            logger.info("Hey! There are no lists of neurons around.");
            // TODO - repond to lack of neuron collections.
        }
    }

}
