package org.janelia.horta.blocks;

import org.aind.omezarr.OmeZarrAxisUnit;
import org.aind.omezarr.OmeZarrDataset;
import org.aind.omezarr.OmeZarrGroup;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OmeZarrBlockTileSource implements BlockTileSource<OmeZarrBlockTileKey> {
    private static final Logger log = LoggerFactory.getLogger(OmeZarrBlockTileSource.class);

    private final URL originatingSampleURL;
    private String sampleOmeZarrTilesBaseDir;
    private OmeZarrJadeReader reader;
    private OmeZarrGroup omeZarrGroup;

    private AutoContrastParameters autoContrastParameters = null;

    private final ArrayList<OmeZarrBlockResolution> resolutions = new ArrayList<>();
    private OmeZarrBlockResolution maximumResolution = null;

    private final ImageColorModel imageColorModel;

    private final boolean useAutoContrast;

    private BoundingBox3d boundingBox3d = new BoundingBox3d();
    private Vec3 voxelCenter = new Vec3(0, 0, 0);

    private static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    private static final double MAX_BLOCKING_RESOLUTION = 30.0;

    public OmeZarrBlockTileSource(URL originatingSampleURL, ImageColorModel imageColorModel) {
        this(originatingSampleURL, imageColorModel, false);
    }

    public OmeZarrBlockTileSource(URL originatingSampleURL, ImageColorModel imageColorModel, boolean useAutoContrast) {
        this.originatingSampleURL = originatingSampleURL;
        this.imageColorModel = imageColorModel;
        this.useAutoContrast = useAutoContrast;
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

            int maxBlockingResolutionIndex = Integer.MIN_VALUE;

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
                    int[] shape = dataset.getRawShape();
                    int[] chunkSize = dataset.getRawChunks();
                    int[] chunkSizeXYZ = {chunkSize[4], chunkSize[3], chunkSize[2]};

                    double resolutionMicrometers = dataset.getMinSpatialResolution();

                    if (!haveExtents) {
                        // z, y,x order
                        List<Double> res = dataset.getSpatialResolution(OmeZarrAxisUnit.MICROMETER);

                        // TODO Respect translate transforms in dataset.
                        ConstVector3 origin = new Vector3(0, 0, 0);
                        Vector3 outerCorner = new Vector3(shape[4] * res.get(2), shape[3] * res.get(1), shape[2] * res.get(0));
                        boundingBox3d = new BoundingBox3d(new Vec3(origin.getX(), origin.getY(), origin.getX()), new Vec3(outerCorner.getX(), outerCorner.getY(), outerCorner.getX()));
                        voxelCenter = boundingBox3d.getCenter();

                        haveExtents = true;
                    }

                    OmeZarrBlockResolution resolution = new OmeZarrBlockResolution(idx, chunkSizeXYZ, resolutionMicrometers, 0);

                    if (resolutionMicrometers < MAX_BLOCKING_RESOLUTION) {
                        if (idx > maxBlockingResolutionIndex) {
                            maxBlockingResolutionIndex = idx;
                        }

                        resolution = new OmeZarrBlockResolution(idx, chunkSizeXYZ, resolutionMicrometers, maxBlockingResolutionIndex - idx + 1);

                        if (maxBlockingResolutionIndex == idx) {
                            maximumResolution = resolution;
                        }
                    }

                    createTileKeysForDataset(resolution, dataset, progressObserver);

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

        return ((OmeZarrBlockResolution) resolution).getBlockInfoSet().getBestContainingBrick(focus, 1);
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
        return tile.loadBrick(useAutoContrast ? autoContrastParameters : null);
    }

    /**
     * Only valid for tczyx OmeZarr datasets.
     */
    private void createTileKeysForDataset(OmeZarrBlockResolution resolution, OmeZarrDataset dataset, OmeZarrReaderProgressObserver progressReceiver) {
        try {
            OmeZarrBlockInfoSet blockInfoSet = resolution.getBlockInfoSet();

            // [x, y, z]
            int[] chunkSize = resolution.getChunkSize();

            int xChunkSegment = chunkSize[0];
            int yChunkSegment = chunkSize[1];
            int zChunkSegment = chunkSize[2];

            // [t, c, z, y, x]
            int[] shape = dataset.getRawShape();

            // [z, y, x]
            List<Double> spatialShape = dataset.getSpatialResolution(OmeZarrAxisUnit.MICROMETER);

            // [x, y, z]
            double[] voxelSize = {spatialShape.get(2), spatialShape.get(1), spatialShape.get(0)};

            int chunkCount = (int) (Math.ceil(1.0 * shape[4] / xChunkSegment) * Math.ceil(1.0 * shape[3] / yChunkSegment) * Math.ceil(1.0 * shape[2] / zChunkSegment));

            List<OmeZarrBlockTileKey> brickInfoList = new ArrayList<>(chunkCount);

            if (progressReceiver != null) {
                progressReceiver.update(this, "Loading dataset " + dataset.getPath() + " (" + chunkCount + " tiles remaining)");
            }

            log.info(String.format("preparing " + chunkCount + " tile keys for resolution: %.1fum/voxel (path " + dataset.getPath() + ", tile size: [" + xChunkSegment + "," + yChunkSegment + "," + zChunkSegment + "])", resolution.getResolutionMicrometers()));

            if (useAutoContrast && autoContrastParameters == null) {
                int[] autoContrastShape = {1, 1, 256, 256, 128};

                AutoContrastParameters parameters = TCZYXRasterZStack.computeAutoContrast(dataset, autoContrastShape);

                double existingMax = parameters.min + (65535.0 / parameters.slope);

                double min = Math.max(100, parameters.min * 0.1);
                double max = Math.min(65535.0, Math.max(min + 100, existingMax * 4));
                double slope = 65535.0 / (max - min);

                autoContrastParameters = new AutoContrastParameters(min, slope);
            }

            int keyDepth = resolution.getDepth();

            // [x, y, z]
            int[] actualChunkSize = new int[3];
            int[] offset = new int[3];

            for (int xIdx = 0; xIdx < shape[4]; xIdx += xChunkSegment) {
                for (int yIdx = 0; yIdx < shape[3]; yIdx += yChunkSegment) {
                    for (int zIdx = 0; zIdx < shape[2]; zIdx += zChunkSegment) {
                        // [x, y, z]
                        offset[0] = xIdx;
                        offset[1] = yIdx;
                        offset[2] = zIdx;

                        actualChunkSize[0] = Math.min(shape[4] - xIdx, xChunkSegment);
                        actualChunkSize[1] = Math.min(shape[3] - yIdx, yChunkSegment);
                        actualChunkSize[2] = Math.min(shape[2] - zIdx, zChunkSegment);

                        // All args [x, y, z]
                        brickInfoList.add(new OmeZarrBlockTileKey(dataset, keyDepth, actualChunkSize, offset, voxelSize, shape[1]));

                        chunkCount--;

                        if (chunkCount % 250000 == 0) {
                            if (progressReceiver != null) {
                                progressReceiver.update(this,"Loading dataset " + dataset.getPath() + " (" + chunkCount + " tiles remaining)");
                            }
                        }
                    }
                }
            }

            if (progressReceiver != null) {
                progressReceiver.update(this, "Loading dataset " + dataset.getPath() + " (building spatial tree)");
            }

            long startTime = System.currentTimeMillis();

            blockInfoSet.addAll(brickInfoList);

            long endTime = System.currentTimeMillis();
            log.info("Spatial index build for path {} took {} ms", dataset.getPath(), endTime - startTime);

            log.error("finished loading path " + dataset.getPath());
        } catch (Exception ex) {
            log.error("failed to load path " + dataset.getPath());
            log.error(ex.getMessage());
        }
    }
}
