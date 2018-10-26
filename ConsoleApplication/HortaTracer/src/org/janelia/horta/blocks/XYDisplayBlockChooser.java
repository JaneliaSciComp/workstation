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

import java.util.ArrayList;
import java.util.List;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;

/**
 * generate a list of blocks in same xy plane as focus, but wider than usual;
 * this is for testing
 */
public class XYDisplayBlockChooser implements BlockChooser<KtxOctreeBlockTileKey, KtxOctreeBlockTileSource> {

    @Override
    public List<KtxOctreeBlockTileKey> chooseBlocks(KtxOctreeBlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus) {
        BlockTileResolution resolution = source.getMaximumResolution();

        ConstVector3 blockSize = source.getMaximumResolutionBlockSize();

        // this gives you a (2nx + 1) by (2ny + 1) square
        int nx = 3;
        int ny = 4;

        float[] xarray = new float[2 * nx + 1];
        float[] yarray = new float[2 * ny + 1];

        for (int i = 0; i < 2 * nx + 1; i++) {
            xarray[i] = (i - nx) * blockSize.getX();
        }
        for (int j = 0; j < 2 * ny + 1; j++) {
            yarray[j] = (j - ny) * blockSize.getY();
        }

        List<KtxOctreeBlockTileKey> result = new ArrayList<>();
        for (float dx : xarray) {
            for (float dy : yarray) {
                ConstVector3 location = focus.plus(new Vector3(dx, dy, 0.0f));
                KtxOctreeBlockTileKey tileKey = source.getBlockKeyAt(location, resolution);
                if (tileKey != null) {
                    result.add(tileKey);
                }
            }
        }

        return result;
    }

}
