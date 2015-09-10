package org.janelia.it.workstation.cache.large_volume;

import java.util.concurrent.ThreadFactory;

/**
 * Makes predictable, tracable thread names.
 *
 * @author fosterl
 */
public class CustomNamedThreadFactory implements ThreadFactory {

    private static int _threadNum = 1;
    private String prefix;
    public CustomNamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName(prefix + "_" + _threadNum++);
        return t;
    }

}
