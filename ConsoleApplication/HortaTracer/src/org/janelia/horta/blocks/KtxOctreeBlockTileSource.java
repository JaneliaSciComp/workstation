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

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.ktx.KtxHeader;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author brunsc
 */
public abstract class KtxOctreeBlockTileSource implements BlockTileSource<KtxOctreeBlockTileKey> {
    private static final Logger LOG = LoggerFactory.getLogger(KtxOctreeBlockTileSource.class);

    private final URL originatingSampleURL;
    protected String sampleKtxTilesBaseDir;
    protected String sourceServerURL;
    protected KtxOctreeBlockTileKey rootKey;
    protected KtxHeader rootHeader;
    protected KtxOctreeResolution maximumResolution;
    protected ConstVector3 origin;
    protected Vector3 outerCorner;

    KtxOctreeBlockTileSource(URL originatingSampleURL) {
        this.originatingSampleURL = originatingSampleURL;
    }

    public KtxOctreeBlockTileSource init(TmSample sample) {
        Preconditions.checkArgument(sample.getFilepath() != null && sample.getFilepath().trim().length() > 0);
        this.sampleKtxTilesBaseDir = getKtxBaseDir(sample.getFilepath());
        this.sourceServerURL = getSourceServerURL(sample);
        this.rootKey = new KtxOctreeBlockTileKey(this, Collections.<Integer>emptyList());
        this.rootHeader = createKtxHeader(rootKey);
        this.maximumResolution = createKtxResolution(rootHeader);
        Pair<ConstVector3, Vector3> volumeCorners = getVolumeCorners(sample, rootHeader);
        this.origin = volumeCorners.getLeft();
        this.outerCorner = volumeCorners.getRight();       
        return this;
    }

    private String getKtxBaseDir(String sampleDir) {
        if (sampleDir.endsWith("/")) {
            return sampleDir + "ktx/";
        } else {
            return sampleDir + "/ktx/";
        }
    }

    protected abstract String getSourceServerURL(TmSample sample);

    protected URI getKeyBlockPathURI(KtxOctreeBlockTileKey key) {
        return URI.create(sampleKtxTilesBaseDir)
                .resolve(key.getKeyPath())
                .resolve(key.getKeyBlockName("_8_xy_"))
                ;
    }

    private KtxHeader createKtxHeader(KtxOctreeBlockTileKey octreeRootKey) {
        try {
            KtxHeader ktxHeader = new KtxHeader();
            ktxHeader.loadStream(streamKeyBlock(octreeRootKey));
            return ktxHeader;
        } catch (IOException e) {
            LOG.error("Error loading KTX header for {}({}) from {}",
                    octreeRootKey, getKeyBlockPathURI(octreeRootKey), sourceServerURL);
            throw new IllegalStateException(e);
        }
    }

    private KtxOctreeResolution createKtxResolution(KtxHeader ktxHeader) {
        // Parse maximum resolution
        int maxRes = Integer.parseInt(ktxHeader.keyValueMetadata.get("multiscale_total_levels").trim()) - 1;
        return new KtxOctreeResolution(maxRes);
    }

    private Pair<ConstVector3, Vector3> getVolumeCorners(TmSample sample, KtxHeader ktxHeader) {
        String cornersString = rootHeader.keyValueMetadata.get("corner_xyzs").trim();
        /*
        Example of what the corners string looks like:
        [
            (68097.320000000007, 13754.192000000001, 27557.100000000002), 
            (79094.79800000001, 13754.192000000001, 27557.100000000002), 
            (68097.320000000007, 21962.162, 27557.100000000002), 
            (79094.79800000001, 21962.162, 27557.100000000002),
            (68097.320000000007, 13754.192000000001, 42164.300000000003), 
            (79094.79800000001, 13754.192000000001, 42164.300000000003),
            (68097.320000000007, 21962.162, 42164.300000000003), 
            (79094.79800000001, 21962.162, 42164.300000000003)
        ]
        */
        String numberPattern = "[-+]?[0-9]+(?:\\.[0-9]+)?";
        String tuple3Pattern = "\\((" + numberPattern + ", " + numberPattern + ", " + numberPattern + ")\\)";
        // Extract just the first and last corner locations from the corner list
        Pattern p = Pattern.compile("^\\[" + tuple3Pattern + ".*" + tuple3Pattern + "\\]$");
        Matcher m = p.matcher(cornersString);
        if (!m.matches()) {
            LOG.error("Error parsing out the corners from {} using {}", cornersString, p);
            throw new IllegalArgumentException("Error extracting the corners from " + cornersString);
        }
        String[] originStrings = m.group(1).split(", ");
        String[] outerCornerStrings = m.group(2).split(", ");
        List<Integer> sampleOriginComps = sample.getOrigin();
        ConstVector3 sampleOrigin;
        if (sampleOriginComps == null || sampleOriginComps.isEmpty()) {
            sampleOrigin = new Vector3(
                    Float.parseFloat(originStrings[0]),
                    Float.parseFloat(originStrings[1]),
                    Float.parseFloat(originStrings[2]));
        } else {
            sampleOrigin = new Vector3(
                    new BigDecimal(sampleOriginComps.get(0)).movePointLeft(3).floatValue(),
                    new BigDecimal(sampleOriginComps.get(1)).movePointLeft(3).floatValue(),
                    new BigDecimal(sampleOriginComps.get(2)).movePointLeft(3).floatValue());
        }
        return ImmutablePair.of(
                sampleOrigin,
                new Vector3(
                        Float.parseFloat(outerCornerStrings[0]),
                        Float.parseFloat(outerCornerStrings[1]),
                        Float.parseFloat(outerCornerStrings[2]))
        );
    }

    protected abstract InputStream streamKeyBlock(KtxOctreeBlockTileKey octreeKey);

    @Override
    public BlockTileResolution getMaximumResolution() {
        return maximumResolution;
    }

    ConstVector3 getMaximumResolutionBlockSize() {
        Vector3 rootBlockSize = outerCorner.minus(origin);
        float scale = (float) Math.pow(2.0, maximumResolution.getResolution());
        return rootBlockSize.multiplyScalar(1.0f / scale);
    }

    private ConstVector3 getBlockSize(KtxOctreeResolution resolution) {
        Vector3 rootBlockSize = outerCorner.minus(origin);
        float scale = (float) Math.pow(2.0, resolution.getResolution());
        return rootBlockSize.multiplyScalar(1.0f / scale);
    }

    @Override
    public KtxOctreeBlockTileKey getBlockKeyAt(ConstVector3 focusLocation, BlockTileResolution resolution) {
        BlockTileResolution ktxResolution;
        if (resolution == null) {
            ktxResolution = maximumResolution;
        } else {
            ktxResolution = resolution;
        }
        if (ktxResolution.compareTo(getMaximumResolution()) > 0)
            return null; // no resolution that high

        if (focusLocation.getX() < origin.getX()) return null;
        if (focusLocation.getY() < origin.getY()) return null;
        if (focusLocation.getZ() < origin.getZ()) return null;

        if (focusLocation.getX() > outerCorner.getX()) return null;
        if (focusLocation.getY() > outerCorner.getY()) return null;
        if (focusLocation.getZ() > outerCorner.getZ()) return null;

        List<Integer> octreePath = new ArrayList<>();
        Vector3 subBlockOrigin = new Vector3(origin);
        Vector3 subBlockExtent = outerCorner.minus(origin);
        while (octreePath.size() < ktxResolution.getResolution()) {
            // Reduce block size to half, per octree level
            subBlockExtent.setX(subBlockExtent.getX() / 2.0f);
            subBlockExtent.setY(subBlockExtent.getY() / 2.0f);
            subBlockExtent.setZ(subBlockExtent.getZ() / 2.0f);

            int octreeStep = 1;
            if (focusLocation.getX() > subBlockOrigin.getX() + subBlockExtent.getX()) { // larger X
                octreeStep += 1;
                subBlockOrigin.setX(subBlockOrigin.getX() + subBlockExtent.getX());
            }
            if (focusLocation.getY() > subBlockOrigin.getY() + subBlockExtent.getY()) { // larger Y
                octreeStep += 2;
                subBlockOrigin.setY(subBlockOrigin.getY() + subBlockExtent.getY());
            }
            if (focusLocation.getZ() > subBlockOrigin.getZ() + subBlockExtent.getZ()) { // larger Z
                octreeStep += 4;
                subBlockOrigin.setZ(subBlockOrigin.getZ() + subBlockExtent.getZ());
            }
            octreePath.add(octreeStep);
        }
        return new KtxOctreeBlockTileKey(this, ImmutableList.copyOf(octreePath));
    }

    @Override
    public ConstVector3 getBlockCentroid(BlockTileKey centerBlock) {
        KtxOctreeBlockTileKey octreeCenterBlockKey = (KtxOctreeBlockTileKey) centerBlock;
        ConstVector3 blockOrigin = getBlockOrigin(octreeCenterBlockKey);
        KtxOctreeResolution ktxResolution = new KtxOctreeResolution(octreeCenterBlockKey.getKeyDepth());
        ConstVector3 blockExtent = getBlockSize(ktxResolution);
        Vector3 centroid = new Vector3(blockExtent);
        centroid.multiplyScalar(0.5f);
        centroid = centroid.plus(blockOrigin);
        return centroid;
    }

    private ConstVector3 getBlockOrigin(KtxOctreeBlockTileKey octreeKey) {
        Vector3 blockOrigin = new Vector3(origin);
        Vector3 subBlockExtent = outerCorner.minus(origin);
        for (int p : octreeKey.getOctreePath()) {
            subBlockExtent.setX(subBlockExtent.getX() / 2.0f);
            subBlockExtent.setY(subBlockExtent.getY() / 2.0f);
            subBlockExtent.setZ(subBlockExtent.getZ() / 2.0f);
            if (p % 2 == 0) { // large X (2,4,6,8)
                blockOrigin.setX(blockOrigin.getX() + subBlockExtent.getX());
            }
            if (p > 4) { // large Z (5,6,7,8)
                blockOrigin.setZ(blockOrigin.getZ() + subBlockExtent.getZ());
            }
            if ((p == 3) || (p == 4) || (p == 7) || (p == 8)) { // large Y (3,4,7,8)
                blockOrigin.setY(blockOrigin.getY() + subBlockExtent.getY());
            }
        }
        return blockOrigin;
    }

    @Override
    public BlockTileData loadBlock(KtxOctreeBlockTileKey key) throws IOException, InterruptedException {
        try (InputStream stream = streamKeyBlock(key)) {
            KtxOctreeBlockTileData data = new KtxOctreeBlockTileData();
            data.loadStreamInterruptably(stream);
            return data;
        }
    }

    @Override
    public URL getOriginatingSampleURL() {
        return originatingSampleURL;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.sourceServerURL);
        hash = 59 * hash + Objects.hashCode(this.sampleKtxTilesBaseDir);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KtxOctreeBlockTileSource other = (KtxOctreeBlockTileSource) obj;
        if (!Objects.equals(this.sourceServerURL, other.sourceServerURL)) {
            return false;
        }
        if (!Objects.equals(this.sampleKtxTilesBaseDir, other.sampleKtxTilesBaseDir)) {
            return false;
        }
        return true;
    }
}
