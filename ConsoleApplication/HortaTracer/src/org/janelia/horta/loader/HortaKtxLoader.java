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
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;
import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.blocks.KtxBlockLoadRunner;
import org.janelia.horta.render.NeuronMPRenderer;

/**
 *
 * @author Christopher Bruns
 */
public class HortaKtxLoader implements FileTypeLoader {

    private final NeuronMPRenderer renderer;

    public HortaKtxLoader(NeuronMPRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public boolean supports(DataSource source) {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("KTX")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException {
        InputStream stream = source.getInputStream();

        final KtxBlockLoadRunner loader = new KtxBlockLoadRunner(stream);
        loader.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (loader.state != KtxBlockLoadRunner.State.LOADED) {
                    return;
                }
                TetVolumeActor parentActor = TetVolumeActor.getInstance();
                parentActor.addChild(loader.blockActor);
                if (!renderer.containsVolumeActor(parentActor)) { // just add singleton actor once...
                    parentActor.setBrightnessModel(renderer.getBrightnessModel());
                    renderer.addVolumeActor(parentActor);
                }
            }
        });
        loader.run();
        return loader.state == KtxBlockLoadRunner.State.LOADED;
    }

}
