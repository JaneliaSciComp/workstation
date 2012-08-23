package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.zip.DataFormatException;

public class V3dRawImageStream 
implements Iterable<V3dRawImageStream.Slice>,
Iterator<V3dRawImageStream.Slice>
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
	
	private void loadHeader() 
	throws IOException, DataFormatException
	{
		// header is 43 bytes long
		ByteBuffer buffer = ByteBuffer.allocateDirect(43);
		inStream.read(buffer.array(), 0, 43);
		buffer.rewind();
		// Parse file type header string (24 bytes)
		headerKey = new String(buffer.array(), 0, 24);
		if (! headerKey.equals(V3DRAW_MAGIC_COOKIE)) {
			throw new DataFormatException(
					"Vaa3D raw file header mismatch: " + headerKey);
		}
		// Parse data endian (one byte)
		char endianChar = buffer.getChar(); // read endianness
		if (endianChar == 'B')
			endian = ByteOrder.BIG_ENDIAN;
		else if (endianChar == 'L')
			endian = ByteOrder.LITTLE_ENDIAN;
		else
			throw new DataFormatException(
					"Unrecognized endian field: " + endianChar);
		buffer.order(endian); // affects interpretation of subsequent multi-byte numbers
		// Parse number of bytes per pixel
		inStream.read(buffer.array(), 0, 2); // read number of bytes per pixel
		pixelBytes = buffer.getShort();
		if ( (pixelBytes < 0) || (pixelBytes > 4) )
			throw new DataFormatException(
					"Illegal number of pixel bytes: " + pixelBytes);
		// Parse dimensions of volume - four four-byte values = 16 bytes
		inStream.read(buffer.array(), 0, 16);
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
	
	@Override
	public boolean hasNext() {
		int sliceCount = dimensions[2] * dimensions[3];
		return currentSlice.getSliceIndex() < (sliceCount - 1);
	}

	@Override
	public Iterator<Slice> iterator() {
		return this;
	}

	public void loadNextSlice() 
	throws IOException
	{
		currentSlice.read(inStream);
	}
	
	@Override
	public Slice next() 
	{
		try {
			currentSlice.read(inStream);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		return currentSlice;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	class Slice 
	{
		private int sliceByteCount;
		private ByteBuffer sliceBuffer;
		private int sliceIndex;
		
		public Slice(int sizeX, int sizeY, int pixelBytes) {
			sliceByteCount = sizeX * sizeY * pixelBytes;
			sliceBuffer = ByteBuffer.allocateDirect(sliceByteCount);
			sliceIndex = -1;
		}
		
		public int getSliceIndex() {
			return sliceIndex;
		}
		
		public void read(InputStream inStream) 
		throws IOException
		{
			inStream.read(sliceBuffer.array(), 0, sliceByteCount);
			++sliceIndex;
		}
	}

}
