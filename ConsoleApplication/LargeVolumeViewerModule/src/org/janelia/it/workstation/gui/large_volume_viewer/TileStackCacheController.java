package org.janelia.it.workstation.gui.large_volume_viewer;

/**
 * Created by murphys on 11/6/2015.
 */
import com.sun.media.jai.codec.ImageDecoder;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.shared.ffmpeg.H5JLoader;
import org.janelia.it.jacs.shared.ffmpeg.ImageStack;
import org.janelia.it.jacs.shared.lvv.*;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.exception.DataSourceInitializeException;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackCacheController {

    private static Logger log = LoggerFactory.getLogger(TileStackCacheController.class);
    private static TileStackCacheController theInstance=new TileStackCacheController();
    private static final CategoryString LTT_CATEGORY_STRING = new CategoryString("loadTileTiffToRam");
    private static final CategoryString LTT_SESSION_CATEGORY_STRING = new CategoryString("openWholeTifFolder");
    
    private static ScheduledThreadPoolExecutor emergencyThreadPool = new ScheduledThreadPoolExecutor(4);
    private static ScheduledThreadPoolExecutor fileLoadThreadPool = new ScheduledThreadPoolExecutor(4);

    public static int REQUEST_FILE_DATA_FROM_CACHE_TIMEOUT_MS=120000;
    public static int FILE_DATA_POLL_INTERVAL_MS=100;
    public static final int MIN_RAW_VAL=10000;
    public static final int MAX_RAW_VAL=23000;

    private String remoteBasePath;
    private File topFolder;
    private long folderOpenTimestamp;
    private URL initUrl;
    private int cacheVolumeSize;
    private int sliceSize = -1;
    protected TileFormat tileFormat;
    SimpleFileCache cache;
    Double zoom;
    Vec3 focus;
    TileIndex focusTileIndex;
    File focusStackFile;
    List<File> focusStackGroup=new ArrayList<>();
    List<File> neighborhoodStackFiles=new ArrayList<>();
    Map<File,int[]> stackPositionMap=new HashMap<>();
    String fileSeparator=null;
    Map<TileIndex, File> stackFileForTileIndexMap=new HashMap<>();

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

    private static synchronized void submitEmergencyFileLoader(FileLoader fl) {
        emergencyThreadPool.schedule(fl, 0, TimeUnit.MILLISECONDS);
    }

    private static synchronized void submitFileLoader(FileLoader fl) {
        fileLoadThreadPool.schedule(fl, 2, TimeUnit.MILLISECONDS);
    }

    public File getTopFolder() {
        return topFolder;
    }

    public boolean isInitialized() { return initialized; }

    private void initFilesystemMetadata() throws DataSourceInitializeException {
        final OctreeMetadataSniffer octreeMetadataSniffer = new OctreeMetadataSniffer(topFolder, tileFormat, new CoordinateToRawTransformFileSource() {
            @Override
            public CoordinateToRawTransform getCoordToRawTransform(String filePath) throws Exception {
                return ModelMgr.getModelMgr().getCoordToRawTransform(filePath);
            }
        });
        octreeMetadataSniffer.setRemoteBasePath(remoteBasePath);
        octreeMetadataSniffer.sniffMetadata(topFolder);
        sliceSize = octreeMetadataSniffer.getSliceSize(); // maybe 16-bit
        log.info("initFilesystemMetadata()");
        folderOpenTimestamp = new Date().getTime();
        SessionMgr.getSessionMgr().logToolEvent(
                LargeVolumeViewerTopComponentDynamic.LVV_LOGSTAMP_ID,
                LTT_SESSION_CATEGORY_STRING,
                new ActionString(remoteBasePath + ":" + folderOpenTimestamp)
        );
        int[] tileSize=tileFormat.getTileSize();
        int[] volumeSize=tileFormat.getVolumeSize();
        int zoomLevels=tileFormat.getZoomLevelCount();
        cacheVolumeSize=tileSize[0]*tileSize[1]*tileSize[2]*tileFormat.getChannelCount(); // always 8-bit
        cache=new SimpleFileCache(300);
        log.info("Tile size="+tileSize[0]+" "+tileSize[1]+" "+tileSize[2]);
        log.info("Volume size="+volumeSize[0]+" "+volumeSize[1]+" "+volumeSize[2]);
        log.info("zoom Levels="+zoomLevels);
        filesystemMetadataInitialized=true;
    }


    public void setZoom(Double zoom) {
        //log.info("setZoom="+zoom);
        this.zoom=zoom;
        if (VolumeCache.useVolumeCache())
            updateFocusTileIndex();
    }

    public void setFocus(Vec3 focus) {
        //log.info("setFocus="+focus.toString());
        this.focus=focus;
        if (VolumeCache.useVolumeCache())
            updateFocusTileIndex();
    }

    public synchronized Map<File, int[]> getCacheStatusMap() {
        Map<File, int[]> statusMap=new HashMap<>();
        for (File file : stackPositionMap.keySet()) {
            int[] positionArr=stackPositionMap.get(file);
            int status=0;
            if (FileLoader.currentlyBeingLoaded(file)) {
                status=1;
            }
            ByteBuffer data=cache.getBytes(file);
            if (data!=null && data.capacity()>3) {
                status=2;
            }
            int[] statusArr=new int[] { positionArr[0], positionArr[1], positionArr[2], status };
            statusMap.put(file, statusArr);
        }
        return statusMap;
    }

    private synchronized void updateFocusTileIndex() {

        int sCount = 0;
        if (zoom!=null && focus!=null && cache!=null) {

            //long start=System.nanoTime();

            emergencyThreadPool.getQueue().clear();
            fileLoadThreadPool.getQueue().clear();

            // Update focus tile/stack
            int zoomLevel=tileFormat.zoomLevelForCameraZoom(zoom);
            TileIndex PREFIX_focusTileIndex = tileFormat.tileIndexForXyz(focus, zoomLevel, CoordinateAxis.Z);
            // Must add zoomOutFactor back in for Z to "correct" result from this method
            focusTileIndex=new TileIndex(PREFIX_focusTileIndex.getX(), PREFIX_focusTileIndex.getY(), ((int)(PREFIX_focusTileIndex.getZ()*Math.pow(2,zoomLevel))),
                    zoomLevel, tileFormat.getZoomLevelCount()-1, tileFormat.getIndexStyle(), CoordinateAxis.Z);
            focusStackFile = getStackFileForTileIndex(focusTileIndex);

//            Set<File> previousNeighborhood=new HashSet<>();
//            previousNeighborhood.addAll(neighborhoodStackFiles);

            // Update neighborhood
            neighborhoodStackFiles.clear();
            stackPositionMap.clear();

            // Sub-groups
            focusStackGroup.clear();

            // We need to build the neighborhood in optimal order

            // First, the focus tile
            neighborhoodStackFiles.add(focusStackFile);
            focusStackGroup.add(focusStackFile);

            //log.info("updateFocusTileIndex="+focusTileIndex.toString()+" focusStackFile="+focusStackFile.getAbsolutePath());

            // Next, the inner same-z set
            for (int yOffset=-1; yOffset<2; yOffset++) {
                for (int xOffset=-1; xOffset<2; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, 0, true);
                    stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, 0});
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        focusStackGroup.add(stackFile);
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }

            // Next, one level down
            for (int yOffset=-1; yOffset<2; yOffset++) {
                for (int xOffset=-1; xOffset<2; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, 1, false);
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, 1});
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }

            // Next, one level up
            for (int yOffset=-1; yOffset<2; yOffset++) {
                for (int xOffset=-1; xOffset<2; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, -1, false);
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, -1});
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }

            // Last, the large outer same-level set
            for (int yOffset=-2; yOffset<3; yOffset++) {
                for (int xOffset=-2; xOffset<3; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, 0, false);
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, 0});
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }

            // Outer, one level down
            for (int yOffset=-2; yOffset<3; yOffset++) {
                for (int xOffset=-2; xOffset<3; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, 1, false);
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, 1});
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }

            // Outer, one level up
            for (int yOffset=-2; yOffset<3; yOffset++) {
                for (int xOffset=-2; xOffset<3; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, -1, false);
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, -1});
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }

            // Last, the large outer same-level set
            for (int yOffset=-3; yOffset<4; yOffset++) {
                for (int xOffset=-3; xOffset<4; xOffset++) {
                    File stackFile=getStackFileByOffset(xOffset, yOffset, 0, false);
                    if (stackFile!=null && !neighborhoodStackFiles.contains(stackFile)) {
                        stackPositionMap.put(stackFile, new int[] { xOffset, yOffset, 0});
                        neighborhoodStackFiles.add(stackFile);
                    }
                }
            }


            int nc=0;
            for (File nf : neighborhoodStackFiles) {
                ByteBuffer data=cache.getBytes(nf);
                if (data!=null && data.capacity()<4) {
                    data=null;
                    try { cache.putBytes(nf, null); } catch (Exception ex) {}
                }
                if (data==null) {
                    if (!FileLoader.currentlyBeingLoaded(nf)) {
                        if (focusStackGroup.contains(nf)) {
                            //log.info("Submitting emergency NEIGHBOR request for file="+nf.getAbsolutePath());
                            submitEmergencyFileLoader(new FileLoader(nf, this));
                            sCount++;
                        } else {
                            //log.info("submitting normal NEIGHBOR request for file="+nf.getAbsolutePath());
                            submitFileLoader(new FileLoader(nf, this));
                            sCount++;
                        }
                    }
                }
                nc++;
            }

            // Clean cache
            //cache.limitToNeighborhood(neighborhoodStackFiles);
            //long time = (System.nanoTime() - start)/1000000;
            //log.info("updateFocusTileIndex: sdded " + sCount + " stack loads. Emergency=" + getEmergencyPoolCount() + " Normal=" + getFileLoadPoolCount() + " CacheSize="+cache.size()+" in "+time+" ms");
        }
    }

    private File getStackFileByOffset(int xOffset, int yOffset, int zOffset, boolean debug) {
        int zIncrement=zOffset*tileFormat.getTileSize()[2]*(int)(Math.pow(2, focusTileIndex.getZoom()));
        int zPosition=focusTileIndex.getZ()+zIncrement;
        TileIndex offsetTileIndex = new TileIndex(focusTileIndex.getX() + xOffset, focusTileIndex.getY() + yOffset, zPosition, focusTileIndex.getZoom(), focusTileIndex.getMaxZoom(),
                focusTileIndex.getIndexStyle(), focusTileIndex.getSliceAxis());
        File stackFile=getStackFileForTileIndex(offsetTileIndex);
//        if (debug) {
//            log.info("offsetTileIndex="+offsetTileIndex.toString()+", file="+stackFile);
//        }
        return stackFile;
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

    /**
     * @return the folderOpenTimestamp
     */
    public long getFolderOpenTimestamp() {
        return folderOpenTimestamp;
    }

    /**
     * @param folderOpenTimestamp the folderOpenTimestamp to set
     */
    public void setFolderOpenTimestamp(long folderOpenTimestamp) {
        this.folderOpenTimestamp = folderOpenTimestamp;
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
        //log.info("loadToRam() start");
        File stackFile=getStackFileForTileIndex(tileIndex);
        if (stackFile==null) {
            //log.info("loadToRam() request for tileIndex="+tileIndex.toString()+" resulted in null stackFile, calling TileLoadError to release thread");
            throw new AbstractTextureLoadAdapter.TileLoadError("null stack file");
        }
        //log.info("loadToRam() request tileIndex="+tileIndex.toString()+" corresponding to file="+stackFile.getAbsolutePath());
        int stackSlice=getStackSliceFromTileIndex(tileIndex);
        //log.info("loadToRam() stackSlice="+stackSlice+", calling getFileDataFromCache() for file="+stackFile.getAbsolutePath());
        ByteBuffer stackData=getFileDataFromCache(stackFile);
        if (stackData==null || stackData.capacity()<3) {
            if (!VolumeCache.useVolumeCache()) {
                return null;
            }
            if (stackData.capacity()<3) {
                // Reset cache entry
                cache.putBytes(stackFile, null);
            }
            throw new AbstractTextureLoadAdapter.TileLoadError("stackData is null");
        }
        //long t2=System.nanoTime();
        TextureData2dGL textureData2dGL=createTextureData2dGLFromCacheVolume(stackData, stackSlice);
        //long t3=System.nanoTime();
        //long report3=(t3-t2)/1000000;
        //log.info("createTextureData2dGLFromCacheVolume ms="+report3);
        //log.info("returning TextureData2dGL for TileIndex request="+tileIndex.toString());
        return textureData2dGL;

        //return createDebugTileFromTileIndex(tileIndex);
    }

    public File getStackFileForTileIndex(TileIndex tileIndex) {
        File stackFile=stackFileForTileIndexMap.get(tileIndex);
        if (stackFile==null) {
            final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, true /*zOriginNegativeShift*/);
            if (octreeFilePath == null) {
                return null;
            }
            stackFile = new File(topFolder, octreeFilePath.toString());
            stackFileForTileIndexMap.put(tileIndex, stackFile);
        }
        return stackFile;
    }

    private TextureData2dGL createTextureData2dGLFromCacheVolume(ByteBuffer stackBuffer, int zSlice) {
        //long t1=System.nanoTime();
        int[] tileSize=tileFormat.getTileSize();
        TextureData2d result = new TextureData2d();
        result.load8bitStackSliceByteBufferTo16bitTexture(tileSize[0], tileSize[1], tileSize[2], 2, zSlice, MIN_RAW_VAL, (MAX_RAW_VAL-MIN_RAW_VAL), stackBuffer);
        //long t2=System.nanoTime();
        //long report2=(t2-t1)/1000000;
        //log.info("load8bitStackSliceByteBufferTo16bitTexture ms="+report2);
        return new TextureData2dGL(result);

//
//        long[] timings=new long[4];
//        int[] tileSize=tileFormat.getTileSize();
//        int sliceSize=tileSize[0]*tileSize[1];
//        int channelSize=sliceSize*tileSize[2];
//        byte[] stackBytes=stackBuffer.array();
//        List<BufferedImage> imageList=new ArrayList<>();
//        for (int c=0;c<tileFormat.getChannelCount();c++) {
//            timings[2*c]=System.nanoTime();
//            BufferedImage bufferedImage=new BufferedImage(tileSize[0], tileSize[1], BufferedImage.TYPE_USHORT_GRAY);
//            Raster imageCopy=bufferedImage.getData();
//            DataBuffer dataBuffer=imageCopy.getDataBuffer();
//            timings[2*c+1]=System.nanoTime();
//            int cOffset=channelSize*c;
//            int zOffset=sliceSize*zSlice;
//            for (int y=0;y<tileSize[1];y++) {
//                int yOffset=y*tileSize[0];
//                int czyOffset=cOffset+zOffset+yOffset;
//                for (int x=0;x<tileSize[0];x++) {
//                    int stackValue = ((stackBytes[czyOffset+x]+128)*(MAX_RAW_VAL-MIN_RAW_VAL))/256 + MIN_RAW_VAL;
//                    dataBuffer.setElem(yOffset+x, stackValue);
//                }
//            }
//            bufferedImage.setData(imageCopy);
//            imageList.add(bufferedImage);
//        }
//        long report1=(timings[1]-timings[0])/1000000;
//        long report2=(timings[3]-timings[2])/1000000;
//        log.info("createTextureData2dGLFromCacheVolume() loop1="+report1+" loop2="+report2);
//        return getCombinedTextureData2dGLFromRenderedImages(imageList);
    }

    private ByteBuffer getFileDataFromCache(File file) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException {
        //log.info("getFileDataFromCache() start for file="+file.getAbsolutePath());
        if (focusStackFile==null) {
            updateFocusTileIndex();
        }
        if (focusStackFile==null) {
            throw new AbstractTextureLoadAdapter.TileLoadError("getFileDataFromCache() requires focusStackFile is initialized");
        }
        if (cache==null) {
            //log.info("cache is NULL! for file="+file.getAbsolutePath());
            throw new AbstractTextureLoadAdapter.TileLoadError("cache is null");
        }
        ByteBuffer data=cache.getBytes(file);
        //log.info("post getBytes() check for file="+file.getAbsolutePath());
        if (data==null) {
            //log.info("getFileDataFromCache() data is null, proceeding with FileLoader submission for file="+file.getAbsolutePath());
            if (!TileStackCacheController.FileLoader.currentFilesBeingLoaded.contains(file)) {
                //log.info("post currentFileBeingLoaded check for file="+file.getAbsolutePath());
                if (focusStackGroup.contains(file)) {
                    //log.info("Scheduling EMERGENCY file="+file.getAbsolutePath());
                    submitEmergencyFileLoader(new FileLoader(file, this));
                    //log.info("EM Check1 for file+"+file.getAbsolutePath());
                } else {
                    //log.info("Scheduling normal file="+file.getAbsolutePath());
                    submitFileLoader(new FileLoader(file, this));
                    //log.info("NORMAL Check1 for file="+file.getAbsolutePath());
                }
            }
            //log.info("Pre-check loop for file="+file.getAbsolutePath());
            int maxCheckCount=REQUEST_FILE_DATA_FROM_CACHE_TIMEOUT_MS / FILE_DATA_POLL_INTERVAL_MS;
            int checkCount=0;
            //log.info("Beginning check loop for file="+file.getAbsolutePath());
            while (data==null && checkCount<maxCheckCount) {
                if (!VolumeCache.useVolumeCache()) {
                    return null;
                }
                try { Thread.sleep(FILE_DATA_POLL_INTERVAL_MS); } catch (Exception ex) {}
                data=cache.getBytes(file);
                if (data==null) {
                    //log.info("loop check: data is null for file="+file.getAbsolutePath());
                    if (!neighborhoodStackFiles.contains(file)) {
                        //log.info("exiting check loop due to no longer being in neighborhood, for file="+file.getAbsolutePath());
                        throw new AbstractTextureLoadAdapter.TileLoadError("no longer in cache neighborhood");
                    }
                } else {
                    //log.info("loop check: data is NOT NULL for file="+file.getAbsolutePath());
                }
                checkCount++;
            }
            //log.info("End of check loop for file="+file.getAbsolutePath());
            if (checkCount==maxCheckCount) {
                String errorString = "Request File data from cache timeout for file="+file.getAbsolutePath();
                log.error(errorString);
                throw new AbstractTextureLoadAdapter.TileLoadError(errorString);
            }
            if (data==null) {
                log.error("data is NULL after check loop for file="+file.getAbsolutePath());
                throw new AbstractTextureLoadAdapter.TileLoadError("data is null for file="+file.getAbsolutePath());
            }
            if (data.capacity()==0) {
                cache.putBytes(file, null);
                throw new AbstractTextureLoadAdapter.MissingTileException("ByteBuffer empty for file="+file.getAbsolutePath());
            } else if (data.capacity()==1) {
                cache.putBytes(file, null);
                throw new AbstractTextureLoadAdapter.TileLoadError("TileLoadError for file="+file.getAbsolutePath());
            }
        } else {
            //log.info("getFileDataFromCache() data is not null, returning cached stack ByteBuffer capacity="+data.capacity()+" for file="+file.getAbsolutePath());
        }
        return data;
    }

    private static class FileLoader implements Runnable {
        private File file;
        private TileStackCacheController tileStackCacheController;
        private static ConcurrentHashSet<File> currentFilesBeingLoaded=new ConcurrentHashSet<>();

        public FileLoader(File file, TileStackCacheController tileStackCacheController) {
            this.file=file;
            this.tileStackCacheController=tileStackCacheController;
        }

        public static synchronized boolean currentlyBeingLoaded(File file) {
            if (file==null) {
                return false;
            } else {
                return currentFilesBeingLoaded.contains(file);
            }
        }

        public static synchronized void setCurrentFileBeingLoaded(File file, boolean status) {
            if (file!=null) {
                if (status) {
                    currentFilesBeingLoaded.add(file);
                } else {
                    currentFilesBeingLoaded.remove(file);
                }
            }
        }

        @Override
        public void run() {

            // Is the cache off?
            if (!VolumeCache.useVolumeCache()) {
                return;
            }

            //log.info("***>>> FileLoader run() for file="+file.getAbsolutePath());
            // Am I already done?
            SimpleFileCache cache=tileStackCacheController.getCache();
            ByteBuffer data=cache.getBytes(file);
            if (data!=null) {
                //log.info("***>>> FileLoader data already populated for file="+file.getAbsolutePath());
                return;
            }

            // Is someone else already loading this file, in which case I can just let them do it?
            if (currentlyBeingLoaded(file)) {
                //log.info("***>>> FileLoader file already being loaded="+file.getAbsolutePath());
                return;
            }

            // Am I out of the neighborhood?
            if (!inTheNeighborhood(file)) {
                //log.info("***>>> FileLoader skipping file because out of neighborhood="+file.getAbsolutePath());
                ByteBuffer errorBuffer=ByteBuffer.wrap(new byte[1]);
                try { cache.putBytes(file, errorBuffer); } catch (Exception ex) {
                    log.error("Error adding byte[1] buffer to cache for file="+file.getAbsolutePath());
                    ex.printStackTrace();
                }
                return;
            }

            // Apparently not, so I will take responsibility
            setCurrentFileBeingLoaded(file, true);

            //log.info("***>>> FileLoader loading file="+file.getAbsolutePath());
            long loadStart=System.currentTimeMillis();

            TileFormat tileFormat=tileStackCacheController.getTileFormat();
            int[] tileSize=tileFormat.getTileSize();
            byte[] volumeData=new byte[tileStackCacheController.getCacheVolumeSize()];
            int channelCount=tileFormat.getChannelCount();
            boolean errorFlag=false;
            try {

                loadTiffToByteArray(file, volumeData, channelCount, tileSize);

                if (!VolumeCache.useVolumeCache()) {
                    return;
                }

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
                iex.printStackTrace();
                ByteBuffer errorBuffer=ByteBuffer.wrap(new byte[1]);
                try { cache.putBytes(file, errorBuffer); } catch (Exception ex2) {}
            }
            //log.info("***>>> FileLoader wrapping volumeData");
            if (!errorFlag) {
                ByteBuffer cacheData = ByteBuffer.wrap(volumeData);
                try {
                    long loadTime = System.currentTimeMillis() - loadStart;
                    log.info("FileLoader loaded=" + file.getAbsolutePath() + " in " + loadTime + " ms");
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
            setCurrentFileBeingLoaded(file, false);
            return;
        }

        private void loadTiffToByteArray(File file, byte[] volumeData, int channelCount, int[] tileSize) throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException, IOException {
            log.info("***>>> loadTiffToByteArray() loading file="+file.getAbsolutePath());
            Long startingTime = System.currentTimeMillis();
            //try {
                int sliceSize = tileSize[0] * tileSize[1];
                int channelSize = sliceSize * tileSize[2];
                ImageDecoder[] decoders = BlockTiffOctreeLoadAdapter.createImageDecoders(file, CoordinateAxis.Z, true, channelCount, true);
                RenderedImage[] channels = new RenderedImage[channelCount];
                for (int z = 0; z < tileSize[2]; z++) {
                    if (!VolumeCache.useVolumeCache()) {
                        return;
                    }
                    // Is there an emergency?
                    if (!tileStackCacheController.focusStackGroup.contains(file)) {
                        if (tileStackCacheController.getEmergencyPoolCount() > 0) {
                            try {
                                //log.info("Waiting 200ms for emergency file load");
                                Thread.sleep(200);
                            }
                            catch (Exception ex) {
                            }
                        }
                    }
//                // Am I out of the neighborhood?
                    if (!inTheNeighborhood(file)) {
                        //log.info("file="+file.getAbsolutePath()+" is not in the neighborhood, terminating load at z="+z+" of "+tileSize[2]);
                        throw new AbstractTextureLoadAdapter.TileLoadError("Out of neighborhood");
                    }
                    //log.info("***>>> FileLoader loading zSlize=" + z + " for file=" + file.getAbsolutePath());
                    for (int c = 0; c < channels.length; c++) {
                        ImageDecoder decoder = decoders[c];
                        channels[c] = decoder.decodeAsRenderedImage(z);
                    }
                    for (int c = 0; c < channels.length; c++) {
                        DataBuffer sliceData = channels[c].getData().getDataBuffer();
                        int zOffset = channelSize * c + z * sliceSize;
                        for (int y = 0; y < tileSize[1]; y++) {
                            int yOffset = y * tileSize[0];
                            for (int x = 0; x < tileSize[0]; x++) {
                                int zyOffset = zOffset + yOffset;
                                int sVal = sliceData.getElem(yOffset + x);
                                int iVal = ((sVal - MIN_RAW_VAL) * 256) / (MAX_RAW_VAL - MIN_RAW_VAL) - 128;
                                if (iVal > 127) {
                                    iVal = 127;
                                } else if (iVal < -128) {
                                    iVal = -128;
                                }
                                volumeData[zyOffset + x] = (byte) iVal;
                            }
                        }
                    }
                }
            //} catch (Exception ex) {
            //    log.error("***>>> loadTiffToByteArray: Exception="+ex.toString());
            //    ex.printStackTrace();
            //}
                Long finalTime = System.currentTimeMillis();
                final long elapsedMs = finalTime - startingTime;
                // Alwoys logging these.  Far less often issued than single
                // slice loads.
                File topFolder = tileStackCacheController.getTopFolder();
                String specificPart = file.toString().substring(topFolder.toString().length());
                SessionMgr.getSessionMgr().logToolEvent(
                    LargeVolumeViewerTopComponentDynamic.LVV_LOGSTAMP_ID,
                    LTT_CATEGORY_STRING,
                    new ActionString(
                            tileStackCacheController.getFolderOpenTimestamp() + ":" + specificPart + ":elapsed_ms="+elapsedMs
                    )
            );
        }

        private synchronized boolean inTheNeighborhood(File file) {
            List<File> neighborhoodFiles=tileStackCacheController.getNeighborhoodStackFiles();
            if (neighborhoodFiles!=null &&  (!neighborhoodFiles.contains(file))) {
                return false;
            }
            return true;
        }

        private void loadH5JToByteArray(File[] files, byte[] volumeData, int channelCount, int[] tileSize)throws AbstractTextureLoadAdapter.TileLoadError, AbstractTextureLoadAdapter.MissingTileException, IOException {
            int channelSize=tileSize[0] * tileSize[1] * tileSize[2];
            try {
                for (int c=0;c<channelCount;c++) {
                    byte[] data=getSingleChannelH5JData(files[c]);
                    if (data.length!=channelSize) {
                        throw new AbstractTextureLoadAdapter.TileLoadError("H5J channel size="+data.length+" does not match expected channel size="+channelSize);
                    }
                    System.arraycopy(data, 0, volumeData, c*channelSize, channelSize);
                }
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

    private static synchronized byte[] getSingleChannelH5JData(File file) throws IOException {
        try {
            H5JLoader h5JLoader = new H5JLoader(file.getAbsolutePath());
            List<String> channelNames = h5JLoader.channelNames();
            ImageStack image = h5JLoader.extract(channelNames.get(0));
            int maxFrames = image.getNumFrames();
            int startingFrame = 0;
            int endingFrame = startingFrame + maxFrames;
            int sliceSize=image.width()*image.height();
            int dataSize=sliceSize*maxFrames;
            byte[] result=new byte[dataSize];
            for (int i = startingFrame; i < endingFrame; i++) {
                int startIndex=sliceSize*i;
                byte[] sliceData=image.interleave(i, 0, 1);
                System.arraycopy(sliceData, 0, result, startIndex, sliceSize);
            }
            return result;
        } catch (Exception ex) {
            throw new IOException(ex);
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
            TextureData2d result = new TextureData2d();
            result.loadRenderedImage(composite);
            return new TextureData2dGL(result);
        } catch (NoClassDefFoundError exc) {
            exc.printStackTrace();
            return null;
        }
    }

}

