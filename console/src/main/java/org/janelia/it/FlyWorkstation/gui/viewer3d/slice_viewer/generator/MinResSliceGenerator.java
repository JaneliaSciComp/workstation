package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generates tile indices that span entire volume at lowest resolution.
 * @author brunsc
 *
 */
public class MinResSliceGenerator 
implements Iterable<TileIndex>, Iterator<TileIndex>
{
	TileIndex index1, index2;
	int sliceMin, sliceMax;
	boolean useFirst = true;
	private CoordinateAxis sliceAxis;

	public MinResSliceGenerator(TileFormat tileFormat, CoordinateAxis sliceAxis) 
	{
		this.sliceAxis = sliceAxis;
		int maxZoom = tileFormat.getZoomLevelCount() - 1;
		int xyz[] = {0,0,0};
		int sa = sliceAxis.index();
		sliceMin = tileFormat.getOrigin()[sa];
		sliceMax = sliceMin + tileFormat.getVolumeSize()[sa];

		// Start at center and move out
		int slice0 = (sliceMin + sliceMax)/2;
		xyz[sa] = slice0;
		index1 = new TileIndex(xyz[0], xyz[1], xyz[2], maxZoom, 
				maxZoom, tileFormat.getIndexStyle(),
				sliceAxis);
		index2 = index1.nextSlice();		
	}

	@Override
	public boolean hasNext() {
		boolean result = false;
		int sa = sliceAxis.index();
		if ((index1 != null) && (index1.getCoordinate(sa) >= sliceMin))
			result = true;
		if ((index2 != null) && (index2.getCoordinate(sa) <= sliceMax))
			result = true;
		return result;
	}

	@Override
	public TileIndex next() {
		TileIndex result = index2;
		if (useFirst)
			result = index1; // because it is the turn of index1
		if ((index1 == null) || (index1.getCoordinate(sliceAxis.index()) < sliceMin))
			result = index2; // because index1 is out of bounds
		if ((index2 == null) || (index2.getCoordinate(sliceAxis.index()) > sliceMax))
			result = index1; // because index2 is out of bounds
		// increment for next time
		if (result == index1)
			index1 = index1.previousSlice();
		else
			index2 = index2.nextSlice();
		// switch to other direction for next time
		useFirst = ! useFirst;
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<TileIndex> iterator() {
		return this;
	}

}
