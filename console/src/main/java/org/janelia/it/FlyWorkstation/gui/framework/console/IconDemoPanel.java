/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.util.WrapLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This panel shows titled images in a grid with optional textual annotation tags beneath each one.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel  {

    private MissingIcon placeholderIcon = new MissingIcon();
    private List<AnnotatedImageButton> annotImages = new ArrayList<AnnotatedImageButton>();

    /**
     * List of all the image files to load.
     */
    private SplashPanel splashPanel;
    private JPanel imagesPanel;
    private JScrollPane scrollPane;
    private Integer currIndex;
    private ButtonGroup buttonGroup;

    // Listen for key strokes and execute the appropriate key bindings
    private KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (KeymapUtil.isModifier(e)) return;
                KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
                ConsoleApp.getKeyBindings().executeBinding(shortcut);
            }
        }
    };

    /**
     * Default constructor for the demo.
     */
    public IconDemoPanel() {

        setBackground(Color.white);
        setLayout(new BorderLayout(0,0));

        splashPanel = new SplashPanel();
        imagesPanel = new ImagesPanel();
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagesPanel);
    }

    /**
     * Creates an ImageIcon if the path is valid.
     * @param path - resource path
     * @return ImageIcon to use
     * @throws MalformedURLException - bad URL exception
     */
    protected ImageIcon createImageIcon(String path) throws MalformedURLException {
        java.net.URL imgURL = new File(path).toURL();
        int tmpSize = getWidth()/8;
        if (imgURL != null) {
            return new ImageIcon(getScaledImage(new ImageIcon(imgURL).getImage(), tmpSize, tmpSize));
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
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

            remove(splashPanel);

            // Clear out the old images
            annotImages.clear();
            buttonGroup = new ButtonGroup();
            for (Component component : imagesPanel.getComponents()) {
                imagesPanel.remove(component);
            }

            if (null == pathToData) {
                add(splashPanel);
                return;
            }

            add(scrollPane, BorderLayout.CENTER);

            File tmpFile = new File(pathToData);
            if (tmpFile.isDirectory()) {
                File[] childImageFiles = tmpFile.listFiles(new FilenameFilter(){
                    public boolean accept(File file, String s) {
                        // todo Need a whole mechanism to categorize the files and editors used for them.
                        return s.endsWith(".tif");
                    }
                });

                int i = 0;
                for (File file : childImageFiles) {
                    AnnotatedImageButton button = new AnnotatedImageButton(file.getName(), file.getAbsolutePath(), i++);
                    annotImages.add(button);
                    buttonGroup.add(button);

                }
            }
            else if (tmpFile.isFile()) {
                AnnotatedImageButton button = new AnnotatedImageButton(tmpFile.getName(), tmpFile.getAbsolutePath(), 0);
                annotImages.add(button);
                buttonGroup.add(button);
            }

            new LoadImageWorker().execute();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add or remove the given tag from the currently selected image button.
     * @param tag
     */
    public void addOrRemoveTag(String tag) {
        if (currIndex == null || currIndex >= annotImages.size()) return;
        AnnotatedImageButton currImage = annotImages.get(currIndex);
        addOrRemoveTag(tag, currImage);
    }

    /**
     * Add or remove the given tag from the currently selected image button.
     * @param tag
     */
    public void addOrRemoveTag(String tag, AnnotatedImageButton button) {

        List<String> tags = button.getTags();

        if (tags.contains(tag)) {
            tags.remove(tag);
        }
        else {
            tags.add(tag);
        }

        button.refreshTags();
        validate();
        SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
    }

    /**
     * This subclass of JPanel disable horizontal scrolling when the panel is inserted into a JScrollPane.
     */
    private class ImagesPanel extends JPanel implements Scrollable, ComponentListener {

        ImagesPanel() {
            setLayout(new GridLayout(0, 2));
            setOpaque(false);
            addComponentListener(this);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (getSize().width > 1400) {
                ((GridLayout)getLayout()).setColumns(3);
            }
            else {
                ((GridLayout)getLayout()).setColumns(2);
            }
        }

        @Override
        public void componentMoved(ComponentEvent e) {}

        @Override
        public void componentShown(ComponentEvent e) {}

        @Override
        public void componentHidden(ComponentEvent e) {}

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * A slot in the ImagesPanel for an image with a title and annotations.
     */
    private class AnnotatedImageButton extends JToggleButton {

        private String imageFilename;
        private String title;
        private int index;
        private JPanel tagPanel;
        private List<String> tags = new ArrayList<String>();

        AnnotatedImageButton(String title, String imageFilename, final int index) {
            this.title = title;
            this.imageFilename = imageFilename;
            this.index = index;
        }

        public void init() throws MalformedURLException {

            Icon icon = createImageIcon(imageFilename);
            if (icon == null) icon = placeholderIcon;

            GridBagConstraints c = new GridBagConstraints();
            JPanel imagePanel = new JPanel(new GridBagLayout());
            imagePanel.setOpaque(false);
            add(imagePanel);

            JTextPane imageCaption = new JTextPane();
            imageCaption.setFocusable(false);
            imageCaption.setText(title);
            imageCaption.setFont(new Font("Sans Serif", Font.PLAIN, 14));
            imageCaption.setAlignmentX(Component.CENTER_ALIGNMENT);
            imageCaption.setEditable(false);
            imageCaption.setOpaque(false);
            StyledDocument doc = imageCaption.getStyledDocument();
            SimpleAttributeSet center = new SimpleAttributeSet();
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
            doc.setParagraphAttributes(0, doc.getLength(), center, false);

            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0,0,5,0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0.5;
            imagePanel.add(imageCaption,c);

            JLabel imageLabel = new JLabel(icon);
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            c.gridx = 0;
            c.gridy = 1;
            c.insets = new Insets(0,0,5,0);
            imagePanel.add(imageLabel,c);

            tagPanel = new JPanel(new WrapLayout());
            tagPanel.setOpaque(false);
            refreshTags();

            c.gridx = 0;
            c.gridy = 2;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.5;
            imagePanel.add(tagPanel, c);

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    currIndex = index;
                    setSelected(true);
                    
                    // Scroll to the newly focused button
                    imagesPanel.scrollRectToVisible(getBounds());
                    SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
                }
            });

            addKeyListener(keyListener);
        }

        public List<String> getTags() {
            return tags;
        }

        public void refreshTags() {

            tagPanel.removeAll();

            Border paddingBorder = BorderFactory.createEmptyBorder(5,5,5,5);
            Border border = BorderFactory.createLineBorder(Color.black, 1);

            for(final String tag : tags) {
                JLabel tagLabel = new JLabel(tag);
                tagLabel.setBorder(BorderFactory.createCompoundBorder(border, paddingBorder));
                tagLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
                tagLabel.setOpaque(true);
                tagLabel.setBackground(Color.white);
                tagLabel.setForeground(Color.black);
                tagPanel.add(tagLabel);

                tagLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount()==2) {
                            addOrRemoveTag(tag, AnnotatedImageButton.this);
                        }
                    }
                });

            }
        }
    }

    /**
     * SwingWorker class that loads the images a background thread and calls publish
     * when a new one is ready to be displayed.
     *
     * We use Void as the first SwingWroker param as we do not need to return
     * anything from doInBackground().
     */
    private class LoadImageWorker extends SwingWorker<Void, AnnotatedImageButton> {
        /**
         * Creates full size and thumbnail versions of the target image files.
         */
        @Override
        protected Void doInBackground() throws Exception {

            System.out.println("loading "+annotImages.size()+" images......");

            for (AnnotatedImageButton annotImage : annotImages) {
                annotImage.init();
                publish(annotImage);
            }
            // unfortunately we must return something, and only null is valid to
            // return when the return type is void.
            return null;
        }

        @Override
        protected void process(java.util.List<AnnotatedImageButton> buttons) {
            for (AnnotatedImageButton button : buttons) {
                imagesPanel.add(button);
                validate();
                SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
            }
        }
    }
}
