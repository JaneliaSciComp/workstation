package org.janelia.it.workstation.gui.framework.viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.concurrent.*;

import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SwingWorker class that loads the image and rescales it to the current imageSizePercent sizing. This
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
            log.debug("Using {} image loading threads.", numWorkerThreads);
            if (useCacheBehind) {
                log.debug("Using cache behind.");
            }
            else {
                log.debug("Using cache ahead.");
            }
        }
    }

    private final String imageFilename;
    private final int displaySize;
    
    private BufferedImage maxSizeImage;
    private BufferedImage scaledImage;
    
    public LoadImageWorker(DynamicImagePanel panel, String imageFilename) {
        this.imageFilename = imageFilename;
        this.displaySize = panel.getDisplaySize();
    }

    @Override
    protected void doStuff() throws Exception {

        StopWatch stopWatch = TIMER ? new LoggingStopWatch("LoadImageWorker") : null;

        ImageCache imageCache = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getImageCache();
        if (imageCache != null) {
            this.maxSizeImage = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getImageCache().get(imageFilename);
            if (maxSizeImage != null) {
                if (TIMER) {
                    stopWatch.lap("getFromCache");
                }
                // Scale image to current image display size
                this.scaledImage = Utils.getScaledImageByWidth(maxSizeImage, displaySize);
                if (TIMER) {
                    stopWatch.lap("getScaledImageByWidth");
                }
                return;
            }
        }

        if (useCacheBehind) {
            // Async cache-behind
            URL imageFileURL = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getURL(imageFilename);
            maxSizeImage = Utils.readImage(imageFileURL);
            if (TIMER) {
                stopWatch.lap("readImage");
            }
            if (maxSizeImage != null) {
                org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getImageCache().put(imageFilename, maxSizeImage);
                if (TIMER) {
                    stopWatch.lap("putInCache");
                }
            }
        }
        else {
            // Sync cache-ahead
            File imageFile = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getCachedFile(imageFilename, false);
            maxSizeImage = Utils.readImage(imageFile.toURI().toURL());
            if (TIMER) {
                stopWatch.lap("readCachedAheadImage");
            }
        }

        if (maxSizeImage != null) {
            // Scale image to current image display size
            this.scaledImage = Utils.getScaledImageByWidth(maxSizeImage, displaySize);
            if (TIMER) {
                stopWatch.lap("getScaledImageByWidth");
            }
        }

        if (TIMER) {
            stopWatch.stop("LoadImageWorker");
        }
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


    /**
     * Adapted from SimpleWorker so that we can use a separate thread pool and customize the number of threads
     */
    private static ExecutorService executorService;
    private static synchronized ExecutorService getWorkersExecutorService() {
        if (executorService == null) {
            //this creates daemon threads.
            ThreadFactory threadFactory = new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                public Thread newThread(final Runnable r) {
                    Thread thread = defaultFactory.newThread(r);
                    thread.setName("LoadImageWorker-" + thread.getName());
                    thread.setDaemon(true);
                    return thread;
                }
            };

            executorService = new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads,
                            10L, TimeUnit.MINUTES,
                            new LinkedBlockingQueue<Runnable>(),
                            threadFactory);
        }

        return executorService;
    }

    public void executeInImagePool() {
        getWorkersExecutorService().execute(this);
    }
}
