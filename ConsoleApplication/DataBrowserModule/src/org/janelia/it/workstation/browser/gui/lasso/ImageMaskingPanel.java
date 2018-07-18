package org.janelia.it.workstation.browser.gui.lasso;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel allows a user to choose areas of an image to mask out with black. It is useful for mask-based searches.
 * 
 * The lasso tool and polygon masking are implementing by code copy and pasted from ImageJ, with many parts commented out.
 * It's not possible to reuse ImageJ jar directly due to how closely coupled it is to the AWT window. 
 *  
 * While this was the quickest way to implement a fully featured lasso tool, it's not very unmaintainable. In the future, 
 * it should be replaced by a lasso tool from ImageJ2, which promises to allow reuse. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageMaskingPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(ImageMaskingPanel.class);

    private BufferedImage image;
    private ImagePlus imagePlus;
    private JButton maskButton;
    private JButton resetButton;
    private JButton continueButton;
    private JButton cancelButton;
    private BufferedImage mask;
    private Consumer<BufferedImage> onMask;
    private Consumer<BufferedImage> onContinue;
    private Consumer<Void> onCancel;
    
    public ImageMaskingPanel() {
        
        setLayout(new BorderLayout());

        maskButton = new JButton("Mask");
        maskButton.setFocusable(false);
        maskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createMask();
                if (onMask!=null) {
                    onMask.accept(mask);
                }
            }
        });

        resetButton = new JButton("Revert");
        resetButton.setEnabled(false);
        resetButton.setFocusable(false);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetButton.setEnabled(false);
                mask = null;
                setImage(image);
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.setFocusable(false);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onContinue!=null) {
                    onCancel.accept(null);
                }
            }
        });
        
        continueButton = new JButton("Continue");
        continueButton.setFocusable(false);
        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mask==null) {
                    createMask();
                }
                if (onContinue!=null) {
                    onContinue.accept(mask);
                }
            }
        });
        
        addUI();
    }

    public void setImage(BufferedImage image) {

        if (image==null) {
            throw new IllegalArgumentException("Null image");
        }
        
        this.image = image;
        
        // ImageJ wants images in this format
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        
        this.imagePlus = new BufferedImagePlus(rgbImage);
        addUI();
    }
    
    public void addUI() {
        removeAll();
        if (imagePlus!=null) {
            add((BufferedImageCanvas)imagePlus.getCanvas(), BorderLayout.CENTER);
        }
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(maskButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.add(continueButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        revalidate();
        repaint();
    }

    public void setOnMask(Consumer<BufferedImage> onMask) {
        this.onMask = onMask;
    }

    public void setOnContinue(Consumer<BufferedImage> onContinue) {
        this.onContinue = onContinue;
    }

    public void setOnCancel(Consumer<Void> onCancel) {
        this.onCancel = onCancel;
    }
    
    private void createMask() {
        
        if (imagePlus.getRoi()==null) {
            if (mask == null) {
                this.mask = image;
            }
            else {
                // Already have mask
            }
            return;
        }
        
        prepareProcessor(imagePlus.getImageProcessor(), imagePlus);
        
        // This is usually done by the PlugInFilterRunner. It must be run for unmasking to work:
        imagePlus.getImageProcessor().snapshot();
        
        // Run the plugin
        Filler fill = new Filler();
        fill.setup("outside", imagePlus);
        fill.run(imagePlus.getImageProcessor());
        
        // Enable revert button
        resetButton.setEnabled(true);
        
        this.mask = imagePlus.getImageProcessor().getBufferedImage();
    }
    
    public BufferedImage getMask() {
        return this.mask;
    }
    
    // Code from PlugInFilterRunner
    private void prepareProcessor(ImageProcessor ip, ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi!=null && roi.isArea())
            ip.setRoi(roi);
        else
            ip.setRoi((Roi)null);
    }
    
    public JButton getMaskButton() {
        return maskButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }

    public JButton getContinueButton() {
        return continueButton;
    }

    /**
     * Test harness.
     */
    public static void main(String[] args) throws Exception {

        File inputFile = new File("/Users/rokickik/Desktop/ColorMIPsEasy/BJD_123B11_AE_01-20170324_26_F2-40x-Brain-JFRC2013_63x-CH1_CDM.png");
        File outputFile = new File("/Users/rokickik/Desktop/ColorMIPsEasy/mask.png");
        
        BufferedImage image = ImageIO.read(inputFile);
        
        ImageMaskingPanel maskingPanel = new ImageMaskingPanel();
        maskingPanel.setImage(image);
        maskingPanel.setOnContinue((BufferedImage mask) -> {
            if (mask!=null) {
                try {
                    ImageIO.write(mask, "png", outputFile);
                    System.out.println("Wrote mask to "+outputFile);
                    System.exit(0);
                }
                catch (Exception e) {
                    log.error("Error", e);
                }
            }
        });
        
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(maskingPanel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        
    }

}