/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationToolbar;
import org.janelia.it.FlyWorkstation.gui.util.WrapLayout;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.SeekableStream;


/**
 * This panel shows titled images in a grid with optional textual annotation tags beneath each one.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel  {

	private static final int MAX_THUMBNAIL_WIDTH = 400;
	
    private MissingIcon placeholderIcon = new MissingIcon();
    private List<AnnotatedImageButton> annotImages = new ArrayList<AnnotatedImageButton>();

    /**
     * List of all the image files to load.
     */
    private SplashPanel splashPanel;
    private AnnotationToolbar toolbar;
    private ImagesPanel imagesPanel;
    private JScrollPane scrollPane;
    private Integer currIndex;
    private ButtonGroup buttonGroup;

    private double imageSizePercent = 1.0d;
    private double maxThumbnailWidth = 500;
    
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
        toolbar = new AnnotationToolbar();
        imagesPanel = new ImagesPanel();
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagesPanel);
        
        toolbar.getSlider().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
	    		int sizePct = (int)source.getValue();
	    		imageSizePercent = (double)sizePct/(double)100;
	    		maxThumbnailWidth = 0;
	            for (Component component : imagesPanel.getComponents()) {
	            	if (component instanceof AnnotatedImageButton) {
	            		AnnotatedImageButton button = (AnnotatedImageButton)component;
	            		button.rescaleImage(imageSizePercent);
	            		int w = button.getImage().getIconWidth();
	            		if (w>maxThumbnailWidth) maxThumbnailWidth = w;
	            	}
	            }
			}
        });
        
    }

    public void reloadData(String pathToData) {
        try {

            remove(splashPanel);

            // Clear out the old images
            annotImages.clear();
            buttonGroup = new ButtonGroup();
            imagesPanel.removeAllButtons();

            if (null == pathToData) {
                add(splashPanel);
                return;
            }

            add(toolbar, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            File tmpFile = new File(pathToData);
            if (tmpFile.isDirectory()) {
                File[] childImageFiles = tmpFile.listFiles(new FilenameFilter(){
                    public boolean accept(File file, String s) {
                        // TODO: Need a whole mechanism to categorize the files and editors used for them.
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
     * 
     * @param path
     * @return
     * @throws MalformedURLException
     */
    private BufferedImage readImage(String path) throws Exception {
        SeekableStream s = new FileSeekableStream(new File(path));
        BufferedImage image = ImageIO.read(s);
        s.close();
        return image;
        
    }
	    
	/**
	 * Self-adjusting panel of images
	 */
    private class ImagesPanel extends JPanel implements ComponentListener, Scrollable {

    	private List<AnnotatedImageButton> buttons = new ArrayList<AnnotatedImageButton>();
    	
        public ImagesPanel() {
            setLayout(new GridLayout(0, 2));
            setOpaque(false);
            addComponentListener(this);
        }
        
        public void addButton(AnnotatedImageButton button) {
        	buttons.add(button);
        	add(button);
        }
        
        public void removeAllButtons() {
            for (Component component : imagesPanel.getComponents()) {
            	if (component instanceof AnnotatedImageButton) {
            		remove(component);
            	}
            }
            buttons.clear();
        }

        @Override
        public void componentResized(ComponentEvent e) {
        	int numCols = (int)Math.floor((double)getParent().getSize().width / (maxThumbnailWidth+20));
            ((GridLayout)getLayout()).setColumns(numCols);
        }

        @Override
        public void componentMoved(ComponentEvent e) {}

        @Override
        public void componentShown(ComponentEvent e) {}

        @Override
        public void componentHidden(ComponentEvent e) {}

        @Override
        public Dimension getPreferredScrollableViewportSize() {
        	Dimension d = getPreferredSize();
        	System.out.println("getPreferredScrollableViewportSize: "+d);
            return d;
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
            return false;
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

    	private BufferedImage maxSizeImage;
        private String imageFilename;
        private String title;
        private int index;
        private JPanel tagPanel;
        private List<String> tags = new ArrayList<String>();
		private JLabel imageLabel;

        public AnnotatedImageButton(String title, String imageFilename, final int index) {
            this.title = title;
            this.imageFilename = imageFilename;
            this.index = index;
        }

        public void rescaleImage(double scale) {
        	imageLabel.setIcon(new ImageIcon(Utils.getScaledImageIcon(maxSizeImage, scale)));
        }
        
        public Icon getImage() {
        	return imageLabel.getIcon();
        }
        
        public void init() throws Throwable {

        	maxSizeImage = readImage(imageFilename);
        	
        	// TODO: what if the filename is not found?

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

            imageLabel = new JLabel();
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            rescaleImage(imageSizePercent);

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
     * We use Void as the first SwingWorker param as we do not need to return
     * anything from doInBackground().
     */
    private class LoadImageWorker extends SwingWorker<Void, AnnotatedImageButton> {
        /**
         * Creates full size and thumbnail versions of the target image files.
         */
        @Override
        protected Void doInBackground() throws Exception {
        	try {
                System.out.println("loading "+annotImages.size()+" images......");

                for (AnnotatedImageButton annotImage : annotImages) {
                    annotImage.init();
                    publish(annotImage);
                }
        	}
        	catch (Throwable e) {
        		e.printStackTrace();
        	}
            // unfortunately we must return something, and only null is valid to
            // return when the return type is void.
            return null;
        }

        @Override
        protected void process(java.util.List<AnnotatedImageButton> buttons) {
            for (AnnotatedImageButton button : buttons) {
                imagesPanel.addButton(button);

        		int w = button.getImage().getIconWidth();
        		if (w>maxThumbnailWidth) maxThumbnailWidth = w;
        		
                int numCols = (int)Math.floor((double)getSize().width / maxThumbnailWidth);
                ((GridLayout)imagesPanel.getLayout()).setColumns(numCols);
                
                validate();
                SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
            }
        }
    }
}
