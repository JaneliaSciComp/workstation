package org.janelia.it.workstation.gui.slice_viewer.generator;

import java.util.Iterator;

/**
 * Generate positive offset slice indices, as part of UmbrellaZGenerator
 * @author brunsc
 *
 */
public class NextSliceUmbrellaGenerator 
implements Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex>, Iterable<org.janelia.it.workstation.gui.slice_viewer.TileIndex>
{
	private int sliceMax;
	private org.janelia.it.workstation.gui.slice_viewer.TileIndex index;
	private int stepCount = 0;
	
	public NextSliceUmbrellaGenerator(org.janelia.it.workstation.gui.slice_viewer.TileIndex seed, int sliceMax) {
		this.sliceMax = sliceMax;
		index = seed.nextSlice();
	}
	
	@Override
	public Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		int axIx = index.getSliceAxis().index();
		return index.getCoordinate(axIx) <= sliceMax;
	}

	@Override
	public org.janelia.it.workstation.gui.slice_viewer.TileIndex next() {
		org.janelia.it.workstation.gui.slice_viewer.TileIndex result = index;
		// Lower resolution as we get farther from center slice
		if (stepCount == 5) { // lower resolution farther from center
			org.janelia.it.workstation.gui.slice_viewer.TileIndex i = index.zoomOut();
			if (i != null)
				index = i;
		}
		if (stepCount == 50) { // lower resolution farther from center
			org.janelia.it.workstation.gui.slice_viewer.TileIndex i = index.zoomOut();
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
