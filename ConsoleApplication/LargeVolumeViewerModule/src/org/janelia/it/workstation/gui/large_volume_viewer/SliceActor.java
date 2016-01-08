package org.janelia.it.workstation.gui.large_volume_viewer;

import java.io.File;
import java.util.*;

import org.janelia.console.viewerapi.model.ImageColorModel;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.cache.TileStackCacheController;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.NumeralShader;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.OutlineShader;
import org.janelia.it.workstation.gui.large_volume_viewer.shader.SliceColorShader;
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

	private ViewTileManager viewTileManager;
	private TileServer tileServer;
	
	private boolean needsGlDisposal = false; // flag for deferred OpenGL data reset
	private boolean needsTextureCacheClear = false; // flag for deferred clear of texture cache
	
	private ImageColorModel imageColorModel;
	private SliceColorShader shader = new SliceColorShader();
	private NumeralShader numeralShader = new NumeralShader();
	private OutlineShader outlineShader = new OutlineShader();
	
	public SliceActor(ViewTileManager viewTileManager, TileServer tileServer)
	{
		this.viewTileManager = viewTileManager;
		this.tileServer = tileServer;
	}

	public void setTileServer(TileServer tileServer) {
		this.tileServer=tileServer;
	}

	@Override
	public void display(GLAutoDrawable glDrawable) {
		// Fetch the best set of tiles to represent this volume
		display(glDrawable, viewTileManager.updateDisplayTiles());
	}

	public void display(GLAutoDrawable glDrawable, TileSet tiles)
	{
	    GL gl = glDrawable.getGL();

		Map<Tile2d,List<Tile2d>> depthViewMap=null;
		if (VolumeCache.useVolumeCache()) {
			depthViewMap=new HashMap<>();
		}

	    // Manage opengl garbage collection, while we have a valid context
	    if (viewTileManager != null) {
	        TextureCache tc = viewTileManager.getTextureCache();
	        if (tc != null) {
	            int[] txIds = tc.popObsoleteTextureIds();
				int historySize=tc.getHistoryCache().size();
				int futureSize=tc.getFutureCache().size();
				//log.info("historySize="+historySize+" futureSize="+futureSize+" txIds="+txIds.length);
	            if (txIds.length > 0) {
					long startTime=System.nanoTime();
					gl.glDeleteTextures(txIds.length, txIds, 0);
					long endTime=System.nanoTime();
					long nTime=endTime-startTime;
					log.info("glDeleteTextures for "+txIds.length+" took "+nTime+" nano seconds");
				}
	        }
	    }
	    
		if (tiles == null)
			return;
		
		if (! tiles.canDisplay())
			return;

		// upload textures to video card, if needed
		TileStackCacheController tileStackCacheController=TileStackCacheController.getInstance();
		if (depthViewMap==null) {
			for (Tile2d tile : tiles) {
				tile.init(glDrawable);
			}
		} else {
			depthViewMap.clear();
			for (Tile2d tile: tiles) {
				log.info("getting depth tiles for zIndex="+tile.getIndex().getZ());
				tile.init(glDrawable);
				List<Tile2d> depthList=new ArrayList<>();
				depthViewMap.put(tile, depthList);
				if (tileServer==null) {
					log.error("tileServer is null");
				}
				TextureCache textureCache = tileServer.getTextureCache();
				AbstractTextureLoadAdapter loadAdapter=tileServer.getLoadAdapter();
				if (loadAdapter==null) {
					log.error("loadAdapter is null");
				}
				TileFormat tileFormat = loadAdapter.getTileFormat();
				// Depth increases with index
				for (int depth = 1; depth < 6; depth++) {
					TileIndex tileIndex = tile.getIndex();
					TileIndex depthIndex = new TileIndex(tileIndex.getX(), tileIndex.getY(), tileIndex.getZ() + depth,
							tileIndex.getZoom(), tileIndex.getMaxZoom(), tileIndex.getIndexStyle(), tileIndex.getSliceAxis());
					TileTexture d2=textureCache.get(depthIndex);
					if (d2==null) {
						File stackFile = tileStackCacheController.getStackFileForTileIndex(depthIndex);
						if (stackFile != null) {
							TileTexture depthTexture = new TileTexture(depthIndex, tileServer.getLoadAdapter());
							TextureLoadWorker textureLoadWorker = new TextureLoadWorker(depthTexture, textureCache, tileServer);
							textureLoadWorker.run();
							d2 = textureCache.get(depthIndex);
						}
					}
					if (d2 != null) {
						Tile2d depthTile = new Tile2d(depthIndex, tileFormat);
						depthList.add(depthTile);
					}
				}
			}
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

		gl2.glDepthMask(true);
		gl2.glEnable(GL2.GL_BLEND);
		gl2.glBlendEquation(GL2.GL_MAX);
		//gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		shader.load(gl2);
		int outerTileIndex=0;
		for (Tile2d tile: tiles) {
			float depthLayers=1.0f;
			List<Tile2d> depthTileList=null;
			if (depthViewMap!=null) {
				depthTileList=depthViewMap.get(tile);
				if (depthTileList!=null) {
					depthLayers+=((float)depthTileList.size());
					shader.setDepthLayers(gl2, 1.0f /* depthLayers*/ );
				}
			} else {
				shader.setDepthLayers(gl2, depthLayers);
			}
			tile.setFilter(filter);
			tile.display(glDrawable, camera);
			if (depthTileList!=null) {
				log.info("Rendering " + depthTileList.size() + " depth tiles for outer index=" + outerTileIndex);
				double zOffset = 0.10; /* range ~ +5.0 - -5.0 */
				double zOffsetIncrement = 0.89 / (depthTileList.size() * 1.0);
				for (Tile2d depthTile : depthTileList) {
					log.info("Render depth tile at zOffset=" + zOffset);
					depthTile.setFilter(filter);
					tile.display(glDrawable, camera, zOffset);
					zOffset += zOffsetIncrement;
				}
			} else {
				log.info("depthTileList is null - not rendering depth tiles");
			}
			outerTileIndex++;
		}
		log.info("Rendered outerIndex="+outerTileIndex+" top-level tiles");
		shader.unload(gl2);
		gl2.glDisable(GL2.GL_BLEND);
		//gl2.glDepthMask(true);

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
			numeralShader.load(gl2);
			for (Tile2d tile: tiles) {
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
