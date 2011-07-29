package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.framework.console.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.framework.console.IconDemoPanel;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * A toolbar which sits on top of the main annotation window and provides functions for dealing with the images
 * presented there. For example, sorting the images, resizing them, and so forth.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationToolbar extends JPanel {
	
    private final IconDemoPanel iconDemoPanel;
	private final JSlider slider;
	
    public JSlider getSlider() {
		return slider;
	}

	public AnnotationToolbar(final IconDemoPanel iconDemoPanel) {
        super(new BorderLayout());
        
        this.iconDemoPanel = iconDemoPanel;
        
        JToolBar toolBar = new JToolBar("Still draggable");
        toolBar.setFloatable(true);
        toolBar.setRollover(true);

        toolBar.add(new JLabel("Show:"));
        
        final JToggleButton showTitlesButton = new JToggleButton("Titles");
        showTitlesButton.setSelected(true);
        showTitlesButton.setToolTipText("Show the image title above each image.");
        showTitlesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(AnnotatedImageButton button : iconDemoPanel.getImagesPanel().getButtons().values()) {
					button.setTitleVisible(showTitlesButton.isSelected());
				}
				iconDemoPanel.getImagesPanel().recalculateGrid();
			}
		});
        toolBar.add(showTitlesButton);

        final JToggleButton showTagsButton = new JToggleButton("Tags");
        showTagsButton.setSelected(true);
        showTagsButton.setToolTipText("Show tags below each images");
        showTagsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for(AnnotatedImageButton button : iconDemoPanel.getImagesPanel().getButtons().values()) {
					button.setTagsVisible(showTagsButton.isSelected());
				}
				iconDemoPanel.getImagesPanel().recalculateGrid();
			}
		});
        toolBar.add(showTagsButton);
        
        toolBar.addSeparator();

        final JToggleButton invertButton = new JToggleButton("Invert colors");
        invertButton.setToolTipText("Invert the color space on all images");
        invertButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Utils.setWaitingCursor(iconDemoPanel);
				try {
					for(AnnotatedImageButton button : iconDemoPanel.getImagesPanel().getButtons().values()) {
						button.setInvertedColors(invertButton.isSelected());
					}
					iconDemoPanel.getImagesPanel().repaint();
					}
				finally {
					Utils.setDefaultCursor(iconDemoPanel);	
				}
			}
		});
        toolBar.add(invertButton);
        
        final JToggleButton hideCompletedButton = new JToggleButton("Hide completed");
        hideCompletedButton.setToolTipText("Hide images which have been annotated completely according to the annotation session's ruleset.");
        hideCompletedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
			}
		});
        toolBar.add(hideCompletedButton);
        
        toolBar.addSeparator();
        
        slider = new JSlider(1,100,100);
        slider.setToolTipText("Image size percentage");
        toolBar.add(slider);
        
        //Lay out the main panel.
        add(toolBar, BorderLayout.PAGE_START);
    }

}
