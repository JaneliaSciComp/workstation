package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.zip.DataFormatException;

public class V3dRawImageStream 
{
	public static final String V3DRAW_MAGIC_COOKIE = 
		"raw_image_stack_by_hpeng";
	
	private InputStream inStream;
	// File metadata fields
	private String headerKey;
	private int pixelBytes = 0;
	private ByteOrder endian = ByteOrder.LITTLE_ENDIAN;
	private int[] dimensions = {0,0,0,0};
	// Keep one slice in memory for streaming
	private Slice currentSlice;

	public V3dRawImageStream(InputStream input) {
		inStream = input;
		try {
			loadHeader();
		}
		catch (IOException exc) {
			throw new IllegalArgumentException(exc);
		}
		catch (DataFormatException exc) {
			throw new IllegalArgumentException(exc);
		}
	}
	
	public int getDimension(int index) {
		return dimensions[index];
	}
	
	public int getPixelBytes() {
		return pixelBytes;
	}
	
	private void loadHeader() 
	throws IOException, DataFormatException
	{
		// header is 43 bytes long
		byte[] buffer0 = new byte[43];
		ByteBuffer buffer = ByteBuffer.wrap(buffer0);
		inStream.read(buffer.array(), 0, 43);
		buffer.rewind();
		// Parse file type header string (24 bytes)
		headerKey = new String(buffer.array(), 0, 24);
		if (! headerKey.equals(V3DRAW_MAGIC_COOKIE)) {
			throw new DataFormatException(
					"Vaa3D raw file header mismatch: " + headerKey);
		}
		// Parse data endian (one byte)
		buffer.position(24);
		char endianChar = (char)buffer.get(); // read endianness
		if (endianChar == 'B')
			endian = ByteOrder.BIG_ENDIAN;
		else if (endianChar == 'L')
			endian = ByteOrder.LITTLE_ENDIAN;
		else
			throw new DataFormatException(
					"Unrecognized endian field: " + endianChar);
		buffer.order(endian); // affects interpretation of subsequent multi-byte numbers
		// Parse number of bytes per pixel
		pixelBytes = buffer.getShort();
		if ( (pixelBytes <= 0) || (pixelBytes > 4) )
			throw new DataFormatException(
					"Illegal number of pixel bytes: " + pixelBytes);
		// Parse dimensions of volume - four four-byte values = 16 bytes
		dimensions = new int[]{
				buffer.getInt(),
				buffer.getInt(),
				buffer.getInt(),
				buffer.getInt()};
		// End of header!
		// Allocate slice
		currentSlice = new Slice(dimensions[0], dimensions[1], pixelBytes);
		currentSlice.read(inStream);
	}
	
	public Slice getCurrentSlice() {
		return currentSlice;
	}
	
	public void loadNextSlice() 
	throws IOException
	{
		currentSlice.read(inStream);
	}
	
	class Slice 
	{
		private int sliceByteCount;
		private ByteBuffer sliceBuffer;
		private int sliceIndex;
		private int sx, sy, pixelBytes;
		
		public Slice(int sizeX, int sizeY, int pixelBytes) {
			sliceByteCount = sizeX * sizeY * pixelBytes;
			byte[] buffer0 = new byte[sliceByteCount];
			sliceBuffer = ByteBuffer.wrap(buffer0);
			sliceIndex = -1;
			sx = sizeX;
			sy = sizeY;
			this.pixelBytes = pixelBytes;
		}
		
		public int getSliceIndex() {
			return sliceIndex;
		}
		
		public int getValue(int x, int y) {
			int index = x + sx * y;
			if (pixelBytes == 1) {
				return sliceBuffer.get(index);
			}
			else if (pixelBytes == 2)
				return sliceBuffer.getShort(index);
			else
				return sliceBuffer.getInt(index);
		}
		
		public void read(InputStream inStream) 
		throws IOException
		{
			inStream.read(sliceBuffer.array(), 0, sliceByteCount);
			++sliceIndex;
		}
	}

}
