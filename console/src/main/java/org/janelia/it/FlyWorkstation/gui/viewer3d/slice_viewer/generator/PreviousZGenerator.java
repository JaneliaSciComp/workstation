package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generate positive offset Z indices, as part of UmbrellaZGenerator
 * @author brunsc
 *
 */
public class PreviousZGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int zMin;
	private TileIndex index;
	
	public PreviousZGenerator(TileIndex seed, int zMin) {
		this.zMin = zMin;
		index = seed.previousSlice();
	}
	
	@Override
	public Iterator<TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return index.getZ() >= zMin;
	}

	@Override
	public TileIndex next() {
		TileIndex result = index;
		// Increment Z for next time.
		index = index.previousSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
