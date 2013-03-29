package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

/*
 * To help convert between octree keys and quadtree keys
 */
public interface PyramidIndexInterpolator {
	public PyramidTileIndex fromQuadtreeIndex(PyramidTileIndex quadtreeIndex);
	public PyramidTileIndex toQuadtreeIndex(PyramidTileIndex otherIndex);
}
