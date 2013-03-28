package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class TileServer 
implements VolumeImage3d
{
	private BoundingBox3d boundingBox3d = new BoundingBox3d();
	//
	private Camera3d camera;
	private Viewport viewport;
	// signal for tile loaded
	private Signal dataChangedSignal = new Signal();
	private double zoomOffset = 0.5; // tradeoff between optimal resolution (0.0) and speed.
	private Signal volumeInitializedSignal = new Signal();
	private PyramidTextureLoadAdapter loadAdapter;
	
	
	public TileServer(String folderName) {
        try {
			loadURL(new File(folderName).toURI().toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TileSet createLatestTiles()
	{
		return createLatestTiles(getCamera(), getViewport());
	}
	
	protected TileSet createLatestTiles(Camera3d camera, Viewport viewport)
	{
		TileSet result = new TileSet();

		// Need to loop over x and y
		// Need to compute z, and zoom
		// 1) zoom
		double maxRes = Math.min(getXResolution(), getYResolution());
		double voxelsPerPixel = 1.0 / (camera.getPixelsPerSceneUnit() * maxRes);
		int zoom = 20; // default to very coarse zoom
		if (voxelsPerPixel > 0.0) {
			double topZoom = Math.log(voxelsPerPixel) / Math.log(2.0);
			zoom = (int)(topZoom + zoomOffset);
		}
		int zoomMin = 0;
		PyramidTileFormat tileFormat = loadAdapter.getTileFormat();
		int zoomMax = tileFormat.getZoomLevelCount() - 1;
		zoom = Math.max(zoom, zoomMin);
		zoom = Math.min(zoom, zoomMax);
		// 2) z
		Vec3 focus = camera.getFocus();
		int z = (int)Math.floor(focus.getZ() / getZResolution() + 0.5);
		// 3) x and y range
		// In scene units
		// Clip to screen space
		double xFMin = focus.getX() - 0.5*viewport.getWidth()/camera.getPixelsPerSceneUnit();
		double xFMax = focus.getX() + 0.5*viewport.getWidth()/camera.getPixelsPerSceneUnit();
		double yFMin = focus.getY() - 0.5*viewport.getHeight()/camera.getPixelsPerSceneUnit();
		double yFMax = focus.getY() + 0.5*viewport.getHeight()/camera.getPixelsPerSceneUnit();
		// Clip to volume space
		// Subtract one half pixel to avoid loading an extra layer of tiles
		double dx = 0.25 * tileFormat.getVoxelMicrometers()[0];
		double dy = 0.25 * tileFormat.getVoxelMicrometers()[1];
		xFMin = Math.max(xFMin, getBoundingBox3d().getMin().getX() + dx);
		yFMin = Math.max(yFMin, getBoundingBox3d().getMin().getY() + dy);
		xFMax = Math.min(xFMax, getBoundingBox3d().getMax().getX() - dx);
		yFMax = Math.min(yFMax, getBoundingBox3d().getMax().getY() - dy);
		double zoomFactor = Math.pow(2.0, zoom);
		// get tile pixel size 1024 from loadAdapter
		int tileSize[] = tileFormat.getTileSize();
		double tileWidth = tileSize[0] * zoomFactor * getXResolution();
		double tileHeight = tileSize[1] * zoomFactor * getYResolution();
		// In tile units
		int xMin = (int)Math.floor(xFMin / tileWidth);
		int xMax = (int)Math.floor(xFMax / tileWidth);
		
		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
		double bottomY = getBoundingBox3d().getMax().getY();
		int yMin = (int)Math.floor((bottomY - yFMax) / tileHeight);
		int yMax = (int)Math.floor((bottomY - yFMin) / tileHeight);
		
		for (int x = xMin; x <= xMax; ++x) {
			for (int y = yMin; y <= yMax; ++y) {
				PyramidTileIndex key = new PyramidTileIndex(x, y, z, zoom);
				Tile2d tile = new Tile2d(key, tileFormat);
				tile.setYMax(getBoundingBox3d().getMax().getY()); // To help flip y
				result.add(tile);
			}
		}
		
		return result;
	}
	
	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
	}

	public Camera3d getCamera() {
		return camera;
	}

	@Override
	public int getMaximumIntensity() {
		return loadAdapter.getTileFormat().getIntensityMax();
	}

	@Override
	public int getNumberOfChannels() {
		return loadAdapter.getTileFormat().getChannelCount();
	}

	@Override
	public Signal getDataChangedSignal() {
		return dataChangedSignal;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public Signal getVolumeInitializedSignal() {
		return volumeInitializedSignal;
	}

	@Override
	public double getXResolution() {
		return loadAdapter.getTileFormat().getVoxelMicrometers()[0];
	}

	@Override
	public double getYResolution() {
		return loadAdapter.getTileFormat().getVoxelMicrometers()[1];
	}

	@Override
	public double getZResolution() {
		return loadAdapter.getTileFormat().getVoxelMicrometers()[2];
	}	

	@Override
	public boolean loadURL(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		// Now we can start replacing the previous state
		try {
			// TODO - create Factory method to insert the correct type of loadAdapter
			boolean useRaveler = true;
			if (useRaveler)
				loadAdapter = new RavelerLoadAdapter(folderUrl);
			else
				loadAdapter = new BlockTiffOctreeLoadAdapter(new File(folderUrl.toURI()));
			// Compute bounding box
			PyramidTileFormat tf = loadAdapter.getTileFormat();
			double sv[] = tf.getVoxelMicrometers();
			int s0[] = tf.getOrigin();
			int s1[] = tf.getVolumeSize();
			Vec3 b0 = new Vec3(sv[0]*s0[0], sv[1]*s0[1], sv[2]*s0[2]);
			Vec3 b1 = new Vec3(sv[0]*(s0[0]+s1[0]), sv[1]*(s0[1]+s1[1]), sv[2]*(s0[2]+s1[2]));
			boundingBox3d.setMin(b0);
			boundingBox3d.setMax(b1);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// queue disposal of textures on next display event
		getVolumeInitializedSignal().emit();
		getDataChangedSignal().emit();
		
		return true;
	}
	
	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	public PyramidTextureLoadAdapter getLoadAdapter() {
		return loadAdapter;
	}
	

}
