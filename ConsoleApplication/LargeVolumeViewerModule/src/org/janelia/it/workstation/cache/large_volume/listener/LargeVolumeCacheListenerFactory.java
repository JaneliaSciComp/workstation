package org.janelia.it.workstation.cache.large_volume.listener;

import java.util.Properties;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;

/**
 * Required to listen to events from EHCache.
 * @author Leslie L Foster
 */
public class LargeVolumeCacheListenerFactory extends CacheEventListenerFactory {

	@Override
	public CacheEventListener createCacheEventListener(Properties prprts) {
		return new LargeVolumeCacheListener();
	}
	
}
