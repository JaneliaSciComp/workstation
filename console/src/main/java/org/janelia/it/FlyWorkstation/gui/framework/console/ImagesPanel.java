package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.Scrollable;

import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * Self-adjusting grid of images which may be resized together.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImagesPanel extends JPanel implements Scrollable {

    private static final int MIN_THUMBNAIL_SIZE = 64;
    public static final int MAX_THUMBNAIL_SIZE = 300;

    private final HashMap<String, AnnotatedImageButton> buttons = new HashMap<String, AnnotatedImageButton>();

    private IconDemoPanel iconDemoPanel;
    private ButtonGroup buttonGroup;
    private int imageSize = MAX_THUMBNAIL_SIZE;

    public ImagesPanel(IconDemoPanel iconDemoPanel) {
        setLayout(new GridLayout(0, 2));
        setOpaque(false);
        this.iconDemoPanel = iconDemoPanel;
    }

    /**
     * Create the image buttons, but leave the images unloaded for now.
     */
    public void setEntities(List<Entity> entities) {

    	// Cancel all loading images
        for (AnnotatedImageButton button : buttons.values()) {
        	SimpleWorker worker = button.getLoadWorker();
            if (worker != null && !worker.isDone()) {
            	System.out.println("Cancel previous load");
                worker.cancel(true);
            }
        }

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
            File file = new File(PathTranslator.convertImagePath(filepath));

            final AnnotatedImageButton button = new AnnotatedImageButton(entity.getName(), file.getAbsolutePath(), i, entity);

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
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations) {
        for (AnnotatedImageButton button : buttons.values()) {
        	loadAnnotations(annotations, button.getEntity());
        }
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations, Entity entity) {
    	AnnotatedImageButton button = getButtonByEntityId(entity.getId());
    	List<OntologyAnnotation> entityAnnotations = annotations.getFilteredAnnotationMap().get(entity.getId());
        button.getTagPanel().setTags(entityAnnotations);
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
        if (imageSize < MIN_THUMBNAIL_SIZE || imageSize > MAX_THUMBNAIL_SIZE) {
            return;
        }
        this.imageSize = imageSize;
        for (AnnotatedImageButton button : buttons.values()) {
        	try {
                button.rescaleImage(imageSize);
	    	}
	    	catch (Exception e) {
	    		e.printStackTrace();
	    	}
        }
		revalidate();
		repaint();
    }

    public int getImageSize() {
		return imageSize;
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
        return getButtonByEntityId(iconDemoPanel.getCurrentEntity().getId());
    }

	/**
     * Returns the button with the given entity.
     *
     * @return
     */
    public AnnotatedImageButton getButtonByEntityId(long entityId) {
        return buttons.get(entityId+"");
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

}