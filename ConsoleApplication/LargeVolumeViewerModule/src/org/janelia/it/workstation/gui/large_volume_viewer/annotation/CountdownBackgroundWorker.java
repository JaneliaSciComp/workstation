package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import java.util.concurrent.atomic.AtomicInteger;
import org.janelia.it.workstation.shared.workers.BackgroundWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a background worker, that simple awaits a countdown of
 * a counting semaphore, prior to back notification of completion.  Good for
 * unifying a lot of small task-completion notices.
 *
 * @author fosterl
 */
public class CountdownBackgroundWorker extends BackgroundWorker {
    
    public static final int WAIT_INTERIM_S = 2;
    public static final int WAIT_INTERIM_MS = 1000 * WAIT_INTERIM_S;
    
    private final AtomicInteger countdownSemaphore;
    private final String name;
    private Integer remainingWaitTime;  // Optional: null-> wait forever.
    
    private final Logger logger = LoggerFactory.getLogger( CountdownBackgroundWorker.class );
    
    public CountdownBackgroundWorker( String name, AtomicInteger countdownSemaphore ) {
        this( name, countdownSemaphore, null );
    }
    
    public CountdownBackgroundWorker( String name, AtomicInteger countdownSemaphore, Integer maxWaitTime ) {
        this.name = name;
        this.countdownSemaphore = countdownSemaphore;
        this.remainingWaitTime = maxWaitTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected void doStuff() throws Exception {
        while ( countdownSemaphore.get() > 0 ) {
            try {
                Thread.sleep( WAIT_INTERIM_MS );
                
                if (remainingWaitTime != null) {
                    remainingWaitTime -= WAIT_INTERIM_MS;
                    if (remainingWaitTime <= 0) {
                        break;
                    }
                }
                
            } catch (InterruptedException ex) {
                logger.warn("Exception {} during countdown for {}.", ex, name);
            }
        }
    }

}
