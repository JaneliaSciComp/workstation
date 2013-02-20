package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Map;
import java.util.Vector;

import com.jogamp.opengl.util.texture.Texture;

public class TileSet 
extends Vector<Tile2d>
{
	private static final long serialVersionUID = 1L;

	Tile2d.Stage getMinStage() {
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
	public void seedFastTextures(Map<TileIndex, Texture> textureCache) {
		// TODO Auto-generated method stub
		
	}

	// Start loading textures of the optimal resolution for these tiles
	public void seedBestTextures(Map<TileIndex, Texture> textureCache) {
		// TODO Auto-generated method stub
		
	}
}
