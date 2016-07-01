package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.jacs.shared.lvv.*;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.octree.ZoomLevel;
import org.janelia.it.jacs.shared.octree.ZoomedVoxelIndex;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.janelia.it.jacs.model.util.ThreadUtils;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.tracing.VoxelPosition;
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
	 * raster data over the network.  This is called only from the
     * 3D viewer feed path.
	 * 
	 * @param center volume around here, in 3D
	 * @param wholeImage 
     * @param micrometerVoxels as might be supplied by tile format, 1/min-voxelMicrometers.
     * @param dimensions how large to make the volume.
	 * @param textureCache
     * @param progressMonitor for reporting relative completion.
	 */
    public Subvolume(
            Vec3 center,
            SharedVolumeImage wholeImage,
            double micrometerVoxels,
            ZoomLevel zoom,
            int[] dimensions,
            TextureCache textureCache,
            IndeterminateNoteProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
        initializeFor3D(center, micrometerVoxels, zoom, dimensions, wholeImage, textureCache);
    }
    
    /**
     * Initializes the sub volume for 3 dimensional fetching of texture data.
     * 
     * @param center middle point.
     * @param dimensions how large a chunk to return.
     * @param wholeImage whole volume's 'image'
     * @param textureCache existing fetches live here.
     */
	private void initializeFor3D(Vec3 center,
            double micrometerVoxels,
            final ZoomLevel zoom,
            int[] dimensions,
            SharedVolumeImage wholeImage,
            final TextureCache textureCache)
	{
                
	    final AbstractTextureLoadAdapter loadAdapter = wholeImage.getLoadAdapter();
	    final TileFormat tileFormat = loadAdapter.getTileFormat();
        allocateRasterMemory(tileFormat, dimensions);

        Set<TileIndex> neededTiles = getCenteredTileSet(tileFormat, center, micrometerVoxels, dimensions, zoom);
        if (logger.isDebugEnabled()) {
            logTileRequest(neededTiles);
        }
        
        final ZoomedVoxelIndex farCorner = new ZoomedVoxelIndex(
                zoom,
                origin.getX() + dimensions[0],
                origin.getY() + dimensions[1],
                origin.getZ() + dimensions[2]
        );
        multiThreadedFetch(neededTiles, textureCache, loadAdapter, tileFormat, zoom, farCorner);
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
        TileFormat.VoxelXyz vix1 = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        corner1.getX(), corner1.getY(), corner1.getZ()
                )
        );
        TileFormat.VoxelXyz vix2 = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        corner2.getX(), corner2.getY(), corner2.getZ()
                )
        );
        
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
        
        final AbstractTextureLoadAdapter loadAdapter = wholeImage.getLoadAdapter();
	    final TileFormat tileFormat = loadAdapter.getTileFormat();        
        
        allocateRasterMemory(tileFormat);

        Set<TileIndex> neededTiles = getNeededTileSet(tileFormat, farCorner, zoom);
        if (logger.isDebugEnabled()) {
            logTileRequest(neededTiles);
        }
        multiThreadedFetch(neededTiles, textureCache, loadAdapter, tileFormat, zoom, farCorner);
        
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

    public int getIntensityGlobal(VoxelPosition p, int channelIndex)
    {
        return getIntensityGlobal(p.getX(), p.getY(), p.getZ(), channelIndex);
    }
    public int getIntensityGlobal(ZoomedVoxelIndex v1, int channelIndex)
    {
        return getIntensityGlobal(
                v1.getX(), v1.getY(), v1.getZ(),
            channelIndex);
    }

    public int getIntensityGlobal(Integer x, Integer y, Integer z, int channelIndex)
    {
        return getIntensityLocal(new VoxelIndex(
            x - origin.getX(),
            y - origin.getY(),
            z - origin.getZ()),
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
        logger.info("===SubVolume:: Requesting: " + bldr);
    }

    private void multiThreadedFetch(Set<TileIndex> neededTiles, final TextureCache textureCache, final AbstractTextureLoadAdapter loadAdapter, final TileFormat tileFormat, final ZoomLevel zoom, final ZoomedVoxelIndex farCorner) {
        ExecutorService executorService = ThreadUtils.establishExecutor(N_THREADS, new CustomNamedThreadFactory("SubvolumeFetch"));
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
                    tileData = new TextureData2dGL(((BlockTiffOctreeLoadAdapter)loadAdapter).loadToRam(tileIx, false));
                }
                else {
                    tileData = new TextureData2dGL(loadAdapter.loadToRam(tileIx));
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
                //System.out.println("TileXyz=" + tileXyz.getX() + "," + tileXyz.getY() + "," + tileXyz.getZ() + ". Start Y=" + startY + ", End Y=" + endY + ", overlapY=" + overlapY);
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

    private void allocateRasterMemory(final TileFormat tileFormat) {
        int[] dimensions = new int[3];
        dimensions[0] = extent.getX();
        dimensions[1] = extent.getY();
        dimensions[2] = extent.getZ();
        allocateRasterMemory(tileFormat, dimensions);
    }
    
    private void allocateRasterMemory(final TileFormat tileFormat, int[] dimensions) {
        bytesPerIntensity = tileFormat.getBitDepth()/8;
        channelCount = tileFormat.getChannelCount();
        int totalBytes = bytesPerIntensity
                * channelCount
                * dimensions[0] * dimensions[1] * dimensions[2];
        bytes = ByteBuffer.allocateDirect(totalBytes);
        bytes.order(ByteOrder.nativeOrder());
        if (bytesPerIntensity == 2)
            shorts = bytes.asShortBuffer();
    }

    private Set<TileIndex> getNeededTileSet(TileFormat tileFormat, ZoomedVoxelIndex farCorner, ZoomLevel zoom) {
        // Load tiles from volume representation
        final CoordinateAxis sliceAxis = CoordinateAxis.Z;
        
        TileIndex tileMin0 = tileFormat.tileIndexForZoomedVoxelIndex(origin, sliceAxis);
        TileIndex tileMax0 = tileFormat.tileIndexForZoomedVoxelIndex(farCorner, sliceAxis);
        
        // Guard against y-flip. Make it so general it could find some other future situation too.
        // Enforce that tileMinCtrZ x/y/z are no larger than tileMaxCtrZ x/y/z
        TileIndex tileMin = new TileIndex(
                Math.min(tileMin0.getX(), tileMax0.getX()),
                Math.min(tileMin0.getY(), tileMax0.getY()),
                Math.min(tileMin0.getZ(), tileMax0.getZ()),
                tileMin0.getZoom(),
                tileMin0.getMaxZoom(),
                tileMin0.getIndexStyle(),
                sliceAxis);
        TileIndex tileMax = new TileIndex(
                Math.max(tileMin0.getX(), tileMax0.getX()),
                Math.max(tileMin0.getY(), tileMax0.getY()),
                Math.max(tileMin0.getZ(), tileMax0.getZ()),
                tileMax0.getZoom(),
                tileMax0.getMaxZoom(),
                tileMax0.getIndexStyle(),
                sliceAxis);
        
        final int minWidth = tileMin.getX();
        final int maxWidth = tileMax.getX();
        final int minHeight = tileMin.getY();
        final int maxHeight = tileMax.getY();
        final int minDepth = tileMin.getZ();
        final int maxDepth = tileMax.getZ();
        
        final int[] xyzFromWhd = new int[] {
            0, 1, 2
        };

        return createTileIndexesOverRanges(
                minDepth, maxDepth, 
                minWidth, maxWidth,
                minHeight, maxHeight,
                xyzFromWhd,
                zoom,
                tileMin.getMaxZoom(),
                tileMin.getIndexStyle(),
                sliceAxis
        );
                
    }
    
    private TileIndex tileIndexForZoomedVoxelIndex(
            TileFormat tileFormat,
            ZoomedVoxelIndex ix,
            CoordinateAxis sliceDirection) 
    {
        int zoom = ix.getZoomLevel().getLog2ZoomOutFactor();
        TileFormat.TileXyz tileXyz = tileXyzForZoomedVoxelIndex(
                ix, 
                sliceDirection,
                tileFormat);
        int zoomMax = tileFormat.getZoomLevelCount() - 1;
        return new TileIndex(
                tileXyz.getX(), tileXyz.getY(), tileXyz.getZ(),
                zoom, zoomMax, tileFormat.getIndexStyle(), sliceDirection);
    }

	/**
	 * TileIndex xyz containing ZoomedVoxel. BORROWED from TilFormat.
	 */
	private TileFormat.TileXyz tileXyzForZoomedVoxelIndex(ZoomedVoxelIndex z, CoordinateAxis sliceAxis, TileFormat tileFormat) {
		int xyz[] = {z.getX(), z.getY(), z.getZ()};
		// Invert Y axis to convert to Raveler convention from image convention.
        int[] volumeSize = tileFormat.getVolumeSize();
        int[] tileSize = tileFormat.getTileSize();
		int zoomFactor = z.getZoomLevel().getZoomOutFactor();
		int maxZoomVoxelY = volumeSize[1] / zoomFactor - 1;
		xyz[1] = maxZoomVoxelY - xyz[1];
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if (i == depthAxis)
				continue; // Don't scale depth axis
            else if (i == (depthAxis + 1) % 3) {
				xyz[i] = (xyz[i] - tileFormat.getOrigin()[i])/tileSize[i]; // scale horizontal                
            }
			else
				xyz[i] = xyz[i]/tileSize[i]; // scale vertical
		}
		return new TileFormat.TileXyz(xyz[0], xyz[1], xyz[2]);
	}

    /** Called from 3D feed. */
    private Set<TileIndex> getCenteredTileSet(TileFormat tileFormat, Vec3 center, double micrometerVoxels, int[] dimensions, ZoomLevel zoomLevel) {
        assert(dimensions[0] % 2 == 0) : "Dimension X must be divisible by 2";
        assert(dimensions[1] % 2 == 0) : "Dimension Y must be divisible by 2";
        assert(dimensions[2] % 2 == 0) : "Dimension Z must be divisible by 2";

        int[] xyzFromWhd = new int[] { 0, 1, 2 };
        CoordinateAxis sliceAxis = CoordinateAxis.Z;
        ViewBoundingBox voxelBounds = tileFormat.findViewBounds(
                dimensions[0], dimensions[1], center, micrometerVoxels, xyzFromWhd
        );
        TileBoundingBox tileBoundingBox = tileFormat.viewBoundsToTileBounds(xyzFromWhd, voxelBounds, zoomLevel.getLog2ZoomOutFactor() );

        // Now I have the tile outline.  Can just iterate over that, and for all
        // required depth.
        TileIndex.IndexStyle indexStyle = tileFormat.getIndexStyle();
        int zoomMax = tileFormat.getZoomLevelCount() - 1;

        double halfDepth = dimensions[2] / 2.0;
        BoundingBox3d bb = tileFormat.calcBoundingBox();
        int maxDepth = this.calcZCoord(bb, xyzFromWhd, tileFormat, (int)(center.getZ() + halfDepth));
        int minDepth = maxDepth - dimensions[xyzFromWhd[2]];

        // Side Effect:  origin and extent must be computed here, but they
        // are being used by other code at caller level.
        TileFormat.VoxelXyz vox = calcLeastCorner(center, tileFormat, xyzFromWhd, dimensions, minDepth);
        origin = tileFormat.zoomedVoxelIndexForVoxelXyz(vox, zoomLevel, sliceAxis);
        extent = new VoxelIndex(
                dimensions[0],
                dimensions[1],
                dimensions[2]);		        

        int minWidth = tileBoundingBox.getwMin();
        int maxWidth = tileBoundingBox.getwMax();
        int minHeight = tileBoundingBox.gethMin();
        int maxHeight = tileBoundingBox.gethMax();
        return createTileIndexesOverRanges(
                minDepth, maxDepth,
                minWidth, maxWidth,
                minHeight, maxHeight,
                xyzFromWhd,
                zoomLevel,
                zoomMax,
                indexStyle,
                sliceAxis);
    }

    private Set<TileIndex> createTileIndexesOverRanges(
            int minDepth, int maxDepth,
            int minWidth, int maxWidth,
            int minHeight, int maxHeight,
            int[] xyzFromWhd, 
            ZoomLevel zoomLevel,
            int zoomMax,
            TileIndex.IndexStyle indexStyle,
            CoordinateAxis sliceAxis) {

        Set<TileIndex> neededTiles = new LinkedHashSet<>();
        for (int d = minDepth; d < maxDepth; d++) {
            for (int w = minWidth; w <= maxWidth; ++w) {
                for (int h = minHeight; h <= maxHeight; ++h) {
                    int whd[] = {w, h, d};                    
                    TileIndex key = new TileIndex(
                            whd[xyzFromWhd[0]],
                            whd[xyzFromWhd[1]],
                            whd[xyzFromWhd[2]],
                            zoomLevel.getLog2ZoomOutFactor(),
                            zoomMax, indexStyle, sliceAxis);
                    neededTiles.add(key);
                    
                }
            }
        }
        return neededTiles;
    }
    
    /**
     * Find the least corner containing the center vec, and within the
     * size of rectangular solid dictated by the dimensions given.
     * 
     * @param center ideal middle of rect solid.
     * @param tileFormat for convenience methods.
     * @param xyzFromWhd indirection for axial indices.
     * @param dimensions sizes for two axes of the rect solid.
     * @param minDepth size for depth axis of rect solid.
     * @return least corner.
     */
    private TileFormat.VoxelXyz calcLeastCorner(Vec3 center, TileFormat tileFormat, int[] xyzFromWhd, int[] dimensions, int minDepth) {
        TileFormat.VoxelXyz vox = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        (int)(center.getX()), 
                        (int)(center.getY()),
                        (int)(center.getZ())
                )
        );
        // NOTE: modified coordinate handling.
        // Other two coords first divide by microns, and then subtract origin.
        // Not so for the x: opposite order of operations: first subtracting
        // origin, and then dividing by micrometers.
        int[] voxelizedCoords = {
            vox.getX() - dimensions[xyzFromWhd[0]]/2,
            vox.getY() - dimensions[xyzFromWhd[1]]/2,
            minDepth
        };
        vox = new TileFormat.VoxelXyz(voxelizedCoords);
        return vox;
    }

    private int calcZCoord(BoundingBox3d bb, int[] xyzFromWhd, TileFormat tileFormat, int focusDepth) {
        double zVoxMicrons = tileFormat.getVoxelMicrometers()[xyzFromWhd[2]];
        int dMin = (int)(bb.getMin().get(xyzFromWhd[2])/zVoxMicrons + 0.5);
        int dMax = (int)(bb.getMax().get(xyzFromWhd[2])/zVoxMicrons - 0.5);
        int absoluteTileDepth = (int)Math.round(focusDepth / zVoxMicrons - 0.5);
        absoluteTileDepth = Math.max(absoluteTileDepth, dMin);
        absoluteTileDepth = Math.min(absoluteTileDepth, dMax);
        return absoluteTileDepth - tileFormat.getOrigin()[xyzFromWhd[2]];
    }
}
