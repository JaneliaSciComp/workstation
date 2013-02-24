package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.util.texture.Texture;


public class RavelerTileServer 
implements GLActor, VolumeImage3d
{
	// private static final Logger log = LoggerFactory.getLogger(RavelerTileServer.class);

	/*
	 * A TileSet is a group of rectangles that complete the SliceViewer image
	 * display.
	 * 
	 * Three TileSets are maintained:
	 * 1) Latest tiles : the tiles representing the current view
	 * 2) LastGood tiles : the most recent tile set that could be successfully 
	 *    displayed.
	 * 3) Emergency tiles : a tile set that is updated with moderate frequency.
	 * 
	 * We would always prefer to display the Latest tiles. But frequently
	 * the image data for those tiles are not yet available. So we choose
	 * among the three tile sets to give the best appearance of a responsive
	 * interface.
	 * 
	 * The tricky part occurs when the user is rapidly changing the view,
	 * faster than we can load the tile images. We load tile images in
	 * multiple threads, but still it is not always possible to keep up. So
	 * one important optimization is to first insert every desired tile image
	 * into the load queue, but then when it is time to actually load an image,
	 * make another check to ensure that the image is still desired. Otherwise
	 * the view can fall farther and farther behind the current state.
	 * 
	 * One approach is to display Latest tiles if they are ready, or the
	 * LastGood tiles otherwise. The problem with this approach is that if
	 * the user is rapidly changing the view, there is never time to fully
	 * update the Latest tiles before they become stale. So the user just
	 * sees a static ancient LastGood tile set. Precisely when the user most
	 * hopes to see things moving fast.  That is where 'emergency' tiles
	 * come in.
	 * 
	 * Sets of emergency tiles are fully loaded as fast as possible, but
	 * no faster. They are not dropped from the load queue, nor are they
	 * updated until the previous set of emergency tiles has loaded and
	 * displayed. During rapid user interaction, the use of emergency
	 * tiles allows the scene to update in the fastest possible way, giving
	 * the comforting impression of responsiveness. 
	 */
	// Latest tiles list stores the current desired tile set, even if
	// not all of the tiles are ready.
	private TileSet latestTiles;
	// Emergency tiles list stores a recently displayed view, so that
	// SOMETHING gets displayed while the current view is being loaded.
	private TileSet emergencyTiles;
	// LastGoodTiles always hold a displayable tile set, even when emergency
	// tiles are loading.
	private TileSet lastGoodTiles;
	
	private URL urlStalk; // url of top level folder
	private BoundingBox3d boundingBox3d = new BoundingBox3d();
	private int numberOfChannels = 3;
	private int maximumIntensity = 255;
	private int bitDepth = 8;
	private double xResolution = 1.0; // micrometers
	private double yResolution = 1.0;
	private double zResolution = 1.0;
	private int zoomMax = 0;
	private int zoomMin = 0;
	private Map<TileIndex, TileTexture> textureCache = new Hashtable<TileIndex, TileTexture>();
	private ExecutorService textureLoadExecutor = Executors.newFixedThreadPool(1);
	private Set<TileIndex> neededTextures;
	private Map<String, String> metadata = new Hashtable<String, String>();
	//
	private Camera3d camera;
	private Viewport viewport;
	// signal for tile loaded
	private QtSignal dataChangedSignal = new QtSignal();
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private double zoomOffset = 0.5; // tradeoff between optimal resolution (0.0) and speed.

	public RavelerTileServer(String folderName) {
        try {
			loadURL(new File(folderName).toURI().toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Camera3d getCamera() {
		return camera;
	}

	public synchronized Set<TileIndex> getNeededTextures() {
		return neededTextures;
	}

	@Override
	public QtSignal getDataChangedSignal() {
		return dataChangedSignal;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
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
				TileIndex key = new TileIndex(x, y, z, zoom);
				Tile2d tile = new Tile2d(key);
				tile.setYMax(getBoundingBox3d().getMax().getY()); // To help flip y
				result.add(tile);
			}
		}
		
		return result;
	}
	
	@Override
	public void display(GL2 gl) {
		// Fetch the best set of tiles to represent this volume
		display(gl, getTiles(camera, viewport));
	}

	public void display(GL2 gl, TileSet tiles) 
	{
		if (tiles == null)
			return;
		// Possibly eliminate texture cache
		if (needsGlDisposal) {
			// log.info("Clearing tile cache");
			dispose(gl);
			textureCache.clear();
			for (Tile2d tile : tiles) {
				tile.setBestTexture(null);
				tile.setStage(Tile2d.Stage.NO_TEXTURE_LOADED);
			}
		}
		if (! tiles.canDisplay())
			return;
		// upload textures to video card, if needed
		for (Tile2d tile: tiles) {
			tile.init(gl);
		}
		// TODO - load shader?
		for (Tile2d tile: tiles) {
			tile.display(gl);
		}
		displayBoundingBox(gl);
	}
	
	private void displayBoundingBox(GL2 gl) {
		// For debugging, draw bounding box
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glColor3d(1.0, 1.0, 0.2);
		Vec3 a = getBoundingBox3d().getMin();
		Vec3 b = getBoundingBox3d().getMax();
		gl.glBegin(GL2.GL_LINE_STRIP);
		gl.glColor3d(0.2, 1.0, 1.0); // zero line cyan
		gl.glVertex3d(a.getX(), a.getY(), 0.0);
		gl.glVertex3d(b.getX(), a.getY(), 0.0);
		gl.glColor3d(1.0, 1.0, 0.2); // rest yellow
		gl.glVertex3d(b.getX(), b.getY(), 0.0);
		gl.glVertex3d(a.getX(), b.getY(), 0.0);
		gl.glVertex3d(a.getX(), a.getY(), 0.0);
		gl.glEnd();
		gl.glColor3d(1.0, 1.0, 1.0);		
	}

	@Override
	public void dispose(GL2 gl) {
		// System.out.println("dispose RavelerTileServer");
		for (TileTexture tileTexture : textureCache.values()) {
			if (tileTexture.getStage().ordinal() < TileTexture.Stage.GL_LOADED.ordinal())
				continue;
			Texture joglTexture = tileTexture.getTexture();
			joglTexture.destroy(gl);
			tileTexture.setStage(TileTexture.Stage.RAM_LOADED);
		}
		needsGlDisposal = false;
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
	}

	@Override
	public int getMaximumIntensity() {
		return maximumIntensity;
	}

	@Override
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	// Produce a list of renderable tiles to complete this view
	public TileSet getTiles(Camera3d camera, Viewport viewport) 
	{
		// Update latest tile set
		latestTiles = createLatestTiles(camera, viewport);
		latestTiles.assignTextures(textureCache);
		
		// Need to assign textures to emergency tiles too...
		if (emergencyTiles != null)
			emergencyTiles.assignTextures(textureCache);
		
		// Maybe initialize emergency tiles
		if (emergencyTiles == null)
			emergencyTiles = latestTiles;
		if (emergencyTiles.size() < 1)
			emergencyTiles = latestTiles;

		// Which tile set will we display this time?
		TileSet result = latestTiles;
		if (latestTiles.canDisplay()) {
			// log.info("Using Latest tiles");
			emergencyTiles = latestTiles;
			lastGoodTiles = latestTiles;
			result = latestTiles;
		}
		else if (emergencyTiles.canDisplay()) {
			// log.info("Using Emergency tiles");
			lastGoodTiles = emergencyTiles;
			result = emergencyTiles;
			// These emergency tiles will now be displayed.
			// So start a new batch of emergency tiles
			emergencyTiles = latestTiles; 
		}
		else {
			// log.info("Using LastGood tiles");
			// Fall back to a known displayable
			result = lastGoodTiles;
		}
		
		// Keep working on loading both emergency and latest tiles only.
		Set<TileIndex> newNeededTextures = new HashSet<TileIndex>();
		newNeededTextures.addAll(emergencyTiles.getFastNeededTextures());
		// Decide whether to load fastest textures or best textures
		Tile2d.Stage stage = latestTiles.getMinStage();
		if (stage.ordinal() < Tile2d.Stage.COARSE_TEXTURE_LOADED.ordinal())
			// First load the fast ones
			newNeededTextures.addAll(latestTiles.getFastNeededTextures());
		else
			// Then load the best ones
			newNeededTextures.addAll(latestTiles.getBestNeededTextures());
		// Use set/getNeededTextures() methods for thread safety
		// log.info("Needed textures:");
		for (TileIndex ix : newNeededTextures) {
			// log.info("  "+ix);
		}
		setNeededTextures(newNeededTextures);
		queueTextureLoad(getNeededTextures());
		
		return result;
	}
	
	@Override
	public void init(GL2 gl) {}

	@Override
	public boolean loadURL(URL folderUrl) {
		// Sanity check before overwriting current view
		if (folderUrl == null)
			return false;
		if (! parseMetadata(folderUrl))
			return false;
		// Now we can start replacing the previous state
		this.urlStalk = folderUrl;
		
		// remove old data
		emergencyTiles = null;
		if (latestTiles != null)
			latestTiles.clear();
		// queue disposal of textures on next display event
		needsGlDisposal = true;
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
	
	private void queueTextureLoad(Set<TileIndex> textures) 
	{
		for (TileIndex ix : textures) {
			if (! textureCache.containsKey(ix)) {
				TileTexture t = new TileTexture(ix, urlStalk);
				t.getRamLoadedSignal().connect(getDataChangedSignal());
				textureCache.put(ix, t);
			}
			TileTexture texture = textureCache.get(ix);
			// TODO - maybe only submit UNINITIALIZED textures, if we don't wish to retry failed ones
			if (texture.getStage().ordinal() < TileTexture.Stage.LOAD_QUEUED.ordinal()) 
				textureLoadExecutor.submit(new TileTextureLoader(texture, this));
		}
	}

	protected void setDefaultParameters() {
		maximumIntensity= 255;
		bitDepth = 8;
		numberOfChannels = 3;
		xResolution = yResolution = zResolution = 1.0; // micrometers
		zoomMax = 0;
	}

	public synchronized void setNeededTextures(Set<TileIndex> neededTextures) {
		this.neededTextures = neededTextures;
	}

	public void setCamera(Camera3d camera) {
		this.camera = camera;
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
}
