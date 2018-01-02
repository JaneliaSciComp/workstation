package org.janelia.it.workstation.browser.gui.lasso;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
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
    private JButton resetButton;
    private JButton maskButton;
    private BufferedImage mask;
    private Consumer<BufferedImage> onMask;
    
    public ImageMaskingPanel() throws IOException {
        
        setLayout(new BorderLayout());

        resetButton = new JButton("Reset");
        resetButton.setFocusable(false);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setImage(image);
            }
        });

        maskButton = new JButton("Mask");
        maskButton.setFocusable(false);
        maskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onMask.accept(createMask());
            }
        });
        
        addUI();
    }

    public void setImage(BufferedImage image) {

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
        buttonPanel.add(resetButton);
        buttonPanel.add(maskButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setOnMask(Consumer<BufferedImage> onMask) {
        this.onMask = onMask;
    }
    
    private BufferedImage createMask() {

        prepareProcessor(imagePlus.getImageProcessor(), imagePlus);
        
        // This is usually done by the PlugInFilterRunner. It must be run for unmasking to work:
        imagePlus.getImageProcessor().snapshot();
        
        // Run the plugin
        Filler fill = new Filler();
        fill.setup("outside", imagePlus);
        fill.run(imagePlus.getImageProcessor());

        this.mask = imagePlus.getImageProcessor().getBufferedImage();
        return mask;
    }
    
    public BufferedImage getMask() {
        return this.mask;
    }
    
    // Code from PlugInFilterRunner
    private void prepareProcessor(ImageProcessor ip, ImagePlus imp) {
        ImageProcessor mask = imp.getMask();
        Roi roi = imp.getRoi();
        if (roi!=null && roi.isArea())
            ip.setRoi(roi);
        else
            ip.setRoi((Roi)null);
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
        maskingPanel.setOnMask((BufferedImage mask) -> {
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