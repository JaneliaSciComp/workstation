package org.janelia.it.workstation.gui.slice_viewer.generator;

import java.util.Iterator;

/**
 * Generate positive offset slice indices, as part of UmbrellaSliceGenerator
 * @author brunsc
 *
 */
public class PreviousSliceUmbrellaGenerator 
implements Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex>, Iterable<org.janelia.it.workstation.gui.slice_viewer.TileIndex>
{
	private int sliceMin;
	private org.janelia.it.workstation.gui.slice_viewer.TileIndex index;
	private int stepCount = 0;
	
	public PreviousSliceUmbrellaGenerator(org.janelia.it.workstation.gui.slice_viewer.TileIndex seed, int sliceMin) {
		this.sliceMin = sliceMin;
		index = seed.previousSlice();
	}
	
	@Override
	public Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return index.getCoordinate(index.getSliceAxis().index()) >= sliceMin;
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
		index = index.previousSlice();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
