package org.janelia.jacs2.cdi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.slf4j.Logger;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ExecutorProducer {

    private final static int DEFAULT_THREAD_POOL_SIZE = 100;

    @Named("SLF4J")
    @Inject
    private Logger logger;

    @PropertyValue(name = "service.executor.ThreadPoolSize")
    @Inject
    private Integer threadPoolSize;

    @Produces
    public ExecutorService createExecutorService() {
        if (threadPoolSize == null || threadPoolSize == 0) {
            threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        }
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("JACS-%d")
                .setDaemon(true)
                .build();
        return Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    }

    public void shutdownExecutor(@Disposes @Default ExecutorService executorService) throws InterruptedException {
        logger.info("Shutting down {}", executorService);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
    }

}
