package org.janelia.jacs2.asyncservice.common;


import com.offbynull.coroutines.user.CoroutineRunner;
import org.janelia.jacs2.cdi.qualifier.SuspendedTaskExecutor;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class SuspendedTaskHandler {

    private final ExecutorService executor;
    private final BlockingQueue<CoroutineRunner> coroutingRunners;

    @Inject
    SuspendedTaskHandler(@SuspendedTaskExecutor ExecutorService executor) {
        this.executor = executor;
        coroutingRunners = new LinkedBlockingQueue<>();
        executor.submit(() -> executeCoroutines());
    }

    private void executeCoroutines() {
        for (;;) {
            try {
                CoroutineRunner runner = coroutingRunners.take();
                if (runner.execute()) {
                    coroutingRunners.offer(runner);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void add(CoroutineRunner coroutineRunner) {
        coroutingRunners.offer(coroutineRunner);
    }
}
