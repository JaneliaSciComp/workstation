package org.janelia.it.workstation.cache.large_volume.stack;

import com.sun.media.jai.codec.ImageDecoder;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.janelia.it.jacs.shared.img_3d_loader.H265FileLoader;
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
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackCacheController {

    private static Logger log = LoggerFactory.getLogger(TileStackCacheController.class);
    private static TileStackCacheController theInstance=new TileStackCacheController();

    private ScheduledThreadPoolExecutor emergencyThreadPool = new ScheduledThreadPoolExecutor(4);
    private ScheduledThreadPoolExecutor fileLoadThreadPool = new ScheduledThreadPoolExecutor(4);

    public static int REQUEST_FILE_DATA_FROM_CACHE_TIMEOUT_MS=120000;
    public static int FILE_DATA_POLL_INTERVAL_MS=50;
    public static final int MIN_RAW_VAL=10000;
    public static final int MAX_RAW_VAL=23000;

    private String remoteBasePath;
    private File topFolder;
    private URL initUrl;
    private int cacheVolumeSize;
    private int sliceSize = -1;
    protected TileFormat tileFormat;
    SimpleFileCache cache;
    Double zoom;
    Vec3 focus;
    TileIndex focusTileIndex;
    File focusStackFile;
    List<File> neighborhoodStackFiles=new ArrayList<>();
    String fileSeparator=null;

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

    public File getTopFolder() {
        return topFolder;
    }

    public boolean isInitialized() { return initialized; }

    private void initFilesystemMetadata() throws DataSourceInitializeException {
        final OctreeMetadataSniffer octreeMetadataSniffer = new OctreeMetadataSniffer(topFolder, tileFormat);
        octreeMetadataSniffer.setRemoteBasePath(remoteBasePath);
        octreeMetadataSniffer.sniffMetadata(topFolder);
        sliceSize = octreeMetadataSniffer.getSliceSize(); // maybe 16-bit
        log.info("initFilesystemMetadata()");
        int[] tileSize=tileFormat.getTileSize();
        int[] volumeSize=tileFormat.getVolumeSize();
        int zoomLevels=tileFormat.getZoomLevelCount();
        cacheVolumeSize=tileSize[0]*tileSize[1]*tileSize[2]*tileFormat.getChannelCount(); // always 8-bit
        cache=new SimpleFileCache(100, cacheVolumeSize);
        log.info("Tile size="+tileSize[0]+" "+tileSize[1]+" "+tileSize[2]);
        log.info("Volume size="+volumeSize[0]+" "+volumeSize[1]+" "+volumeSize[2]);
        log.info("zoom Levels="+zoomLevels);
        filesystemMetadataInitialized=true;
    }


    public void setZoom(Double zoom) {
        //log.info("setZoom="+zoom);
        this.zoom=zoom;
        updateFocusTileIndex();
    }

    public void setFocus(Vec3 focus) {
        //log.info("setFocus="+focus.toString());
        this.focus=focus;
        updateFocusTileIndex();
    }

    private synchronized void updateFocusTileIndex() {
        if (zoom!=null && focus!=null) {

            // Update focus tile/stack
            focusTileIndex = tileFormat.tileIndexForXyz(focus, tileFormat.zoomLevelForCameraZoom(zoom), CoordinateAxis.Z);
            focusStackFile = getStackFileForTileIndex(focusTileIndex);

            log.info("focusStackFile="+focusStackFile.getAbsolutePath());

            // Update neighborhood
            neighborhoodStackFiles.clear();

            for (int zOffset=-1;zOffset<2;zOffset++) {
                for (int yOffset = -1; yOffset < 2; yOffset++) {
                    for (int xOffset = -1; xOffset < 2; xOffset++) {
                        TileIndex offsetTileIndex = new TileIndex(focusTileIndex.getX() + xOffset, focusTileIndex.getY() + yOffset, focusTileIndex.getZ()+(zOffset*tileFormat.getTileSize()[2]), focusTileIndex.getZoom(), focusTileIndex.getMaxZoom(),
                                focusTileIndex.getIndexStyle(), focusTileIndex.getSliceAxis());
                        File octreeFile=OctreeMetadataSniffer.getOctreeFilePath(offsetTileIndex, tileFormat, true);
                        if (octreeFile!=null) {
                            neighborhoodStackFiles.add(new File(topFolder, octreeFile.toString()));
                        }
                    }
                }
            }

            for (File nf : neighborhoodStackFiles) {
                int sCount = 0;
                //log.info("Neighborhood file="+nf.getAbsolutePath());
                if (cache.getBytes(nf) == null) {
                    if (nf.equals(focusStackFile)) {
                        emergencyThreadPool.schedule(new FileLoader(nf, this), 0, TimeUnit.MILLISECONDS);
                        sCount++;
                    } else {
                        fileLoadThreadPool.schedule(new FileLoader(nf, this), 2, TimeUnit.MILLISECONDS);
                        sCount++;
                    }
                }
                log.info("Added "+sCount+" stack loads. Emergency="+getEmergencyPoolCount()+" Normal="+getFileLoadPoolCount());
            }
        }
    }

    public int getEmergencyPoolCount() {
        return emergencyThreadPool.getActiveCount()+emergencyThreadPool.getQueue().size();
    }

    public int getFileLoadPoolCount() {
        return fileLoadThreadPool.getActiveCount()+fileLoadThreadPool.getQueue().size();
    }

    public List<File> getNeighborhoodStackFiles() {
        return neighborhoodStackFiles;
    }

//    private File getNeighborhoodFileByOffsetAtConstantZoom(File file, int x, int y, int z) {
//        List<Integer> oct
//
//    }

//    private List<Integer> getOctantListFromFilename(String filename) {
//        List<Integer> octantList=new ArrayList<>();
//        String[] cList=filename.split(getFileSeparator());
//        for (int c=cList.length-1;c>0;c--) {
//            Integer iVal=null;
//            try { iVal=new Integer(cList[c]); } catch (Exception ex) {}
//            if (iVal!=null && iVal>0 && iVal<9) {
//                octantList.add(iVal);
//            } else {
//                break;
//            }
//        }
//        return octantList;
//    }

    public void shutdown() {

    }

    public SimpleFileCache getCache() {
        return cache;
    }

    public int getCacheVolumeSize() {
        return cacheVolumeSize;
    }

    public TileFormat getTileFormat() {
        return tileFormat;
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
        File stackFile=getStackFileForTileIndex(tileIndex);

        //log.info("loadToRam() calling getStackSliceFromTileIndex() for stackFile="+stackFile.getAbsolutePath());
        int stackSlice=getStackSliceFromTileIndex(tileIndex);
        //log.info("loadToRam() stackSlice="+stackSlice+", calling getFileDataFromCache()");
        ByteBuffer stackData=getFileDataFromCache(stackFile);
        if (stackData==null || stackData.capacity()<3) {
            throw new AbstractTextureLoadAdapter.TileLoadError("stackData is null");
        }
        return createTextureData2dGLFromCacheVolume(stackData, stackSlice);

        //return createDebugTileFromTileIndex(tileIndex);
    }

    public File getStackFileForTileIndex(TileIndex tileIndex) {
        final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, true /*zOriginNegativeShift*/);
        if (octreeFilePath == null) {
            log.error("Received null octreeFilePath for file="+octreeFilePath.getAbsolutePath());
            return null;
        }

        File stackFile = new File(topFolder, octreeFilePath.toString());
        return stackFile;
    }

    private TextureData2dGL createTextureData2dGLFromCacheVolume(ByteBuffer stackBuffer, int zSlice) {
        int[] tileSize=tileFormat.getTileSize();
        int sliceSize=tileSize[0]*tileSize[1];
        int channelSize=sliceSize*tileSize[2];
        byte[] stackBytes=stackBuffer.array();
        List<BufferedImage> imageList=new ArrayList<>();
        for (int c=0;c<tileFormat.getChannelCount();c++) {
            BufferedImage bufferedImage=new BufferedImage(tileSize[0], tileSize[1], BufferedImage.TYPE_USHORT_GRAY);
            Raster imageCopy=bufferedImage.getData();
            DataBuffer dataBuffer=imageCopy.getDataBuffer();
            int cOffset=channelSize*c;
            int zOffset=sliceSize*zSlice;
            for (int y=0;y<tileSize[1];y++) {
                int yOffset=y*tileSize[0];
                int czyOffset=cOffset+zOffset+yOffset;
                for (int x=0;x<tileSize[0];x++) {
                    int stackValue = ((stackBytes[czyOffset+x]+128)*(MAX_RAW_VAL-MIN_RAW_VAL))/256 + MIN_RAW_VAL;
                    dataBuffer.setElem(yOffset+x, stackValue);
                }
            }
            bufferedImage.setData(imageCopy);
            imageList.add(bufferedImage);
        }
        return getCombinedTextureData2dGLFromRenderedImages(imageList);
    }

    private ByteBuffer getFileDataFromCache(File file) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException {
        ByteBuffer data=cache.getBytes(file);
        if (data==null) {
            //log.info("getFileDataFromCache() data is null, proceeding with FileLoader submission to fileLoadThreadPool()");
            if (!TileStackCacheController.FileLoader.currentFilesBeingLoaded.contains(file)) {
                if (file.getAbsolutePath().equals(focusStackFile.getAbsolutePath())) {
                    emergencyThreadPool.schedule(new FileLoader(file, this), 0, TimeUnit.MILLISECONDS);
                } else {
                    fileLoadThreadPool.schedule(new FileLoader(file, this), 2, TimeUnit.MILLISECONDS);
                }
            }
            int maxCheckCount=REQUEST_FILE_DATA_FROM_CACHE_TIMEOUT_MS / FILE_DATA_POLL_INTERVAL_MS;
            int checkCount=0;
            while (data==null && checkCount<maxCheckCount) {
                try { Thread.sleep(FILE_DATA_POLL_INTERVAL_MS); } catch (Exception ex) {}
                data=cache.getBytes(file);
                checkCount++;
            }
            if (checkCount==maxCheckCount) {
                String errorString = "Request File data from cache timeout for file="+file.getAbsolutePath();
                log.error(errorString);
                throw new AbstractTextureLoadAdapter.TileLoadError(errorString);
            }
            if (data.capacity()==0) {
                cache.putBytes(file, null);
                throw new AbstractTextureLoadAdapter.MissingTileException("ByteBuffer empty for file="+file.getAbsolutePath());
            } else if (data.capacity()==1) {
                cache.putBytes(file, null);
                throw new AbstractTextureLoadAdapter.TileLoadError("TileLoadError for file="+file.getAbsolutePath());
            }
        } else {
           // log.info("getFileDataFromCache() data is not null, returning cached stack ByteBuffer");
        }
        return data;
    }

    private static class FileLoader implements Runnable {
        private File file;
        private TileStackCacheController tileStackCacheController;
        private static ConcurrentHashSet<File> currentFilesBeingLoaded=new ConcurrentHashSet<>();

        public static ConcurrentHashSet<File> getCurrentFilesBeingLoaded() {
            return currentFilesBeingLoaded;
        }

        public FileLoader(File file, TileStackCacheController tileStackCacheController) {
            this.file=file;
            this.tileStackCacheController=tileStackCacheController;
        }

        @Override
        public void run() {

            log.info("***>>> FileLoader run()");
            // Am I already done?
            SimpleFileCache cache=tileStackCacheController.getCache();
            ByteBuffer data=cache.getBytes(file);
            if (data!=null) {
                log.info("***>>> FileLoader data already populated for file="+file.getAbsolutePath());
                return;
            }

            // Is someone else already loading this file, in which case I can just let them do it?
            if (currentFilesBeingLoaded.contains(file)) {
                log.info("***>>> FileLoader file already being loaded="+file.getAbsolutePath());
                return;
            }

            // Am I out of the neighborhood?
            if (!inTheNeighborhood(file)) {
                log.info("***>>> FileLoader skipping file because out of neighborhood="+file.getAbsolutePath());
                ByteBuffer errorBuffer=ByteBuffer.wrap(new byte[1]);
                try { cache.putBytes(file, errorBuffer); } catch (Exception ex) {}
                return;
            }

            // Apparently not, so I will take responsibility
            currentFilesBeingLoaded.add(file);

            log.info("***>>> FileLoader loading file="+file.getAbsolutePath());
            long loadStart=System.currentTimeMillis();

            TileFormat tileFormat=tileStackCacheController.getTileFormat();
            int[] tileSize=tileFormat.getTileSize();
            byte[] volumeData=new byte[tileStackCacheController.getCacheVolumeSize()];
            int channelCount=tileFormat.getChannelCount();
            boolean errorFlag=false;
            try {
                loadTiffToByteArray(file, volumeData, channelCount, tileSize);

                //System.out.println("Has JavaCPP? " + System.getProperty("java.class.path").contains("javacpp"));
                //File[] h5jFiles=convertTiffFilenameToH5j(file);
                //loadH5JToByteArray(h5jFiles, volumeData, channelCount, tileSize);

                //log.info("***>>> Slice Values: min="+minSliceValue+" max="+maxSliceValue+"  bmin="+minByteValue+" bmax="+maxByteValue);
            } catch (AbstractTextureLoadAdapter.MissingTileException mte) {
                errorFlag=true;
                log.error("***>>> FileLoader MissingTileException");
                ByteBuffer emptyBuffer=ByteBuffer.wrap(new byte[0]); // to be interpreted as missing
                try { cache.putBytes(file, emptyBuffer); } catch (Exception ex) {}
            } catch (AbstractTextureLoadAdapter.TileLoadError tle) {
                errorFlag=true;
                log.error("***>>> FileLoader TileLoadError");
                ByteBuffer errorBuffer=ByteBuffer.wrap(new byte[1]);
                try { cache.putBytes(file, errorBuffer); } catch (Exception ex) {}
            } catch (IOException iex) {
                errorFlag=true;
                log.error("***>>> FileLoader IOException");
                ByteBuffer errorBuffer=ByteBuffer.wrap(new byte[1]);
                try { cache.putBytes(file, errorBuffer); } catch (Exception ex2) {}
            }
            //log.info("***>>> FileLoader wrapping volumeData");
            if (!errorFlag) {
                ByteBuffer cacheData = ByteBuffer.wrap(volumeData);
                try {
                    long loadTime = System.currentTimeMillis() - loadStart;
                    log.info("***>>> FileLoader done loading file=" + file.getAbsolutePath() + " in " + loadTime + " ms");
                    cache.putBytes(file, cacheData);
                }
                catch (AbstractTextureLoadAdapter.TileLoadError tileLoadError) {
                    log.error("***>>> FileLoader error calling cache.putBytes()");
                    ByteBuffer errorBuffer = ByteBuffer.wrap(new byte[1]);
                    try {
                        cache.putBytes(file, errorBuffer);
                    }
                    catch (Exception ex) {
                    }
                }
            }
            currentFilesBeingLoaded.remove(file);
            if (!errorFlag) {
                log.info("FileLoader returning after loading file="+file.getAbsolutePath());
            } else {
                log.info("FileLoader did not succeed with file="+file.getAbsolutePath());
            }
            return;
        }

        private void loadTiffToByteArray(File file, byte[] volumeData, int channelCount, int[] tileSize) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException, IOException {
            int sliceSize=tileSize[0]*tileSize[1];
            int channelSize=sliceSize*tileSize[2];
            ImageDecoder[] decoders = BlockTiffOctreeLoadAdapter.createImageDecoders(file, CoordinateAxis.Z, true, channelCount);
            RenderedImage[] channels = new RenderedImage[channelCount];
            for (int z=0;z<tileSize[2];z++) {
                // Is there an emergency?
                if (!file.equals(tileStackCacheController.focusStackFile)) {
                    if (tileStackCacheController.getEmergencyPoolCount() > 0) {
                        try {
                            Thread.sleep(20);
                        }
                        catch (Exception ex) {
                        }
                    }
                }
//                // Am I out of the neighborhood?
                if (!inTheNeighborhood(file)) {
                    log.info("file="+file.getAbsolutePath()+" is not in the neighborhood");
                    throw new AbstractTextureLoadAdapter.TileLoadError("Out of neighborhood");
                }
                //log.info("***>>> FileLoader loading zSlize="+z+" for file="+file.getAbsolutePath());
                for (int c = 0; c < channels.length; c++) {
                    ImageDecoder decoder = decoders[c];
                    channels[c] = decoder.decodeAsRenderedImage(z);
                }
                for (int c=0;c<channels.length;c++) {
                    DataBuffer sliceData=channels[c].getData().getDataBuffer();
                    int zOffset=channelSize*c+z*sliceSize;
                    for (int y=0;y<tileSize[1];y++) {
                        int yOffset=y*tileSize[0];
                        for (int x=0;x<tileSize[0];x++) {
                            int zyOffset=zOffset+yOffset;
                            int sVal=sliceData.getElem(yOffset+x);
                            int iVal=((sVal-MIN_RAW_VAL)*256)/(MAX_RAW_VAL-MIN_RAW_VAL) - 128;
                            if (iVal>127) {
                                iVal=127;
                            } else if(iVal<-128) {
                                iVal=-128;
                            }
                            volumeData[zyOffset+x]=(byte)iVal;
                        }
                    }
                }
            }
        }

        private boolean inTheNeighborhood(File file) {
            List<File> neighborhoodFiles=tileStackCacheController.getNeighborhoodStackFiles();
            if (neighborhoodFiles!=null && neighborhoodFiles.size()==27 && (!neighborhoodFiles.contains(file))) {
                return false;
            }
            return true;
        }

        private void loadH5JToByteArray(File[] files, byte[] volumeData, int channelCount, int[] tileSize)throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException, IOException {
            H265FileLoader loader = new H265FileLoader();
//            loader.saveFramesAsPPM(filename);
            try {
                log.info("Trying to load="+files[0].getAbsolutePath());
                loader.loadVolumeFile(files[0].getAbsolutePath());
                int pixelBytes=loader.getPixelBytes();
                int sx=loader.getSx();
                int sy=loader.getSy();
                int sz=loader.getSz();
                byte[] h5jBytes=loader.getTextureByteArray();
                boolean h5jBytesNull=false;
                if (h5jBytes==null) {
                    h5jBytesNull=true;
                }
                log.info("pixelBytes="+pixelBytes+" sx="+sx+" sy="+sy+" sz="+sz+" h5jBytesNull="+h5jBytesNull);
                log.info("Done trying to load="+files[0].getAbsolutePath());
            } catch (Exception ex) {
                log.error("Exception loading H5J: "+ex.toString());
                ex.printStackTrace();
            }
        }

        private File[] convertTiffFilenameToH5j(File file) {
            File topFolder=tileStackCacheController.getTopFolder();
            String topFolderPath=topFolder.getAbsolutePath();
            int lastSeparatorPosition=0;
            String fileSeparator=tileStackCacheController.getFileSeparator();
            lastSeparatorPosition=topFolderPath.lastIndexOf(fileSeparator);
            String part1=topFolderPath.substring(0,lastSeparatorPosition);
            String part2=topFolderPath.substring(lastSeparatorPosition+1,topFolderPath.length());
            String h5jTopFolderPath=part1+fileSeparator+"h5j-"+part2;
            String fileFullPath=file.getAbsolutePath();
            String afterTopFolderPart=fileFullPath.substring(topFolderPath.length(), fileFullPath.length());
            afterTopFolderPart.replace(".tif", ".h5j");
            String h5jPath=h5jTopFolderPath+afterTopFolderPart;
            File[] h5jArr=new File[2];
            h5jArr[0]=new File(h5jPath+fileSeparator+"default.0.h5j");
            h5jArr[1]=new File(h5jPath+fileSeparator+"default.1.h5j");
            return h5jArr;
        }

    }

    private String getFileSeparator() {
        if (fileSeparator==null) {
            String topFolderPath=topFolder.getAbsolutePath();
            if (topFolderPath.contains("\\")) {
                fileSeparator="\\";
            } else if (topFolderPath.contains("/")) {
                fileSeparator="/";
            }
        }
        return fileSeparator;
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
