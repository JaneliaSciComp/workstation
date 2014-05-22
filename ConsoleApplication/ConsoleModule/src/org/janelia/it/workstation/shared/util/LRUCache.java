package org.janelia.it.workstation.shared.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A max size cache with a Least-Recently-Used removal policy.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {

	private int maxEntries;
	
	public LRUCache(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return size() > maxEntries;
	}
	
}
