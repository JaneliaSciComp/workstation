package org.janelia.it.workstation.gui.browser.gui.support;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import loci.formats.FormatException;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.LoadImageWorker;
import org.janelia.it.workstation.gui.util.Icons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoadedImagePanel extends JPanel {
    
    private static final Logger log = LoggerFactory.getLogger(LoadedImagePanel.class);
    
    protected final String imageFilename;
    
    protected final JLabel loadingLabel;
    protected final JLabel imageLabel;
    protected final JLabel errorLabel;
    
    private LoadImageWorker loadWorker;
    
    private Double aspectRatio;
    
    public LoadedImagePanel(String imageFilename) {
        
        this.imageFilename = imageFilename;
        
        setLayout(new BorderLayout());
        setOpaque(false);
        
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
                
//        setBorder(BorderFactory.createLineBorder(Color.ORANGE));
        
        setImageLabel(loadingLabel);
        
        load();
    }
    
    private void load() {
        
        loadWorker = new LoadImageWorker(imageFilename) {

            @Override
            protected void hadSuccess() {

                if (isCancelled()) {
                    return;
                }
                
                BufferedImage image = getNewScaledImage();
                if (getNewScaledImage() == null) {
                    log.warn("Scaled image is null");
                    return;
                }

                imageLabel.setIcon(new StretchIcon(image));
                aspectRatio = (double)image.getWidth() / (double)image.getHeight();
                setImageLabel(imageLabel);
                                
                loadWorker = null;
                updateUI();
                doneLoading();
            }

            @Override
            protected void hadError(Throwable error) {
                if (error instanceof FileNotFoundException) {
                    log.warn("File not found: " + imageFilename, error);
                    errorLabel.setText("File not found");
                }
                else if (error.getCause() != null && (error.getCause() instanceof FormatException)) {
                    log.warn("Image format not supported for: " + imageFilename, error);
                    errorLabel.setText("Image format not supported");
                }
                else {
                    log.warn("Image could not be loaded: " + imageFilename, error);
                    errorLabel.setText("Image could not be loaded");
                }
                setImageLabel(errorLabel);
                updateUI();
            }
        };

        loadWorker.executeInImagePool();
    }
    
    protected void doneLoading() {
        
    }

    private void setImageLabel(JLabel label) {
        removeAll();
        add(label, BorderLayout.CENTER);
    }

    public void scaleImage(int w) {
        if (aspectRatio==null) {
            log.error("Cannot scale image before it is loaded");
            return;
        }
        int h = (int) Math.round(w / aspectRatio);
//        log.info("old={} new={}",getPreferredSize(),new Dimension(w, h));
        setPreferredSize(new Dimension(w, h));
    }
}
