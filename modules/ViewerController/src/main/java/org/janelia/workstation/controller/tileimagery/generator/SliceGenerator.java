package org.janelia.workstation.controller.tileimagery.generator;

import java.util.Iterator;

import org.janelia.workstation.controller.tileimagery.Tile2d;
import org.janelia.workstation.controller.tileimagery.TileFormat;
import org.janelia.workstation.controller.tileimagery.TileIndex;
import org.janelia.workstation.controller.tileimagery.TileSet;

/**
 * Generate adjacent Z-slices.
 * With decreasing level-of-detail farther from current tileSet
 * @author brunsc
 *
 */
public class SliceGenerator implements Iterable<TileIndex>, Iterator<TileIndex> {
	private static final int DEFAULT_UMBRELLA_RANGE = 16;

	// Outer loop iterates over slice
	private Iterator<TileIndex> sliceGenerator;
	private TileIndex baseIndex;
	// Inner loop iterates over tiles
	private TileSet tileSet;
	private Iterator<Tile2d> tileIter;
	private Tile2d tile;

	public SliceGenerator(TileFormat tileFormat, TileSet tileSet) {
		// Identify slice boundaries
		baseIndex = tileSet.iterator().next().getIndex();
		int axisIx = baseIndex.getSliceAxis().index();
		int sliceMin = 0;
		int sliceMax = sliceMin + tileFormat.getVolumeSize()[axisIx] - 1;
		// Choose one tile to initialize search area in slice
		PreviousSliceUmbrellaGenerator down = new PreviousSliceUmbrellaGenerator(baseIndex, Math.max(sliceMin, baseIndex.getZ() - DEFAULT_UMBRELLA_RANGE));
		NextSliceUmbrellaGenerator up = new NextSliceUmbrellaGenerator(baseIndex, Math.min(sliceMax, baseIndex.getZ() + DEFAULT_UMBRELLA_RANGE));
		sliceGenerator = new InterleavedIterator<>(down, up);
		this.tileSet = tileSet;
		tileIter = this.tileSet.iterator();
	}
	
	@Override
	public boolean hasNext() {
		// Are more tiles available? (inner loop)
		if (tileIter.hasNext())
			return true;
		// No more tiles?, how about slice values? (outer loop)
		return sliceGenerator.hasNext();
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
		int[] xyz = {result.getX(), result.getY(), result.getZ()};
		// Take slice index from baseIndex
		int sliceIx = result.getSliceAxis().index();
		xyz[sliceIx] = baseIndex.getCoordinate(sliceIx);
		// Now merge baseIndex with tile index
		result = new TileIndex(
				xyz[0],
				xyz[1],
				xyz[2],
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
			baseIndex = sliceGenerator.next();
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
