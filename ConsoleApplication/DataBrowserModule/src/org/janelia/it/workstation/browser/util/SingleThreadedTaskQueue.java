package org.janelia.it.workstation.browser.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple asynchronous queue for single-threaded execution of any void callable.
 *
 * @author fosterl
 */
public class SingleThreadedTaskQueue {
    private static ExecutorService service = Executors.newSingleThreadExecutor();
    public static void submit(Callable<Void> callable) {
        service.submit(callable);
    }
}
