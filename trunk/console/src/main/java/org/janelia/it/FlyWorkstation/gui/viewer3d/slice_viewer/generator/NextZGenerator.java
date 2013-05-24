package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;

/**
 * Generate positive offset Z indices, as part of UmbrellaZGenerator
 * @author brunsc
 *
 */
public class NextZGenerator 
implements Iterator<TileIndex>, Iterable<TileIndex>
{
	private int zMax;
	private TileIndex index;
	
	public NextZGenerator(TileIndex seed, int zMax) {
		this.zMax = zMax;
		index = seed.nextZ();
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
		// Increment Z for next time.
		index = index.nextZ();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
