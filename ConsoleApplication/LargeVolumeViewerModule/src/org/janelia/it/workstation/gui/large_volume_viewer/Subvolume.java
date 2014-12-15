package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.janelia.it.workstation.raster.VoxelIndex;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subvolume {

    public static final int N_THREADS = 20;
    private static final String PROGRESS_REPORT_FORMAT = "%d of %d to go...";
    
    private IndeterminateNoteProgressMonitor progressMonitor;
    
	// private int extentVoxels[] = {0, 0, 0};
	private ZoomedVoxelIndex origin; // upper left front corner within parent volume
	private VoxelIndex extent; // width, height, depth
	private ByteBuffer bytes;
	private ShortBuffer shorts;
	private int bytesPerIntensity = 1;
	private int channelCount = 1;
    private int totalTiles = 0;
    private int remainingTiles = 0;
    
    private static final Logger logger = LoggerFactory.getLogger( Subvolume.class );
	
    /**
     * You probably want to run this constructor in a worker
     * thread, because it can take a while to load its
     * raster data over the network.
     * 
     * @param corner1
     * @param corner2
     * @param wholeImage
     */
	public Subvolume(
	        ZoomedVoxelIndex corner1,
	        ZoomedVoxelIndex corner2,
	        SharedVolumeImage wholeImage)
	{
	    initialize(corner1, corner2, wholeImage, null);
	}
	
	/**
	 * You probably want to run this constructor in a worker
	 * thread, because it can take a while to load its
	 * raster data over the network.
	 * 
	 * @param corner1 start from here, in 3D
	 * @param corner2 end here, in 3D
	 * @param wholeImage 
	 * @param textureCache
	 */
    public Subvolume(
            ZoomedVoxelIndex corner1,
            ZoomedVoxelIndex corner2,
            SharedVolumeImage wholeImage,
            TextureCache textureCache)
    {
        initialize(corner1, corner2, wholeImage, textureCache);
    }
    
	/**
	 * You probably want to run this constructor in a worker
	 * thread, because it can take a while to load its
	 * raster data over the network.
	 * 
	 * @param corner1 start from here, in 3D
	 * @param corner2 end here, in 3D
	 * @param wholeImage 
	 * @param textureCache
     * @param progressMonitor for reporting relative completion.
	 */
    public Subvolume(
            ZoomedVoxelIndex corner1,
            ZoomedVoxelIndex corner2,
            SharedVolumeImage wholeImage,
            TextureCache textureCache,
            IndeterminateNoteProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
        initialize(corner1, corner2, wholeImage, textureCache);
    }
    
	/**
	 * You probably want to run this constructor in a worker
	 * thread, because it can take a while to load its
	 * raster data over the network.
	 * 
	 * @param center volume around here, in 3D
	 * @param wholeImage 
     * @param dimensions how large to make the volume.
	 * @param textureCache
     * @param progressMonitor for reporting relative completion.
	 */
    public Subvolume(
            ZoomedVoxelIndex center,
            SharedVolumeImage wholeImage,
            int[] dimensions,
            TextureCache textureCache,
            IndeterminateNoteProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
        initialize(center, dimensions, wholeImage, textureCache);
    }
    
	private void initialize(ZoomedVoxelIndex corner1,
            ZoomedVoxelIndex corner2,
            SharedVolumeImage wholeImage,
            final TextureCache textureCache)
	{
	    // Both corners must be the same zoom resolution
	    assert(corner1.getZoomLevel().equals(corner2.getZoomLevel()));
	    // Populate data fields
	    final ZoomLevel zoom = corner1.getZoomLevel();
	    origin = new ZoomedVoxelIndex(
	            zoom,
	            Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));
	    final ZoomedVoxelIndex farCorner = new ZoomedVoxelIndex(
                zoom,
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
	    extent = new VoxelIndex(
	            farCorner.getX() - origin.getX() + 1,
                farCorner.getY() - origin.getY() + 1,
                farCorner.getZ() - origin.getZ() + 1);
	    // Allocate raster memory
	    final AbstractTextureLoadAdapter loadAdapter = wholeImage.getLoadAdapter();
	    final TileFormat tileFormat = loadAdapter.getTileFormat();
	    bytesPerIntensity = tileFormat.getBitDepth()/8;
	    channelCount = tileFormat.getChannelCount();
	    int totalBytes = bytesPerIntensity 
	            * channelCount
	            * extent.getX() * extent.getY() * extent.getZ();
	    bytes = ByteBuffer.allocateDirect(totalBytes);
	    bytes.order(ByteOrder.nativeOrder());
	    if (bytesPerIntensity == 2)
	        shorts = bytes.asShortBuffer();

        Set<TileIndex> neededTiles = getNeededTileSet(tileFormat, farCorner, zoom);
        if (logger.isDebugEnabled()) {
            logTileRequest(neededTiles);
        }
        ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );
        List<Future<Boolean>> followUps = new ArrayList<>();
        totalTiles = neededTiles.size();
        remainingTiles = neededTiles.size();
        reportProgress( totalTiles, totalTiles );
        
        for (final TileIndex tileIx : neededTiles) {
            Callable<Boolean> fetchTask = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return fetchTileData(
                            textureCache, tileIx, loadAdapter, tileFormat, zoom, farCorner
                    );
                }
            };
            followUps.add( executorService.submit( fetchTask ) );
        }
        executorService.shutdown();
        
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
            for (Future<Boolean> result : followUps) {
                if (!result.get()) {
                    logger.info("Request for {}..{} had tile gaps.", origin, extent);
                    break;
                }
            }
        } catch ( InterruptedException | ExecutionException ex ) {
            if ( progressMonitor != null ) {
                progressMonitor.close();
            }
            logger.error(
                    "Failure awaiting completion of fetch threads for request {}..{}.  Exception report follows.",
                    origin, extent
            );
            ex.printStackTrace();
        }
	}

	private void initialize(ZoomedVoxelIndex center,
            int[] dimensions,
            SharedVolumeImage wholeImage,
            final TextureCache textureCache)
	{
	    // Populate data fields
	    final ZoomLevel zoom = center.getZoomLevel();

        // Allocate raster memory
	    final AbstractTextureLoadAdapter loadAdapter = wholeImage.getLoadAdapter();
	    final TileFormat tileFormat = loadAdapter.getTileFormat();
	    bytesPerIntensity = tileFormat.getBitDepth()/8;
	    channelCount = tileFormat.getChannelCount();
	    int totalBytes = bytesPerIntensity 
	            * channelCount
	            * dimensions[0] * dimensions[1] * dimensions[2];
	    bytes = ByteBuffer.allocateDirect(totalBytes);
	    bytes.order(ByteOrder.nativeOrder());
	    if (bytesPerIntensity == 2)
	        shorts = bytes.asShortBuffer();

        Set<TileIndex> neededTiles = getCenteredTileSet(tileFormat, center, dimensions, zoom);
        if (logger.isDebugEnabled()) {
            logTileRequest(neededTiles);
        }
        ExecutorService executorService = Executors.newFixedThreadPool( N_THREADS );
        List<Future<Boolean>> followUps = new ArrayList<>();
        totalTiles = neededTiles.size();
        remainingTiles = neededTiles.size();
        reportProgress( totalTiles, totalTiles );
        
        final ZoomedVoxelIndex farCorner = new ZoomedVoxelIndex(
                zoom,
                origin.getX() + dimensions[0],
                origin.getY() + dimensions[1],
                origin.getZ() + dimensions[2]
        );
        for (final TileIndex tileIx : neededTiles) {
            Callable<Boolean> fetchTask = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return fetchTileData(
                            textureCache, tileIx, loadAdapter, tileFormat, zoom, farCorner
                    );
                }
            };
            followUps.add( executorService.submit( fetchTask ) );
        }
        executorService.shutdown();
        
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
            for (Future<Boolean> result : followUps) {
                logger.debug("DEBUG: checking a follow-up.");
                boolean failureOnResult = false;
                try {
                    if (result == null || !result.get()) {
                        failureOnResult = true;
                    }
                } catch ( ExecutionException | RuntimeException rte ) {
                    logger.error(
                            "Exception during subvolume fetch.  Request {}..{}.  Exception report follows.",
                            origin, extent
                    );
                    rte.printStackTrace();
                    failureOnResult = true;
                }
                if ( failureOnResult ) {
                    logger.info("Request for {}..{} had tile gaps.", origin, extent);
                }
            }            
        } catch ( InterruptedException ex ) {
            if ( progressMonitor != null ) {
                progressMonitor.close();
            }
            logger.error(
                    "Failure awaiting completion of fetch threads for request {}..{}.  Exception report follows.",
                    origin, extent
            );
            ex.printStackTrace();
        }
	}

    // Load an octree subvolume into memory as a dense volume block
    public Subvolume(
            Vec3 corner1,
            Vec3 corner2,
            double micrometerResolution,
            SharedVolumeImage wholeImage)
    {
        // Use the TileFormat class to convert between micrometer coordinates
        // and the arcane integer TileIndex coordinates.
        TileFormat tileFormat = wholeImage.getLoadAdapter().getTileFormat();
        // Compute correct zoom level based on requested resolution
        int zoom = tileFormat.zoomLevelForCameraZoom(1.0/micrometerResolution);
        ZoomLevel zoomLevel = new ZoomLevel(zoom);
        // Compute extreme tile indices
        //
        // NOTE: have to pre-compensate "fat x" value. LLF
//        int xOriginMicrometers = (int)(tileFormat.getOrigin()[0] * tileFormat.getVoxelMicrometers()[0]);
        TileFormat.VoxelXyz vix1 = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        (corner1.getX()) /*- xOriginMicrometers)*tileFormat.getVoxelMicrometers()[0]*/, corner1.getY(), corner1.getZ()));
        TileFormat.VoxelXyz vix2 = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        (corner2.getX()) /*- xOriginMicrometers)*tileFormat.getVoxelMicrometers()[0]*/, corner2.getY(), corner2.getZ()));
        //
        ZoomedVoxelIndex zvix1 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vix1, 
                zoomLevel, CoordinateAxis.Z);
        ZoomedVoxelIndex zvix2 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vix2, 
                zoomLevel, CoordinateAxis.Z);
        //
        initialize(zvix1, zvix2, wholeImage, null);
    }

    public BufferedImage[] getAsBufferedImages() {
		int sx = extent.getX();
		int sy = extent.getY();
		int sz = extent.getZ();
		BufferedImage result[] = new BufferedImage[sz];
		int[] bits;
		int[] bandOffsets = {0};
		int dataType;
		if ((channelCount == 1) && (bytesPerIntensity == 2)) {
		    dataType = DataBuffer.TYPE_USHORT;
		    bits = new int[] {16};
		}
		else if ((channelCount == 1) && (bytesPerIntensity == 1)) {
		    dataType = DataBuffer.TYPE_BYTE;
            bits = new int[] {8};
		}
		else
		    throw new RuntimeException("Unsuported image type"); // TODO
		
		ColorModel colorModel = new ComponentColorModel(
		        ColorSpace.getInstance(ColorSpace.CS_GRAY),
		        bits,
		        false,
		        false,
		        Transparency.OPAQUE,
		        dataType);
		for (int z = 0; z < sz; ++z) {
	        WritableRaster raster = Raster.createInterleavedRaster(
	                dataType,
	                sx, sy,
	                sy*channelCount, // scan-line stride
	                channelCount, // pixel stride
	                bandOffsets,
	                null);
			result[z] = new BufferedImage(colorModel, raster, false, null);
			if (bytesPerIntensity == 2) {
			    short[] a = ( (DataBufferUShort) result[z].getRaster().getDataBuffer() ).getData();
			    // TODO
			    // System.arraycopy(data, 0, a, 0, data.length); // TODO
			}
		}
		return null;
	}

    public ByteBuffer getByteBuffer() {
        return bytes;
    }

    public int getBytesPerIntensity() {
        return bytesPerIntensity;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public VoxelIndex getExtent() {
        return extent;
    }

    public int getIntensityGlobal(ZoomedVoxelIndex v1, int channelIndex)
    {
        return getIntensityLocal(new VoxelIndex(
            v1.getX() - origin.getX(),
            v1.getY() - origin.getY(),
            v1.getZ() - origin.getZ()),
            channelIndex);
    }

    public ZoomedVoxelIndex getOrigin() {
        return origin;
    }

    public int getIntensityLocal(VoxelIndex v1, int channelIndex) {
        int c = channelIndex;
        int x = v1.getX();
        int y = v1.getY();
        int z = v1.getZ();
        // Compute offset into local raster
        int offset = 0;
        // color channel is fastest moving dimension
        int stride = 1;
        offset += c * stride;
        // x
        stride *= channelCount;
        offset += x * stride;
        // y
        stride *= extent.getX();
        offset += y * stride;
        // z
        stride *= extent.getY();
        offset += z * stride;
        // our data is unsigned but Java doesn't do unsigned, so strip off the
        //  sign bit explicitly; since we're returning int, no worry about overflow
        if (bytesPerIntensity == 2)
            return shorts.get(offset) & 0xffff;
        else
            return bytes.get(offset) & 0xff;
    }
	
    /**
     * Report how much is left to be done, for this phase.
     * 
     * @param remaining still to come.
     * @param total all to do.
     */
    private void reportProgress( int remaining, int total ) {
        if ( progressMonitor != null ) {
            progressMonitor.setNote( String.format( PROGRESS_REPORT_FORMAT, remaining, total ) );
        }
    }

    private void logTileRequest(Set<TileIndex> neededTiles) {
        StringBuilder bldr = new StringBuilder();
        for ( TileIndex tx: neededTiles ) {
            bldr.append( "[" )
                    .append(new Double(tx.getX()).intValue() ).append(",")
                    .append(new Double(tx.getY()).intValue() ).append( "," )
                    .append(new Double(tx.getZ()).intValue() ).append("]");
        }
        logger.info("Requesting\n" + bldr);
    }

    private boolean fetchTileData(TextureCache textureCache, TileIndex tileIx, AbstractTextureLoadAdapter loadAdapter, TileFormat tileFormat, ZoomLevel zoom, ZoomedVoxelIndex farCorner) {
        boolean filledToEnd = true;
        try {
            if ( tileIx.getX() < 0 || tileIx.getY() < 0 || tileIx.getZ() < 0 ) {
                remainingTiles --;
                return false;
            }
            TextureData2dGL tileData = null;
            // First try to get image from cache...
            if ( (textureCache != null) && (textureCache.containsKey(tileIx)) ) {
                TileTexture tt = textureCache.get(tileIx);
                tileData = tt.getTextureData();
            }
            // ... if that fails, load the data right now.
            if (tileData == null) {
                if (loadAdapter instanceof BlockTiffOctreeLoadAdapter) {
                    tileData = ((BlockTiffOctreeLoadAdapter)loadAdapter).loadToRam(tileIx, false);
                }
                else {
                    tileData = loadAdapter.loadToRam(tileIx);
                }
            }
            if (tileData == null) {
                logger.info("Found no tile data for " + tileIx);
                filledToEnd = false;
            }
            else if (origin.getZ() >= 0) {
                // If the origin were negative, it would mean there was a
                // z-drill-in beyond the meaningful boundaries of the nascent
                //  volume, and no data would be available to be added.
                TileFormat.TileXyz tileXyz = new TileFormat.TileXyz(
                        tileIx.getX(), tileIx.getY(), tileIx.getZ());
                ZoomedVoxelIndex tileOrigin = tileFormat.zoomedVoxelIndexForTileXyz(
                        tileXyz, zoom, tileIx.getSliceAxis());
                // One Z-tile goes to one destination Z coordinate in this subvolume.
                int dstZ = tileOrigin.getZ() - origin.getZ(); // local Z coordinate
                // Y
                int startY = Math.max(origin.getY(), tileOrigin.getY());
                int endY = Math.min(farCorner.getY(), tileOrigin.getY() + tileData.getHeight());
                int overlapY = endY - startY;
                // X
                int startX = Math.max(origin.getX(), tileOrigin.getX());
                int endX = Math.min(farCorner.getX(), tileOrigin.getX() + tileData.getUsedWidth());
                int overlapX = endX - startX;
                // byte array offsets
                int pixelBytes = channelCount * bytesPerIntensity;
                int tileLineBytes = pixelBytes * tileData.getWidth();
                int subvolumeLineBytes = pixelBytes * extent.getX();
                // Where to start putting bytes into subvolume?
                // Probable source of bug: may not be getting proper start location in "subsequent" volume, and/or wrong overlap values.
                int dstOffset = dstZ * subvolumeLineBytes * extent.getY() // z plane offset
                        + (startY - origin.getY()) * subvolumeLineBytes // y scan-line offset
                        + (startX - origin.getX()) * pixelBytes;
                int srcOffset = (startY - tileOrigin.getY()) * tileLineBytes // y scan-line offset
                        + (startX - tileOrigin.getX()) * pixelBytes;
                // Copy one scan line at a time
OVERFLOW_LABEL:
                for (int y = 0; y < overlapY; ++y) {
                    for (int x = 0; x < overlapX; ++x) {
                        // TODO faster copy
                        for (int b = 0; b < pixelBytes; ++b) {
                            int d = dstOffset + x * pixelBytes + b;
                            int s = srcOffset + x * pixelBytes + b;
                            try {
                                bytes.put(d, tileData.getPixels().get(s));
                            } catch (Exception ex) {
                                logger.error("Failed to copy data into pixels buffer: " + ex.getMessage() + ".  Skipping remainder.");
                                /* for debugging */
                                if (logger.isDebugEnabled()  &&  d >= bytes.capacity()) {
                                    logger.info("overflow destination: {} vs {}.", d, bytes.capacity());
                                    logger.info("dstOffset={}; dstZ={}; extent-Y={}; subvolumeLineBytes={}", dstOffset, dstZ, extent.getY(), subvolumeLineBytes);
                                    logger.info("dstZ=tileOrigin.getZ() - origin.getZ(); tileOrigin.getZ()={}; origin.getZ()={}.", tileOrigin.getZ(), origin.getZ());
                                    logger.info("startX={}. endX={}. startY={}. endY={}", startX, endX, startY, endY);
                                    logger.info("Destination X={}, Y={}, dstZ={}. srcOffset={}.", x, y, dstZ, srcOffset);
                                    logger.info("overlapX={}, overlapY={}.   tileLineBytes={}.", overlapX, overlapY, tileLineBytes );
                                }
                                if (s >= tileData.getPixels().capacity()) {
                                    logger.info("overflow source: {} vs {}.", s, bytes.capacity());
                                }
                                if (s < 0) {
                                    logger.info("Underflow source: {} vs {}.", s, bytes.capacity());
                                }
                                if (d < 0) {
                                    logger.info("underflow destination: {} vs {}.", d, bytes.capacity());
                                }
                                break OVERFLOW_LABEL;
                            }
                        }
                    }
                    dstOffset += subvolumeLineBytes;
                    srcOffset += tileLineBytes;
                }

                // There is a slim chance this could be decremented to sub-zero,
                // since there are multiple threads using this method.  However,
                // we will avoid incurring the overhead of AtomicInteger by simple
                // accepting that risk (off-by-a-few is not terrible, here), and
                // simply ensuring the user never sees a negative remainder.
                remainingTiles--;
                int remaining = Math.max(0, remainingTiles);
                reportProgress(remaining, totalTiles);
            }
            
        } catch (AbstractTextureLoadAdapter.TileLoadError | AbstractTextureLoadAdapter.MissingTileException e) {
            // TODO Auto-generated catch block
            logger.error( "Request for {}..{} failed with error {}.", origin, extent, e.getMessage() );
            e.printStackTrace();
        }
        return filledToEnd;
    }

    private Set<TileIndex> getNeededTileSet(TileFormat tileFormat, ZoomedVoxelIndex farCorner, ZoomLevel zoom) {
        Set<TileIndex> neededTiles = new LinkedHashSet<>();
        // Load tiles from volume representation
        TileIndex tileMin0 = tileFormat.tileIndexForZoomedVoxelIndex(origin, CoordinateAxis.Z);
        TileIndex tileMax0 = tileFormat.tileIndexForZoomedVoxelIndex(farCorner, CoordinateAxis.Z);
        // Guard against y-flip. Make it so general it could find some other future situation too.
        // Enforce that tileMinCtrZ x/y/z are no larger than tileMaxCtrZ x/y/z
        TileIndex tileMin = new TileIndex(
                Math.min(tileMin0.getX(), tileMax0.getX()),
                Math.min(tileMin0.getY(), tileMax0.getY()),
                Math.min(tileMin0.getZ(), tileMax0.getZ()),
                tileMin0.getZoom(),
                tileMin0.getMaxZoom(),
                tileMin0.getIndexStyle(),
                tileMin0.getSliceAxis());
        TileIndex tileMax = new TileIndex(
                Math.max(tileMin0.getX(), tileMax0.getX()),
                Math.max(tileMin0.getY(), tileMax0.getY()),
                Math.max(tileMin0.getZ(), tileMax0.getZ()),
                tileMax0.getZoom(),
                tileMax0.getMaxZoom(),
                tileMax0.getIndexStyle(),
                tileMax0.getSliceAxis());
        for (int x = tileMin.getX(); x <= tileMax.getX(); ++x) {
            for (int y = tileMin.getY(); y <= tileMax.getY(); ++y) {
                for (int z = tileMin.getZ(); z <= tileMax.getZ(); ++z) {
                    neededTiles.add(new TileIndex(
                            x, y, z,
                        zoom.getLog2ZoomOutFactor(),
                        tileMin.getMaxZoom(),
                        tileMin.getIndexStyle(),
                        tileMin.getSliceAxis()));
                }
            }
        }
        
        return neededTiles;
    }

    private Set<TileIndex> getCenteredTileSet(TileFormat tileFormat, ZoomedVoxelIndex center, int[] dimensions, ZoomLevel zoom) {
        assert(dimensions[0] % 2 == 0) : "Dimension X must be divisible by 2";
        assert(dimensions[1] % 2 == 0) : "Dimension Y must be divisible by 2";
        assert(dimensions[2] % 2 == 0) : "Dimension Z must be divisible by 2";
        Set<TileIndex> neededTiles = new LinkedHashSet<>();
        // Load tiles from volume representation
	    ZoomedVoxelIndex centerTileMinVI = new ZoomedVoxelIndex(
	            zoom,
	            Math.min(center.getX() - dimensions[0]/2, center.getX() + dimensions[0]/2 + 1),
                Math.min(center.getY() - dimensions[1]/2, center.getY() + dimensions[1]/2 + 1),
                center.getZ());

        // Load tiles from volume representation
	    ZoomedVoxelIndex centerTileMaxVI = new ZoomedVoxelIndex(
	            zoom,
	            Math.max(center.getX() - dimensions[0]/2, center.getX() + dimensions[0]/2 + 1),
                Math.max(center.getY() - dimensions[1]/2, center.getY() + dimensions[1]/2 + 1),
                center.getZ());

        TileIndex tileMinCtrZ0 = tileFormat.tileIndexForZoomedVoxelIndex(centerTileMinVI, CoordinateAxis.Z);
        TileIndex tileMaxCtrZ0 = tileFormat.tileIndexForZoomedVoxelIndex(centerTileMaxVI, CoordinateAxis.Z);
        
        // Guard against y-flip. Make it so general it could find some other future situation too.
        // Enforce that tileMinCtrZ x/y/z are no larger than tileMaxCtrZ x/y/z
        // tileMinCtrZ is the minimum xy value for the tile at the center-z point.
        TileIndex tileMinCtrZ = new TileIndex(
                Math.min(tileMinCtrZ0.getX(), tileMaxCtrZ0.getX()),
                Math.min(tileMinCtrZ0.getY(), tileMaxCtrZ0.getY()),
                Math.min(tileMinCtrZ0.getZ(), tileMaxCtrZ0.getZ()),
                tileMinCtrZ0.getZoom(),
                tileMinCtrZ0.getMaxZoom(),
                tileMinCtrZ0.getIndexStyle(),
                tileMinCtrZ0.getSliceAxis());
        TileIndex tileMaxCtrZ = new TileIndex(
                Math.max(tileMinCtrZ0.getX(), tileMaxCtrZ0.getX()),
                Math.max(tileMinCtrZ0.getY(), tileMaxCtrZ0.getY()),
                Math.max(tileMinCtrZ0.getZ(), tileMaxCtrZ0.getZ()),
                tileMaxCtrZ0.getZoom(),
                tileMaxCtrZ0.getMaxZoom(),
                tileMaxCtrZ0.getIndexStyle(),
                tileMaxCtrZ0.getSliceAxis());

        int minZ = Integer.MAX_VALUE; // Seminal value.
        int maxZ = Integer.MIN_VALUE;
        
        for (int x = tileMinCtrZ.getX(); x <= tileMaxCtrZ.getX(); ++x) {
            for (int y = tileMinCtrZ.getY(); y <= tileMaxCtrZ.getY(); ++y) {
                TileIndex centerTileIndex = new TileIndex(
                        x,y,center.getZ(),
                            zoom.getLog2ZoomOutFactor(),
                            tileMinCtrZ.getMaxZoom(),
                            tileMinCtrZ.getIndexStyle(),
                            tileMinCtrZ.getSliceAxis()
                );
                
                neededTiles.add( centerTileIndex );
                TileIndex nextSlice = centerTileIndex;
                
                // Find the center forward in z.
                int totalPlanes = 0;
                for (int z = 0; z < dimensions[2]/2 - 1; ++z) {
                    nextSlice = nextSlice.nextSlice();
                    neededTiles.add( nextSlice );
                    if ( nextSlice.getZ() < minZ ) {
                        minZ = nextSlice.getZ();
                    }
                    if ( nextSlice.getZ() > maxZ ) {
                        maxZ = nextSlice.getZ();
                    }
                    totalPlanes++;
                }
                logger.debug("Center forward found " + totalPlanes + " planes.");

                // Find the center backward in z.
                nextSlice = centerTileIndex;
                for (int z = 0; z < dimensions[2]/2; ++z) {
                    nextSlice = nextSlice.previousSlice();
                    neededTiles.add( nextSlice );
                    if ( nextSlice.getZ() < minZ ) {
                        minZ = nextSlice.getZ();
                    }
                    if ( nextSlice.getZ() > maxZ ) {
                        maxZ = nextSlice.getZ();
                    }
                    totalPlanes++;
                }
                logger.debug("Center forward + center + center backward, found " + totalPlanes + " planes.");

            }
            logger.debug("Total tile set size is " + neededTiles.size());

            // Side Effect:  These values must be computed here, but they
            // are being used by other code at caller level.
            origin = new ZoomedVoxelIndex(
                    zoom,
                    center.getX() - dimensions[0]/2,
                    center.getY() - dimensions[1]/2,
                    Math.min(minZ, maxZ));
            extent = new VoxelIndex(
                    dimensions[0],
                    dimensions[1],
                    dimensions[2]);
        }
        
        return neededTiles;
    }
}
