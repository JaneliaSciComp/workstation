package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;

import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * Self-adjusting grid of images which may be resized together.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImagesPanel extends JPanel implements Scrollable {

    private static final int MIN_THUMBNAIL_SIZE = 64;
    private static final int MAX_THUMBNAIL_SIZE = 300;

    private final HashMap<String, AnnotatedImageButton> buttons = new HashMap<String, AnnotatedImageButton>();

    private IconDemoPanel iconDemoPanel;
    private ButtonGroup buttonGroup;
    private int imageSize = MAX_THUMBNAIL_SIZE;
    private List<SwingWorker> workers = new ArrayList<SwingWorker>();


    public ImagesPanel(IconDemoPanel iconDemoPanel) {
        setLayout(new GridLayout(0, 2));
        setOpaque(false);
        this.iconDemoPanel = iconDemoPanel;

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                recalculateGrid();
            }
        });
    }

    /**
     * Load in some new images asynchronously. In the meantime, show the filenames with a "loading" spinner for
     * each image to be loaded.
     */
    public void load(List<Entity> entities) {

        for (SwingWorker worker : workers) {
            if (worker != null && !worker.isDone()) {
            	System.out.println("Cancel previous load");
                worker.cancel(true);
            }
        }

        ((GridLayout) getLayout()).setColumns(10);

        buttons.clear();
        buttonGroup = new ButtonGroup();
        for (Component component : getComponents()) {
            if (component instanceof AnnotatedImageButton) {
                remove(component);
            }
        }

        for (int i = 0; i < entities.size(); i++) {
            final Entity entity = entities.get(i);
            String filepath = iconDemoPanel.getFilePath(entity);
            File file = new File(iconDemoPanel.convertImagePath(filepath));

            final AnnotatedImageButton button = new AnnotatedImageButton(file.getName(), file.getAbsolutePath(), i, entity);

            button.addKeyListener(iconDemoPanel.getKeyListener());

            button.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    iconDemoPanel.setCurrentEntity(entity);
                    button.setSelected(true);

                    // Scroll to the newly focused button
                    ImagesPanel.this.scrollRectToVisible(button.getBounds());
                    revalidate();
                }
            });

            buttons.put(entity.getId().toString(), button);
            buttonGroup.add(button);
            add(button);
        }

        for (AnnotatedImageButton button : buttons.values()) {
            SwingWorker worker = new LoadImageWorker(button);
            worker.execute();
            workers.add(worker);
        }
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations) {
        for (AnnotatedImageButton button : buttons.values()) {
        	List<OntologyAnnotation> entityAnnotations = annotations.getFilteredAnnotationMap().get(button.getEntity().getId());
            button.getTagPanel().setTags(entityAnnotations);
        }
    }

    /**
     * Scale all the images to the desired percent of their true size.
     *
     * @param imageSizePercent
     */
    public void rescaleImages(double imageSizePercent) {
        if (imageSizePercent < 0 || imageSizePercent > 1) {
            return;
        }
        double range = (double) (MAX_THUMBNAIL_SIZE - MIN_THUMBNAIL_SIZE);
        rescaleImages(MIN_THUMBNAIL_SIZE + (int) (range * imageSizePercent));
    }

    /**
     * Scale all the images to the given max size.
     *
     * @param imageSize
     */
    public void rescaleImages(int imageSize) {
        if (imageSize < MIN_THUMBNAIL_SIZE || imageSize > MAX_THUMBNAIL_SIZE || imageSize == this.imageSize) {
            return;
        }
        this.imageSize = imageSize;
        for (AnnotatedImageButton button : buttons.values()) {
            button.rescaleImage(imageSize);
        }
    }

    /**
     * Returns the currently selected image button in the panel.
     *
     * @return
     */
    public AnnotatedImageButton getSelectedButton() {
        if (iconDemoPanel.getCurrentEntity() == null) {
            return null;
        }
        return buttons.get(iconDemoPanel.getCurrentEntity().getId().toString());
    }

    public void setSelectedImage(Entity entity) {
        AnnotatedImageButton button = buttons.get(entity.getId().toString());
        if (button != null) {
	        button.setSelected(true);
	        button.requestFocusInWindow();
        }
    }

    /**
     * Set the number of columns in the grid layout based on the width of the parent component and the width of the
     * buttons.
     */
    public void recalculateGrid() {
        double maxButtonWidth = 0;
        for (AnnotatedImageButton button : buttons.values()) {
            int w = button.getPreferredSize().width;
            if (w > maxButtonWidth) maxButtonWidth = w;
        }

        // Should not be needed, but just in case, lets make sure we never divide by zero
        if (maxButtonWidth == 0) maxButtonWidth = 400;

        int numCols = (int) Math.floor((double) getParent().getSize().width / maxButtonWidth);
        if (numCols > 0) {
            ((GridLayout) getLayout()).setColumns(numCols);
        }

        revalidate();
        repaint();
    }

    public HashMap<String, AnnotatedImageButton> getButtons() {
        return buttons;
    }

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
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * SwingWorker class that loads the images and rescales them to the current imageSizePercent sizing.  This
     * thread supports being canceled.
     */
    private class LoadImageWorker extends SwingWorker<Void, AnnotatedImageButton> {

        private AnnotatedImageButton button;

        public LoadImageWorker(AnnotatedImageButton button) {
            this.button = button;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                if (isCancelled()) return null;
                button.loadImage(MAX_THUMBNAIL_SIZE);
                if (isCancelled()) return null;
                publish(button);
                if (isCancelled()) return null;
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void process(List<AnnotatedImageButton> buttons) {
            // If the scale has changed since the image began loading then we have to rescale it
            for (AnnotatedImageButton button : buttons) {
                if (iconDemoPanel.isInverted()) {
                    button.setInvertedColors(iconDemoPanel.isInverted());
                }
                else if (button.getDisplaySize() != imageSize) {
                    button.rescaleImage(imageSize);
                }
            }
            recalculateGrid();
        }

    }
}