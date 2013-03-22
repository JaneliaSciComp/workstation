package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class RavelerTileServer 
implements VolumeImage3d
{
	private BoundingBox3d boundingBox3d = new BoundingBox3d();
	private int numberOfChannels = 3;
	private int maximumIntensity = 255;
	private int bitDepth = 8;
	private double xResolution = 1.0; // micrometers
	private double yResolution = 1.0;
	private double zResolution = 1.0;
	private int zoomMax = 0;
	private int zoomMin = 0;
	private Map<String, String> metadata = new Hashtable<String, String>();
	//
	private Camera3d camera;
	private Viewport viewport;
	// signal for tile loaded
	private Signal dataChangedSignal = new Signal();
	private double zoomOffset = 0.5; // tradeoff between optimal resolution (0.0) and speed.
	private Signal volumeInitializedSignal = new Signal();
	private PyramidTextureLoadAdapter loadAdapter;
	
	
	public RavelerTileServer(String folderName) {
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
		xFMin = Math.max(xFMin, getBoundingBox3d().getMin().getX());
		yFMin = Math.max(yFMin, getBoundingBox3d().getMin().getY());
		xFMax = Math.min(xFMax, getBoundingBox3d().getMax().getX());
		yFMax = Math.min(yFMax, getBoundingBox3d().getMax().getY());
		double zoomFactor = Math.pow(2.0, zoom);
		// TODO - store tile pixel size 1024 someplace
		double tileWidth = 1024 * zoomFactor * getXResolution();
		double tileHeight = 1024 * zoomFactor * getYResolution();
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
				Tile2d tile = new Tile2d(key);
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
		return maximumIntensity;
	}

	@Override
	public int getNumberOfChannels() {
		return numberOfChannels;
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
		return xResolution;
	}

	@Override
	public double getYResolution() {
		return yResolution;
	}

	@Override
	public double getZResolution() {
		return zResolution;
	}	

	@Override
	public boolean loadURL(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		if (! parseMetadata(folderUrl))
			return false;
		// Now we can start replacing the previous state
		loadAdapter = new RavelerLoadAdapter(folderUrl);

		// queue disposal of textures on next display event
		getVolumeInitializedSignal().emit();
		getDataChangedSignal().emit();
		
		return true;
	}
	
	protected boolean parseMetadata(URL folderUrl) {
		// Parse metadata BEFORE overwriting current data
		try {
			URL metadataUrl = new URL(folderUrl, "tiles/metadata.txt");
			BufferedReader in = new BufferedReader(new InputStreamReader(metadataUrl.openStream()));
			String line;
			// finds lines like "key=value"
			Pattern pattern = Pattern.compile("^(.*)=(.*)\\n?$");
			metadata.clear();
			while ((line = in.readLine()) != null) {
				Matcher m = pattern.matcher(line);
				if (! m.matches())
					continue;
				String key = m.group(1);
				String value = m.group(2);
				metadata.put(key, value);
			}
			// Parse particular metadata values
			setDefaultParameters();
			// TODO - parse voxel size first
			if (metadata.containsKey("zmax"))
				boundingBox3d.getMax().setZ(zResolution * Integer.parseInt(metadata.get("zmax")));
			if (metadata.containsKey("zmin"))
				boundingBox3d.getMin().setZ(zResolution * Integer.parseInt(metadata.get("zmin")));
			if (metadata.containsKey("width")) {
				boundingBox3d.getMin().setX(0.0);
				boundingBox3d.getMax().setX(xResolution * (Integer.parseInt(metadata.get("width")) - 1) );
			}
			if (metadata.containsKey("height")) {
				boundingBox3d.getMin().setY(0.0);
				boundingBox3d.getMax().setY(yResolution * (Integer.parseInt(metadata.get("height")) - 1) );
			}
			assert(! boundingBox3d.isEmpty());
	        // Data range
	        if (metadata.containsKey("imax")) {
	        		int i = Integer.parseInt(metadata.get("imax"));
	            if (i < 1024) {
	                maximumIntensity = 255; // 8-bit
	            }
	            else if (i < 16384) {
	            		maximumIntensity = 4095; // 12-bit
	            }
	            else {
	            		maximumIntensity = 65535; // 16-bit
	            }
	            if (i > 255) {
	            		bitDepth = 16;
	            }
	        }
	        if (metadata.containsKey("bitdepth")) {
	        		bitDepth = Integer.parseInt(metadata.get("bitdepth"));
	        }
	        if (metadata.containsKey("channel-count")) {
	        		numberOfChannels = Integer.parseInt(metadata.get("channel-count"));
	        		System.out.println("channel count = "+numberOfChannels);
	        }
	        // Compute zoom min/max from dimensions...
	        double tileMax = Math.max(boundingBox3d.getMax().getX() / xResolution / 1024.0, 
	        		boundingBox3d.getMax().getY() / yResolution / 1024.0);
	        if (tileMax != 0) {
	        		zoomMax = (int)Math.ceil(Math.log(tileMax)/Math.log(2.0));
	        }
	        if (zoomMax < 0) {
	            zoomMax = 0;
	        }
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected void setDefaultParameters() {
		maximumIntensity= 255;
		bitDepth = 8;
		numberOfChannels = 3;
		xResolution = yResolution = zResolution = 1.0; // micrometers
		zoomMax = 0;
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
