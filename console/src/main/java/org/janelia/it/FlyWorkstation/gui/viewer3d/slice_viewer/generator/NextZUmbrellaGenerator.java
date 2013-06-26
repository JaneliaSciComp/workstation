package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generate positive offset Z indices, as part of UmbrellaZGenerator
 * @author brunsc
 *
 */
public class NextZUmbrellaGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int zMax;
	private TileIndex index;
	private int stepCount = 0;
	
	public NextZUmbrellaGenerator(TileIndex seed, int zMax) {
		this.zMax = zMax;
		index = seed.nextSlice();
	}
	
	@Override
	public Iterator<TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return index.getZ() <= zMax;
	}

	@Override
	public TileIndex next() {
		TileIndex result = index;
		// Lower resolution as we get farther from center Z
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
		// Increment Z for next time.
		index = index.nextSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
