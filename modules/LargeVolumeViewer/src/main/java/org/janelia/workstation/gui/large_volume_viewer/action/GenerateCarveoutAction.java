package org.janelia.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import org.apache.commons.io.FileUtils;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.rendering.Streamable;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.access.TiledMicroscopeRestClient;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.tileimagery.*;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.IndeterminateNoteProgressMonitor;
import org.janelia.workstation.geom.CoordinateAxis;
import org.janelia.workstation.geom.Vec3;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.octree.ZoomLevel;
import org.janelia.workstation.octree.ZoomedVoxelIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a carveout of the TIFF files within a certain radius from the vertices in a neuron
 */
public class GenerateCarveoutAction extends AbstractAction {
    private final Logger log = LoggerFactory.getLogger(GenerateCarveoutAction.class);
    private final RenderedVolumeLocation renderedVolumeLocation;
    private final static String FILE_SEP = System.getProperty("file.separator");
    private final static String LINUX_FILE_SEP = "/";
    String serverURL = ConsoleProperties.getInstance().getProperty("jadestorage.rest.url");
    TileFormat tileFormat;
    List<TmGeoAnnotation> vertices;
    HashMap<String, HashMap> tileSet;

    public GenerateCarveoutAction(
            TileFormat tileFormat,
            RenderedVolumeLocation renderedVolumeLocation
    ) {
        this.tileFormat = tileFormat;
        putValue(Action.NAME, "Generate Carveout of Selected Neuron");
        this.renderedVolumeLocation = renderedVolumeLocation;
    }

    public void getCarveoutStack(TmNeuronMetadata neuron) {
        BackgroundWorker saver = new BackgroundWorker() {
            @Override
            public String getName() {
                return "Generating Carveout Stack";
            }

            @Override
            protected void doStuff() throws Exception {
                // specify save directory for all these files
                List<Long> root = neuron.getNeuronData().getRootAnnotationIds();
                final int requiredZoomLevel = 0;
                String[][][] tileCube = new String[3][3][3];
                HashSet<String> finalTileSet = new HashSet<>();
                for (Long rootId: root) {
                    TmGeoAnnotation rootNode = neuron.getGeoAnnotationMap().get(rootId);
                    float[] convVec = TmModelManager.getInstance().getLocationInMicrometers(rootNode.getX(),
                            rootNode.getY(), rootNode.getZ());

                    int cubeIndex = 0;
                    int x=0, y=0, z=0;
                    for (float xind = convVec[0] - 50; xind <= convVec[0] + 50; xind += 50) {
                        for (float yind = convVec[1] - 50; yind <= convVec[1] + 50; yind += 50) {
                            for (float zind = convVec[2] - 150; zind <= convVec[2] + 150; zind += 150) {
                                Vec3 vertexLoc = new Vec3(xind, yind, zind);
                                TileIndex index = tileFormat.tileIndexForXyz(vertexLoc, requiredZoomLevel, CoordinateAxis.Z);
                                Path path = FileBasedOctreeMetadataSniffer.getOctreeFilePath(index, tileFormat);
                                String filePathStr = path.toString().replace(FILE_SEP, LINUX_FILE_SEP) + "/default.0.tif";
                                tileCube[x][y][z] = filePathStr;
                                z++;
                            }
                            z = 0;
                            y++;
                        }
                        y = 0;
                        x++;
                    }

                    String baseUrl = renderedVolumeLocation.getBaseStorageLocationURI().toString().replace("/SAMPLES","/data_content/SAMPLES");
                    Client httpClient = RestJsonClientManager.getInstance().getHttpClient(true);
                    for (int xind = 0; xind<3; xind++) {
                        for (int yind = 0; yind < 3; yind++) {
                            for (int zind = 0; zind < 3; zind++) {
                                if (finalTileSet.contains(tileCube[xind][yind][zind]))
                                    continue;
                                WebTarget target = httpClient.target(baseUrl)
                                        .path(tileCube[xind][yind][zind]);
                                Response response = target.request()
                                        .get();
                                int responseStatus = response.getStatus();
                                if (responseStatus == Response.Status.OK.getStatusCode()) {

                                    File targetFile = new File("C:/mouselightwow/foo/" + xind+
                                            "_" + yind + "_" + zind + ".tif");
                                    InputStream is = (InputStream) response.getEntity();
                                    FileUtils.copyInputStreamToFile(is, targetFile);
                                }
                                finalTileSet.add(tileCube[xind][yind][zind]);
                            }
                        }
                    }

                    /*TIFFEncodeParam params = new TIFFEncodeParam();
                    Iterator<BufferedImage> it = Iterators.forArray(outSlices);
                    if (it.hasNext()) it.next(); // Avoid duplicate first slice
                    params.setExtraImages(it);
                    OutputStream out = new FileOutputStream(new File("C:/mouselightwow/carveouts/carveout.tiff"));
                    ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", out, params);
                    encoder.encode(outSlices[0]);
                    out.close();*/
                }
                /*tileSet = new HashMap<>();
                final int requiredZoomLevel = 0;
                List<TmGeoAnnotation> vertices = neuron.getNeuronData().getGeoAnnotations();
                HashMap<String, CarveoutMeta> carveoutMeta = new HashMap<>();
                for (TmGeoAnnotation vertex: vertices) {
                    float[] convVec = TmModelManager.getInstance().getLocationInMicrometers(vertex.getX(), vertex.getY(), vertex.getZ());
                    Vec3 vertexLoc = new Vec3(convVec[0], convVec[1], convVec[2]);
                    TileIndex index = tileFormat.tileIndexForXyz(vertexLoc, requiredZoomLevel, CoordinateAxis.Z);
                    Path path = FileBasedOctreeMetadataSniffer.getOctreeFilePath(index, tileFormat);
                    String filePathStr = path.toString().replace(FILE_SEP, LINUX_FILE_SEP) + "/default.0.tif";
                    String baseUrl = renderedVolumeLocation.getBaseStorageLocationURI().toString().replace("/SAMPLES","/data_content/SAMPLES");

                    CarveoutMeta newEntry;
                    if (carveoutMeta.containsKey(filePathStr)) {
                        newEntry = carveoutMeta.get(filePathStr);
                        newEntry.addVertex(vertexLoc);
                    } else {
                        newEntry = new CarveoutMeta();
                        newEntry.tileIndex = index;
                        newEntry.relativePath = filePathStr;
                        newEntry.baseUrl = baseUrl;
                        carveoutMeta.put(filePathStr, newEntry);
                    }
                }

                Client httpClient = RestJsonClientManager.getInstance().getHttpClient(true);
                ObjectMapper metaSerializer = new ObjectMapper();
                JsonGenerator g = metaSerializer.getFactory().createGenerator(
                        new FileOutputStream(new File("C:/mouselightwow/foo/info.json")));

                for (CarveoutMeta meta: carveoutMeta.values()) {
                    metaSerializer.writeValue(g, meta);
                    WebTarget target = httpClient.target(meta.baseUrl)
                            .path(meta.relativePath);
                    Response response = target.request()
                            .get();
                    int responseStatus = response.getStatus();
                    if (responseStatus == Response.Status.OK.getStatusCode()) {
                        File targetFile = new File("C:/mouselightwow/foo/" + meta.tileIndex.getX()+
                                "_" + meta.tileIndex.getY() + "_" + meta.tileIndex.getZ() + ".tif");
                        InputStream is = (InputStream) response.getEntity();
                        FileUtils.copyInputStreamToFile(is, targetFile);
                    }
                }*/


            }
        };
        saver.executeWithEvents();
    }

    class CarveoutMeta {
        public String baseUrl;
        public String relativePath;
        public TileIndex tileIndex;
        List<Vec3> vertices = new ArrayList<>();

        public void addVertex (Vec3 vertex) {
            vertices.add(vertex);
        }


    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TmNeuronMetadata selectedNeuron = TmModelManager.getInstance().getCurrentSelections().getCurrentNeuron();
        getCarveoutStack(selectedNeuron);
    }
}
