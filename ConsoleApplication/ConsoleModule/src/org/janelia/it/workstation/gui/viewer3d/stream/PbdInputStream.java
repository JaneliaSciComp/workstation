package org.janelia.it.workstation.gui.viewer3d.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class PbdInputStream extends FilterInputStream 
{
	enum State {
		STATE_BEGIN, // Ready to start new run of bytes
		STATE_LITERAL, // Within a run of direct copying
		STATE_DIFFERENCE, // Within a run of difference encoding
		STATE_DIFFERENCE_SUBPIXEL, // Part way through unpacking a single compressed difference byte
		STATE_REPEAT // Within a run of runlength encoding
	}
	protected State state = State.STATE_BEGIN;
	protected int leftToFill = 0; // How many bytes left in the current run

	public static PbdInputStream createPbdInputStream(InputStream in, int bytesPerPixel, ByteOrder byteOrder)
	{
		if (bytesPerPixel == 1)
			return new Pbd8InputStream(in);
		else if (bytesPerPixel == 2)
			return new Pbd16InputStream(in, byteOrder);
		throw new IllegalArgumentException("Unsupported bytes per pixel "+bytesPerPixel);
	}

	protected PbdInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void mark(int readLimit) {}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@Override
	public int read() 
	throws IOException
	{
		byte[] b = new byte[1];
		try {
			read(b, 0, 1);
			return b[0] & 0xff;
		} catch (IOException exc) {
			return -1;
		}
	}
	
	@Override
	public int read(byte[] b) 
	throws IOException
	{
		return read(b, 0, b.length);
	}
	
	
	@Override
	public void reset() 
	throws IOException
	{
		throw new IOException();
	}
}
