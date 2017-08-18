package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loci.formats.FormatException;

/**
 * An image panel that supports dynamic loading/unloading of image data to conserve memory usage when the panel is not
 * visible. Also supports on-the-fly resizing.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImagePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DynamicImagePanel.class);
    
    protected final String imageFilename;
    protected final List<ImageDecorator> decorators;
    protected final Integer maxSize;
    
    protected BufferedImage maxSizeImage;
    protected int displaySize;
    protected boolean viewable = false;

    protected final JLabel loadingLabel;
    protected final JLabel errorLabel;
    protected DecoratedImagePanel imagePanel;

    private LoadImageWorker loadWorker;
    
    private String title;
    private String subtitle;
    
    public DynamicImagePanel(String imageFilename, List<ImageDecorator> decorators, Integer maxSize) {

        setLayout(new BorderLayout());
        setOpaque(false);

        this.imageFilename = imageFilename;
        this.decorators = decorators;
        this.maxSize = maxSize;
        if (maxSize != null) {
            setPreferredSize(new Dimension(maxSize, maxSize));
        }

        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);

        errorLabel = new JLabel();
        errorLabel.setOpaque(false);
        errorLabel.setForeground(Color.red);
        errorLabel.setIcon(Icons.getMissingIcon());
        errorLabel.setVerticalTextPosition(JLabel.BOTTOM);
        errorLabel.setHorizontalTextPosition(JLabel.CENTER);
        errorLabel.setVerticalAlignment(SwingConstants.CENTER);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        setMainComponent(loadingLabel);
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
    public BufferedImage getImage() {
        return imagePanel.getImage();
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
                BufferedImage image = Utils.getScaledImageByWidth(maxSizeImage, imageSize);
                imagePanel.setImage(image);
            }
        }

        this.displaySize = imageSize;
        invalidate();
    }

    /**
     * Tell the panel if its image should be viewable. When this is set to false, the images can be released from
     * memory to save space. When it's set to true, the image will be reloaded from disk if necessary.
     *
     * This method must be called from the EDT.
     *
     * @param wantViewable
     */
    public synchronized void setViewable(final boolean wantViewable, final Callable<?> success) {
        
        log.trace("setViewable({}->{},{})",viewable,wantViewable,imageFilename);
        
        if (imageFilename != null) {
            if (wantViewable) {
                if (!this.viewable) {

                    log.trace("LoadImageWorker: {}",imageFilename);
                    
                    loadWorker = new LoadImageWorker(imageFilename, displaySize) {

                        @Override
                        protected void hadSuccess() {

                            log.trace("Load complete: {}",imageFilename);
                            
                            if (isCancelled()) {
                                log.debug("Load was cancelled");
                                return;
                            }

                            setDisplaySize(getNewDisplaySize());

                            BufferedImage image = getNewScaledImage();
                            if (image == null) {
                                log.warn("Scaled image is null: {}",imageFilename);
                                return;
                            }

                            setMaxSizeImage(getNewMaxSizeImage());
                            
                            imagePanel = new DecoratedImagePanel(decorators);
                            imagePanel.addMouseListener(new MouseForwarder(DynamicImagePanel.this, "DecoratedImagePanel->DynamicImagePanel"));
                            imagePanel.setImage(image);
                            setMainComponent(imagePanel);

                            ConcurrentUtils.invokeAndHandleExceptions(success);

                            loadWorker = null;
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            
                            String errorType;
                            if (error instanceof FileNotFoundException) {
                                log.warn("File not found: " + imageFilename);
                                errorType = "File not found";
                            }
                            else if (error.getCause() != null && (error.getCause() instanceof FormatException)) {
                                log.warn("Image format not supported for: " + imageFilename, error);
                                errorType = "Image format not supported";
                            }
                            else {
                                log.warn("Image could not be loaded: " + imageFilename, error);
                                errorType = "Image could not be loaded";
                            }

                            BufferedImage image = Icons.getImage("file_error.png");
                            Color fontColor = Color.red;
                            imagePanel = new DecoratedErrorPanel(decorators, errorType, fontColor);
                            imagePanel.addMouseListener(new MouseForwarder(DynamicImagePanel.this, "DecoratedErrorPanel->DynamicImagePanel"));
                            imagePanel.setImage(image);
                            setMainComponent(imagePanel);
                        }
                    };
                    
                    loadWorker.executeInImagePool();
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
                if (isUnloadImages()) {
                    // Clear all references to the image data so that it can be cleared out of memory
                    maxSizeImage = null;
                    imagePanel.setImage(null);
                    // Show the loading label until the image needs to be loaded again
                    setMainComponent(loadingLabel);
                }
                // Call the callback
                try {
                    if (success != null) {
                        success.call();
                    }
                }
                catch (Exception e) {
                    ConsoleApp.handleException(e);
                }
            }
        }
        this.viewable = wantViewable;
    }

    private boolean isUnloadImages() {
        Boolean unloadImagesBool = (Boolean) ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.UNLOAD_IMAGES_PROPERTY);
        return unloadImagesBool != null && unloadImagesBool;
    }

    public BufferedImage getMaxSizeImage() {
        return maxSizeImage;
    }
    
    @Override
    public int getWidth() {
        if (getPreferredSize()!=null) {
            return getPreferredSize().width;
        }
        if (imagePanel.getImage()!=null) {
            return imagePanel.getImage().getWidth();
        }
        return super.getWidth();
    }
    
    @Override
    public int getHeight() {
        if (getPreferredSize()!=null) {
            return getPreferredSize().height;
        }
        if (imagePanel.getImage()!=null) {
            return imagePanel.getImage().getHeight();
        }
        return super.getHeight();
    }
    
    private void setMaxSizeImage(BufferedImage maxSizeImage) {
        if (viewable && maxSizeImage != null) {
            this.maxSizeImage = maxSizeImage;
        }
    }

    private void setMainComponent(JComponent component) {
        removeAll();
        add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setDisplaySize(int displaySize) {
        if (displaySize>0) {
            this.displaySize = displaySize;
        }
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

}
