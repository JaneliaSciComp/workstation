package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.shader.NumeralShader;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.shader.OutlineShader;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.shader.SliceColorShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
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
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(SliceActor.class);

	private ViewTileManager viewTileManager;
	
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private boolean needsTextureCacheClear = false; // flag for deferred clear of texture cache
	
	private ImageColorModel imageColorModel;
	private SliceColorShader shader = new SliceColorShader();
	private NumeralShader numeralShader = new NumeralShader();
	private OutlineShader outlineShader = new OutlineShader();
	
	public SliceActor(ViewTileManager viewTileManager)
	{
		this.viewTileManager = viewTileManager;
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		// Fetch the best set of tiles to represent this volume
		display(glDrawable, viewTileManager.updateDisplayTiles());
	}

	public void display(GLAutoDrawable glDrawable, TileSet tiles) 
	{
		if (tiles == null)
			return;
		
		if (! tiles.canDisplay())
			return;
		// upload textures to video card, if needed
		for (Tile2d tile: tiles) {
			tile.init(glDrawable);
		}
		
		// Render tile textures.
		// Pixelate at high zoom.
		Camera3d camera = viewTileManager.getTileConsumer().getCamera();
		double ppu = camera.getPixelsPerSceneUnit();
		double upv = viewTileManager.getVolumeImage().getXResolution();
		double pixelsPerVoxel = ppu*upv;
		// log.info("pixelsPerVoxel = "+pixelsPerVoxel);
		int filter = GL2.GL_LINEAR; // blended voxels at lower zoom
		if (pixelsPerVoxel > 5.0)
			filter = GL2.GL_NEAREST; // distinct voxels at high zoom
        GL2 gl = glDrawable.getGL().getGL2();
		shader.load(gl);
		for (Tile2d tile: tiles) {
			tile.setFilter(filter);
			tile.display(glDrawable, camera);
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
			// Put numerals right side up, even in rotated viewers
			CoordinateAxis sliceAxis = viewTileManager.getTileConsumer().getSliceAxis();
			if (sliceAxis == CoordinateAxis.Z)
				numeralShader.setQuarterRotations(0);
			else if (sliceAxis == CoordinateAxis.X)
				numeralShader.setQuarterRotations(1);
			else // Y
				numeralShader.setQuarterRotations(3);
			// render numerals
			numeralShader.load(gl);
			for (Tile2d tile: tiles) {
				tile.setFilter(GL2.GL_NEAREST);
				// numeralShader.setTexturePixels(???);
				tile.display(glDrawable, camera);
			}
			numeralShader.unload(gl);
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
	public void dispose(GLAutoDrawable glDrawable) {
		// System.out.println("dispose RavelerTileServer");
		TextureCache textureCache = viewTileManager.getTextureCache();
        GL2 gl = glDrawable.getGL().getGL2();
		for (TileTexture tileTexture : textureCache.values()) {
			if (tileTexture.getLoadStatus().ordinal() < TileTexture.LoadStatus.GL_LOADED.ordinal())
				continue;
			PyramidTexture joglTexture = tileTexture.getTexture();
			joglTexture.destroy(gl);
			tileTexture.setLoadStatus(TileTexture.LoadStatus.RAM_LOADED);
		}
		needsGlDisposal = false;
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return viewTileManager.getVolumeImage().getBoundingBox3d();
	}
	
	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	public ViewTileManager getViewTileManager() {
		return viewTileManager;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
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
		TextureCache textureCache = viewTileManager.getTextureCache();
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

}
