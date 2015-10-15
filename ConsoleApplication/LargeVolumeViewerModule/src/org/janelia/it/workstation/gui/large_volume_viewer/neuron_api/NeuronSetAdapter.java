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

package org.janelia.it.workstation.gui.large_volume_viewer.neuron_api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.janelia.console.viewerapi.model.BasicNeuronSet;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.GlobalAnnotationListener;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 Expose NeuronSet interface, using in-memory data resident in LVV
 */
public class NeuronSetAdapter
extends BasicNeuronSet
implements NeuronSet
{
    private Skeleton m_skeleton = null;
    private final Map<Long, NeuronModel> neuronMap = new HashMap<>();
    private final LargeVolumeViewViewer lvvv;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public NeuronSetAdapter(LargeVolumeViewViewer lvvv)
    {
        super("LVV Neurons", new HashSet<NeuronModel>());
        this.lvvv = lvvv;
        updateNeurons();
        // TODO actually expose neurons
    }
    
    // Update skeleton just-in-time, since it's null at startup
    private Skeleton getSkeleton() {
        if (m_skeleton != null)
            return m_skeleton; // skeleton was already initialized
        QuadViewUi qview = lvvv.getQuadViewUi();
        if (qview == null) 
            return null;
        m_skeleton = qview.getSkeleton();
        return m_skeleton;
    }
    
    private void updateNeurons() {
        Skeleton skeleton = getSkeleton();
        if (skeleton == null)
            return; // not ready yet
        for (Anchor anchor : skeleton.getAnchors()) {
            Long neuronId = anchor.getNeuronID();
            if (! neuronMap.containsKey(neuronId)) {
                NeuronModel neuron = new NeuronModelAdapter(neuronId);
                neuronMap.put(neuronId, neuron);
                neurons.add(neuron);
            }
            NeuronModel neuron = neuronMap.get(neuronId);
            neuron.getVertexes().add(new NeuronVertexAdapter(anchor));
        }
    }
    
    
    // TODO use this class
    private class AnnotationListener implements GlobalAnnotationListener {

        @Override
        public void workspaceLoaded(TmWorkspace workspace)
        {
            logger.info("Workspace loaded");
        }

        @Override
        public void neuronSelected(TmNeuron neuron)
        {}

        @Override
        public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style)
        {}
        
    }
}
