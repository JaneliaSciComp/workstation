package org.janelia.it.workstation.gui.large_volume_viewer.cache;

/**
 * Created by murphys on 11/6/2015.
 */
import org.janelia.it.workstation.gui.large_volume_viewer.AbstractTextureLoadAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by murphys on 10/23/2015.
 */
public class SimpleFileCache {

    private static Logger log = LoggerFactory.getLogger(SimpleFileCache.class);

    private int cacheSize=0;

    private Map<File, ByteBuffer> cache = Collections.synchronizedMap(new LinkedHashMap<File, ByteBuffer>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<File,ByteBuffer> eldest) {
            if (cache.size()>cacheSize) {
                return true;
            }
            return false;
        }
    });

    public boolean containsFile(File file) {
        ByteBuffer data=cache.get(file);
        if (data!=null && data.capacity()>3) {
            return true;
        }
        return false;
    }

    public SimpleFileCache(int cacheSize) {
        this.cacheSize=cacheSize;
    }

    public ByteBuffer getBytes(File file) {
        return cache.get(file);
    }

    public void putBytes(File file, ByteBuffer data) throws AbstractTextureLoadAdapter.TileLoadError {
        cache.put(file, data);
    }

    public int size() {
        return cache.size();
    }

    public synchronized void limitToNeighborhood(List<File> neighborhoodList) {
        Set<File> removeSet=new HashSet<>();
        for (File f : cache.keySet()) {
            if (!neighborhoodList.contains(f))
                removeSet.add(f);
        }
        for (File f : removeSet) {
            cache.remove(f);
        }
    }


}

