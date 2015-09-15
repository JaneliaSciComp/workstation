package org.janelia.it.workstation.cache.large_volume;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;

/**
 * May require serializable objects. Neither 'Future' nor its generic target
 * will ever be serializable. However, since our cache resides at all times in
 * memory, making the cached object transient should not be problematic.
 */
public class CachableWrapper implements Serializable {

        // The sizing of this transient, future object must be ignored,
    // because otherwise, the ObjectGraphWalker from ehcache will
    // leak into the entire JVM, signalling many warnings.
    @IgnoreSizeOf
    private transient Future<byte[]> wrappedObject;

        // Not seeing max depth exceeded problems with empty future ref.
    //private Future<String> someObjectWhichMightCauseProblems;
        // No max depth exceeded problem with just transient;
    //private transient String aThing = "I BET YOU THIS WILL CAUSE PROBLEMS\n";
    private byte[] bytes; // The actual, final data.

    public CachableWrapper(Future<byte[]> object, byte[] bytes) {
        wrappedObject = object;
        this.bytes = bytes;
    }

    public byte[] getBytes() throws InterruptedException, ExecutionException {
        if (getWrappedObject() != null && (!getWrappedObject().isDone())) {
            getWrappedObject().get();  // All threads await...
        }
        wrappedObject = null;      // Eligible for GC
        return bytes;
    }

    Future<byte[]> getWrappedObject() {
        return wrappedObject;
    }

}    
