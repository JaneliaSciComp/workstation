package org.janelia.jacs2.cdi;

import org.janelia.jacs2.cdi.qualifier.PropertyValue;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorProducer {

    private final static int DEFAULT_THREAD_POOL_SIZE = 100;

    @PropertyValue(name = "service.executor.ThreadPoolSize")
    @Inject
    private Integer threadPoolSize;

    @Singleton
    @Produces
    public Executor createExecutor() {
        if (threadPoolSize == null || threadPoolSize == 0) {
            threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        }
        return Executors.newFixedThreadPool(threadPoolSize);
    }

}
