package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Map;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

public class Tile2d 
implements GLActor
{
	public enum Stage {
	    NO_TEXTURE_LOADED,
	    COARSE_TEXTURE_LOADED,
	    BEST_TEXTURE_LOADED
	}


	private Stage stage = Stage.NO_TEXTURE_LOADED;
	private TileTexture bestTexture;
	private TileIndex index;

	
	public Tile2d(TileIndex key) {
		this.index = key;
	}

	// Choose the best available texture for this tile
	public void assignTexture(Map<TileIndex, TileTexture> textureCache) 
	{
		if (getStage().ordinal() >= Stage.BEST_TEXTURE_LOADED.ordinal())
			return; // Already as good as it gets
		TileIndex ix = getIndex();
		TileTexture texture = textureCache.get(ix);
		if ((texture != null) && (texture.getStage().ordinal() >= TileTexture.Stage.RAM_LOADED.ordinal()))
		{
			bestTexture = texture;
			setStage(Stage.BEST_TEXTURE_LOADED);
			return;
		}
		ix = ix.zoomOut();
		while (ix != null) {
			texture = textureCache.get(ix);
			if ((texture != null) && (texture.getStage().ordinal() >= TileTexture.Stage.RAM_LOADED.ordinal()))
			{
				bestTexture = texture;
				setStage(Stage.COARSE_TEXTURE_LOADED);
				return;
			}
			ix = ix.zoomOut();
		}
		// No texture was found; maybe next time
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
		if (bestTexture == null)
			return;
		if (getStage().ordinal() < Stage.COARSE_TEXTURE_LOADED.ordinal())
			return;
		bestTexture.init(gl);
		Texture texture = bestTexture.getTexture();
		assert(texture != null);
		texture.enable(gl);
		texture.bind(gl);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		int filter = GL2.GL_LINEAR; // TODO - pixelate at high zoom
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, filter);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, filter);
		TextureCoords tc0 = texture.getImageTexCoords();
		// Adjust texture coordinates for relative zoom level
		int dZoom = getIndex().getZoom() - bestTexture.getIndex().getZoom();
		int zoomScale = (int)(Math.pow(2, dZoom) + 0.1);
		// Remember texture coordinates might already not go from 0->1
		double tcXTotal = tc0.right() - tc0.left();
		assert(tcXTotal > 0.0);
		double tcYTotal = tc0.top() - tc0.bottom();
		// assert(tcYTotal > 0.0); // no, it's -1.0
		// Compute texture coordinate offset due to tile not being at upper left of texture
		double dXTex = tcXTotal * (getIndex().getX() % zoomScale) / (double)zoomScale;
		double dYTex = tcYTotal * (getIndex().getY() % zoomScale) / (double)zoomScale;
		double tcLeft = tc0.left() + dXTex;
		double tcRight = tcLeft + tcXTotal/zoomScale;
		double tcBottom = tc0.bottom() + dYTex;
		double tcTop = tcBottom + tcYTotal/zoomScale;
		// compute corner vertices for tile, not for texture
		double voxelSize = 1.0; // TODO
		double tileWidth = texture.getWidth() * voxelSize;
		double tileHeight = texture.getHeight() * voxelSize;
		gl.glBegin(GL2.GL_QUADS);
			// draw quad
	        double z = 0.0; // As far as OpenGL is concerned, all Z's are zero
	        double x0 = getIndex().getX() * 1024.0 * voxelSize;
	        double x1 = x0 + tileWidth;
	        double y0 = getIndex().getY() * 1024.0 * voxelSize;
	        double y1 = y0 - tileHeight; // y inverted in OpenGL relative to image convention
	        gl.glTexCoord2d(tcLeft, tcBottom); gl.glVertex3d(x0, y1, z);
	        gl.glTexCoord2d(tcRight, tcBottom); gl.glVertex3d(x1, y1, z);
	        gl.glTexCoord2d(tcRight, tcTop); gl.glVertex3d(x1, y0, z);
	        gl.glTexCoord2d(tcLeft, tcTop); gl.glVertex3d(x0, y0, z);
		gl.glEnd();
		texture.disable(gl);
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
		// TODO Auto-generated method stub
		
	}

	public TileTexture getBestTexture() {
		return bestTexture;
	}
}
