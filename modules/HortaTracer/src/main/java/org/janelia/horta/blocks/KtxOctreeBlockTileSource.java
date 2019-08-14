package org.janelia.horta.blocks;

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

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.StringUtils;
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
    private String sampleKtxTilesBaseDir;
    private KtxOctreeBlockTileKey rootKey;
    private KtxHeader rootHeader;
    private KtxOctreeResolution maximumResolution;
    private ConstVector3 origin;
    private Vector3 outerCorner;

    KtxOctreeBlockTileSource(URL originatingSampleURL) {
        this.originatingSampleURL = originatingSampleURL;
    }

    public KtxOctreeBlockTileSource init(TmSample sample) {
        this.sampleKtxTilesBaseDir =
                StringUtils.appendIfMissing(
                        StringUtils.defaultIfBlank(
                                sample.getLargeVolumeKTXFilepath(),
                                StringUtils.appendIfMissing(sample.getLargeVolumeOctreeFilepath(), "/") + "ktx"),
                        "/");
        this.rootKey = new KtxOctreeBlockTileKey(this, Collections.emptyList());
        this.rootHeader = loadKtxHeader(rootKey);
        this.maximumResolution = getKtxResolution(rootHeader);
        Pair<ConstVector3, Vector3> volumeCorners = getVolumeCorners(sample, rootHeader);
        this.origin = volumeCorners.getLeft();
        this.outerCorner = volumeCorners.getRight();
        return this;
    }

    /**
     *
     * @param key
     * @return absolute path URL for a key block
     */
    URI getKeyBlockAbsolutePathURI(KtxOctreeBlockTileKey key) {
        return URI.create(sampleKtxTilesBaseDir)
                .resolve(key.getKeyPath())
                .resolve(key.getKeyBlockName("_8_xy_"))
                ;
    }

    private KtxHeader loadKtxHeader(KtxOctreeBlockTileKey octreeRootKey) {
        KtxHeader ktxHeader = new KtxHeader();
        try (InputStream blockStream = streamKeyBlock(octreeRootKey)) {
            ktxHeader.loadStream(blockStream);
            return ktxHeader;
        } catch (IOException e) {
            LOG.error("Error loading KTX header for {}({}) from {}", octreeRootKey, getKeyBlockAbsolutePathURI(octreeRootKey), originatingSampleURL);
            throw new IllegalStateException(e);
        }
    }

    private KtxOctreeResolution getKtxResolution(KtxHeader ktxHeader) {
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
        return new Vector3(blockExtent)
                .multiplyScalar(0.5f)
                .plus(blockOrigin);
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
        try (InputStream blockStream = streamKeyBlock(key)) {
            KtxOctreeBlockTileData data = new KtxOctreeBlockTileData();
            data.loadStream(blockStream);
            return data;
        }
    }

    abstract URI getDataServerURI();

    @Override
    public URL getOriginatingSampleURL() {
        return originatingSampleURL;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.originatingSampleURL);
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
        if (!Objects.equals(this.originatingSampleURL, other.originatingSampleURL)) {
            return false;
        }
        if (!Objects.equals(this.sampleKtxTilesBaseDir, other.sampleKtxTilesBaseDir)) {
            return false;
        }
        return true;
    }
}
