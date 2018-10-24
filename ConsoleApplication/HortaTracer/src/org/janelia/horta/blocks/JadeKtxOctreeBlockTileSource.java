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

import org.janelia.geometry3d.ConstVector3;
import org.janelia.it.workstation.browser.api.web.JadeServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class JadeKtxOctreeBlockTileSource implements BlockTileSource {

    private static final Logger LOG = LoggerFactory.getLogger(JadeKtxOctreeBlockTileSource.class);

    private final JadeServiceClient jadeServiceClient;
    private final String tileBaseDir;
    private String tileServerURL;
    private URL tileBaseURL;

    public JadeKtxOctreeBlockTileSource(JadeServiceClient jadeServiceClient, String tileBaseDir) {
        this.jadeServiceClient = jadeServiceClient;
        this.tileBaseDir = tileBaseDir;
        initialize();
    }

    private void initialize() {
        try {
            tileServerURL = jadeServiceClient.findStorageURL(tileBaseDir);
            tileBaseURL = new URL(tileServerURL + "/storage_path/" + tileBaseDir);
        } catch (Exception e) {
            LOG.error("Error initializing JADE Ktx tile source for {}", tileBaseDir, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public BlockTileResolution getMaximumResolution() {
        return null;
    }

    @Override
    public BlockTileKey getBlockKeyAt(ConstVector3 focus, BlockTileResolution resolution) {
        return null;
    }

    @Override
    public ConstVector3 getBlockCentroid(BlockTileKey centerBlock) {
        return null;
    }

    @Override
    public BlockTileData loadBlock(BlockTileKey key) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public URL getRootUrl() {
        return null;
    }
}
