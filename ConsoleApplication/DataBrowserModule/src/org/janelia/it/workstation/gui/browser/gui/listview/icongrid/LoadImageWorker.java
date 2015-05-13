package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.concurrent.*;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.ImageCache;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
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
    
    public LoadImageWorker(String imageFilename) {
        this.imageFilename = imageFilename;
        this.displaySize = 0;
    }
    
    public LoadImageWorker(String imageFilename, int width) {
        this.imageFilename = imageFilename;
        this.displaySize = width;
    }

    @Override
    protected void doStuff() throws Exception {

        ImageCache imageCache = SessionMgr.getBrowser().getImageCache();
        if (imageCache != null) {
            this.maxSizeImage = imageCache.get(imageFilename);
            if (maxSizeImage != null) {
                // Scale image to current image display size
                rescaleToDisplaySize();
                return;
            }
        }

        if (useCacheBehind) {
            // Async cache-behind
            log.info("Async cache-behind loading: "+imageFilename);
            URL imageFileURL = SessionMgr.getURL(imageFilename);
            maxSizeImage = Utils.readImage(imageFileURL);
            if (maxSizeImage != null && imageCache != null) {
                imageCache.put(imageFilename, maxSizeImage);
            }
        }
        else {
            // Sync cache-ahead
            log.info("Cache-ahead loading: "+imageFilename);
            File imageFile = SessionMgr.getCachedFile(imageFilename, false);
            maxSizeImage = Utils.readImage(imageFile.toURI().toURL());
        }

        if (maxSizeImage != null) {
            // Scale image to current image display size
            rescaleToDisplaySize();
        }
    }

    private void rescaleToDisplaySize() {
        if (displaySize>0) {
            this.scaledImage = Utils.getScaledImageByWidth(maxSizeImage, displaySize);
        }
        else {
            this.scaledImage = maxSizeImage;
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
