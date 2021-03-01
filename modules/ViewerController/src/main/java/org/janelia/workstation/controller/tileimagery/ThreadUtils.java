package org.janelia.workstation.controller.tileimagery;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    public ThreadUtils() {
    }

    public static void followUpExecution(ExecutorService executorService, List<Future<Void>> callbacks, int awaitMin) throws Exception {
        executorService.shutdown();
        boolean completed = executorService.awaitTermination((long)awaitMin, TimeUnit.MINUTES);
        if (!completed) {
            throw new Exception("One or more operations were not completed as of shutdown.  More time than " + awaitMin + " min may be needed.");
        } else {
            Iterator var4 = callbacks.iterator();

            while(var4.hasNext()) {
                Future<Void> future = (Future)var4.next();
                future.get();
            }

        }
    }

    public static ExecutorService establishExecutor(int threadCount, ThreadFactory threadFactory) {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount, threadFactory);
        return executorService;
    }

    public static class CustomNamedThreadFactory implements ThreadFactory {
        private static Map<String, Integer> _threadNameMap = new HashMap();
        private String prefix;

        public CustomNamedThreadFactory(String prefix) {
            this.prefix = prefix;
            _threadNameMap.put(prefix, 1);
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            Integer threadNumber = (Integer)_threadNameMap.get(this.prefix);
            t.setName(this.prefix + "-" + threadNumber);
            _threadNameMap.put(this.prefix, threadNumber + 1);
            return t;
        }
    }
}
