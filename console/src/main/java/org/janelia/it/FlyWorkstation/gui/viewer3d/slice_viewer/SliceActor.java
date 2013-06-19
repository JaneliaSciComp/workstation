package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.NumeralShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.OutlineShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader.SliceColorShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(SliceActor.class);

	private TileServer tileServer;
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private boolean needsTextureCacheClear = false; // flag for deferred clear of texture cache
	
	private ImageColorModel imageColorModel;
	private SliceColorShader shader = new SliceColorShader();
	private NumeralShader numeralShader = new NumeralShader();
	private OutlineShader outlineShader = new OutlineShader();
	
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
		
		// Delete uncached textures
		if ( (tileServer != null) && (tileServer.getTextureCache() != null) ) {
			int obsoleteIds[] = tileServer.getTextureCache().popObsoleteTextureIds();
			if (obsoleteIds.length > 0) {
				// log.info("deleting "+obsoleteIds.length+" OpenGL textures");
				gl.glDeleteTextures(obsoleteIds.length, obsoleteIds, 0);
			}
		}
		
		// Possibly eliminate texture cache
		if (needsGlDisposal) {
			// log.info("Clearing tile cache");
			dispose(gl);
			if (needsTextureCacheClear) {
				tileServer.getTextureCache().clear();
				// delete texture opengl ids
				int obsoleteIds[] = tileServer.getTextureCache().popObsoleteTextureIds();
				if (obsoleteIds.length > 0) {
					log.info("deleting "+obsoleteIds.length+" OpenGL textures");
					gl.glDeleteTextures(obsoleteIds.length, obsoleteIds, 0);
				}
				// Note that tiles now have no textures
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
		Camera3d camera = tileServer.getTileConsumers().iterator().next().getCamera();
		double ppu = camera.getPixelsPerSceneUnit();
		double upv = tileServer.getSharedVolumeImage().getXResolution();
		double pixelsPerVoxel = ppu*upv;
		// log.info("pixelsPerVoxel = "+pixelsPerVoxel);
		int filter = GL2.GL_LINEAR; // blended voxels at lower zoom
		if (pixelsPerVoxel > 5.0)
			filter = GL2.GL_NEAREST; // distinct voxels at high zoom
		shader.load(gl);
		for (Tile2d tile: tiles) {
			tile.setFilter(filter);
			tile.display(gl, camera);
		}
		shader.unload(gl);

		// Numeral display at high zoom			
		if (pixelsPerVoxel > 40.0) {
			numeralShader.setMicrometersPerPixel(1.0/pixelsPerVoxel);
			// fetch (typical?) texture dimensions
			int th = 10;
			int tw = 10;
			for (Tile2d tile: tiles) {
				TileTexture texture = tile.getBestTexture();
				if (texture != null) {
					th = texture.getTexture().getHeight();
					tw = texture.getTexture().getWidth();
					break;
				}
			}
			numeralShader.setTexturePixels(tw, th);
			// render numerals
			numeralShader.load(gl);
			for (Tile2d tile: tiles) {
				tile.setFilter(GL2.GL_NEAREST);
				// numeralShader.setTexturePixels(???);
				tile.display(gl, camera);
			}
			numeralShader.unload(gl);
		}

		// Outline tiles for viewer debugging
		// TODO - why are outlines black on Windows?
		final boolean bOutlineTiles = false;
		if (bOutlineTiles) {
			// Use simplest possible shader, to get color to work on Windows
			outlineShader.load(gl);
			gl.glLineWidth(1.0f);
			for (Tile2d tile: tiles) {
				tile.displayBoundingBox(gl, camera);
			}
			outlineShader.unload(gl);
		}
		
		// Outline volume for debugging
		final boolean bOutlineVolume = false;
		if (bOutlineVolume) {
			gl.glLineWidth(1.0f);
			outlineShader.load(gl);
			displayBoundingBox(gl);
			outlineShader.unload(gl);
		}
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
		if ((tileServer != null) && (tileServer.getTextureCache() != null)) {
			for (TileTexture tileTexture : tileServer.getTextureCache().values()) {
				if (tileTexture.getStage().ordinal() < TileTexture.Stage.GL_LOADED.ordinal())
					continue;
				PyramidTexture joglTexture = tileTexture.getTexture();
				joglTexture.destroy(gl);
				tileTexture.setStage(TileTexture.Stage.RAM_LOADED);
			}
		}
		needsGlDisposal = false;
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return tileServer.getSharedVolumeImage().getBoundingBox3d();
	}
	
	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	@Override
	public void init(GL2 gl) {
		try {
			shader.init(gl);
			numeralShader.init(gl);
			outlineShader.init(gl);
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
