package org.janelia.it.workstation.shared.util.filecache;

import java.io.File;
import java.util.Set;

/**
 * Interface for receiving asynchronous cache event notifications.
 *
 * @author Eric Trautman
 */
public interface CacheLoadEventListener {

    /**
     * Called when the cache has completed loading previously
     * cached files from the local file system.
     *
     * @param  unregisteredFiles  set of files that could not be
     *                            registered (loaded).
     */
    public void loadCompleted(Set<File> unregisteredFiles);
}
