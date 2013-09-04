package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.geom.Point2D;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
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
	public static enum LoadStatus {
	    NO_TEXTURE_LOADED,
	    COARSE_TEXTURE_LOADED,
	    BEST_TEXTURE_LOADED
	}

	private static final Logger log = LoggerFactory.getLogger(Tile2d.class);
	
	private LoadStatus loadStatus = LoadStatus.NO_TEXTURE_LOADED;
	private TileTexture bestTexture;
	private TileIndex index;
	private double yMax = 0; // To help flip Raveler tiles in Y
	private TileFormat tileFormat;
	private int filter = GL2.GL_LINEAR;
	private BoundingBox3d boundingBox3d;
	
	public Tile2d(TileIndex key, TileFormat tileFormat) {
		if (key == null) {
			log.error("Tile with null index constructed");
		}
		this.index = key;
		this.tileFormat = tileFormat;
		this.boundingBox3d = computeBoundingBox();
	}

	// Choose the best available texture for this tile
	public void assignTexture(TextureCache textureCache) 
	{
		if (getLoadStatus().ordinal() >= LoadStatus.BEST_TEXTURE_LOADED.ordinal())
			return; // Already as good as it gets
		TileIndex ix = getIndex();
		if (ix == null) {
			log.error("Tile with null index");
			return;
		}
		TileTexture texture = textureCache.get(ix);
		if ((texture != null) && (texture.getLoadStatus().ordinal() >= TileTexture.LoadStatus.RAM_LOADED.ordinal()))
		{
			// Hey! I just noticed I have the best possible texture
			bestTexture = texture;
			setLoadStatus(LoadStatus.BEST_TEXTURE_LOADED);
			return;
		}
		if ((ix != null) && (ix.getSliceAxis() == CoordinateAxis.X)) {
			// log.info("cache miss "+ix);
		}
		ix = ix.zoomOut(); // Try some lower resolution textures
		if ((ix != null) && (ix.getSliceAxis() == CoordinateAxis.X)) {
			// log.info("try lower texture "+ix);
		}
		while (ix != null) {
			texture = textureCache.get(ix);
			if (texture == null) {
				// log.info("cache miss no texture "+ix);
				if (ix.getZoom() == ix.getMaxZoom()) {
					// log.warn("should already have this persistent texture "+ix+", "+textureCache.size());
					// texture = textureCache.get(ix);
				}
			}
			else if (texture.getLoadStatus().ordinal() < TileTexture.LoadStatus.RAM_LOADED.ordinal()) {
				// log.info("cache miss texture not loaded "+ix);
			}
			else {
				if ((ix != null) && (ix.getSliceAxis() == CoordinateAxis.X)) {
					// log.info("choosing lower texture "+ix);
				}
				bestTexture = texture;
				setLoadStatus(LoadStatus.COARSE_TEXTURE_LOADED);
				return;
			}
			ix = ix.zoomOut();
			if ((ix != null) && (ix.getSliceAxis() == CoordinateAxis.X)) {
				// log.info("try lower texture "+ix);
			}
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

	public LoadStatus getLoadStatus() {
		return loadStatus;
	}

	public void setLoadStatus(LoadStatus stage) {
		this.loadStatus = stage;
	}

	public void setBestTexture(TileTexture bestTexture) {
		this.bestTexture = bestTexture;
		boundingBox3d = computeBoundingBox();
	}

	@Override
	public void display(GL2 gl) {
		display(gl, null);
	}
	
	public void display(GL2 gl, Camera3d camera) {
		if (bestTexture == null) {
			log.info("tile with no texture "+getIndex());
			return;
		}
		if (getLoadStatus().ordinal() < LoadStatus.COARSE_TEXTURE_LOADED.ordinal())
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
		gl.glBegin(GL2.GL_QUADS);
			// draw quad
	        Vec3 corners[] = computeCornerPositions(camera);
	        Point2D texCoords[] = computeTextureCoordinates();
	        int cornerOrder[] = {0, 1, 2, 3};
	        for (int c : cornerOrder) {
		        gl.glTexCoord2d(texCoords[c].getX(), texCoords[c].getY());
		        gl.glVertex3d(corners[c].getX(), corners[c].getY(), corners[c].getZ());
	        }
	        
		gl.glEnd();
		texture.disable(gl);
		
		// Record display time, if first display for texture
		if (bestTexture.getFirstDisplayTime() == bestTexture.getInvalidTime()) {
			bestTexture.setFirstDisplayTime(System.nanoTime());
		}
	}

	public void displayBoundingBox(GL2 gl) 
	{
		displayBoundingBox(gl, null);
	}
	
	public void displayBoundingBox(GL2 gl, Camera3d camera) 
	{
		Vec3 corners[] = computeCornerPositions(camera);
		int cornerOrder[] = {0, 1, 2, 3, 0};
		gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glColor3f(1.0f, 1.0f, 0.3f);
			for (int c : cornerOrder)
		        gl.glVertex3d(corners[c].x(), corners[c].y(), corners[c].z());				
        gl.glEnd();
		gl.glColor3d(1.0, 1.0, 1.0);
	}

	private Point2D[] computeTextureCoordinates() {
		PyramidTexture texture = bestTexture.getTexture();
		assert(texture != null);
		TextureCoords tc0 = texture.getImageTexCoords();
		// Adjust texture coordinates for relative zoom level
		int dZoom = bestTexture.getIndex().getZoom() - getIndex().getZoom();
		int textureScale = (int)(Math.pow(2, dZoom) + 0.1);
		// Remember texture coordinates might already not go from 0->1
		double tcXTotal = tc0.right() - tc0.left();
		assert(tcXTotal > 0.0);
		double tcYTotal = tc0.top() - tc0.bottom();
		// assert(tcYTotal > 0.0); // no, it's -1.0
		
		// Permute coordinates for tiles that have non-Z orientations.
		int whdToXyz[] = getWhdToXyz();
		// Compute texture coordinate offset due to tile not being at upper left of texture
		double dXTex = tcXTotal * (getIndex().getCoordinate(whdToXyz[0]) % textureScale) / (double)textureScale;
		double dYTex = tcYTotal * (getIndex().getCoordinate(whdToXyz[1]) % textureScale) / (double)textureScale;
		double tcLeft = tc0.left() + dXTex;
		double tcRight = tcLeft + tcXTotal/textureScale;
		double tcBottom = tc0.bottom() + dYTex;
		double tcTop = tcBottom + tcYTotal/textureScale;
		
		Point2D[] textureCoordinates = new Point2D[4];
		textureCoordinates[0] = new Point2D.Double(tcLeft, tcTop);
		textureCoordinates[1] = new Point2D.Double(tcRight, tcTop);
		textureCoordinates[2] = new Point2D.Double(tcRight, tcBottom);
		textureCoordinates[3] = new Point2D.Double(tcLeft, tcBottom);
		
		Point2D[] result = {
				textureCoordinates[0],
				textureCoordinates[1],
				textureCoordinates[2],
				textureCoordinates[3]};

		// Reorder texture coordinates for X and Y viewers.
		// Sorry I don't have a theory for this, I just want it to work.
		// X
		if (getIndex().getSliceAxis() == CoordinateAxis.X) {
			result[0] = textureCoordinates[0];
			result[1] = textureCoordinates[3];
			result[2] = textureCoordinates[2];
			result[3] = textureCoordinates[1];
		}
		// Y
		else if (getIndex().getSliceAxis() == CoordinateAxis.Y) {
			result[0] = textureCoordinates[0];
			result[1] = textureCoordinates[3];
			result[2] = textureCoordinates[2];
			result[3] = textureCoordinates[1];
		}
		
		return result;
	}
	
	private int[] getWhdToXyz() {
		int whdToXyz[] = {0,1,2};
		CoordinateAxis sliceAxis = getIndex().getSliceAxis();
		if (sliceAxis == CoordinateAxis.X) whdToXyz = new int[]{2,1,0};
		else if (sliceAxis == CoordinateAxis.Y) whdToXyz = new int[]{0,2,1};
		return whdToXyz;
	}
	
	private int[] getXyzToWhd() {
		int xyzToWhd[] = {0,1,2};
		CoordinateAxis sliceAxis = getIndex().getSliceAxis();
		if (sliceAxis == CoordinateAxis.X) xyzToWhd = new int[]{0,1,2};
		else if (sliceAxis == CoordinateAxis.Y) xyzToWhd = new int[]{0,1,2};
		return xyzToWhd;
	}
	
	private BoundingBox3d computeBoundingBox() {
		// Permute coordinates for tiles that have non-Z orientations.
		int zoomScale = (int)(Math.pow(2.0, getIndex().getZoom()) + 0.1);
		// Permute coordinates for tiles that have non-Z orientations.
		int whdToXyz[] = getWhdToXyz();
		double tileWidth = tileFormat.getTileSize()[whdToXyz[0]] * zoomScale * tileFormat.getVoxelMicrometers()[whdToXyz[0]];
		double tileHeight = tileFormat.getTileSize()[whdToXyz[1]] * zoomScale * tileFormat.getVoxelMicrometers()[whdToXyz[1]];
		// Actual tile image might be smaller than tile size at edges...
		if (bestTexture != null) {
			tileWidth = bestTexture.getTexture().getUsedWidth() * zoomScale * tileFormat.getVoxelMicrometers()[whdToXyz[0]];
			tileHeight = bestTexture.getTexture().getHeight() * zoomScale * tileFormat.getVoxelMicrometers()[whdToXyz[1]];
		}
		// draw quad
        // double z = 0.0; // As far as OpenGL is concerned, all Z's are zero
		// Z index does not change with scale; XY do
        double z = (getIndex().getCoordinate(whdToXyz[2])+0.5) * tileFormat.getVoxelMicrometers()[whdToXyz[2]];
        double x0 = getIndex().getCoordinate(whdToXyz[0]) * tileFormat.getTileSize()[whdToXyz[0]] * zoomScale * tileFormat.getVoxelMicrometers()[whdToXyz[0]];
        double x1 = x0 + tileWidth;
        if ((whdToXyz[0] == 1) && (yMax != 0)) {
        	x0 = yMax - x0;
        	x1 = x0 - tileWidth;
        }
        // Raveler tile index has origin at BOTTOM left, unlike TOP left for images and
        // our coordinate system
        double y0 = getIndex().getCoordinate(whdToXyz[1]) * tileFormat.getTileSize()[whdToXyz[1]] * zoomScale * tileFormat.getVoxelMicrometers()[whdToXyz[1]];
        double y1 = y0 + tileHeight; // y inverted in OpenGL relative to image convention
        if ((whdToXyz[1] == 1) && (yMax != 0)) {
        	y0 = yMax - y0;
        	y1 = y0 - tileHeight;
        }
        BoundingBox3d result = new BoundingBox3d();
        // Bounding box will be one-voxel thick in slice direction
        double dz = 0.50 * tileFormat.getTileSize()[whdToXyz[2]];
        result.include(permutedVertex3d(x0, y0, z-dz, whdToXyz));
        result.include(permutedVertex3d(x1, y1, z+dz, whdToXyz));
		return result;
	}
	
	private Vec3[] computeCornerPositions(Camera3d camera) {
		// Permute coordinates for tiles that have non-Z orientations.
		int whdToXyz[] = getWhdToXyz();
        double z0 = getBoundingBox3d().getMin().get(whdToXyz[2]);
        double z1 = getBoundingBox3d().getMax().get(whdToXyz[2]);
        double z = 0.5 * (z0 + z1); // Center in depth
        if (camera != null)
        	z = camera.getFocus().get(whdToXyz[2]);
        double x0 = getBoundingBox3d().getMin().get(whdToXyz[0]);
        double x1 = getBoundingBox3d().getMax().get(whdToXyz[0]);
        double y0 = getBoundingBox3d().getMin().get(whdToXyz[1]);
        double y1 = getBoundingBox3d().getMax().get(whdToXyz[1]);
		Vec3 corners[] = new Vec3[4];
        corners[0] = permutedVertex3d(x0, y0, z, whdToXyz);
        corners[1] = permutedVertex3d(x1, y0, z, whdToXyz);
        corners[2] = permutedVertex3d(x1, y1, z, whdToXyz);
        corners[3] = permutedVertex3d(x0, y1, z, whdToXyz);
		return corners;
	}

	private Vec3 permutedVertex3d(double x, double y, double z, int whdToXyz[]) {
		double xyzIn[] = {x, y, z};
		int xyzToWhd[] = {0, 1, 2};
		for (int i = 0; i < 3; ++i)
			xyzToWhd[whdToXyz[i]] = i;
		Vec3 result = new Vec3(
				xyzIn[xyzToWhd[0]],
				xyzIn[xyzToWhd[1]],
				xyzIn[xyzToWhd[2]]);
		return result;
	}
	
	@Override
	public BoundingBox3d getBoundingBox3d() {
		return boundingBox3d;
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
		boundingBox3d = computeBoundingBox();
	}

	public ImageBrightnessStats getBrightnessStats() {
		if (bestTexture == null)
			return null;
		return bestTexture.getBrightnessStats();
	}

}
