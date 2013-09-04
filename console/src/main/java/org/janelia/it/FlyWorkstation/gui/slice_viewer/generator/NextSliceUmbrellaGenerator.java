package org.janelia.it.FlyWorkstation.gui.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.TileIndex;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;

/**
 * Generate positive offset slice indices, as part of UmbrellaZGenerator
 * @author brunsc
 *
 */
public class NextSliceUmbrellaGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int sliceMax;
	private TileIndex index;
	private int stepCount = 0;
	
	public NextSliceUmbrellaGenerator(TileIndex seed, int sliceMax) {
		this.sliceMax = sliceMax;
		index = seed.nextSlice();
	}
	
	@Override
	public Iterator<TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		int axIx = index.getSliceAxis().index();
		return index.getCoordinate(axIx) <= sliceMax;
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
		index = index.nextSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
