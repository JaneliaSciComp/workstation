package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generates tile indices that span entire volume at lowest resolution.
 * @author brunsc
 *
 */
public class MinResZGenerator 
implements Iterable<TileIndex>, Iterator<TileIndex>
{
	TileIndex index1, index2;
	int zMin, zMax;
	boolean useFirst = true;

	public MinResZGenerator(TileFormat tileFormat) 
	{
		int maxZoom = tileFormat.getZoomLevelCount() - 1;
		int x = 0; // only one value of x and y at lowest resolution
		int y = 0;
		zMin = tileFormat.getOrigin()[2];
		zMax = zMin + tileFormat.getVolumeSize()[2];

		// Start at center and move out
		int z0 = (zMin + zMax)/2;
		index1 = new TileIndex(x, y, z0, maxZoom, 
				maxZoom, tileFormat.getIndexStyle());
		index2 = index1.nextZ();		
	}

	@Override
	public boolean hasNext() {
		boolean result = false;
		if ((index1 != null) && (index1.getZ() >= zMin))
			result = true;
		if ((index2 != null) && (index2.getZ() <= zMax))
			result = true;
		return result;
	}

	@Override
	public TileIndex next() {
		TileIndex result = index2;
		if (useFirst)
			result = index1; // because it is the turn of index1
		if ((index1 == null) || (index1.getZ() < zMin))
			result = index2; // because index1 is out of bounds
		if ((index2 == null) || (index2.getZ() > zMax))
			result = index1; // because index2 is out of bounds
		// increment for next time
		if (result == index1)
			index1 = index1.previousZ();
		else
			index2 = index2.nextZ();
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
