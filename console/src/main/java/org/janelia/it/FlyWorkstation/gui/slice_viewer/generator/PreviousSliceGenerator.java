package org.janelia.it.FlyWorkstation.gui.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileIndex;

/**
 * Generate positive offset slice indices, as part of SliceGenerator
 * @author brunsc
 *
 */
public class PreviousSliceGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int sliceMin;
	private TileIndex index;
	
	public PreviousSliceGenerator(TileIndex seed, int sliceMin) {
		this.sliceMin = sliceMin;
		index = seed.previousSlice();
	}
	
	@Override
	public Iterator<TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return index.getCoordinate(index.getSliceAxis().index()) >= sliceMin;
	}

	@Override
	public TileIndex next() {
		TileIndex result = index;
		// Increment slice for next time.
		index = index.previousSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
