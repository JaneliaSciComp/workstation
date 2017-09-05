package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loci.formats.FormatException;

/**
 * An AnnotatedImageButton with a dynamic image, i.e. one that is loaded
 * from via the network, not a locally available icon.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImageButton<T,S> extends AnnotatedImageButton<T,S> {

    private static final Logger log = LoggerFactory.getLogger(DynamicImageButton.class);

    // Definition
    private final String imageFilename;
    private List<ImageDecorator> decorators;
    
    // UI
    private final JLabel loadingLabel;
    private final DecoratedImagePanel imagePanel;
    
    // State
    private BufferedImage maxSizeImage;
    private int displaySize;
    private boolean viewable = false;
    private LoadImageWorker loadWorker;
    
    
    public DynamicImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, ImagesPanel<T,S> imagesPanel, String filepath) {
        super(imageObject, imageModel, selectionModel, imagesPanel, filepath);
        
        this.imageFilename = filepath;
        this.decorators = imageModel.getDecorators(imageObject);
        
        this.loadingLabel = new JLabel();
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        setMainComponent(loadingLabel);

        this.imagePanel = new DecoratedImagePanel(null, decorators);
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

    @Override
    public void setViewable(boolean viewable) {
        super.setViewable(viewable);
        setViewable(viewable, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (getMaxSizeImage() != null && imagePanel.getImage() != null) {
                    int w = imagePanel.getPreferredSize().width;
                    int h = imagePanel.getPreferredSize().height;
                    registerAspectRatio(w, h);
                }
                return null;
            }

        });
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

                            imagePanel.setImage(Icons.getImage("file_error.png"));
                            imagePanel.setText(errorType, Color.red);
                            setMainComponent(imagePanel);
                            
                            ConcurrentUtils.invokeAndHandleExceptions(success);
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
