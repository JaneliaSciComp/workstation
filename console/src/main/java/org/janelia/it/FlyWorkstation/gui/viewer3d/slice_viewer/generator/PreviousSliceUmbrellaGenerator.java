package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generate positive offset slice indices, as part of UmbrellaSliceGenerator
 * @author brunsc
 *
 */
public class PreviousSliceUmbrellaGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int sliceMin;
	private TileIndex index;
	private int stepCount = 0;
	
	public PreviousSliceUmbrellaGenerator(TileIndex seed, int sliceMin) {
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
		// Lower resolution as we get farther from center slice
		if (stepCount == 5) { // lower resolution farther from center
			TileIndex i = index.zoomOut();
			if (i != null)
				index = i;
		}
		if (stepCount == 50) { // lower resolution farther from center
			TileIndex i = index.zoomOut();
			if (i != null)
				index = i;
		}
		stepCount += 1;
		// Increment slice for next time.
		index = index.previousSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
