package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generate positive offset slice indices, as part of SliceGenerator
 * @author brunsc
 *
 */
public class NextSliceGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int sliceMax;
	private TileIndex index;
	
	public NextSliceGenerator(TileIndex seed, int sliceMax) {
		this.sliceMax = sliceMax;
		index = seed.nextSlice();
	}
	
	@Override
	public Iterator<TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return index.getCoordinate(index.getSliceAxis().index()) <= sliceMax;
	}

	@Override
	public TileIndex next() {
		TileIndex result = index;
		// Increment slice for next time.
		index = index.nextSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
