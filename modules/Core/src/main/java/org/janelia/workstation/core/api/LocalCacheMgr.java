package org.janelia.workstation.core.api;

import java.nio.file.Paths;

import com.google.common.eventbus.Subscribe;

import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.filecacheutils.LocalFileCacheStorageBuilder;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.prefs.LocalPreferenceChanged;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.ConsoleProperties;

/**
 * Singleton for local cache storage.
 */
public class LocalCacheMgr {
    private static final long GB_TO_KB_CONST = 1024 * 1024;

    // Singleton
    private static LocalCacheMgr instance;
    public static synchronized LocalCacheMgr getInstance() {
        if (instance==null) {
            instance = new LocalCacheMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private LocalFileCacheStorage localFileCacheStorage;

    private LocalCacheMgr() {
    }

    public LocalFileCacheStorage getLocalFileCacheStorage() {
        if (localFileCacheStorage == null) {
            localFileCacheStorage = new LocalFileCacheStorageBuilder()
                    .withCacheDir(Paths.get(ConsoleProperties.getLocalCacheDir()))
                    .withCapacityInKB(LocalPreferenceMgr.getInstance().getFileCacheGigabyteCapacity() * GB_TO_KB_CONST)
                    .withDisabled(LocalPreferenceMgr.getInstance().getFileCacheDisabled())
                    .build();
        }
        return localFileCacheStorage;
    }

    /**
     * @return the total size (in gigabytes) of all currently cached files.
     */
    public double getFileCacheGigabyteUsage() {
        double usage = 0.0;
        if (localFileCacheStorage != null) {
            final long kilobyteUsage = localFileCacheStorage.getCurrentSizeInKB();
            usage = kilobyteUsage / (1024.0 * 1024.0);
        }
        return usage;
    }

    public int getFileCacheGigabyteUsagePercent() {
        if (localFileCacheStorage != null) {
            return localFileCacheStorage.getUsageAsPercentage();
        } else {
            return 0;
        }
    }

    @Subscribe
    public void propChanged(LocalPreferenceChanged event) {
        if (localFileCacheStorage != null) {
            Object eventKey = event.getKey();
            if (eventKey instanceof String) {
                String propertyKey = (String) eventKey;
                switch (propertyKey) {
                    case OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY:
                        Integer newLocalCacheCapacityInGB = (Integer) event.getNewValue();
                        if (localFileCacheStorage != null && newLocalCacheCapacityInGB != null) {
                            localFileCacheStorage.setCapacityInKB(newLocalCacheCapacityInGB * GB_TO_KB_CONST);
                        }
                        break;
                    case OptionConstants.FILE_CACHE_DISABLED_PROPERTY:
                        Boolean localCacheDisabled = (Boolean) event.getNewValue();
                        localFileCacheStorage.setDisabled(localCacheDisabled);
                        break;
                }
            }
        }
    }

    /**
     * Removes all locally cached files.
     */
    public void clearFileCache() {
        if (localFileCacheStorage != null) {
            localFileCacheStorage.clear();
        }
    }

}
