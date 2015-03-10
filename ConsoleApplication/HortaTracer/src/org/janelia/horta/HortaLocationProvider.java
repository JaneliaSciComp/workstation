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

import java.net.URL;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can advertise to acceptors, where Horta is "looking".
 *
 * @author fosterl
 */
@ServiceProvider(service = Tiled3dSampleLocationProviderAcceptor.class, path=Tiled3dSampleLocationProviderAcceptor.LOOKUP_PATH)
public class HortaLocationProvider implements Tiled3dSampleLocationProviderAcceptor {
    public static final String UNIQUE_NAME = "NeuronTracer";
    public static final String DESCRIPTION = "Neuron Tracer / Horta";
    
    private final Logger logger = LoggerFactory.getLogger(HortaLocationProvider.class);
    
    @Override
    public URL getSampleUrl() {
        URL rtnVal = null;
        NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc != null) {
            try {
                rtnVal = nttc.getCurrentSourceURL();
            } catch (Exception ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
            }            
        }
        else {
            logger.info("No neuron tracer component found.");
        }
        return rtnVal;
    }

    @Override
    public double[] getCoords() {
        double[] rtnVal = null;
        NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc != null) {
            rtnVal = nttc.getStageLocation();
        }
        else {
            logger.info("No neuron tracer component found.");
        }
        return rtnVal;
    }

    @Override
    public String getProviderUniqueName() {
        return UNIQUE_NAME;
    }

    @Override
    public String getProviderDescription() {
        return DESCRIPTION;
    }
    
    private NeuronTracerTopComponent getNeuronTracer() {
        return NeuronTracerTopComponent.findThisComponent();
    }

    @Override
    public ParticipantType getParticipantType() {
        return ParticipantType.both;
    }

    @Override
    public void setSampleLocation(URL sampleUrl, double[] coords) {
        NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc == null) {
            throw new IllegalStateException("Failed to find Neuron Tracer.");
        }
        if (! nttc.isOpened()) {
            nttc.open();
        }
        if (nttc.isOpened()) {
            nttc.requestActive();
            try {
                nttc.setLocation(sampleUrl, coords);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
            }
        }
        else {
            throw new IllegalStateException("Failed to open Neuron Tracer.");
        }
    }
}
