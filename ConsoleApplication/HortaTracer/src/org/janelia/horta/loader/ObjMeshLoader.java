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
import javax.swing.SwingUtilities;
import org.apache.commons.io.FilenameUtils;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.WavefrontObjLoader;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.DiffuseMaterial;
import org.janelia.gltools.material.IBLDiffuseMaterial;
import org.janelia.gltools.material.TransparentEnvelope;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.util.Exceptions;

/**
 *
 * @author brunsc
 */
public class ObjMeshLoader implements FileTypeLoader
{
    private final NeuronTracerTopComponent horta;

    public ObjMeshLoader(NeuronTracerTopComponent horta) {
        this.horta = horta;
    }

    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("OBJ"))
            return true;
        return false;
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException 
    {
        Runnable meshLoadTask = new Runnable() {
            @Override
            public void run() {
                MeshGeometry meshGeometry;
                try {
                    meshGeometry = WavefrontObjLoader.load(source.getInputStream());
                    final GL3Actor meshActor = new MeshActor(
                            meshGeometry,
                            new TransparentEnvelope(),
                            null
                    );
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            horta.addMeshActor(meshActor);
                        }
                    });
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        
        new Thread(meshLoadTask).start();
        
        return true;
    }
    
}
