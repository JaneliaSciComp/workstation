package org.janelia.it.workstation.gui.slice_viewer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.slice_viewer.shader.NumeralShader;
import org.janelia.it.workstation.gui.slice_viewer.shader.OutlineShader;
import org.janelia.it.workstation.gui.slice_viewer.shader.SliceColorShader;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader.ShaderCreationException;
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

	private org.janelia.it.workstation.gui.slice_viewer.ViewTileManager viewTileManager;
	
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private boolean needsTextureCacheClear = false; // flag for deferred clear of texture cache
	
	private org.janelia.it.workstation.gui.slice_viewer.ImageColorModel imageColorModel;
	private SliceColorShader shader = new SliceColorShader();
	private NumeralShader numeralShader = new NumeralShader();
	private OutlineShader outlineShader = new OutlineShader();
	
	public SliceActor(org.janelia.it.workstation.gui.slice_viewer.ViewTileManager viewTileManager)
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
	    GL gl = glDrawable.getGL();
	    // Manage opengl garbage collection, while we have a valid context
	    if (viewTileManager != null) {
	        org.janelia.it.workstation.gui.slice_viewer.TextureCache tc = viewTileManager.getTextureCache();
	        if (tc != null) {
	            int[] txIds = tc.popObsoleteTextureIds();
	            if (txIds.length > 0)
	                gl.glDeleteTextures(txIds.length, txIds, 0);
	        }
	    }
	    
		if (tiles == null)
			return;
		
		if (! tiles.canDisplay())
			return;
		
        // upload textures to video card, if needed
        for (org.janelia.it.workstation.gui.slice_viewer.Tile2d tile: tiles) {
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
        GL2 gl2 = glDrawable.getGL().getGL2();
		shader.load(gl2);
		for (org.janelia.it.workstation.gui.slice_viewer.Tile2d tile: tiles) {
			tile.setFilter(filter);
			tile.display(glDrawable, camera);
		}
		shader.unload(gl2);

		// Numeral display at high zoom			
		if (pixelsPerVoxel > 40.0) {
			numeralShader.setMicrometersPerPixel(1.0/pixelsPerVoxel);
			// fetch (typical?) texture dimensions
			int th = 10;
			int tw = 10;
			for (org.janelia.it.workstation.gui.slice_viewer.Tile2d tile: tiles) {
				org.janelia.it.workstation.gui.slice_viewer.TileTexture texture = tile.getBestTexture();
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
			numeralShader.load(gl2);
			for (org.janelia.it.workstation.gui.slice_viewer.Tile2d tile: tiles) {
				tile.setFilter(GL2.GL_NEAREST);
				// numeralShader.setTexturePixels(???);
				tile.display(glDrawable, camera);
			}
			numeralShader.unload(gl2);
		}

		// Outline volume for debugging
		final boolean bOutlineVolume = false;
		if (bOutlineVolume) {
			gl.glLineWidth(1.0f);
			outlineShader.load(gl2);
			displayBoundingBox(gl2);
			outlineShader.unload(gl2);
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
		org.janelia.it.workstation.gui.slice_viewer.TextureCache textureCache = viewTileManager.getTextureCache();
        GL2 gl = glDrawable.getGL().getGL2();
		for (org.janelia.it.workstation.gui.slice_viewer.TileTexture tileTexture : textureCache.values()) {
			if (tileTexture.getLoadStatus().ordinal() < org.janelia.it.workstation.gui.slice_viewer.TileTexture.LoadStatus.GL_LOADED.ordinal())
				continue;
			org.janelia.it.workstation.gui.slice_viewer.PyramidTexture joglTexture = tileTexture.getTexture();
			joglTexture.destroy(gl);
			tileTexture.setLoadStatus(org.janelia.it.workstation.gui.slice_viewer.TileTexture.LoadStatus.RAM_LOADED);
		}
		needsGlDisposal = false;
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return viewTileManager.getVolumeImage().getBoundingBox3d();
	}
	
	public org.janelia.it.workstation.gui.slice_viewer.ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	public org.janelia.it.workstation.gui.slice_viewer.ViewTileManager getViewTileManager() {
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
		org.janelia.it.workstation.gui.slice_viewer.TextureCache textureCache = viewTileManager.getTextureCache();
		if (textureCache.size() == 0) {
			System.out.println("(No textures were loaded)");
			return;
		}
		for (org.janelia.it.workstation.gui.slice_viewer.TileTexture texture : textureCache.values()) {
			// Time to download image bytes
			long value = texture.getDownloadDataTime();
			if (value != texture.getInvalidTime()) {
				value -= texture.getConstructTime();
			}
		}
	}
	
	public void setImageColorModel(org.janelia.it.workstation.gui.slice_viewer.ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
		shader.setImageColorModel(imageColorModel);
		numeralShader.setImageColorModel(imageColorModel);
	}

}
