package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

import javax.swing.*;

import loci.formats.FormatException;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.util.ConcurrentUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An image panel that supports dynamic loading/unloading of image data to conserve memory usage when the panel is not
 * visible. Also supports on-the-fly resizing.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DynamicImagePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DynamicImagePanel.class);

    protected final String imageFilename;
    protected final Integer maxSize;
    protected BufferedImage maxSizeImage;
    protected BufferedImage invertedMaxSizeImage;
    protected int displaySize;
    protected boolean inverted = false;
    protected boolean viewable = false;

    protected final JLabel loadingLabel;
    protected final JLabel imageLabel;
    protected final JLabel errorLabel;

    private LoadImageWorker loadWorker;

    public DynamicImagePanel(String imageFilename, Integer maxSize) {

        setLayout(new GridBagLayout());
        setOpaque(false);

        this.imageFilename = imageFilename;
        this.maxSize = maxSize;
        if (maxSize != null) {
            setPreferredSize(new Dimension(maxSize, maxSize));
        }

        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        errorLabel = new JLabel();
        errorLabel.setOpaque(false);
        errorLabel.setForeground(Color.red);
        errorLabel.setIcon(Icons.getMissingIcon());
        errorLabel.setVerticalTextPosition(JLabel.BOTTOM);
        errorLabel.setHorizontalTextPosition(JLabel.CENTER);

        imageLabel = new JLabel();
        imageLabel.setOpaque(false);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        setImageLabel(loadingLabel);
    }

    /**
     * Cancel the current load, if there is one in progress.
     *
     * @return
     */
    public boolean cancelLoad() {
        if (loadWorker != null && !loadWorker.isDone()) {
            return loadWorker.cancel(true);
        }
        return false;
    }

    /**
     * Get the current size of the image.
     *
     * @return
     */
    public int getDisplaySize() {
        return displaySize;
    }

    /**
     * Get the underlying image, if it's currently loaded.
     *
     * @return
     */
    public Icon getImage() {
        return imageLabel.getIcon();
    }

    /**
     * Returns true if the image colors have been inverted.
     *
     * @return
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Returns true if the image is viewable (loaded or loading).
     *
     * @return
     */
    public boolean isViewable() {
        return viewable;
    }

    public synchronized void rescaleImage(int imageSize) {
        if (displaySize == imageSize) {
            return;
        }

        if (viewable) {
            if (maxSizeImage == null) {
                // Must be currently loading, in which case this method will get called again when the loading is done
            }
            else {
                if (inverted && invertedMaxSizeImage == null) {
                    setInvertedColors(true);
                }
                BufferedImage orig = inverted ? invertedMaxSizeImage : maxSizeImage;
                BufferedImage image = Utils.getScaledImage(orig, imageSize);
                imageLabel.setIcon(new ImageIcon(image));
            }
        }

        this.displaySize = imageSize;
        invalidate();
    }

    public synchronized void setInvertedColors(boolean inverted) {

        this.inverted = inverted;
        if (inverted == true && maxSizeImage != null) {
            invertedMaxSizeImage = Utils.invertImage(maxSizeImage);
        }
        else {
            // Free up memory when we don't need inverted images
            invertedMaxSizeImage = null;
        }

        rescaleImage(displaySize);
    }

    /**
     * Tell the panel if its image should be viewable. When this is set to false, the images can be released from
     * memory to save space. When it's set to true, the image will be reloaded from disk if necessary.
     *
     * This method must be called from the EDT.
     *
     * @param wantViewable
     */
    public synchronized void setViewable(final boolean wantViewable, final Callable success) {

        if (imageFilename != null) {
            if (wantViewable) {
                if (!this.viewable) {

                    loadWorker = new LoadImageWorker(this, imageFilename) {

                        @Override
                        protected void hadSuccess() {

                            if (isCancelled()) {
                                return;
                            }

                            setDisplaySize(getNewDisplaySize());

                            if (getNewScaledImage() == null) {
                                log.warn("Scaled image is null");
                                return;
                            }

                            setMaxSizeImage(getNewMaxSizeImage());
                            setImageLabel(imageLabel);
                            imageLabel.setIcon(new ImageIcon(getNewScaledImage()));
//				            syncToViewerState();
//					        invalidate();

                            ConcurrentUtils.invokeAndHandleExceptions(success);

                            loadWorker = null;
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            if (error instanceof FileNotFoundException) {
                                log.warn("File not found: " + imageFilename);
                                errorLabel.setText("File not found");
                            }
                            else if (error.getCause() != null && (error.getCause() instanceof FormatException)) {
                                log.warn("Image format not supported for: " + imageFilename);
                                errorLabel.setText("Image format not supported");
                                error.printStackTrace();
                            }
                            else {
                                log.warn("Image could not be loaded: " + imageFilename);
                                errorLabel.setText("Image could not be loaded");
                                error.printStackTrace();
                            }
                            setImageLabel(errorLabel);
                            revalidate();
                            repaint();
                        }
                    };
                    loadWorker.executeInImagePool();

                }
                else {
                    syncToViewerState();
                }
            }
            else {
                if (!this.viewable) {
                    return;
                }
                if (loadWorker != null && !loadWorker.isDone()) {
                    loadWorker.cancel(true);
                    loadWorker = null;
                }
                if (SessionMgr.getSessionMgr().isUnloadImages()) {
                    // Clear all references to the image data so that it can be cleared out of memory
                    maxSizeImage = null;
                    invertedMaxSizeImage = null;
                    imageLabel.setIcon(null);
                    // Show the loading label until the image needs to be loaded again
                    setImageLabel(loadingLabel);
                }
                // Call the callback
                try {
                    if (success != null) {
                        success.call();
                    }
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        }
        this.viewable = wantViewable;
    }

    public BufferedImage getMaxSizeImage() {
        return (inverted) ? invertedMaxSizeImage : maxSizeImage;
    }

    private void setMaxSizeImage(BufferedImage maxSizeImage) {
        if (viewable && maxSizeImage != null) {
            this.maxSizeImage = maxSizeImage;
        }
    }

    private void setImageLabel(JLabel label) {
        removeAll();
        add(label);
    }

    protected abstract void syncToViewerState();

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setDisplaySize(int displaySize) {
        this.displaySize = displaySize;
    }
}
