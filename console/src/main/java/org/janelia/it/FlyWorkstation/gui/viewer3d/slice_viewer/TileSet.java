package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A group of Tile2d that together form a complete image on the SliceViewer,
 * when the RavelerTileServer is used.
 * 
 * @author brunsc
 *
 */
public class TileSet 
extends HashSet<Tile2d>
{
	public static enum LoadStatus {
		NO_TEXTURES_LOADED,
		SOME_TEXTURES_LOADED,
		COARSE_TEXTURES_LOADED,
		BEST_TEXTURES_LOADED,
	};

	private LoadStatus loadStatus;
	
	public void assignTextures(TextureCache textureCache) {
		int bestCount = 0;
		int coarseCount = 0;
		int noneCount = 0;
		int totalCount = 0;
		for (Tile2d tile : this) {
			tile.assignTexture(textureCache);
			totalCount += 1;
			if (tile.getLoadStatus() == Tile2d.LoadStatus.BEST_TEXTURE_LOADED)
				bestCount += 1;
			else if (tile.getLoadStatus() == Tile2d.LoadStatus.COARSE_TEXTURE_LOADED)
				coarseCount += 1;
			else
				noneCount += 1;
		}
		if (totalCount == bestCount)
			setLoadStatus(LoadStatus.BEST_TEXTURES_LOADED);
		else if (noneCount == 0)
			setLoadStatus(LoadStatus.COARSE_TEXTURES_LOADED);
		else if (noneCount == totalCount)
			setLoadStatus(LoadStatus.NO_TEXTURES_LOADED);
		else
			setLoadStatus(LoadStatus.SOME_TEXTURES_LOADED);
	}
	
	// 1.0 means fully up-to-date
	// 0.0 means no textures have been loaded
	public float getPercentDisplayable() {
		if (this.size() == 0)
			return 1.0f; // TODO up-to-date
		float result = 0.0f;
		for (Tile2d tile : this) {
			TileTexture texture = tile.getBestTexture();
			if (texture == null)
				continue; // no texture at all
			if (texture.getLoadStatus().ordinal() < TileTexture.LoadStatus.RAM_LOADED.ordinal())
				continue; // texture is not loaded yet
			if (texture.getIndex().equals(tile.getIndex()))
				result += 1.0f; // full resolution texture
			else
				result += 0.75f; // lower resolution texture
		}
		result /= this.size();
		return result;
	}
	
	public boolean canDisplay() 
	{
		float pd = getPercentDisplayable();
		// Display if a threshold number of tiles are displayable
		final double minProportion = 0.3;
		if (pd >= minProportion)
			return true;
		else return false;
	}

	public Tile2d.LoadStatus getMinStage() {
		if (size() < 1)
			return Tile2d.LoadStatus.NO_TEXTURE_LOADED;
		Tile2d.LoadStatus result = Tile2d.LoadStatus.BEST_TEXTURE_LOADED; // start optimistic
		for (Tile2d tile : this) {
			Tile2d.LoadStatus stage = tile.getLoadStatus();
			if (stage.ordinal() < result.ordinal())
				result = stage;
		}
		return result;
	}

	public LoadStatus getLoadStatus() {
		return loadStatus;
	}

	public void setLoadStatus(LoadStatus loadStatus) {
		this.loadStatus = loadStatus;
	}

	// Start loading textures to quickly populate these tiles, even if the
	// textures are not the optimal resolution
	public Set<TileIndex> getFastNeededTextures() 
	{
		// Which tiles need to be textured?
		Set<Tile2d> untexturedTiles = new HashSet<Tile2d>();
		for (Tile2d tile : this) {
			if (tile.getLoadStatus().ordinal() < Tile2d.LoadStatus.COARSE_TEXTURE_LOADED.ordinal())
				untexturedTiles.add(tile);
		}
		// Whittle down the list one texture at a time.
		Set<TileIndex> neededTextures = new HashSet<TileIndex>();
		// Warning! This algorithm is O(n^2) on the number of tiles! TODO
		while (untexturedTiles.size() > 0) {
			// Store a score for each candidate texture
			Map<TileIndex, Double> textureScores = new HashMap<TileIndex, Double>();
			// Remember which tiles could use a particular texture
			Map<TileIndex, Set<Tile2d>> tilesByTexture = new HashMap<TileIndex, Set<Tile2d>>();
			// Accumulate a score for each candidate texture from each tile
			// TODO - cache something so we don't need to do O(n) in this loop every time
			// TODO - downweight textures that have already failed to load
			for (Tile2d tile : untexturedTiles) {
				for (TileIndex.TextureScore textureScore : tile.getIndex().getTextureScores()) 
				{
					// TODO - maybe skip textures that have failed to load
					double initialScore = 0.0;
					TileIndex textureKey = textureScore.getTextureKey();
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
			TileIndex bestTexture = null;
			// O(n) on candidate tile list is better than sorting
			for (TileIndex textureIndex : textureScores.keySet()) {
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
	public Set<TileIndex> getBestNeededTextures() {
		// Which tiles need to be textured?
		Set<TileIndex> neededTextures = new HashSet<TileIndex>();
		for (Tile2d tile : this) {
			// The best texture for each tile is always the one with the same index
			// TODO - maybe skip textures that have failed to load
			// or use the highest resolution tile that has not failed yet
			if (tile.getLoadStatus().ordinal() < Tile2d.LoadStatus.BEST_TEXTURE_LOADED.ordinal())
				neededTextures.add(tile.getIndex());
		}
		return neededTextures;
	}
	
	static class ValueComparator implements Comparator<TileIndex> {
		Map<TileIndex, Double> base;
		
		public ValueComparator(Map<TileIndex, Double> base) {
			this.base = base;
		}

		@Override
		public int compare(TileIndex lhs, TileIndex rhs) {
			if (base.get(lhs) >= base.get(rhs))
				return -1;
			return 1;					
		}
	}

}
