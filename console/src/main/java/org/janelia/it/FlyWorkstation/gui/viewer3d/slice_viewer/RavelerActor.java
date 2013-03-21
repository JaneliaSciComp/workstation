package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.NumeralShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.SliceColorShader;

import com.jogamp.opengl.util.texture.Texture;

/**
 * Attempt to factor out GLActor portion of RavelerTileServer,
 * if it's not already too late.
 * 
 * @author Christopher M. Bruns
 *
 */
public class RavelerActor 
implements GLActor
{
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

	private RavelerTileServer server;
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private boolean needsTextureCacheClear = false; // flag for deferred clear of texture cache
	private Map<RavelerZTileIndex, TileTexture> textureCache = new Hashtable<RavelerZTileIndex, TileTexture>();
	private ExecutorService textureLoadExecutor = Executors.newFixedThreadPool(4);
	private Set<RavelerZTileIndex> neededTextures;
	
	private ImageColorModel imageColorModel;
	private SliceColorShader shader = new SliceColorShader();
	private NumeralShader numeralShader = new NumeralShader();
	
	private Signal dataChangedSignal = new Signal();
	
	private Slot clearDataSlot = new Slot() {
		@Override
		public void execute() {
			needsGlDisposal = true;
			needsTextureCacheClear = true;
			// remove old data
			emergencyTiles = null;
			if (latestTiles != null)
				latestTiles.clear();
			if (lastGoodTiles != null)
				lastGoodTiles.clear();
		}
	};
	
	public RavelerActor(RavelerTileServer server)
	{
		this.server = server;
		server.getVolumeInitializedSignal().connect(clearDataSlot);
		server.getDataChangedSignal().connect(dataChangedSignal); 
	}

	@Override
	public void display(GL2 gl) {
		// Fetch the best set of tiles to represent this volume
		display(gl, getDisplayTiles());
	}

	public void display(GL2 gl, TileSet tiles) 
	{
		if (tiles == null)
			return;
		// Possibly eliminate texture cache
		if (needsGlDisposal) {
			// log.info("Clearing tile cache");
			dispose(gl);
			if (needsTextureCacheClear) {
				textureCache.clear();
				for (Tile2d tile : tiles) {
					tile.setBestTexture(null);
					tile.setStage(Tile2d.Stage.NO_TEXTURE_LOADED);
				}
			}
		}
		if (! tiles.canDisplay())
			return;
		// upload textures to video card, if needed
		for (Tile2d tile: tiles) {
			tile.init(gl);
		}
		
		// Render tile textures
		// TODO - pixellate at high zoom
		shader.load(gl);
		for (Tile2d tile: tiles) {
			tile.display(gl);
		}
		shader.unload(gl);
		
		// TODO optional numeral display at high zoom

		// Outline tiles for viewer debugging
		boolean bOutlineTiles = false;
		if (bOutlineTiles) {
			for (Tile2d tile: tiles) {
				tile.displayBoundingBox(gl);
			}
		}
		
		// Outline volume for debugging
		boolean bOutlineVolume = false;
		if (bOutlineVolume)
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
		return server.getBoundingBox3d();
	}
	
	public Signal getDataChangedSignal() {
		return dataChangedSignal;
	}

	// Produce a list of renderable tiles to complete this view
	public TileSet getDisplayTiles() 
	{
		// Update latest tile set
		latestTiles = server.createLatestTiles();
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
		Set<RavelerZTileIndex> newNeededTextures = new HashSet<RavelerZTileIndex>();
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
		/*
		for (TileIndex ix : newNeededTextures) {
			log.info("  "+ix);
		}
		*/
		setNeededTextures(newNeededTextures);
		queueTextureLoad(getNeededTextures());
		
		return result;
	}	

	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	public synchronized Set<RavelerZTileIndex> getNeededTextures() {
		return neededTextures;
	}

	@Override
	public void init(GL2 gl) {
		try {
			shader.init(gl);
		} catch (ShaderCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void queueGlDisposal() {
		needsGlDisposal = true;
	}
	
	private void queueTextureLoad(Set<RavelerZTileIndex> textures) 
	{
		for (RavelerZTileIndex ix : textures) {
			if (! textureCache.containsKey(ix)) {
				if (server.getUrlStalk() == null)
					continue;
				TileTexture t = new TileTexture(ix, server.getUrlStalk());
				t.getRamLoadedSignal().connect(getDataChangedSignal());
				textureCache.put(ix, t);
			}
			TileTexture texture = textureCache.get(ix);
			// TODO - maybe only submit UNINITIALIZED textures, if we don't wish to retry failed ones
			if (texture.getStage().ordinal() < TileTexture.Stage.LOAD_QUEUED.ordinal()) 
				textureLoadExecutor.submit(new TileTextureLoader(texture, this));
		}
	}

	public void reportTextureTimings() {
		if (textureCache.size() == 0) {
			System.out.println("(No textures were loaded)");
			return;
		}
		for (TileTexture texture : textureCache.values()) {
			// Time to download image bytes
			long value = texture.getDownloadDataTime();
			if (value != texture.getInvalidTime()) {
				value -= texture.getConstructTime();
			}
		}
	}
	
	public void setImageColorModel(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
		shader.setImageColorModel(imageColorModel);
		numeralShader.setImageColorModel(imageColorModel);
	}

	public synchronized void setNeededTextures(Set<RavelerZTileIndex> neededTextures) {
		this.neededTextures = neededTextures;
	}

}
