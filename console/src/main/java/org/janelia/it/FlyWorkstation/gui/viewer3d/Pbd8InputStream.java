package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Decompresses a binary InputStream using Sean Murphy's fast PBD 
 * pack-bits plus difference encoding.
 * Adapted from ImageLoader.cpp in Vaa3d project.
 * 
 * Used by V3dRawImageStream class.
 * 
 * @author brunsc
 *
 */
public class Pbd8InputStream extends FilterInputStream 
{
	enum State {
		STATE_BEGIN, // Ready to start new run of bytes
		STATE_LITERAL, // Within a run of direct copying
		STATE_DIFFERENCE, // Within a run of difference encoding
		STATE_DIFFERENCE_SUBPIXEL, // Part way through unpacking a single compressed difference byte
		STATE_REPEAT // Within a run of runlength encoding
	}
	private State state = State.STATE_BEGIN;
	
	private int leftToFill = 0; // How many bytes left in the current run
	private final byte mask = 0x0003;
	private byte decompressionPrior = 0; // Value of locally canonical voxel
	private byte repeatValue = 0; // Current repeat run value
	// These differenceGroup values are global only because of the rare
	// case where a single difference unpacking crosses a read buffer boundary.
	private byte[] differenceGroup = new byte[4]; // set of 4 unpacked difference values
	private int dgPos = 0; // current position in differenceGroup
	private int fillNumber = 0; // number of used positions in differenceGroup

	protected Pbd8InputStream(InputStream in) {
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
		read(b, 0, 1);
		return b[0] & 0xff;
	}
	
	@Override
	public int read(byte[] b) 
	throws IOException
	{
		return read(b, 0, b.length);
	}
	
	@Override
	public int read(byte[] b, int off, int len) 
	throws IOException
	{
		if (len < 1) return 0;
		
		ByteBuffer out = ByteBuffer.wrap(b, off, len);
		while (out.hasRemaining())
		{
			if (state == State.STATE_BEGIN)
			{
				// Read one byte
				int value = in.read(); // unsigned
				assert(value >= 0);
				if (value < 33) { // literal 0-32
					state = State.STATE_LITERAL;
					leftToFill = value + 1;
				}
				else if (value < 128) {
					state = State.STATE_DIFFERENCE;
					leftToFill = value - 32;
				}
				else { // Repeat 128-255
					state = State.STATE_REPEAT;
					leftToFill = value - 127;
					repeatValue = (byte)in.read();
				}
			}
			else if (state == State.STATE_LITERAL)
			{
				int numBytesToRead = Math.min(out.remaining(), leftToFill);
				in.read(out.array(), off+out.position(), numBytesToRead); // copy block
				out.position(out.position() + numBytesToRead);
				leftToFill -= numBytesToRead;
				if (leftToFill == 0) {
					state = State.STATE_BEGIN;
					leftToFill = 1;
				}
				decompressionPrior = out.get(out.position() - 1);
			}
			else if (state == State.STATE_DIFFERENCE)
			{
				// TODO - difference bytes might overflow OUT buffer
				while ( (leftToFill > 0) && out.hasRemaining() ) {
	                fillNumber = (leftToFill < 4 ? leftToFill : 4);
	                byte sourceChar = (byte)in.read();
	                
	                byte p0 = (byte)(sourceChar & mask);
	                sourceChar >>>= 2;
	                byte p1 = (byte)(sourceChar & mask);
	                sourceChar >>>= 2;
	                byte p2 = (byte)(sourceChar & mask);
	                sourceChar >>>= 2;
	                byte p3 = (byte)(sourceChar & mask);
	                
	                // Precompute the sequence of four possible difference 
	                // bytes before trying to write them.
	                byte[] pv = differenceGroup;
	                pv[0] = (byte)((p0==3 ? -1:p0) + decompressionPrior);
	                pv[1] = (byte)(pv[0]+(p1==3?-1:p1));
	                pv[2] = (byte)((p2==3?-1:p2)+pv[1]);
	                pv[3] = (byte)((p3==3?-1:p3)+pv[2]);
	                dgPos = 0;
	                
	                while (dgPos < fillNumber) {
	                		// multiple difference bytes might overflow OUT buffer
	                		if (out.hasRemaining()) {
	                			out.put(pv[dgPos++]);
	                			leftToFill--;
	                		} else {
	                			// Ouch, output buffer ended in the middle of unpacking one byte
	                			state = State.STATE_DIFFERENCE_SUBPIXEL;	                			
	                		}
	                }

	                decompressionPrior = out.get(out.position() - 1);
				}
				if (leftToFill < 1)
					state = State.STATE_BEGIN;
			}
			else if (state == State.STATE_DIFFERENCE_SUBPIXEL)
			{
				state = State.STATE_DIFFERENCE; // Initially assume we will return to DIFFERENCE_STATE
                byte[] pv = differenceGroup;
                while (dgPos < fillNumber) {
                		// multiple difference bytes might overflow OUT buffer
                		if (out.hasRemaining()) {
                			out.put(pv[dgPos++]);
                			leftToFill--;
                		} else {
                			// Ouch, output buffer ended in the middle of unpacking one byte AGAIN
                			state = State.STATE_DIFFERENCE_SUBPIXEL;	                			
                		}
                }
			}
			else if (state == State.STATE_REPEAT)
			{
				int repeatCount = Math.min(leftToFill, out.remaining());
				for (int j = 0; j < repeatCount; ++j)
					out.put(repeatValue);
				leftToFill -= repeatCount;
				if (leftToFill < 1)
					state = State.STATE_BEGIN;
				decompressionPrior = repeatValue;
			}
		}
		return out.position();
	}
	
	@Override
	public void reset() 
	throws IOException
	{
		throw new IOException();
	}
}
