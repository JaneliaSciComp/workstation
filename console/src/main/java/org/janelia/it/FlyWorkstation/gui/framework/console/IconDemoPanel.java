/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationToolbar;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This panel shows titled images in a grid with optional textual annotation tags beneath each one.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel {

    private SplashPanel splashPanel;
    private AnnotationToolbar toolbar;
    private ImagesPanel imagesPanel;
    private JScrollPane scrollPane;

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
	    		imagesPanel.rescaleImages((double)source.getValue()/(double)100);
				imagesPanel.recalculateGrid();
			}
        });

        addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				imagesPanel.recalculateGrid();
			}
        	
        });
    }

    /**
     * Load the given image files and display them in a grid. If files is null then redisplay the splash image.
     * @param files
     */
    public void loadImages(List<File> files) {
    	
        try {
            remove(splashPanel);
           
            if (files == null) {
                add(splashPanel);
                return;
            }

            add(toolbar, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            List<String> labels = new ArrayList<String>();
            List<String> filenames = new ArrayList<String>();

			for (File file : files) {
				labels.add(file.getName());
				filenames.add(file.getAbsolutePath());
			}

            imagesPanel.load(labels, filenames);
            SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the given image entities and display them in a grid. If files is null then redisplay the splash image.
     * @param files
     */
    public void loadImageEntities(List<Entity> images) {
    	
        try {
            remove(splashPanel);
           
            if (images == null) {
                add(splashPanel);
                return;
            }

            add(toolbar, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            List<String> labels = new ArrayList<String>();
            List<String> filenames = new ArrayList<String>();

			for (Entity entity : images) {
				
				if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D)) {
					// Ignore things we can't display
					continue;
				}
				
				String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
				File file = new File(convertPath(filepath));
				labels.add(file.getName());
				filenames.add(file.getAbsolutePath());
			}

            imagesPanel.load(labels, filenames);
            SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // TODO: move this into a configuration file
    private static final String JACS_DATA_PATH = "/Volumes/jacsData";
    public String convertPath(String filepath) {
    	return filepath.replace("/groups/scicomp/jacsData", JACS_DATA_PATH);
    }
    
    /**
     * Return the currently selected image.
     * @return
     */
    public AnnotatedImageButton getSelectedImage() {
    	// TODO: this should probably return an Entity, not the Component
    	return imagesPanel.getSelectedImage();
    }

    /**
     * Add or remove the given tag from the currently selected image.
     * @param tag
     */
    public boolean addOrRemoveTag(String tag) {
        return imagesPanel.addOrRemoveTag(tag);
    }

}
