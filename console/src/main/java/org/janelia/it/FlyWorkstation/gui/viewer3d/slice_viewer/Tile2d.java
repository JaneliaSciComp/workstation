package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.util.texture.TextureCoords;

/**
 * One rectangular region that forms part of the SliceViewer display.
 * 
 * A particular Tile2d may use a lower resolution TileTexture, in case
 * its corresponding full-resolution TileTexture is not yet available.
 * 
 * There is a subtle distinction between a Tile2d and a TileTexture.
 * A Tile2d represents a square block of pixels on the screen viewport.
 * A TileTexture represents a square image loadable from the data store.
 * There is usually a one-to-one correspondence between Tile2d and
 * TileTexture. But sometimes a Tile2d might use a sub-region of a 
 * lower resolution TileTexture.
 * 
 * @author brunsc
 *
 */
public class Tile2d 
implements GLActor
{
	public static enum Stage {
	    NO_TEXTURE_LOADED,
	    COARSE_TEXTURE_LOADED,
	    BEST_TEXTURE_LOADED
	}

	private static final Logger log = LoggerFactory.getLogger(Tile2d.class);
	
	private Stage stage = Stage.NO_TEXTURE_LOADED;
	private TileTexture bestTexture;
	private TileIndex index;
	private double yMax; // To help flip Raveler tiles in Y
	private TileFormat tileFormat;
	private int filter = GL2.GL_LINEAR;

	
	public Tile2d(TileIndex key, TileFormat tileFormat) {
		if (key == null) {
			log.error("Tile with null index constructed");
		}
		this.index = key;
		this.tileFormat = tileFormat;
	}

	// Choose the best available texture for this tile
	public void assignTexture(TextureCache textureCache) 
	{
		if (getStage().ordinal() >= Stage.BEST_TEXTURE_LOADED.ordinal())
			return; // Already as good as it gets
		TileIndex ix = getIndex();
		if (ix == null) {
			log.error("Tile with null index");
			return;
		}
		TileTexture texture = textureCache.get(ix);
		if ((texture != null) && (texture.getStage().ordinal() >= TileTexture.Stage.RAM_LOADED.ordinal()))
		{
			// Hey! I just noticed I have the best possible texture
			bestTexture = texture;
			setStage(Stage.BEST_TEXTURE_LOADED);
			return;
		}
		// System.out.println("cache miss "+ix);
		ix = ix.zoomOut(); // Try some lower resolution textures
		while (ix != null) {
			texture = textureCache.get(ix);
			if (texture == null) {
				// log.info("cache miss no texture "+ix);
				if (ix.getZoom() == ix.getMaxZoom()) {
					// log.warn("should already have this persistent texture "+ix+", "+textureCache.size());
					// texture = textureCache.get(ix);
				}
			}
			else if (texture.getStage().ordinal() < TileTexture.Stage.RAM_LOADED.ordinal()) {
				// log.info("cache miss texture not loaded "+ix);
			}
			else {
				bestTexture = texture;
				setStage(Stage.COARSE_TEXTURE_LOADED);
				return;
			}
			ix = ix.zoomOut();
		}
		// No texture was found; maybe next time
		// log.info("texture cache miss "+getIndex());
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((index == null) ? 0 : index.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tile2d other = (Tile2d) obj;
		if (index == null) {
			if (other.index != null)
				return false;
		} else if (!index.equals(other.index))
			return false;
		return true;
	}

	public int getFilter() {
		return filter;
	}

	public void setFilter(int filter) {
		this.filter = filter;
	}

	public TileIndex getIndex() {
		return index;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void setBestTexture(TileTexture bestTexture) {
		this.bestTexture = bestTexture;
	}

	@Override
	public void display(GL2 gl) {
		if (bestTexture == null) {
			log.info("tile with no texture "+getIndex());
			return;
		}
		if (getStage().ordinal() < Stage.COARSE_TEXTURE_LOADED.ordinal())
			return;
		// log.info("Rendering tile "+getIndex());
		bestTexture.init(gl);
		PyramidTexture texture = bestTexture.getTexture();
		assert(texture != null);
		if (texture == null)
			return;
		if (! bestTexture.getIndex().equals(getIndex())) {
			// log.info("using imperfect texture "+bestTexture.getIndex()+" for tile "+getIndex());
		}
		texture.enable(gl);
		texture.bind(gl);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, filter);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, filter);
		TextureCoords tc0 = texture.getImageTexCoords();
		// Adjust texture coordinates for relative zoom level
		int dZoom = bestTexture.getIndex().getZoom() - getIndex().getZoom();
		int textureScale = (int)(Math.pow(2, dZoom) + 0.1);
		// Remember texture coordinates might already not go from 0->1
		double tcXTotal = tc0.right() - tc0.left();
		assert(tcXTotal > 0.0);
		double tcYTotal = tc0.top() - tc0.bottom();
		// assert(tcYTotal > 0.0); // no, it's -1.0
		// Compute texture coordinate offset due to tile not being at upper left of texture
		double dXTex = tcXTotal * (getIndex().getX() % textureScale) / (double)textureScale;
		double dYTex = tcYTotal * (getIndex().getY() % textureScale) / (double)textureScale;
		double tcLeft = tc0.left() + dXTex;
		double tcRight = tcLeft + tcXTotal/textureScale;
		double tcBottom = tc0.bottom() + dYTex;
		double tcTop = tcBottom + tcYTotal/textureScale;
		// compute corner vertices for tile, not for texture
		int zoomScale = (int)(Math.pow(2.0, getIndex().getZoom()) + 0.1);
		double tileWidth = texture.getUsedWidth() * zoomScale * tileFormat.getVoxelMicrometers()[0];
		double tileHeight = texture.getHeight() * zoomScale * tileFormat.getVoxelMicrometers()[1];
		gl.glBegin(GL2.GL_QUADS);
			// draw quad
	        double z = 0.0; // As far as OpenGL is concerned, all Z's are zero
	        double x0 = getIndex().getX() * tileFormat.getTileSize()[0] * zoomScale * tileFormat.getVoxelMicrometers()[0];
	        double x1 = x0 + tileWidth;
	        // Raveler tile index has origin at BOTTOM left, unlike TOP left for images and
	        // our coordinate system
	        double y0 = yMax - getIndex().getY() * tileFormat.getTileSize()[1] * zoomScale * tileFormat.getVoxelMicrometers()[1];
	        double y1 = y0 - tileHeight; // y inverted in OpenGL relative to image convention
	        gl.glTexCoord2d(tcLeft, tcBottom); gl.glVertex3d(x0, y0, z);
	        gl.glTexCoord2d(tcRight, tcBottom); gl.glVertex3d(x1, y0, z);
	        gl.glTexCoord2d(tcRight, tcTop); gl.glVertex3d(x1, y1, z);
	        gl.glTexCoord2d(tcLeft, tcTop); gl.glVertex3d(x0, y1, z);
		gl.glEnd();
		texture.disable(gl);
		
		// Record display time, if first display for texture
		if (bestTexture.getFirstDisplayTime() == bestTexture.getInvalidTime()) {
			bestTexture.setFirstDisplayTime(System.nanoTime());
		}
	}

	public void displayBoundingBox(GL2 gl) 
	{
		if (bestTexture == null)
			return;
		PyramidTexture texture = bestTexture.getTexture();
		int zoomScale = (int)(Math.pow(2.0, getIndex().getZoom()) + 0.1);
		double tileWidth = texture.getUsedWidth() * zoomScale * tileFormat.getVoxelMicrometers()[0];
		double tileHeight = texture.getHeight() * zoomScale * tileFormat.getVoxelMicrometers()[1];
		gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glColor3f(1.0f, 1.0f, 0.3f);
			// draw quad
	        double z = 0.0; // As far as OpenGL is concerned, all Z's are zero
	        double x0 = getIndex().getX() * tileFormat.getTileSize()[0] * zoomScale * tileFormat.getVoxelMicrometers()[0];
	        double x1 = x0 + tileWidth;
	        // Raveler tile index has origin at BOTTOM left, unlike TOP left for images and
	        // our coordinate system
	        double y0 = yMax - getIndex().getY() * tileFormat.getTileSize()[1] * zoomScale * tileFormat.getVoxelMicrometers()[1];
	        double y1 = y0 - tileHeight; // y inverted in OpenGL relative to image convention
	        gl.glVertex3d(x0, y0, z);
	        gl.glVertex3d(x0, y1, z);
	        gl.glVertex3d(x1, y1, z);
	        gl.glVertex3d(x1, y0, z);
	        gl.glVertex3d(x0, y0, z);
        gl.glEnd();
		gl.glColor3d(1.0, 1.0, 1.0);
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(GL2 gl) {
		if (bestTexture == null)
			return;
		bestTexture.init(gl);
	}

	@Override
	public void dispose(GL2 gl) {
	}

	public TileTexture getBestTexture() {
		return bestTexture;
	}

	public void setYMax(double yMax) {
		this.yMax  = yMax;
	}

	public ImageBrightnessStats getBrightnessStats() {
		if (bestTexture == null)
			return null;
		return bestTexture.getBrightnessStats();
	}

}
