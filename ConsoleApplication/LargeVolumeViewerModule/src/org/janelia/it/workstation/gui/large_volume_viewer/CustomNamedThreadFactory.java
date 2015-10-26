package org.janelia.it.workstation.gui.large_volume_viewer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * Makes predictable, traceable thread names.
 *
 * @author fosterl
 */
public class CustomNamedThreadFactory implements ThreadFactory {

    private static Map<String,Integer> _threadNameMap = new HashMap<>();
    private String prefix;
    public CustomNamedThreadFactory(String prefix) {
        this.prefix = prefix;
        _threadNameMap.put(prefix, 1);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        final Integer threadNumber = _threadNameMap.get(prefix);
        t.setName(prefix + "-" + threadNumber);
        _threadNameMap.put(prefix, threadNumber + 1);
        return t;
    }

}
