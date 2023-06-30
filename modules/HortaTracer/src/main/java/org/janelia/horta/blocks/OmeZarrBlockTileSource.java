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
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.core.api.FileMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OmeZarrBlockTileSource implements BlockTileSource<OmeZarrBlockTileKey> {
    private static final Logger log = LoggerFactory.getLogger(OmeZarrBlockTileSource.class);

    private URL originatingSampleURL;
    private String sampleOmeZarrTilesBaseDir;
    private OmeZarrJadeReader reader;
    private OmeZarrGroup omeZarrGroup;

    private ConstVector3 origin;
    private Vector3 outerCorner;

    private AutoContrastParameters autoContrastParameters = null;

    private ArrayList<OmeZarrBlockResolution> resolutions = new ArrayList();
    private OmeZarrBlockResolution maximumResolution = null;

    private ImageColorModel imageColorModel;

    private static final double MAX_BLOCKING_RESOLUTION = 30.0;

    public OmeZarrBlockTileSource() {
    }

    public OmeZarrBlockTileSource(URL originatingSampleURL, ImageColorModel imageColorModel) {
        this.originatingSampleURL = originatingSampleURL;
        this.imageColorModel = imageColorModel;
    }

    public OmeZarrBlockTileSource init(String localPath) throws IOException {
        this.sampleOmeZarrTilesBaseDir = localPath;
        this.reader = null;

        omeZarrGroup = OmeZarrGroup.open(Paths.get(localPath));

        init();

        return this;
    }

    public OmeZarrBlockTileSource init(TmSample sample) throws IOException {
        this.sampleOmeZarrTilesBaseDir = StringUtils.appendIfMissing(sample.getFiles().get(FileType.LargeVolumeZarr),"/");

        this.reader = new OmeZarrJadeReader(FileMgr.getFileMgr().getStorageService(), this.sampleOmeZarrTilesBaseDir);

        omeZarrGroup = OmeZarrGroup.open(new JadeZarrStoreProvider("", reader));

        init();

        return this;
    }

    private void init() {
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
                    origin = new Vector3(0, 0, 0);
                    outerCorner = new Vector3(shape[4] * res.get(2).doubleValue(), shape[3] * res.get(1).doubleValue(), shape[2] * res.get(0).doubleValue());

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

                createTileKeysForDataset(resolution, idx, dataset);

                resolutions.add(resolution);

                if (maximumResolution == null) {
                    maximumResolution = resolutions.get(resolutions.size() - 1);
                }

            } catch (Exception ex) {
                log.info("failed to initialize dataset at index " + idx);
            }
        }
    }

    @Override
    public BlockTileResolution getMaximumResolution() {
        return maximumResolution;
    }

    @Override
    public OmeZarrBlockTileKey getBlockKeyAt(ConstVector3 focus, BlockTileResolution resolution) {
        return ((OmeZarrBlockResolution) resolution).getBlockInfoSet().getBestContainingBrick(focus);
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

    public ImageColorModel getColorModel(){
        return imageColorModel;
    }

    public ArrayList<OmeZarrBlockResolution> getResolutions() {
        return resolutions;
    }

    ConstVector3 getBlockSize(OmeZarrBlockResolution resolution) {
        Vector3 rootBlockSize = outerCorner.minus(origin);
        return rootBlockSize.multiplyScalar(1.0f / resolution.getBlockSizeScale());
    }

    public Texture3d loadBrick(OmeZarrBlockTileKey tile, int colorChannel) {
        // setColorChannelIndex(colorChannel);
        return loadBrick(tile);
    }

    public Texture3d loadBrick(OmeZarrBlockTileKey tile) {
        return tile.loadBrick(autoContrastParameters);
    }

    private void createTileKeysForDataset(OmeZarrBlockResolution resolution, int keyDepth, OmeZarrDataset dataset)  {

        log.info(String.format("creating tile key set for resolution: %.1f", resolution.getResolutionMicrometers()));

        OmeZarrBlockInfoSet blockInfoSet = resolution.getBlockInfoSet();

        int[] chunkSize = resolution.getChunkSize();

        List<OmeZarrBlockTileKey> chunks = createTilesForResolution(dataset, keyDepth, chunkSize[0], chunkSize[1], chunkSize[2]);

        for (OmeZarrBlockTileKey chunk : chunks) {
            blockInfoSet.add(chunk);
        }
    }

    /**
     * Only valid for tczyx OmeZarr datasets.
     *
     * @param dataset
     * @return
     * @throws IOException
     */
    private List<OmeZarrBlockTileKey> createTilesForResolution(OmeZarrDataset dataset, int keyDepth, int xChunkSegment, int yChunkSegment, int zChunkSegment) {
        List<OmeZarrBlockTileKey> brickInfoList = new ArrayList<>();

        try {
            // [t, c, z, y, x]
            int[] shape = dataset.getRawShape();

            if (autoContrastParameters == null) {
                int[] autoContrastShape = {1, 1, 256, 256, 128};

                AutoContrastParameters parameters = TCZYXRasterZStack.computeAutoContrast(dataset, autoContrastShape);

                if (parameters != null) {
                    double existingMax = parameters.min + (65535.0 / parameters.slope);

                    double min = Math.max(100, parameters.min * 0.1);
                    double max = Math.min(65535.0, Math.max(min + 100, existingMax * 4));
                    double slope = 65535.0 / (max - min);

                    autoContrastParameters = new AutoContrastParameters(min, slope);
                }
            }

            // [z, y, x]
            List<Double> spatialShape = dataset.getSpatialResolution(OmeZarrAxisUnit.MICROMETER);

            log.info("chunkSegments for dataset path " + dataset.getPath() + ": " + xChunkSegment + "," + yChunkSegment + "," + zChunkSegment);

            int chunkCount = 0;

            // [x, y, z]
            int[] chunkSize = new int[3];

            for (int xIdx = 0; xIdx < shape[4]; xIdx += xChunkSegment) {
                for (int yIdx = 0; yIdx < shape[3]; yIdx += yChunkSegment) {
                    for (int zIdx = 0; zIdx < shape[2]; zIdx += zChunkSegment) {
                        // [x, y, z]
                        int[] offset = {xIdx, yIdx, zIdx};

                        chunkSize[0] = Math.min(shape[4] - xIdx, xChunkSegment);
                        chunkSize[1] = Math.min(shape[3] - yIdx, yChunkSegment);
                        chunkSize[2] = Math.min(shape[2] - zIdx, zChunkSegment);

                        // [x, y, z]
                        double[] voxelSize = {spatialShape.get(2), spatialShape.get(1), spatialShape.get(0)};

                        // All args [x, y, z]
                        brickInfoList.add(new OmeZarrBlockTileKey(dataset, keyDepth, chunkSize, offset, voxelSize, shape[1]));

                        chunkCount++;
                    }
                }
            }

            log.info(chunkCount + " chunks for dataset path " + dataset.getPath());
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }

        return brickInfoList;
    }
}
