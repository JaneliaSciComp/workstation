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
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * An image panel that supports dynamic loading/unloading of image data to conserve memory usage when the panel is not
 * visible. Also supports on-the-fly resizing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DynamicImagePanel extends JPanel {

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
	            BufferedImage image = Utils.getScaledImage(orig, imageSize);
	            imageLabel.setIcon(new ImageIcon(image));
    		}
    	}
    	else {
    		if (maxSizeImage != null) {
				System.out.println("Warning: nonviewable image has a non-null maxSizeImage in memory");
    		}
    	}

        this.displaySize = imageSize;
        invalidate();
	}

	public synchronized void rescaleImage(double scale) {
    	if (viewable) {
    		if (maxSizeImage == null) {
    			// Must be currently loading, in which case this method will get called again when the loading is done
    		}
    		else {
	    		if (inverted && invertedMaxSizeImage == null) {
	    			setInvertedColors(true);
	    		}
	    		BufferedImage orig = inverted ? invertedMaxSizeImage : maxSizeImage;
	            BufferedImage image = Utils.getScaledImage(orig, scale);
	            imageLabel.setIcon(new ImageIcon(image));
	    		displaySize = Math.max(image.getWidth(), image.getHeight());
    		}
    	}
    	else {
    		if (maxSizeImage != null) {
				System.out.println("Warning: nonviewable image has a non-null maxSizeImage in memory");
    		}
    	}
    	
		revalidate();
		repaint();
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
				invalidate();
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
            	maxSizeImage = Utils.readImage(imageFilename);
            	if (isCancelled()) return;
            	if (maxSize != null) {
            		maxSizeImage = Utils.getScaledImage(maxSizeImage, maxSize);
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
        syncToViewerState();
        revalidate();
        repaint();
        loadWorker = null;
    }
    
    private synchronized void loadError(Throwable error) {
        if (error instanceof FileNotFoundException) {
        	System.out.println("File not found: "+imageFilename);
        	errorLabel.setText("File not found");
        }
        else if (error.getCause()!=null && (error.getCause() instanceof FormatException)) {
        	System.out.println("File not found: "+imageFilename);
            errorLabel.setText("Image format not supported");
        }
        else {
        	System.out.println("Image could not be loaded: "+imageFilename);
        	error.printStackTrace();
            errorLabel.setText("Image could not be loaded");
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
