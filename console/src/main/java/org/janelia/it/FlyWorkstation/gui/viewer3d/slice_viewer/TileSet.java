package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * A group of Tile2d that together form a complete image on the SliceViewer,
 * when the RavelerTileServer is used.
 * 
 * @author brunsc
 *
 */
public class TileSet 
extends Vector<Tile2d>
{
	private static final long serialVersionUID = 1L;

	public void assignTextures(Map<PyramidTileIndex, TileTexture> textureCache) {
		for (Tile2d tile : this) {
			tile.assignTexture(textureCache);
		}
	}
	
	public boolean canDisplay() 
	{
		for (Tile2d tile : this) {
			TileTexture texture = tile.getBestTexture();
			if (texture == null)
				return false;
			if (texture.getStage().ordinal() < TileTexture.Stage.RAM_LOADED.ordinal())
				return false;
		}
		return true;
	}

	public Tile2d.Stage getMinStage() {
		if (size() < 1)
			return Tile2d.Stage.NO_TEXTURE_LOADED;
		Tile2d.Stage result = Tile2d.Stage.BEST_TEXTURE_LOADED; // start optimistic
		for (Tile2d tile : this) {
			Tile2d.Stage stage = tile.getStage();
			if (stage.ordinal() < result.ordinal())
				result = stage;
		}
		return result;
	}

	// Start loading textures to quickly populate these tiles, even if the
	// textures are not the optimal resolution
	public Set<PyramidTileIndex> getFastNeededTextures() 
	{
		// Which tiles need to be textured?
		Set<Tile2d> untexturedTiles = new HashSet<Tile2d>();
		for (Tile2d tile : this) {
			if (tile.getStage().ordinal() < Tile2d.Stage.COARSE_TEXTURE_LOADED.ordinal())
				untexturedTiles.add(tile);
		}
		// Whittle down the list one texture at a time.
		Set<PyramidTileIndex> neededTextures = new HashSet<PyramidTileIndex>();
		// Warning! This algorithm is O(n^2) on the number of tiles! TODO
		while (untexturedTiles.size() > 0) {
			// Store a score for each candidate texture
			Map<PyramidTileIndex, Double> textureScores = new HashMap<PyramidTileIndex, Double>();
			// Remember which tiles could use a particular texture
			Map<PyramidTileIndex, Set<Tile2d>> tilesByTexture = new HashMap<PyramidTileIndex, Set<Tile2d>>();
			// Accumulate a score for each candidate texture from each tile
			// TODO - cache something so we don't need to do O(n) in this loop every time
			// TODO - downweight textures that have already failed to load
			for (Tile2d tile : untexturedTiles) {
				for (PyramidTileIndex.TextureScore textureScore : tile.getIndex().getTextureScores()) 
				{
					// TODO - maybe skip textures that have failed to load
					double initialScore = 0.0;
					PyramidTileIndex textureKey = textureScore.getTextureKey();
					if (textureScores.containsKey(textureKey))
						initialScore = textureScores.get(textureKey);
					textureScores.put(textureKey, initialScore + textureScore.getScore());
					// remember that this tile could use this texture
					if (! tilesByTexture.containsKey(textureKey))
						tilesByTexture.put(textureKey, new HashSet<Tile2d>());
					tilesByTexture.get(textureKey).add(tile);
				}
			}
			// Choose the highest scoring texture for inclusion
			double bestScore = 0.0;
			PyramidTileIndex bestTexture = null;
			// O(n) on candidate tile list is better than sorting
			for (PyramidTileIndex textureIndex : textureScores.keySet()) {
				if (textureScores.get(textureIndex) > bestScore) {
					bestTexture = textureIndex;
				}
			}
			assert(bestTexture != null);
			neededTextures.add(bestTexture);
			// remove satisfied tiles from untextured tiles list
			for (Tile2d tile : tilesByTexture.get(bestTexture)) {
				untexturedTiles.remove(tile);
			}
		}
		return neededTextures;
	}

	// Start loading textures of the optimal resolution for these tiles
	public Set<PyramidTileIndex> getBestNeededTextures() {
		// Which tiles need to be textured?
		Set<PyramidTileIndex> neededTextures = new HashSet<PyramidTileIndex>();
		for (Tile2d tile : this) {
			// The best texture for each tile is always the one with the same index
			// TODO - maybe skip textures that have failed to load
			// or use the highest resolution tile that has not failed yet
			if (tile.getStage().ordinal() < Tile2d.Stage.BEST_TEXTURE_LOADED.ordinal())
				neededTextures.add(tile.getIndex());
		}
		return neededTextures;
	}
	
	class ValueComparator implements Comparator<PyramidTileIndex> {
		Map<PyramidTileIndex, Double> base;
		
		public ValueComparator(Map<PyramidTileIndex, Double> base) {
			this.base = base;
		}

		@Override
		public int compare(PyramidTileIndex lhs, PyramidTileIndex rhs) {
			if (base.get(lhs) >= base.get(rhs))
				return -1;
			return 1;					
		}
	}

}
