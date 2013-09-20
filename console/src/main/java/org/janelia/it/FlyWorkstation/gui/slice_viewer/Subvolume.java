package org.janelia.it.FlyWorkstation.gui.slice_viewer;

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
import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileFormat.TileXyz;
import org.janelia.it.FlyWorkstation.octree.ZoomLevel;
import org.janelia.it.FlyWorkstation.octree.ZoomedVoxelIndex;
import org.janelia.it.FlyWorkstation.raster.VoxelIndex;

public class Subvolume {
	
	// private int extentVoxels[] = {0, 0, 0};
	private ZoomedVoxelIndex origin; // upper left front corner within parent volume
	private VoxelIndex extent; // width, height, depth
	private ByteBuffer bytes;
	private ShortBuffer shorts;
	private int bytesPerIntensity = 1;
	private int channelCount = 1;
	
	public Subvolume(
	        ZoomedVoxelIndex corner1,
	        ZoomedVoxelIndex corner2,
	        SharedVolumeImage wholeImage) 
	{
	    initialize(corner1, corner2, wholeImage);
	}
	
	private void initialize(ZoomedVoxelIndex corner1,
            ZoomedVoxelIndex corner2,
            SharedVolumeImage wholeImage) {
	    // Both corners must be the same zoom resolution
	    assert(corner1.getZoomLevel().equals(corner2.getZoomLevel()));
	    // Populate data fields
	    ZoomLevel zoom = corner1.getZoomLevel();
	    origin = new ZoomedVoxelIndex(
	            zoom,
	            Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));
	    ZoomedVoxelIndex farCorner = new ZoomedVoxelIndex(
                zoom,
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
	    extent = new VoxelIndex(
	            farCorner.getX() - origin.getX() + 1,
                farCorner.getY() - origin.getY() + 1,
                farCorner.getZ() - origin.getZ() + 1);
	    // Allocate raster memory
	    AbstractTextureLoadAdapter loadAdapter = wholeImage.getLoadAdapter();
	    TileFormat tileFormat = loadAdapter.getTileFormat();
	    bytesPerIntensity = tileFormat.getBitDepth()/8;
	    channelCount = tileFormat.getChannelCount();
	    int totalBytes = bytesPerIntensity 
	            * channelCount
	            * extent.getX() * extent.getY() * extent.getZ();
	    bytes = ByteBuffer.allocateDirect(totalBytes);
	    bytes.order(ByteOrder.nativeOrder());
	    if (bytesPerIntensity == 2)
	        shorts = bytes.asShortBuffer();
	    // Load tiles from volume representation
	    TileIndex tileMin0 = tileFormat.tileIndexForZoomedVoxelIndex(origin, CoordinateAxis.Z);
        TileIndex tileMax0 = tileFormat.tileIndexForZoomedVoxelIndex(farCorner, CoordinateAxis.Z);
        // Guard against y-flip. Make it so general it could find some other future situation too.
        // Enforce that tileMin x/y/z are no larger than tileMax x/y/z
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
        Set<TileIndex> neededTiles = new LinkedHashSet<TileIndex>();
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
        for (TileIndex tileIx : neededTiles) {
            try {
                TextureData2dGL tileData = loadAdapter.loadToRam(tileIx);
                TileXyz tileXyz = new TileXyz(
                        tileIx.getX(), tileIx.getY(), tileIx.getZ());
                ZoomedVoxelIndex tileOrigin = tileFormat.zoomedVoxelIndexForTileXyz(
                        tileXyz, zoom, tileIx.getSliceAxis());
                
                ShortBuffer sourceShorts = null;
                if (bytesPerIntensity == 2)
                    sourceShorts = tileData.getPixels().asShortBuffer();

                // One Z-tile goes to one destination Z coordinate in this subvolume.
                int dstZ = tileOrigin.getZ() - origin.getZ(); // local Z coordinate
                // Y
                int startY = Math.max(origin.getY(), tileOrigin.getY());
                int endY = Math.min(farCorner.getY(), tileOrigin.getY()+tileData.getHeight()-1);
                int overlapY = endY - startY + 1;
                // X
                int startX = Math.max(origin.getX(), tileOrigin.getX());
                int endX = Math.min(farCorner.getX(), tileOrigin.getX()+tileData.getUsedWidth()-1);
                int overlapX = endX - startX + 1;
                // byte array offsets
                int pixelBytes = channelCount * bytesPerIntensity;
                int tileLineBytes = pixelBytes * tileData.getWidth();
                int subvolumeLineBytes = pixelBytes * extent.getX();
                // Where to start putting bytes into subvolume?
                int dstOffset = dstZ * subvolumeLineBytes * extent.getY() // z plane offset
                        + (startY - origin.getY()) * subvolumeLineBytes // y scan-line offset
                        + (startX - origin.getX()) * pixelBytes;
                int srcOffset = (startY - tileOrigin.getY()) * tileLineBytes // y scan-line offset
                        + (startX - tileOrigin.getX()) * pixelBytes;
                // Copy one scan line at a time
                for (int y = 0; y < overlapY; ++y) {
                    for (int x = 0; x < overlapX; ++x) {
                        // TODO faster copy
                        for (int b = 0; b < pixelBytes; ++b) {
                            int d = dstOffset + x * pixelBytes + b;
                            int s = srcOffset + x * pixelBytes + b;
                            /* for debugging
                            if (d >= bytes.capacity()) {
                                System.out.println("overflow destination");
                            }
                            if (s >= tileData.getPixels().capacity()) {
                                System.out.println("overflow source");
                            }
                            if (bytesPerIntensity == 2) {
                                int value = sourceShorts.get(s/2) & 0xffff;
                                if (value > 40370) {
                                    System.out.println("large value");
                                }
                            }
                            */
                            bytes.put(d, tileData.getPixels().get(s));
                        }
                    }
                    dstOffset += subvolumeLineBytes;
                    srcOffset += tileLineBytes;
                }
            } catch (TileLoadError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MissingTileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
        TileFormat.VoxelXyz vix1 = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        corner1.getX(), corner1.getY(), corner1.getZ()));
        TileFormat.VoxelXyz vix2 = tileFormat.voxelXyzForMicrometerXyz(
                new TileFormat.MicrometerXyz(
                        corner2.getX(), corner2.getY(), corner2.getZ()));
        //
        ZoomedVoxelIndex zvix1 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vix1, 
                zoomLevel, CoordinateAxis.Z);
        ZoomedVoxelIndex zvix2 = tileFormat.zoomedVoxelIndexForVoxelXyz(
                vix2, 
                zoomLevel, CoordinateAxis.Z);
        //
        initialize(zvix1, zvix2, wholeImage);
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

    public long getIntensityGlobal(ZoomedVoxelIndex v1, int channelIndex) 
    {
        int c = channelIndex;
        int x = v1.getX() - origin.getX();
        int y = v1.getY() - origin.getY();
        int z = v1.getZ() - origin.getZ();
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
        if (bytesPerIntensity == 2)
            return shorts.get(offset);
        else
            return bytes.get(offset);
    }

    public ZoomedVoxelIndex getOrigin() {
        return origin;
    }
	
}
