package org.janelia.it.workstation.gui.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.workstation.gui.slice_viewer.TileSet;

/**
 * Generate adjacent Z-slices.
 * With decreasing level-of-detail farther from current tileSet
 * @author brunsc
 *
 */
public class SliceGenerator 
implements Iterable<org.janelia.it.workstation.gui.slice_viewer.TileIndex>, Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex>
{
	// Outer loop iterates over slice
	private Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex> sliceGenerator;
	private org.janelia.it.workstation.gui.slice_viewer.TileIndex baseIndex;
	// Inner loop iterates over tiles
	private TileSet tileSet;
	private Iterator<org.janelia.it.workstation.gui.slice_viewer.Tile2d> tileIter;
	private org.janelia.it.workstation.gui.slice_viewer.Tile2d tile;

	public SliceGenerator(org.janelia.it.workstation.gui.slice_viewer.TileFormat tileFormat, TileSet tileSet) {
		// Identify slice boundaries
		org.janelia.it.workstation.gui.slice_viewer.TileIndex ix1 = tileSet.iterator().next().getIndex();
		int axisIx = ix1.getSliceAxis().index();
		int sliceMin = tileFormat.getOrigin()[axisIx];
		int sliceMax = sliceMin + tileFormat.getVolumeSize()[axisIx] - 1;
		// Choose one tile to initialize search area in slice
		PreviousSliceGenerator down = new PreviousSliceGenerator(ix1, sliceMin);
		NextSliceGenerator up = new NextSliceGenerator(ix1, sliceMax);
		sliceGenerator = new InterleavedIterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex>(down, up);
		this.tileSet = tileSet;
		tileIter = this.tileSet.iterator();
		baseIndex = tileSet.iterator().next().getIndex();
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
	private org.janelia.it.workstation.gui.slice_viewer.TileIndex currentIndex() {
		org.janelia.it.workstation.gui.slice_viewer.TileIndex result = tile.getIndex();
		// First correct zoom
		while (result.getZoom() < baseIndex.getZoom())
			result = result.zoomOut();
		int xyz[] = {result.getX(), result.getY(), result.getZ()};
		// Take slice index from baseIndex
		int sliceIx = result.getSliceAxis().index();
		xyz[sliceIx] = baseIndex.getCoordinate(sliceIx);
		// Now merge baseIndex with tile index
		result = new org.janelia.it.workstation.gui.slice_viewer.TileIndex(
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
	public org.janelia.it.workstation.gui.slice_viewer.TileIndex next() {
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
	public Iterator<org.janelia.it.workstation.gui.slice_viewer.TileIndex> iterator() {
		return this;
	}

}
