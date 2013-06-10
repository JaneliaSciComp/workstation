package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Tile2d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileFormat;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileSet;

/**
 * Generate adjacent Z-slices.
 * With decreasing level-of-detail farther from current tileSet
 * @author brunsc
 *
 */
public class ZGenerator 
implements Iterable<TileIndex>, Iterator<TileIndex>
{
	// Outer loop iterates over Z
	private Iterator<TileIndex> zGenerator;
	private TileIndex baseIndex;
	// Inner loop iterates over tiles
	private TileSet tileSet;
	private Iterator<Tile2d> tileIter;
	private Tile2d tile;

	public ZGenerator(TileFormat tileFormat, TileSet tileSet) {
		// Identify Z boundaries
		int zMin = tileFormat.getOrigin()[2];
		int zMax = zMin + tileFormat.getVolumeSize()[2] - 1;
		// Choose one tile to initialize search area in Z
		TileIndex ix1 = tileSet.iterator().next().getIndex();
		PreviousZGenerator down = new PreviousZGenerator(ix1, zMin);
		NextZGenerator up = new NextZGenerator(ix1, zMax);
		zGenerator = new InterleavedIterator<TileIndex>(down, up);
		this.tileSet = tileSet;
		tileIter = this.tileSet.iterator();
		baseIndex = tileSet.iterator().next().getIndex();
	}
	
	@Override
	public boolean hasNext() {
		// Are more tiles available? (inner loop)
		if (tileIter.hasNext())
			return true;
		// No more tiles?, how about Z values? (outer loop)
		return zGenerator.hasNext();
	}

	/**
	 * Compute current TileIndex from current tile and baseIndex.
	 * @return
	 */
	private TileIndex currentIndex() {
		TileIndex result = tile.getIndex();
		// First correct zoom
		while (result.getZoom() < baseIndex.getZoom())
			result = result.zoomOut();
		// Now merge baseIndex with tile index
		result = new TileIndex(
				result.getX(),
				result.getY(),
				baseIndex.getZ(),
				baseIndex.getZoom(),
				baseIndex.getMaxZoom(),
				baseIndex.getIndexStyle(),
				baseIndex.getSliceAxis());
		return result;
	}
	
	@Override
	public TileIndex next() {
		// First check for more tiles (inner loop)
		if (tileIter.hasNext())
			tile = tileIter.next();
		else { // How about more Z values? (outer loop)
			tileIter = tileSet.iterator(); // reset tiles
			baseIndex = zGenerator.next();
		}
		return currentIndex();
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
