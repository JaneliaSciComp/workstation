package org.janelia.it.workstation.cache.large_volume.listener;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log event passages.
 *
 * @author Leslie L Foster
 */
public class LargeVolumeCacheListener implements CacheEventListener {

	private Logger log = LoggerFactory.getLogger(LargeVolumeCacheListener.class);
	
	@Override
	public void notifyElementRemoved(Ehcache ehch, Element elmnt) throws CacheException {
		log.info("Element {} removed.", elmnt.getKey());
	}

	@Override
	public void notifyElementPut(Ehcache ehch, Element elmnt) throws CacheException {
		log.info("Element {} put.", elmnt.getKey());
	}

	@Override
	public void notifyElementUpdated(Ehcache ehch, Element elmnt) throws CacheException {
		log.info("Element {} updated.", elmnt.getKey());
	}

	@Override
	public void notifyElementExpired(Ehcache ehch, Element elmnt) {
	}

	@Override
	public void notifyElementEvicted(Ehcache ehch, Element elmnt) {
		log.info("Element {} evicted.", elmnt.getKey());
	}

	@Override
	public void notifyRemoveAll(Ehcache ehch) {
	}

	@Override
	public void dispose() {
	}
	
	public Object clone() throws CloneNotSupportedException {
		return new LargeVolumeCacheListener();
	}
	
}

