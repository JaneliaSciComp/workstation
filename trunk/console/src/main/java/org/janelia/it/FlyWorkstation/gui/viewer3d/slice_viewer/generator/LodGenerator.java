package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.generator;

import java.util.Iterator;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Tile2d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileIndex;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileServer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.TileSet;

public class LodGenerator 
implements Iterable<TileIndex>, Iterator<TileIndex>
{
	private TileServer tileServer;
	Iterator<Integer> allLod;
	Iterator<Tile2d> tileIter;
	int zoom;
	
	public LodGenerator(TileServer tileServer) {
		this.tileServer = tileServer;
		TileSet tileSet = tileServer.createLatestTiles();
		if (tileSet.size() < 1)
			return;
		Tile2d tile = tileSet.iterator().next();
		int minZoom = 0;
		int startZoom = tile.getIndex().getZoom();
		int maxZoom = tile.getIndex().getMaxZoom();
		// Go up in zoom
		Iterator<Integer> coarseLod = new RangeIterator(startZoom+1, maxZoom+1, 1);		
		// Go down in zoom
		Iterator<Integer> fineLod = new RangeIterator(startZoom-1, minZoom-1, -1);
		// Alternate in zoom
		allLod = new InterleavedIterator<Integer>(coarseLod, fineLod);
	}
	
	@Override
	public boolean hasNext() {
		if (allLod == null)
			return false;
		if (allLod.hasNext()) // outer loop
			return true;
		if (tileIter == null)
			return false;
		return tileIter.hasNext(); // inner loop
	}

	@Override
	public TileIndex next() {
		// inner loop over tiles
		if ((tileIter != null) && (tileIter.hasNext()))
			return tileIter.next().getIndex();
		// outer loop over zoom levels
		zoom = allLod.next();
		Camera3d c0 = tileServer.getCamera();
		Camera3d camera = new BasicCamera3d();
		camera.setFocus(c0.getFocus());
		camera.setPixelsPerSceneUnit(c0.getPixelsPerSceneUnit());
		camera.setRotation(c0.getRotation());
		TileSet tileSet = tileServer.createLatestTiles(camera, tileServer.getViewport());
		int testZoom = tileSet.iterator().next().getIndex().getZoom();
		while (testZoom != zoom) {
			double factor = Math.pow(2, testZoom - zoom);
			camera.setPixelsPerSceneUnit(factor * camera.getPixelsPerSceneUnit());
			tileSet = tileServer.createLatestTiles(camera, tileServer.getViewport());
			testZoom = tileSet.iterator().next().getIndex().getZoom();
		}
		tileIter = tileSet.iterator();
		return tileIter.next().getIndex();
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
