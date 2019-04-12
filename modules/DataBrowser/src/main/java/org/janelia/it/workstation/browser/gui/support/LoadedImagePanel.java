package org.janelia.it.workstation.browser.gui.support;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.janelia.it.workstation.browser.gui.listview.icongrid.DecoratedImage;
import org.janelia.it.workstation.browser.gui.listview.icongrid.LoadImageWorker;
import org.janelia.workstation.common.gui.model.ImageDecorator;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loci.formats.FormatException;

/**
 * An image that is loaded asynchronously from a standard filename. 
 * 
 * If the image cannot be loaded, a user-friendly warning is displayed instead.
 * 
 * The panel may be scaled as needed, resizing the image while keeping the aspect ratio intact. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoadedImagePanel extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(LoadedImagePanel.class);
    
    private static final int DEFAULT_HEIGHT = 20;
    
    protected final String imageFilename;
    protected final List<ImageDecorator> decorators;
    
    protected final JLabel loadingLabel;
    protected JComponent activeComponent;
    private final DecoratedImage imagePanel;
    
    private LoadImageWorker loadWorker;
    private Double aspectRatio;

    public LoadedImagePanel(String imageFilename) {
        this(imageFilename, null);
    }
    
    public LoadedImagePanel(String imageFilename, List<ImageDecorator> decorators) {
        
        this.imageFilename = imageFilename;
        this.decorators = decorators;
        this.imagePanel = new DecoratedImage(null, decorators);
        
        setLayout(new BorderLayout());
        setOpaque(false);
        
        loadingLabel = new JLabel();
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (imageFilename!=null) {
            load();
        }
    }

    private void load() {
        
        setImageComponent(loadingLabel);
        loadWorker = new LoadImageWorker(imageFilename) {

            @Override
            protected void hadSuccess() {

                if (isCancelled()) {
                    return;
                }
                
                BufferedImage image = getNewScaledImage();
                if (image == null) {
                    log.warn("Scaled image is null");
                    return;
                }

                aspectRatio = (double)image.getWidth() / (double)image.getHeight();
                
                imagePanel.setImage(image);
                imagePanel.setFillParent(true);
                setImageComponent(imagePanel);
                                
                doneLoading();
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
                
                imagePanel.setImage(image);
                imagePanel.setText(errorType, Color.red);
                setImageComponent(imagePanel);
                
                doneLoading();
                loadWorker = null;
            }
        };

        loadWorker.executeInImagePool();
    }
    
    protected void doneLoading() {
        
    }

    private void setImageComponent(JComponent component) {
        this.activeComponent = component;
        removeAll();
        component.addMouseListener(new MouseForwarder(this, "MainComponent->LoadedImagePanel"));
        add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void scaleImage(int w) {
        if (w<0) return;
        int h = DEFAULT_HEIGHT;
        if (aspectRatio==null) {
            if (activeComponent==loadingLabel) {
                h = loadingLabel.getIcon().getIconHeight()+10;
            }
            else {
                Dimension preferredSize = imagePanel.getPreferredSize();
                if (preferredSize!=null) {
                    h = preferredSize.height;
                }
            }
        }
        else {
            h = (int) Math.round(w / aspectRatio);
        }

        log.trace("Scaling image with preferredSize=({},{})", w, h);
        setPreferredSize(new Dimension(w, h));
    }
}
