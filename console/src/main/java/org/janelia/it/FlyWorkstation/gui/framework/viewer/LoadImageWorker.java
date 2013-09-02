package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.*;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.awt.AppContext;

/**
 * SwingWorker class that loads the image and rescales it to the current imageSizePercent sizing.  This
 * thread supports being canceled.
 * if an ImageCache has been set with setImageCache then this method will look there first.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class LoadImageWorker extends SimpleWorker {
	
    private static final Logger log = LoggerFactory.getLogger(LoadImageWorker.class);

    private static final boolean TIMER = log.isDebugEnabled();
    
    private static final String LOAD_IMAGE_WORKER_THREADS_PROPERTY = "console.images.workerThreads";
    private static final String CACHE_BEHIND_PROPERTY = "console.images.ayncCacheBehind";
    
    public static final int numWorkerThreads = ConsoleProperties.getInt(LOAD_IMAGE_WORKER_THREADS_PROPERTY, 10);
    public static final boolean useCacheBehind = ConsoleProperties.getBoolean(CACHE_BEHIND_PROPERTY, true);
        
    static {
        if (log.isDebugEnabled()) {
            log.debug("Using {} image loading threads.",numWorkerThreads);
            if (useCacheBehind) {
                log.debug("Using cache behind.");
            }
            else {
                log.debug("Using cache ahead.");
            }
        }
    }
    
    private String imageFilename;
    private BufferedImage maxSizeImage;
    private BufferedImage scaledImage;
    private int displaySize;
    
	public LoadImageWorker(DynamicImagePanel panel, String imageFilename) {
	    this.imageFilename = imageFilename;
	    this.displaySize = panel.getDisplaySize();
	}
	
    @Override
	protected void doStuff() throws Exception {
        
        StopWatch stopWatch = TIMER ? new LoggingStopWatch("LoadImageWorker") : null;
        
        ImageCache imageCache = SessionMgr.getBrowser().getImageCache();
        if (imageCache!=null) {
            this.maxSizeImage = SessionMgr.getBrowser().getImageCache().get(imageFilename);
            if (maxSizeImage != null) {
                if (TIMER) stopWatch.lap("getFromCache");
                // Scale image to current image display size
                this.scaledImage = Utils.getScaledImageByWidth(maxSizeImage, displaySize);
                if (TIMER) stopWatch.lap("getScaledImageByWidth");
                return;
            }
        }
        
        if (useCacheBehind) {
            // Async cache-behind
            URL imageFileURL = SessionMgr.getURL(imageFilename);
            maxSizeImage = Utils.readImage(imageFileURL);
            if (TIMER) stopWatch.lap("readImage");
            if (maxSizeImage!=null) {
                SessionMgr.getBrowser().getImageCache().put(imageFilename, maxSizeImage);
                if (TIMER) stopWatch.lap("putInCache");
            }
        }
        else {
            // Sync cache-ahead
            File imageFile = SessionMgr.getCachedFile(imageFilename, false);
            maxSizeImage = Utils.readImage(imageFile.toURI().toURL());
            if (TIMER) stopWatch.lap("readCachedAheadImage");
        }
        
        if (maxSizeImage != null) {
            // Scale image to current image display size
            this.scaledImage = Utils.getScaledImageByWidth(maxSizeImage, displaySize);
            if (TIMER) stopWatch.lap("getScaledImageByWidth");
        }
        
        if (TIMER) stopWatch.stop("LoadImageWorker");
	}
    
    protected BufferedImage getNewMaxSizeImage() {
        return maxSizeImage;
    }
    protected BufferedImage getNewScaledImage() {
        return scaledImage;
    }
    
    protected int getNewDisplaySize() {
        return displaySize;
    }
    
    /** Copied from SimpleWorker so that we can use a separate thread pool and customize the number of threads */
    private static synchronized ExecutorService getWorkersExecutorService() {
        final AppContext appContext = AppContext.getAppContext();
        ExecutorService executorService =
            (ExecutorService) appContext.get(LoadImageWorker.class);
        if (executorService == null) {
            //this creates daemon threads.
            ThreadFactory threadFactory = 
                new ThreadFactory() {
                    final ThreadFactory defaultFactory = 
                        Executors.defaultThreadFactory();
                    public Thread newThread(final Runnable r) {
                        Thread thread = 
                            defaultFactory.newThread(r);
                        thread.setName("LoadImageWorker-" 
                            + thread.getName());
                        thread.setDaemon(true);
                        return thread;
                    }
                };

            executorService =
                new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads,
                                       10L, TimeUnit.MINUTES,
                                       new LinkedBlockingQueue<Runnable>(),
                                       threadFactory);

            appContext.put(LoadImageWorker.class, executorService);

            // Don't use ShutdownHook here as it's not enough. We should track
            // AppContext disposal instead of JVM shutdown, see 6799345 for details
            final ExecutorService es = executorService;

            appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        boolean disposed = (Boolean)pce.getNewValue();
                        if (disposed) {
                            final WeakReference<ExecutorService> executorServiceRef =
                                new WeakReference<ExecutorService>(es);
                            final ExecutorService executorService =
                                executorServiceRef.get();
                            if (executorService != null) {
                                AccessController.doPrivileged(
                                    new PrivilegedAction<Void>() {
                                        public Void run() {
                                            executorService.shutdown();
                                            return null;
                                        }
                                    }
                                );
                            }
                        }
                    }
                }
            );
        }
        return executorService;
    }

    public void executeInImagePool() {
        getWorkersExecutorService().execute(this);
    }
}