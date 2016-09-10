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

package org.janelia.horta.volume;

import java.io.IOException;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.VolumeMipMaterial;
import org.janelia.gltools.material.VolumeMipMaterial.VolumeState;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.BrainTileInfo;
import org.janelia.horta.actors.BrainTileMesh;

/**
 *
 * @author Christopher Bruns
 */
public class BrickActor extends MeshActor
{
    private final BrainTileInfo brainTile;
    private final BrickMaterial brickMaterial;
    
    public BrickActor(BrainTileInfo brainTile, 
            BrightnessModel brightnessModel, 
            VolumeState volumeState, 
            int colorChannel) throws IOException 
    {
        super(
                new BrainTileMesh(brainTile), 
                new BrickMaterial(brainTile, brightnessModel, volumeState, colorChannel),
                null);
        this.brainTile = brainTile;
        this.brickMaterial = (BrickMaterial)getMaterial();
    }

    // Constructor version that uses preloaded Texture3d
    public BrickActor(
            BrainTileInfo brainTile, 
            Texture3d texture3d, 
            BrightnessModel brightnessModel, 
            VolumeState volumeState) 
    {
        super(
                new BrainTileMesh(brainTile), 
                new BrickMaterial(brainTile, texture3d, brightnessModel, volumeState),
                null);
        this.brainTile = brainTile;
        this.brickMaterial = (BrickMaterial)getMaterial();    }
    
    public void setOpaqueDepthTexture(Texture2d depthTexture, float zNear, float zFar) {
        brickMaterial.setOpaqueDepthTexture(depthTexture, zNear, zFar);
    }

    public BrainTileInfo getBrainTile()
    {
        return brainTile;
    }

    public void setRelativeZNear(float zNear)
    {
        brickMaterial.setRelativeZNear(zNear);
    }

    public void setRelativeZFar(float zFar)
    {
        brickMaterial.setRelativeZFar(zFar);
    }

    private static class BrickMaterial extends VolumeMipMaterial {

        private BrickMaterial(
                BrainTileInfo brainTile, 
                BrightnessModel brightnessModel,
                VolumeState volumeState,
                int colorChannel) throws IOException
        {
            super(safeLoadBrick(brainTile, colorChannel), brightnessModel);
            setVolumeState(volumeState);
        }

        private static Texture3d safeLoadBrick(BrainTileInfo brainTile, int colorChannel) throws IOException {
            Texture3d brick = brainTile.loadBrick(10, colorChannel);
            if (brick==null) {
                throw new IOException("Load was interrupted");
            }
            return brick;
        }

        private BrickMaterial(
                BrainTileInfo brainTile, 
                Texture3d texture3d, 
                BrightnessModel brightnessModel, 
                VolumeState volumeState) 
        {
            super(texture3d, brightnessModel);
            setVolumeState(volumeState);
        }
        
    }
}
