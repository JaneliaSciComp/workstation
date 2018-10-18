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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.ktx.KtxData;
import org.janelia.horta.ktx.KtxHeader;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.openide.util.Exceptions;
import org.python.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author brunsc
 */
public class KtxOctreeBlockTileSource implements BlockTileSource {
    private final URL rootUrl;

    private final KtxHeader rootHeader;
    // private final File rootFolder;
    private final KtxOctreeResolution maximumResolution;

    private final ConstVector3 origin;
    private final Vector3 outerCorner;
    private final KtxOctreeBlockTileKey rootKey;

    private final String compressionString;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Like "1/2/3/3"
    private String subfolderForKey(KtxOctreeBlockTileKey key) {
        String result = key.toString();
        if (result.length() > 0)
            result = result + "/"; // Add trailing (but not initial) slash
        return result;
    }

    private URL folderForKey(BlockTileKey key0) {
        KtxOctreeBlockTileKey key = (KtxOctreeBlockTileKey) key0;
        String subfolderStr = subfolderForKey(key);
        URL folderUrl = null;
        try {
            folderUrl = new URL(rootUrl, subfolderStr);
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return folderUrl;
    }

    private URL blockUrlForKey(KtxOctreeBlockTileKey key, String compressionString) throws IOException {
        URL folder = folderForKey(key);

        String subfolderStr = subfolderForKey(key);
        subfolderStr = subfolderStr.replaceAll("/", "");
        String fileName = "block" + compressionString + subfolderStr + ".ktx";

        URL blockUrl = new URL(folder, fileName);
        return blockUrl;
    }

    public InputStream streamForKey(BlockTileKey key) throws IOException {
        URL url = blockUrlForKey((KtxOctreeBlockTileKey) key, compressionString);
        return new BufferedInputStream(url.openStream());
    }

    public KtxOctreeBlockTileSource(URL rootUrl, TmSample sample) throws IOException {
        this.rootUrl = rootUrl;
        rootKey = new KtxOctreeBlockTileKey(new ArrayList<Integer>(), this);

        String[] pathParts = rootUrl.getPath().split("/");
        String specimenName = pathParts[pathParts.length - 1];

        // Figure out what the file names should be for this data set.
        // Try the redundant specimen name scheme first, since it's currently used by most MouseLight data. 
        // TODO: Once we move away from it, we can flip these two options.
        String[] namingSchemesToTry = new String[]{"_" + specimenName, ""};
        // Try different compression schemes.
        String[] compressionStringsToTry = new String[]{"_8_xy_", "_"};
        InputStream stream = null;
        IOException exception = null;
        String chosenCompressionString = null;
        OUTER:
        for (String nameScheme : namingSchemesToTry) {
            for (String cs : compressionStringsToTry) {
                String scheme = nameScheme + cs;
                URL url = blockUrlForKey(rootKey, scheme);
                try {
                    stream = new BufferedInputStream(url.openStream());
                } catch (IOException exc) {
                    logger.info("Tried looking for KTX tiles at {}", exc.getMessage());
                    exception = exc;
                    continue;
                }
                logger.info("Found KTX tiles at {}", url);
                chosenCompressionString = scheme;
                break OUTER;
            }
        }
        if (stream == null)
            throw exception;
        compressionString = chosenCompressionString;

        rootHeader = new KtxHeader();
        rootHeader.loadStream(stream);

        // Parse maximum resolution
        int maxRes = Integer.parseInt(rootHeader.keyValueMetadata.get("multiscale_total_levels").trim()) - 1;
        maximumResolution = new KtxOctreeResolution(maxRes);

        // Parse outer corner locations of volume block
        // Trivia note: All this parsing would be a one-liner in PERL.
        String cornersString = rootHeader.keyValueMetadata.get("corner_xyzs").trim();
        // logger.info(cornersString);
        // [(68097.320000000007, 13754.192000000001, 27557.100000000002), (79094.79800000001, 13754.192000000001, 27557.100000000002), (68097.320000000007, 21962.162, 27557.100000000002), (79094.79800000001, 21962.162, 27557.100000000002), (68097.320000000007, 13754.192000000001, 42164.300000000003), (79094.79800000001, 13754.192000000001, 42164.300000000003), (68097.320000000007, 21962.162, 42164.300000000003), (79094.79800000001, 21962.162, 42164.300000000003)]
        String numberPattern = "[-+]?[0-9]+(?:\\.[0-9]+)?";
        String tuple3Pattern = "\\((" + numberPattern + ", " + numberPattern + ", " + numberPattern + ")\\)";
        // Extract just the first and last corner locations from the corner list
        Pattern p = Pattern.compile("^\\[" + tuple3Pattern + ".*" + tuple3Pattern + "\\]$");
        Matcher m = p.matcher(cornersString);
        if (!m.matches()) {
            throw new IOException("Error parsing Ktx block corners");
        }
        // logger.info("Matches");
        // logger.info("" + m.groupCount());
        // logger.info(m.group(1));
        // logger.info(m.group(2));
        //String[] originStrings = m.group(1).split(", ");
        String[] originStrings = m.group(1).split(", ");
        String[] outerCornerStrings = m.group(2).split(", ");
        List<Integer> sampleOrigin = sample.getOrigin();
        if (sampleOrigin == null) {
            origin = new Vector3(
                    Float.parseFloat(originStrings[0]),
                    Float.parseFloat(originStrings[1]),
                    Float.parseFloat(originStrings[2]));
        } else {
            origin = new Vector3(
                    new BigDecimal(sampleOrigin.get(0)).movePointLeft(3).floatValue(),
                    new BigDecimal(sampleOrigin.get(1)).movePointLeft(3).floatValue(),
                    new BigDecimal(sampleOrigin.get(2)).movePointLeft(3).floatValue());
        }
        outerCorner = new Vector3(
                Float.parseFloat(outerCornerStrings[0]),
                Float.parseFloat(outerCornerStrings[1]),
                Float.parseFloat(outerCornerStrings[2]));
        // logger.info("" + origin);
        // logger.info("" + outerCorner);
        assert origin.getX() < outerCorner.getX();
        assert origin.getY() < outerCorner.getY();
        assert origin.getZ() < outerCorner.getZ();
    }

    @Override
    public BlockTileResolution getMaximumResolution() {
        return maximumResolution;
    }

    public ConstVector3 getBlockSize(BlockTileResolution resolution) {
        Vector3 rootBlockSize = outerCorner.minus(origin);
        float scale = (float) Math.pow(2.0, ((KtxOctreeResolution) resolution).octreeLevel);
        Vector3 result = rootBlockSize.multiplyScalar(1.0f / scale);
        return result;
    }

    public ConstVector3 getBlockOrigin(BlockTileKey key0) {
        KtxOctreeBlockTileKey key = (KtxOctreeBlockTileKey) key0;
        Vector3 blockOrigin = new Vector3(origin);
        Vector3 subBlockExtent = outerCorner.minus(origin);
        for (int p : key.path) {
            subBlockExtent.setX(subBlockExtent.getX() / 2.0f);
            subBlockExtent.setY(subBlockExtent.getY() / 2.0f);
            subBlockExtent.setZ(subBlockExtent.getZ() / 2.0f);

            if (p % 2 == 0) // large X (2,4,6,8)
                blockOrigin.setX(blockOrigin.getX() + subBlockExtent.getX());
            if (p > 4) // large Z (5,6,7,8)
                blockOrigin.setZ(blockOrigin.getZ() + subBlockExtent.getZ());
            if ((p == 3) || (p == 4) || (p == 7) || (p == 8)) // large Y (3,4,7,8)
                blockOrigin.setY(blockOrigin.getY() + subBlockExtent.getY());
        }
        return blockOrigin;
    }

    @Override
    public BlockTileKey getBlockKeyAt(ConstVector3 location, BlockTileResolution resolution0) {
        if (resolution0 == null)
            resolution0 = getMaximumResolution();
        KtxOctreeResolution resolution = (KtxOctreeResolution) resolution0;
        if (resolution.compareTo(getMaximumResolution()) > 0)
            return null; // no resolution that high

        if (location.getX() < origin.getX()) return null;
        if (location.getY() < origin.getY()) return null;
        if (location.getZ() < origin.getZ()) return null;

        if (location.getX() > outerCorner.getX()) return null;
        if (location.getY() > outerCorner.getY()) return null;
        if (location.getZ() > outerCorner.getZ()) return null;

        List<Integer> octreePath = new ArrayList<>();
        Vector3 subBlockOrigin = new Vector3(origin);
        Vector3 subBlockExtent = outerCorner.minus(origin);
        while (octreePath.size() < resolution.octreeLevel) {
            // Reduce block size to half, per octree level
            subBlockExtent.setX(subBlockExtent.getX() / 2.0f);
            subBlockExtent.setY(subBlockExtent.getY() / 2.0f);
            subBlockExtent.setZ(subBlockExtent.getZ() / 2.0f);

            int octreeStep = 1;
            if (location.getX() > subBlockOrigin.getX() + subBlockExtent.getX()) { // larger X
                octreeStep += 1;
                subBlockOrigin.setX(subBlockOrigin.getX() + subBlockExtent.getX());
            }
            if (location.getY() > subBlockOrigin.getY() + subBlockExtent.getY()) { // larger Y
                octreeStep += 2;
                subBlockOrigin.setY(subBlockOrigin.getY() + subBlockExtent.getY());
            }
            if (location.getZ() > subBlockOrigin.getZ() + subBlockExtent.getZ()) { // larger Z
                octreeStep += 4;
                subBlockOrigin.setZ(subBlockOrigin.getZ() + subBlockExtent.getZ());
            }

            octreePath.add(octreeStep);
        }

        return new KtxOctreeBlockTileKey(octreePath, this);
    }

    @Override
    public ConstVector3 getBlockCentroid(BlockTileKey centerBlock) {
        ConstVector3 blockOrigin = getBlockOrigin(centerBlock);
        KtxOctreeBlockTileKey key = (KtxOctreeBlockTileKey) centerBlock;
        KtxOctreeResolution resolution = new KtxOctreeResolution(key.path.size());
        ConstVector3 blockExtent = getBlockSize(resolution);
        Vector3 centroid = new Vector3(blockExtent);
        centroid.multiplyScalar(0.5f);
        centroid = centroid.plus(blockOrigin);
        return centroid;
    }


    @Override
    public BlockTileData loadBlock(BlockTileKey key)
            throws IOException, InterruptedException {
        long t0 = System.nanoTime();
        try (InputStream stream = streamForKey(key)) {
            KtxOctreeBlockTileData data = new KtxOctreeBlockTileData();
            data.loadStreamInterruptably(stream);
            long t1 = System.nanoTime();
            float elapsed = (t1 - t0) / 1e9f;
            logger.info("Ktx tile '" + key + "' stream load took "
                    + new DecimalFormat("#0.000").format(elapsed)
                    + "seconds");
            return data;
        }
    }

    @Override
    public URL getRootUrl() {
        return rootUrl;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.rootUrl.getFile());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KtxOctreeBlockTileSource other = (KtxOctreeBlockTileSource) obj;
        if (!Objects.equals(this.rootUrl.getFile(), other.rootUrl.getFile())) {
            return false;
        }
        return true;
    }

    static public class KtxOctreeBlockTileData
            extends KtxData
            implements BlockTileData {

    }


    static public class KtxOctreeBlockTileKey
            implements BlockTileKey {
        private final KtxOctreeBlockTileSource source;
        private final List<Integer> path;

        private KtxOctreeBlockTileKey(List<Integer> octreePath, KtxOctreeBlockTileSource source) {
            this.source = source;
            this.path = octreePath;
        }

        @Override
        public ConstVector3 getCentroid() {
            return source.getBlockCentroid(this);
        }

        @Override
        public BlockTileSource getSource() {
            return source;
        }

        @Override
        public String toString() {
            List<String> steps = new ArrayList<>();
            for (Integer s : path) {
                steps.add(s.toString());
            }
            String subfolderStr = Joiner.on("/").join(steps);
            return subfolderStr;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(this.source);
            hash = 53 * hash + Objects.hashCode(this.path);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KtxOctreeBlockTileKey other = (KtxOctreeBlockTileKey) obj;
            if (!Objects.equals(this.source, other.source)) {
                return false;
            }
            if (!Objects.equals(this.path, other.path)) {
                return false;
            }
            return true;
        }
    }


    static public class KtxOctreeResolution
            implements BlockTileResolution {
        final int octreeLevel; // zero-based level; zero means tip of pyramid

        public KtxOctreeResolution(int octreeLevel) {
            this.octreeLevel = octreeLevel;
        }

        @Override
        public int hashCode() {
            return octreeLevel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KtxOctreeResolution other = (KtxOctreeResolution) obj;
            if (this.octreeLevel != other.octreeLevel) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(BlockTileResolution o) {
            KtxOctreeResolution rhs = (KtxOctreeResolution) o;
            return octreeLevel < rhs.octreeLevel ? -1 : octreeLevel > rhs.octreeLevel ? 1 : 0;
        }

    }

}
