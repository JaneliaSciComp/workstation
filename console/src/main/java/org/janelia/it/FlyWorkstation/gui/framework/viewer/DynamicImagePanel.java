    package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.swing.*;

import loci.formats.FormatException;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
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
    private ImageCache imageCache; 

	public DynamicImagePanel(String imageFilename) {
		this(imageFilename, null);
	}
	
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
     * @return
     */
    public int getDisplaySize() {
        return displaySize;
    }
    
    /**
     * Get the underlying image, if it's currently loaded.
     * @return
     */
    public Icon getImage() {
        return imageLabel.getIcon();
    }
    
    /**
     * Returns true if the image colors have been inverted.
     * @return
     */
    public boolean isInverted() {
		return inverted;
	}

    /**
     * Returns true if the image is viewable (loaded or loading). 
     * @return
     */
	public boolean isViewable() {
		return viewable;
	}

	public void setCache(ImageCache imageCache) {
		this.imageCache = imageCache;
	}

	public synchronized void rescaleImage(int imageSize) {
    	if (viewable) {
    		if (maxSizeImage == null) {
    			// Must be currently loading, in which case this method will get called again when the loading is done
    		}
    		else {
	    		if (inverted && invertedMaxSizeImage == null) {
	    			setInvertedColors(true);
	    		}
	    		BufferedImage orig = inverted ? invertedMaxSizeImage : maxSizeImage;
        		int newWidth = imageSize;
                double scale = (double) newWidth / (double) maxSizeImage.getWidth();
                int newHeight = (int) Math.round(scale * maxSizeImage.getHeight());
                BufferedImage image = Utils.getScaledImage(orig, newWidth, newHeight);
	            imageLabel.setIcon(new ImageIcon(image));
    		}
    	}
    	else {
    		if (maxSizeImage != null) {
				log.warn("Non-viewable image has a non-null maxSizeImage in memory");
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
	public synchronized void setViewable(boolean wantViewable, Callable success) {
		
		if (imageFilename!=null) {
			if (wantViewable) {
				if (!this.viewable) {
					loadWorker = new LoadImageWorker(success);
					loadWorker.execute();
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
		    	// Clear all references to the image data so that it can be cleared out of memory
		    	maxSizeImage = null;
		    	invertedMaxSizeImage = null;
		    	imageLabel.setIcon(null);
		    	// Show the loading label until the image needs to be loaded again
		        setImageLabel(loadingLabel);
				// Call the callback
				try {
					if (success!=null) success.call();
				}
				catch (Exception e) {
					SessionMgr.getSessionMgr().handleException(e);
				}
			}
		}
		this.viewable = wantViewable;
	}
    
	/**
     * SwingWorker class that loads the image and rescales it to the current imageSizePercent sizing.  This
     * thread supports being canceled.
     * if an ImageCache has been set with setImageCache then this method will look there first.
     */
    private class LoadImageWorker extends SimpleWorker {
    	
    	private Callable success;
    	
    	public LoadImageWorker(Callable success) {
    		this.success = success;
    	}
    	
        @Override
		protected void doStuff() throws Exception {
            BufferedImage maxSizeImage = imageCache==null ? null : imageCache.get(imageFilename);
            if (maxSizeImage == null) {
                URL imageFileURL = SessionMgr.getURL(imageFilename);
                // Some extra finagling is required because LOCI libraries do not like the file protocol for some reason
                if (imageFileURL.getProtocol().equals("file")) {
                    String localFilepath = imageFileURL.toString().replace("file:","");
                    log.trace("loading cached file: {}", localFilepath);
                    maxSizeImage = Utils.readImage(localFilepath);
                }
                else {
                    log.trace("loading url: {}", imageFileURL);
                    maxSizeImage = Utils.readImage(imageFileURL);
                }
                
            	if (isCancelled()) return;
            	if (maxSize != null) {
            		int newWidth = maxSize;
                    double scale = (double) newWidth / (double) maxSizeImage.getWidth();
                    int newHeight = (int) Math.round(scale * maxSizeImage.getHeight());
            		maxSizeImage = Utils.getScaledImage(maxSizeImage, newWidth, newHeight);
            		displaySize = maxSize;
            	}
            	else {
            		displaySize = Math.max(maxSizeImage.getWidth(), maxSizeImage.getHeight());
            	}
            }
            if (isCancelled()) return;
    		if (imageCache!=null) {
    			imageCache.put(imageFilename, maxSizeImage);
    		}
            setMaxSizeImage(maxSizeImage);
		}

		@Override
		protected void hadSuccess() {
            loadDone();
            try {
            	if (success!=null) success.call();
            }
            catch (Exception e) {
            	SessionMgr.getSessionMgr().handleException(e);
            }
		}

		@Override
		protected void hadError(Throwable error) {
            loadError(error);
		}
    }

    private synchronized void loadDone() {
        setImageLabel(imageLabel);
        revalidate();
        syncToViewerState();
        loadWorker = null;
    }
    
    private synchronized void loadError(Throwable error) {
        if (error instanceof FileNotFoundException) {
        	log.warn("File not found: "+imageFilename);
        	errorLabel.setText("File not found");
        }
        else if (error.getCause()!=null && (error.getCause() instanceof FormatException)) {
        	log.warn("Image format not supported for: "+imageFilename);
            errorLabel.setText("Image format not supported");
            error.printStackTrace();
        }
        else {
        	log.warn("Image could not be loaded: "+imageFilename);
            errorLabel.setText("Image could not be loaded");
            error.printStackTrace();
        }
        setImageLabel(errorLabel);
        revalidate();
        repaint();
    }
    
	public synchronized BufferedImage getMaxSizeImage() {
		return (inverted) ? invertedMaxSizeImage : maxSizeImage;
	}
	
	private synchronized void setMaxSizeImage(BufferedImage maxSizeImage) {
		if (viewable) this.maxSizeImage = maxSizeImage;
	}
	
    private synchronized void setImageLabel(JLabel label) {
        removeAll();
        add(label);
    }

    protected abstract void syncToViewerState(); 
}
