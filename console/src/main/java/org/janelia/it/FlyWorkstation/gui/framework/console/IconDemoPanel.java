package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
public class IconDemoPanel extends JPanel {
    private MissingIcon placeholderIcon = new MissingIcon();
    /**
     * List of all the descriptions of the image files. These correspond one to
     * one with the image file names
     */
    private String[] imageCaptions;

    /**
     * List of all the image files to load.
     */
    private String[] imageFileNames;

    /**
     * Default constructor for the demo.
     */
    public IconDemoPanel() {
        setLayout(new GridLayout(0, 2));
        setSize(400, 300);

        // start the image loading SwingWorker in a background thread
        loadimages.execute();
    }

    /**
     * SwingWorker class that loads the images a background thread and calls publish
     * when a new one is ready to be displayed.
     *
     * We use Void as the first SwingWroker param as we do not need to return
     * anything from doInBackground().
     */
    private SwingWorker<Void, JToggleButton> loadimages = new SwingWorker<Void, JToggleButton>() {

        /**
         * Creates full size and thumbnail versions of the target image files.
         */
        @Override
        protected Void doInBackground() throws Exception {
            for (int i = 0; i < imageCaptions.length; i++) {
                ImageIcon icon = createImageIcon(imageFileNames[i], imageCaptions[i]);

                JToggleButton imageToggle;
                if(icon != null){
                    imageToggle = new JToggleButton(imageCaptions[i], icon);
                }
                else{
                    // the image failed to load for some reason
                    // so load a placeholder instead
                    imageToggle = new JToggleButton(imageCaptions[i], placeholderIcon);
                }
                publish(imageToggle);
            }
            // unfortunately we must return something, and only null is valid to
            // return when the return type is void.
            return null;
        }

        @Override
        protected void process(java.util.List<JToggleButton> imageToggles) {
            for (JToggleButton imageToggle : imageToggles) {
                add(imageToggle);
                SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
            }
        }

    };

    /**
     * Creates an ImageIcon if the path is valid.
     * @param path - resource path
     * @param description - description of the file
     * @return ImageIcon to use
     * @throws MalformedURLException - bad URL exception
     */
    protected ImageIcon createImageIcon(String path,
            String description) throws MalformedURLException {
        java.net.URL imgURL = new File(path).toURL();
        int tmpSize = getWidth()/8;
        if (imgURL != null) {
            return new ImageIcon(getScaledImage(new ImageIcon(imgURL).getImage(), tmpSize, tmpSize), description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public void setImageFileNames(String[] imageFileNames) {
        this.imageFileNames = imageFileNames;
    }

    public void setImageCaptions(String[] imageCaptions) {
        this.imageCaptions = imageCaptions;
    }

    /**
     * Resizes an image using a Graphics2D object backed by a BufferedImage.
     * @param srcImg - source image to scale
     * @param w - desired width
     * @param h - desired height
     * @return - the new resized image
     */
    private Image getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

    public void reloadData(String pathToData) {
        try {
            System.out.println("reload");
            if (null==pathToData) {
                // Do nothing
                return;
            }
            // Clear out the old images
            for (Component component : getComponents()) {
                if (component instanceof JToggleButton) {
                    remove(component);
                }
            }
            File tmpFile = new File(pathToData);
            if (tmpFile.isDirectory()) {
                File[] childImageFiles = tmpFile.listFiles(new FilenameFilter(){
                    public boolean accept(File file, String s) {
                        // todo Need a whole mechanism to categorize the files and editors used for them.
                        return s.endsWith(".tif");
                    }
                });
                ArrayList<String> captionList = new ArrayList<String>();
                ArrayList<String> fileList = new ArrayList<String>();
                for (File childImageFile : childImageFiles) {
                    captionList.add(childImageFile.getName());
                    fileList.add(childImageFile.getAbsolutePath());
                }
                setImageCaptions(captionList.toArray(new String[]{}));
                setImageFileNames(fileList.toArray(new String[]{}));
            }
            else if (tmpFile.isFile()) {
                setImageCaptions(new String[] {tmpFile.getName()});
                setImageFileNames(new String[]{tmpFile.getAbsolutePath()});
            }
            loadimages = new SwingWorker<Void, JToggleButton>() {

                /**
                 * Creates full size and thumbnail versions of the target image files.
                 */
                @Override
                protected Void doInBackground() throws Exception {
                    for (int i = 0; i < imageCaptions.length; i++) {
                        ImageIcon icon = createImageIcon(imageFileNames[i], imageCaptions[i]);

                        JToggleButton imageToggle;
                        if(icon != null){
                            imageToggle = new JToggleButton(imageCaptions[i], icon);
                        }
                        else{
                            // the image failed to load for some reason
                            // so load a placeholder instead
                            imageToggle = new JToggleButton(imageCaptions[i], placeholderIcon);
                        }
                        publish(imageToggle);
                    }
                    // unfortunately we must return something, and only null is valid to
                    // return when the return type is void.
                    return null;
                }

                @Override
                protected void process(java.util.List<JToggleButton> imageToggles) {
                    for (JToggleButton imageToggle : imageToggles) {
                        add(imageToggle);
                        SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
                    }
                }

            };
            loadimages.execute();
            // else do nothing
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
