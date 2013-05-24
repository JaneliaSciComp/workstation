package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PersistentCache 
{
	private Set<Integer> obsoleteGlTextures = new HashSet<Integer>();
	private Map<TileIndex, TileTexture> map = new HashMap<TileIndex, TileTexture>();
	
	public void clear() {
		for (TileTexture tile : map.values()) {
			if (tile == null)
				continue;
			PyramidTexture texture1 = tile.getTexture();
			if (texture1 == null)
				continue;
			int id = texture1.getTextureId();
			if (id < 1)
				continue;
			obsoleteGlTextures.add(id); // remember OpenGl texture IDs for later deletion.			
		}
		map.clear();
	}
	
	public boolean containsKey(TileIndex index) {
		return map.containsKey(index);
	}
	
	public TileTexture get(TileIndex index) {
		return map.get(index);
	}
	
	public Collection<? extends Integer> popObsoleteGlTextures() {
		Set<Integer> result = obsoleteGlTextures;
		obsoleteGlTextures = new HashSet<Integer>();
		return result;
	}
	
	public TileTexture put(TileIndex index, TileTexture tile) {
		return map.put(index, tile);
	}
	
	public int size() {
		return map.size();
	}
	
	public Collection<TileTexture> values() {
		return map.values();
	}

}
