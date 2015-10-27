package org.janelia.it.workstation.cache.large_volume.stack;

import com.sun.media.jai.codec.ImageDecoder;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.*;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackCacheController {

    private static Logger log = LoggerFactory.getLogger(TileStackCacheController.class);
    private static TileStackCacheController theInstance=new TileStackCacheController();

    private String remoteBasePath;
    private File topFolder;
    private URL initUrl;
    private int standardVolumeSize;
    private int sliceSize = -1;
    protected TileFormat tileFormat;
    SimpleFileCache cache;
    Double zoom;
    Vec3 focus;
    TileIndex focusTileIndex;

    boolean filesystemMetadataInitialized=false;
    boolean initialized=false;

    public static TileStackCacheController getInstance() {
        return theInstance;
    }

    public void setTileFormat(TileFormat tileFormat) {
        this.tileFormat=tileFormat;
    }

    public void setFilesystemConfiguration(String remoteBasePath, File topFolder) throws DataSourceInitializeException {
        this.remoteBasePath=remoteBasePath;
        this.topFolder=topFolder;
        initFilesystemMetadata();
    }

    public void init(URL url) throws Exception {
        this.initUrl=url;
        if (!initialized && filesystemMetadataInitialized) {
            init();
        } else {
            log.error("Expect filesystemMetadataInitialized before init() called");
        }
    }

    private void init() {

    }

    public boolean isInitialized() { return initialized; }

    private void initFilesystemMetadata() throws DataSourceInitializeException {
        final OctreeMetadataSniffer octreeMetadataSniffer = new OctreeMetadataSniffer(topFolder, tileFormat);
        octreeMetadataSniffer.setRemoteBasePath(remoteBasePath);
        octreeMetadataSniffer.sniffMetadata(topFolder);
        standardVolumeSize = octreeMetadataSniffer.getStandardVolumeSize();
        sliceSize = octreeMetadataSniffer.getSliceSize();
        log.info("initFilesystemMetadata()");
        int[] tileSize=tileFormat.getTileSize();
        int[] volumeSize=tileFormat.getVolumeSize();
        int zoomLevels=tileFormat.getZoomLevelCount();
        log.info("Tile size="+tileSize[0]+" "+tileSize[1]+" "+tileSize[2]);
        log.info("Volume size="+volumeSize[0]+" "+volumeSize[1]+" "+volumeSize[2]);
        log.info("zoom Levels="+zoomLevels);
        filesystemMetadataInitialized=true;
    }


    public void setZoom(Double zoom) {
        log.info("setZoom="+zoom);
        this.zoom=zoom;
        updateFocusTileIndex();
    }

    public void setFocus(Vec3 focus) {
        log.info("setFocus="+focus.toString());
        this.focus=focus;
        updateFocusTileIndex();
    }

    private synchronized void updateFocusTileIndex() {
        if (zoom!=null && focus!=null) {
            focusTileIndex = tileFormat.tileIndexForXyz(focus, tileFormat.zoomLevelForCameraZoom(zoom), CoordinateAxis.Z);
            log.info("Current tile=" + focusTileIndex.toString());
        }
    }

    public void shutdown() {

    }

    private int getStackSliceFromTileIndex(TileIndex tileIndex) {
        int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
        int axisIx = tileIndex.getSliceAxis().index();
        int tileDepth = tileFormat.getTileSize()[axisIx];
        int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
        int relativeSlice = absoluteSlice % tileDepth;
        // Raveller y is flipped so flip when slicing in Y (right?)
        if (axisIx == 1)
            relativeSlice = tileDepth - relativeSlice - 1;
        return relativeSlice;
    }

    public TextureData2dGL loadToRam(TileIndex tileIndex) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException {
        //log.info("loadToRam() request tileIndex="+tileIndex.getX()+" "+tileIndex.getY()+" "+tileIndex.getZ()+" zoom="+tileIndex.getZoom());
        //try { Thread.sleep(1000L); } catch (Exception ex) {}

//        final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, true /*zOriginNegativeShift*/);
//        if (octreeFilePath == null) {
//            return null;
//        }
//
//        File stackFile = new File(topFolder, octreeFilePath.toString());
//        int stackSlice=getStackSliceFromTileIndex(tileIndex);
//
//        ByteBuffer stackData=getFileDataFromCache(stackFile);
//
//        // Calling this with "true" means I, the caller, accept that the array
//        // returned may have one or more nulls in it.
//        ImageDecoder[] decoders = createImageDecoders(folder, tileIndex.getSliceAxis(), true, tileFormat.getChannelCount());
//
//        // log.info(tileIndex + "" + folder + " : " + relativeSlice);
//        TextureData2dGL result = loadSlice(relativeSlice, decoders, tileFormat.getChannelCount());
//        localLoadTimer.mark("finished slice load");




        return createDebugTileFromTileIndex(tileIndex);
    }

    private ByteBuffer getFileDataFromCache(File file) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException {
        ByteBuffer data=cache.getBytes(file);
        if (data==null) {
            data=getDataFromFile(file);
            if (data!=null) {
                cache.putBytes(file, data);
            } else {
                throw new AbstractTextureLoadAdapter.TileLoadError("null returned from getDataFromFile() file="+file.getAbsolutePath());
            }
        }
        return data;
    }

    private ByteBuffer getDataFromFile(File file) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException {
        return null;
    }

    private TextureData2dGL createDebugTileFromTileIndex(TileIndex tileIndex) {
        int[] tileSize = tileFormat.getTileSize();

        BufferedImage bufferedImage1 = new BufferedImage(tileSize[0], tileSize[1], BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics2D1 = bufferedImage1.createGraphics();
        graphics2D1.setColor(Color.WHITE);
        graphics2D1.fillRect(0, 0, 10, 10);
        graphics2D1.drawString("" + tileIndex.getX() + ", " + tileIndex.getY() + ", " + tileIndex.getZ() + ", " + tileIndex.getZoom(), 50, 50);

        BufferedImage bufferedImage2 = new BufferedImage(tileSize[0], tileSize[1], BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics2D2 = bufferedImage2.createGraphics();
        graphics2D2.setColor(Color.WHITE);
        graphics2D2.fillRect(tileSize[0] - 11, tileSize[1] - 11, 10, 10);
        graphics2D2.drawString("" + tileIndex.getX() + ", " + tileIndex.getY() + ", " + tileIndex.getZ() + ", " + tileIndex.getZoom(), 200, 200);

        List<BufferedImage> imageList = new ArrayList<>();
        imageList.add(bufferedImage1);
        imageList.add(bufferedImage2);

        TextureData2dGL textureData2dGL = getCombinedTextureData2dGLFromRenderedImages(imageList);
        return textureData2dGL;
    }

    private TextureData2dGL getCombinedTextureData2dGLFromRenderedImages(List<BufferedImage> imageList) {
        try {
            ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
            for (BufferedImage image : imageList) {
                pb.addSource(image);
            }
            RenderedImage composite = JAI.create("bandmerge", pb);
            TextureData2dGL result = new TextureData2dGL();
            result.loadRenderedImage(composite);
            return result;
        } catch (NoClassDefFoundError exc) {
            exc.printStackTrace();
            return null;
        }
    }

}
