package org.janelia.workstation.browser.gui.listview.icongrid;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import loci.formats.FormatException;
import org.janelia.workstation.browser.gui.options.BrowserOptions;
import org.janelia.workstation.core.model.Decorator;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.events.selection.SelectionModel;
import org.janelia.workstation.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An button containing a dynamic image, i.e. one that is loaded
 * from via the network, not a locally available icon.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImageButton<T,S> extends AnnotatedImageButton<T,S> {

    private static final Logger log = LoggerFactory.getLogger(DynamicImageButton.class);

    // GUI
    private final JLabel loadingLabel;
    private final DecoratedImage imagePanel;
    
    // Model
    private final String imageFilename;
    private List<Decorator> decorators;
    
    // State
    private BufferedImage maxSizeImage;
    private int displaySize;
    private boolean viewable = false;
    private LoadImageWorker loadWorker;
    
    
    public DynamicImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, String filepath) {
        super(imageObject, imageModel, selectionModel, filepath);
        
        this.imageFilename = filepath;
        this.decorators = imageModel.getDecorators(imageObject);
        this.imagePanel = new DecoratedImage(null, decorators);
        
        this.loadingLabel = new JLabel();
//        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        setMainComponent(loadingLabel);
    }

    public boolean cancelLoad() {
        if (loadWorker != null && !loadWorker.isDone()) {
            return loadWorker.cancel(true);
        }
        return false;
    }

    @Override
    public void setImageSize(int width, int height) {
        super.setImageSize(width, height);
        rescaleImage(width, height);
    }

    private synchronized void rescaleImage(int width, int height) {
        if (displaySize == width) {
            return;
        }
        
        if (width>0 && height>0) {
            
            if (viewable) {
                if (maxSizeImage == null) {
                    // Must be currently loading, in which case this method will get called again when the loading is done
                }
                else {
                    imagePanel.setImage(Utils.getScaledImageByWidth(maxSizeImage, width));
                }
            }
    
            this.displaySize = width;
            invalidate();
        }
    }

    /**
     * Tell the panel if its image should be viewable. When this is set to false, the images can be released from
     * memory to save space. When it's set to true, the image will be reloaded from disk if necessary.
     *
     * This method must be called from the EDT.
     *
     * @param wantViewable
     */
    @Override
    public void setViewable(boolean wantViewable) {
        super.setViewable(viewable);
        
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

                            BufferedImage image = getNewMaxSizeImage();
                            setMaxSizeImage(image);
                            registerAspectRatio(image.getWidth(), image.getHeight());

                            BufferedImage scaledImage = getNewScaledImage();
                            if (scaledImage == null) {
                                log.warn("Scaled image is null: {}",imageFilename);
                                return;
                            }

                            imagePanel.setImage(scaledImage);
                            setMainComponent(imagePanel);

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

                            imagePanel.setImage(Icons.getImage("file_error.png"));
                            imagePanel.setText(errorType, Color.red);
                            setMainComponent(imagePanel);
                            
                            loadWorker = null;
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
            }
        }
        this.viewable = wantViewable;
    }

    private boolean isUnloadImages() {
        return BrowserOptions.getInstance().isUnloadImages();
    }

    private BufferedImage getMaxSizeImage() {
        return maxSizeImage;
    }
    
    private void setMaxSizeImage(BufferedImage maxSizeImage) {
        if (viewable && maxSizeImage != null) {
            this.maxSizeImage = maxSizeImage;
        }
    }

    private void setDisplaySize(int displaySize) {
        if (displaySize>0) {
            this.displaySize = displaySize;
        }
    }

    /**
     * Override this method to hear when an aspect ratio is determined.
     */
    protected synchronized void registerAspectRatio(int width, int height) {
    }
    
    // TODO: in the future, we may want to display titles directly on the image. 
    // Currently disabled, because it will take a bit more work to finish the implementation.
    
//    @Override
//    public void setTitle(String title, int maxWidth) {
//        setTitle(title);
//    }
//
//    @Override
//    public void setSubtitle(String subtitle, int maxWidth) {
//        setSubtitle(subtitle);
//    }
    
}
