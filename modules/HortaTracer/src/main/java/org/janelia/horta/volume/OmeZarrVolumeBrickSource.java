package org.janelia.horta.volume;

import com.google.common.collect.ImmutableSet;
import org.aind.omezarr.OmeZarrAxisUnit;
import org.aind.omezarr.OmeZarrDataset;
import org.aind.omezarr.OmeZarrGroup;
import org.aind.omezarr.image.AutoContrastParameters;
import org.aind.omezarr.image.TCZYXRasterZStack;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * StaticVolumeBrickSource for Ome-Zarr datasets.
 */
public class OmeZarrVolumeBrickSource implements StaticVolumeBrickSource {
    private final ArrayList<Double> resolutionsMicrometers;

    private final Map<Double, BrickInfoSet> brickInfoSets;

    private final Map<String, AutoContrastParameters> autoContrastMap = new HashMap<>();

    private final int minDataset = 7;

    public OmeZarrVolumeBrickSource(Path path) throws IOException {
        OmeZarrGroup fileset = OmeZarrGroup.open(path);

        int datasetCount = fileset.getAttributes().getMultiscales()[0].getDatasets().size();

        resolutionsMicrometers = new ArrayList<>();

        brickInfoSets = new HashMap<>();

        for (int idx = minDataset; idx < datasetCount; idx++) {
            OmeZarrDataset dataset = fileset.getAttributes().getMultiscales()[0].getDatasets().get(idx);

            if (!dataset.isValid()) {
                continue;
            }

            Pair<Double, BrickInfoSet> pair = createBricksetForDataset(dataset);

            resolutionsMicrometers.add(pair.getLeft());

            brickInfoSets.put(pair.getLeft(), pair.getRight());
        }
    }

    @Override
    public Collection<Double> getAvailableResolutions() {
        return resolutionsMicrometers;
    }

    // TODO temp
    private Double lastResolution;

    @Override
    public BrickInfoSet getAllBrickInfoForResolution(Double resolution) {
        if (resolution != lastResolution) {
            System.out.println(String.format("requesting resolution %.1f", resolution.floatValue()));
            lastResolution = resolution;
        }

        return brickInfoSets.get(resolution);
    }

    @Override
    public FileType getFileType() {
        return FileType.ZARR;
    }

    private Pair<Double, BrickInfoSet> createBricksetForDataset(OmeZarrDataset dataset) throws IOException {

        double resolutionMicrometers = dataset.getMinSpatialResolution();

        System.out.println(String.format("creating brickset for resolution: %.1f", resolutionMicrometers));

        BrickInfoSet brickInfoSet = new BrickInfoSet();

        List<BrainChunkInfo> chunks = createTilesForResolution(dataset);

        for (BrainChunkInfo chunk : chunks) {
            brickInfoSet.add(chunk);
        }

        return Pair.of(resolutionMicrometers, brickInfoSet);
    }

    private final int chunkSegment = 512;

    /**
     * Only valid for tczyx OmeZarr datasets.
     *
     * @param dataset
     * @return
     * @throws IOException
     */
    private List<BrainChunkInfo> createTilesForResolution(OmeZarrDataset dataset) throws IOException {
        List<BrainChunkInfo> brickInfoList = new ArrayList<>();

        // [t, c, z, y, x]
        int[] shape = dataset.getShape();

        // [x, y, z]
        int[] chunkSize = {shape[4], shape[3], shape[2]};

        int[] autoContrastShape = {1, 1, 512,512, 512};

        AutoContrastParameters parameters = TCZYXRasterZStack.computeAutoContrast(dataset, autoContrastShape);

        autoContrastMap.put(dataset.toString(), parameters);

        // Raw TIFF chunks are 1024 x 1536 x 251 for reference (~400M voxels) or 350 x 450 x 250 um (~150k um3).
        for (int xIdx = 0; xIdx < shape[4]; xIdx += chunkSegment) {
            for (int yIdx = 0; yIdx < shape[3]; yIdx += chunkSegment) {

                // [x, y, z]
                int[] offset = {xIdx, yIdx, 0};

                // [z, y, x]
                List<Double> spatialShape = dataset.getSpatialResolution(OmeZarrAxisUnit.MICROMETER);

                chunkSize[0] = Math.min(shape[4] - xIdx, chunkSegment);
                chunkSize[1] = Math.min(shape[3] - yIdx, chunkSegment);

                // [x, y, z]
                double[] voxelSize = {spatialShape.get(2), spatialShape.get(1), spatialShape.get(0)};

                // All args [x, y, z]
                brickInfoList.add(new BrainChunkInfo(dataset, chunkSize, offset, voxelSize, shape[1], parameters));
            }
        }

        return brickInfoList;
    }
}
