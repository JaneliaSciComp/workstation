package org.janelia.workstation.gui.large_volume_viewer.generator;

import java.util.Iterator;

import org.janelia.workstation.gui.large_volume_viewer.TileIndex;

/**
 * Generate positive offset slice indices, as part of UmbrellaSliceGenerator
 * @author brunsc
 *
 */
public class PreviousSliceUmbrellaGenerator implements Iterator<TileIndex>, Iterable<TileIndex> {
	private static final int _2X_ZOOMOUT_STEP = 8;
	private static final int _4X_ZOOMOUT_STEP = 12;

	private int sliceMin;
	private TileIndex index;
	private int stepCount = 0;
	
	PreviousSliceUmbrellaGenerator(TileIndex seed, int sliceMin) {
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
		if (stepCount == _2X_ZOOMOUT_STEP) { // lower resolution farther from center
			TileIndex i = index.zoomOut();
			if (i != null)
				index = i;
		} else  if (stepCount == _4X_ZOOMOUT_STEP) { // lower resolution farther from center
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
