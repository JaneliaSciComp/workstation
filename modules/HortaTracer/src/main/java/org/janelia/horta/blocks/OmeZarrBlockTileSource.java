package org.janelia.horta.blocks;

import org.aind.omezarr.*;
import org.aind.omezarr.image.AutoContrastParameters;
import org.aind.omezarr.image.TCZYXRasterZStack;
import org.apache.commons.lang3.StringUtils;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.omezarr.JadeZarrStoreProvider;
import org.janelia.horta.omezarr.OmeZarrJadeReader;
import org.janelia.horta.omezarr.OmeZarrReaderProgressObserver;
import org.janelia.horta.omezarr.OmeZarrReaderCompletionObserver;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.workstation.geom.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OmeZarrBlockTileSource implements BlockTileSource<OmeZarrBlockTileKey> {
    private static final Logger log = LoggerFactory.getLogger(OmeZarrBlockTileSource.class);

    private final URL originatingSampleURL;
    private String sampleOmeZarrTilesBaseDir;
    private OmeZarrJadeReader reader;
    private OmeZarrGroup omeZarrGroup;

    private final ArrayList<OmeZarrBlockResolution> resolutions = new ArrayList<>();
    private OmeZarrBlockResolution maximumResolution = null;

    private final ImageColorModel imageColorModel;

    private BoundingBox3d boundingBox3d = new BoundingBox3d();
    private Vec3 voxelCenter = new Vec3(0, 0, 0);

    private static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    public OmeZarrBlockTileSource(URL originatingSampleURL, ImageColorModel imageColorModel) {
        this.originatingSampleURL = originatingSampleURL;
        this.imageColorModel = imageColorModel;
    }

    public OmeZarrBlockTileSource init(String localPath) throws IOException {
        return init(localPath, null, null);
    }

    public OmeZarrBlockTileSource init(String localPath, OmeZarrReaderProgressObserver progressObserver, OmeZarrReaderCompletionObserver completionObserver) throws IOException {
        this.sampleOmeZarrTilesBaseDir = localPath;

        this.reader = null;

        omeZarrGroup = OmeZarrGroup.open(Paths.get(localPath));

        return init(progressObserver, completionObserver);
    }

    public OmeZarrBlockTileSource init(TmSample sample) throws IOException {
        return init(sample, null, null);
    }

    public OmeZarrBlockTileSource init(TmSample sample, OmeZarrReaderProgressObserver progressObserver, OmeZarrReaderCompletionObserver completionObserver) throws IOException {
        this.sampleOmeZarrTilesBaseDir = StringUtils.appendIfMissing(sample.getFiles().get(FileType.LargeVolumeZarr), "/");

        this.reader = new OmeZarrJadeReader(FileMgr.getFileMgr().getStorageService(), this.sampleOmeZarrTilesBaseDir);

        omeZarrGroup = OmeZarrGroup.open(new JadeZarrStoreProvider("", reader));

        return init(progressObserver, completionObserver);
    }

    private OmeZarrBlockTileSource init(OmeZarrReaderProgressObserver progressObserver, OmeZarrReaderCompletionObserver completionObserver) {
        cachedThreadPool.submit(() -> {
            int datasetCount = omeZarrGroup.getAttributes().getMultiscales()[0].getDatasets().size();

            boolean haveExtents = false;

            for (int idx = datasetCount - 1; idx >= 0; idx--) {
                try {
                    OmeZarrDataset dataset = omeZarrGroup.getAttributes().getMultiscales()[0].getDatasets().get(idx);

                    if (this.reader != null) {
                        dataset.setExternalZarrStore(new JadeZarrStoreProvider(dataset.getPath(), reader));
                    }

                    if (!dataset.isValid()) {
                        // Might be a dataset listed in .zattrs that is not available in the datastore.
                        continue;
                    }

                    // z, y, x order
                    OmeZarrIndex shapeIndex = dataset.getShapeIndex();
                    OmeZarrIndex chunkSize = dataset.getChunksIndex();
                    int[] chunkSizeXYZ = {chunkSize.getX(), chunkSize.getY(), chunkSize.getZ()};

                    double resolutionMicrometers = dataset.getMinSpatialResolution();

                    OmeZarrValue res = dataset.getSpatialResolution(OmeZarrAxisUnit.MICROMETER);
                    double[] voxelSize = new double[]{res.getX(), res.getY(), res.getZ()};

                    if (!haveExtents) {
                        // z, y,x order

                        // TODO Respect translate transforms in dataset.
                        ConstVector3 origin = new Vector3(0, 0, 0);
                        Vector3 outerCorner = new Vector3(shapeIndex.getX() * res.getX(), shapeIndex.getY() * res.getY(), shapeIndex.getZ() * res.getZ());
                        boundingBox3d = new BoundingBox3d(new Vec3(origin.getX(), origin.getY(), origin.getZ()), new Vec3(outerCorner.getX(), outerCorner.getY(), outerCorner.getZ()));
                        voxelCenter = boundingBox3d.getCenter();

                        haveExtents = true;
                    }

                    OmeZarrBlockResolution resolution = new OmeZarrBlockResolution(dataset, idx, chunkSizeXYZ, voxelSize, resolutionMicrometers);

                    if (progressObserver != null) {
                        progressObserver.update(this, "Loading dataset " + dataset.getPath());
                    }

                    log.info("finished loading path " + dataset.getPath());

                    resolutions.add(resolution);

                    if (maximumResolution == null) {
                        maximumResolution = resolutions.get(resolutions.size() - 1);
                    }

                } catch (Exception ex) {
                    log.info("failed to initialize dataset at index " + idx);
                }
            }

            completionObserver.complete(this);
        });

        return this;
    }

    public BoundingBox3d getBoundingBox3d() {
        return boundingBox3d;
    }

    public Vec3 getVoxelCenter() {
        return voxelCenter;
    }

    @Override
    public BlockTileResolution getMaximumResolution() {
        return maximumResolution;
    }

    @Override
    public OmeZarrBlockTileKey getBlockKeyAt(ConstVector3 focus, BlockTileResolution resolution) {
        if (resolution == null) {
            return null;
        }

        OmeZarrBlockResolution res = (OmeZarrBlockResolution) resolution;

        OmeZarrDataset dataset = res.getDataset();

        OmeZarrValue location = new OmeZarrValue(0, 0, focus.getZ(), focus.getY(), focus.getX());

        OmeZarrBlockTileKey key2 = null;

        try {
            OmeZarrReadChunk chunk = dataset.readChunkForLocation(location);

            if (chunk != null) {
                key2 = new OmeZarrBlockTileKey(dataset, res.getDepth(), chunk.getShape(), chunk.getOffset(), res.getVoxelSize(), 1);
            }
        } catch (Exception ignored) {
        }

        return key2;

    }

    @Override
    public ConstVector3 getBlockCentroid(BlockTileKey centerBlock) {
        return centerBlock.getCentroid();
    }

    @Override
    public BlockTileData loadBlock(OmeZarrBlockTileKey key) {
        return null;
    }

    @Override
    public URL getOriginatingSampleURL() {
        return this.originatingSampleURL;
    }

    public ImageColorModel getColorModel() {
        return imageColorModel;
    }

    public ArrayList<OmeZarrBlockResolution> getResolutions() {
        return resolutions;
    }

    ConstVector3 getBlockSize(OmeZarrBlockResolution resolution) {
        return new Vector3(resolution.getChunkSize()[0] * resolution.getResolutionMicrometers(),
                resolution.getChunkSize()[1] * resolution.getResolutionMicrometers(),
                resolution.getChunkSize()[2] * resolution.getResolutionMicrometers());
    }

    public Texture3d loadBrick(OmeZarrBlockTileKey tile, int colorChannel) {
        // setColorChannelIndex(colorChannel);
        return loadBrick(tile);
    }

    public Texture3d loadBrick(OmeZarrBlockTileKey tile) {
        return tile.loadBrick();
    }
}
