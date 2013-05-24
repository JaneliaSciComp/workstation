package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Comparator;

/**
 * Compare texture indices for priority in cache
 * @author brunsc
 *
 */
public class BasicTextureComparator implements Comparator<TileIndex> {

	private TileIndex viewPosition;
	
	@Override
	public int compare(TileIndex ix1, TileIndex ix2) {
		int d1 = distance(viewPosition, ix1);
		int d2 = distance(viewPosition, ix2);
		return d1-d2; // TODO other direction?
	}
	
	private int distance(TileIndex ix1, TileIndex ix2) 
	{		
		int dx = Math.abs(ix1.getX() - ix2.getX());
		int dy = Math.abs(ix1.getY() - ix2.getY());
		int dz = Math.abs(ix1.getZ() - ix2.getZ());
		int dLod = Math.abs(ix1.getZoom() - ix2.getZoom());
		// Coefficients to rate importance of each dimension
		int kz = 1; // easy to move along Z
		int kxy = 10; // don't go so far in X/Y
		int kLod = 2; // cannot go far in LOD, but whatever
		// Moves along just one dimension should be cheaper
		// than moves along two dimensions at once.
		// But moves along X and Y together are OK.
		int dist = (1 + kxy*dx + kxy*dy) * (1 + kz*dz) + (1 + kLod*dLod) - 1;
		return dist;
	}

	public void setCurrentTile(Tile2d tile) {
		this.viewPosition = tile.getIndex();
	}

}
