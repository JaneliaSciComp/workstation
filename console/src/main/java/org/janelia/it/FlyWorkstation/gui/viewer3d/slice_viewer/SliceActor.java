package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.NumeralShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.SliceColorShader;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

/**
 * Attempt to factor out GLActor portion of RavelerTileServer,
 * if it's not already too late.
 * 
 * @author Christopher M. Bruns
 *
 */
public class SliceActor 
implements GLActor
{
	// private static final Logger log = LoggerFactory.getLogger(SliceActor.class);

	private TileServer tileServer;
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private boolean needsTextureCacheClear = false; // flag for deferred clear of texture cache
	
	private ImageColorModel imageColorModel;
	private SliceColorShader shader = new SliceColorShader();
	private NumeralShader numeralShader = new NumeralShader();
	
	private Slot clearDataSlot = new Slot() {
		@Override
		public void execute() {
			needsGlDisposal = true;
			needsTextureCacheClear = true;
		}
	};
	
	public SliceActor(TileServer tileServer)
	{
		this.tileServer = tileServer;
		tileServer.getVolumeInitializedSignal().connect(clearDataSlot);
	}

	@Override
	public void display(GL2 gl) {
		// Fetch the best set of tiles to represent this volume
		display(gl, tileServer.getDisplayTiles());
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
				tileServer.getTextureCache().clear();
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
		
		// Render tile textures.
		// Pixelate at high zoom.
		double ppu = tileServer.getCamera().getPixelsPerSceneUnit();
		double upv = tileServer.getXResolution();
		double pixelsPerVoxel = ppu*upv;
		// log.info("pixelsPerVoxel = "+pixelsPerVoxel);
		int filter = GL2.GL_LINEAR; // blended voxels at lower zoom
		if (pixelsPerVoxel > 5.0)
			filter = GL2.GL_NEAREST; // distinct voxels at high zoom
		shader.load(gl);
		for (Tile2d tile: tiles) {
			tile.setFilter(filter);
			tile.display(gl);
		}
		shader.unload(gl);
		
		// TODO optional numeral display at high zoom

		// Outline tiles for viewer debugging
		final boolean bOutlineTiles = false;
		if (bOutlineTiles) {
			for (Tile2d tile: tiles) {
				tile.displayBoundingBox(gl);
			}
		}
		
		// Outline volume for debugging
		final boolean bOutlineVolume = false;
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
		for (TileTexture tileTexture : tileServer.getTextureCache().values()) {
			if (tileTexture.getStage().ordinal() < TileTexture.Stage.GL_LOADED.ordinal())
				continue;
			PyramidTexture joglTexture = tileTexture.getTexture();
			joglTexture.destroy(gl);
			tileTexture.setStage(TileTexture.Stage.RAM_LOADED);
		}
		needsGlDisposal = false;
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return tileServer.getBoundingBox3d();
	}
	
	public ImageColorModel getImageColorModel() {
		return imageColorModel;
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
	
	public void reportTextureTimings() {
		if (tileServer.getTextureCache().size() == 0) {
			System.out.println("(No textures were loaded)");
			return;
		}
		for (TileTexture texture : tileServer.getTextureCache().values()) {
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

}
