package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;


public class RavelerTileServer 
implements GLActor, VolumeImage3d
{
	private URL urlStalk; // url of top level folder
	private BoundingBox3d boundingBox3d = new BoundingBox3d();
	private int numberOfChannels = 3;
	private int maximumIntensity = 255;
	private int bitDepth = 8;
	private double voxelX = 1.0; // micrometers
	private double voxelY = 1.0;
	private double voxelZ = 1.0;
	private int zoomMax = 0;
	// Emergency tiles list stores a recently displayed view, so that
	// SOMETHING gets displayed while the current view is being loaded.
	private TileSet emergencyTiles;
	// Latest tiles list stores the current desired tile set, even if
	// not all of the tiles are ready.
	private TileSet latestTiles;
	private Map<TileIndex, TileTexture> textureCache = new Hashtable<TileIndex, TileTexture>();
	private ExecutorService textureLoadExecutor = Executors.newFixedThreadPool(4);
	private Set<TileIndex> neededTextures;
	private Map<String, String> metadata = new Hashtable<String, String>();
	//
	private Camera3d camera;
	private Viewport viewport;
	private Tile2d testTile;
	// TODO add signal for tile loaded
	private QtSignal tileLoadedSignal = new QtSignal();

	public Camera3d getCamera() {
		return camera;
	}

	public synchronized Set<TileIndex> getNeededTextures() {
		return neededTextures;
	}

	public QtSignal getTileLoadedSignal() {
		return tileLoadedSignal;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	// canDisplay() allows controller to make subtler decisions about rendering
	public boolean canDisplay(GL2 gl, TileSet tiles) {
		if (tiles == null)
			return false;
		if (tiles.size() < 1)
			return false;
		// Initialize and possibly trigger loading of the tile set
		for (Tile2d tile : tiles)
			tile.init(gl);
		// Check whether tiles are actually displayable
		for (Tile2d tile: tiles) {
			if (tile.getBestTexture().getStage().ordinal() < TileTexture.Stage.GL_LOADED.ordinal()) {
				return false; // wait for more data to load
			}
		}
		return true;
	}
	
	protected TileSet createLatestTiles(Camera3d camera, Viewport viewport)
	{
		TileSet result = new TileSet();
		
		// TODO
		// for initial testing, just return one low resolution tile
		if (testTile == null) {
			int zMin = (int)Math.round(getBoundingBox3d().getMin().getZ() / voxelZ);
			TileIndex key = new TileIndex(0, 0, zMin, 0);
			testTile = new Tile2d(key);
			// TODO - load textures
		}
		result.add(testTile);
		
		return result;
	}
	
	@Override
	public void display(GL2 gl) {
		// Fetch the best set of tiles to represent this volume
		display(gl, getTiles(camera, viewport));
	}

	public void display(GL2 gl, TileSet tiles) 
	{
		if (! canDisplay(gl, tiles))
			return;
		// TODO - load shader?
		for (Tile2d tile: tiles) {
			tile.display(gl);
		}
	}

	@Override
	public void dispose(GL2 gl) {}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
	}

	@Override
	public int getMaximumIntensity() {
		return maximumIntensity;
	}

	@Override
	public double getMaxResolution() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	// Produce a list of renderable tiles to complete this view
	public TileSet getTiles(Camera3d camera, Viewport viewport) 
	{
		latestTiles = createLatestTiles(camera, viewport);
		latestTiles.assignTextures(textureCache);
		Tile2d.Stage stage = latestTiles.getMinStage();
		if (stage.ordinal() < Tile2d.Stage.COARSE_TEXTURE_LOADED.ordinal())
		{
			setNeededTextures(latestTiles.getFastNeededTextures());
			queueTextureLoad(getNeededTextures());
			return emergencyTiles;
		}
		else if (stage.ordinal() < Tile2d.Stage.BEST_TEXTURE_LOADED.ordinal())
		{
			setNeededTextures(latestTiles.getBestNeededTextures());
			queueTextureLoad(getNeededTextures());
		}
		// if we get this far, the new latestTiles are next-time's emergency tiles
		emergencyTiles = latestTiles;
		return latestTiles;
	}
	
	@Override
	public void init(GL2 gl) {}

	public boolean openFolder(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		if (! parseMetadata(folderUrl))
			return false;
		// Now we can start replacing the previous state
		this.urlStalk = folderUrl;
		// TODO - set up threads?
		
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
				boundingBox3d.getMax().setZ(voxelZ * Integer.parseInt(metadata.get("zmax")));
			if (metadata.containsKey("zmin"))
				boundingBox3d.getMin().setZ(voxelZ * Integer.parseInt(metadata.get("zmin")));
			if (metadata.containsKey("width")) {
				boundingBox3d.getMin().setX(0.0);
				boundingBox3d.getMax().setX(voxelX * (Integer.parseInt(metadata.get("width")) - 1) );
			}
			if (metadata.containsKey("height")) {
				boundingBox3d.getMin().setY(0.0);
				boundingBox3d.getMax().setY(voxelY * (Integer.parseInt(metadata.get("height")) - 1) );
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
	            		maximumIntensity = 16;
	            }
	        }
	        if (metadata.containsKey("bitdepth")) {
	        		bitDepth = Integer.parseInt(metadata.get("bitdepth"));
	        }
	        if (metadata.containsKey("channel-count")) {
	        		numberOfChannels = Integer.parseInt(metadata.get("channel-count"));
	        }
	        // Compute zoom min/max from dimensions...
	        double tileMax = Math.max(boundingBox3d.getMax().getX() / voxelX / 1024.0, 
	        		boundingBox3d.getMax().getY() / voxelY / 1024.0);
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
	
	private void queueTextureLoad(Set<TileIndex> textures) 
	{
		for (TileIndex ix : textures) {
			if (! textureCache.containsKey(ix)) {
				TileTexture t = new TileTexture(ix, urlStalk);
				t.getRamLoadedSignal().connect(getTileLoadedSignal());
				textureCache.put(ix, t);
			}
			TileTexture texture = textureCache.get(ix);
			if (texture.getStage().ordinal() < TileTexture.Stage.RAM_LOADING.ordinal()) 
			{
				textureLoadExecutor.submit(new TileTextureLoader(texture, this));
			}
		}
	}

	protected void setDefaultParameters() {
		maximumIntensity= 255;
		bitDepth = 8;
		numberOfChannels = 3;
		voxelX = voxelY = voxelZ = 1.0; // micrometers
		zoomMax = 0;
	}

	public synchronized void setNeededTextures(Set<TileIndex> neededTextures) {
		this.neededTextures = neededTextures;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
	}
}
