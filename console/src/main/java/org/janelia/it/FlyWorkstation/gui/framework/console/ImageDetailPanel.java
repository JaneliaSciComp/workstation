package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * A panel which displays a single image entity with information about it. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageDetailPanel extends JPanel {

	private IconDemoPanel iconDemoPanel;
	
	private JLabel imageCaption;
	private JLabel imageLabel;
	
	private SimpleWorker worker;
	private Entity entity;
	
	private BufferedImage maxSizeImage;
	private BufferedImage invertedMaxSizeImage;
	
	private boolean inverted;
	private double scale = 1.0d;
	
	public ImageDetailPanel(IconDemoPanel iconDemoPanel) {

		setLayout(new BorderLayout());
		
		add(createToolbar(), BorderLayout.PAGE_START);
		
		JPanel imageViewer = new JPanel();
		add(imageViewer, BorderLayout.CENTER);
		
		imageViewer.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
		this.iconDemoPanel = iconDemoPanel;
        
        imageCaption = new JLabel();
        
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0,0,5,0);
        imageViewer.add(imageCaption,c);

		this.imageLabel = new JLabel((ImageIcon)Icons.loadingIcon);		
		imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		imageLabel.setVerticalAlignment(SwingConstants.CENTER);
		JPanel imagePanel = new JPanel(new BorderLayout());
		imagePanel.add(imageLabel, BorderLayout.CENTER);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(imagePanel);
		// TODO: implement drag scrolling and disable the scroll bars:
//		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0,0,5,0);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        imageViewer.add(scrollPane,c);
		
        
        // Remove the scrollpane's listeners so that mouse wheel events get propagated up
        for(MouseWheelListener l : scrollPane.getMouseWheelListeners()) {
        	scrollPane.removeMouseWheelListener(l);
        }
	}

	private JToolBar createToolbar() {
		
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(true);
        toolBar.setRollover(true);
        
        final JButton backButton = new JButton("Back to index");
        backButton.setToolTipText("Return to the index of images.");
        backButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				iconDemoPanel.showAllEntities();
			}
		});
        toolBar.add(backButton);
        
        toolBar.addSeparator();
        
        JSlider slider = new JSlider(100,400,100);
        slider.setToolTipText("Image size percentage");
        slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				double imageSizePercent = (double)source.getValue()/(double)100;
				rescaleImage(imageSizePercent);
			}
        });
        toolBar.add(slider);
        
        return toolBar;
    }

	
	public void load(Entity entity, List<Entity> annotations) {

    	if (worker != null && !worker.isDone()) {
    		worker.cancel(true);
    	}
    	
    	this.entity = entity;

        imageCaption.setText(entity.getName());
        
    	worker = new LoadImageWorker();
		worker.execute();
	}

    public void rescaleImage(double scale) {
    	if (maxSizeImage == null) return;
    	BufferedImage image = Utils.getScaledImageIcon(maxSizeImage, scale);
		imageLabel.setIcon(new ImageIcon(image));
    	this.scale = scale;
    }

	public void setInvertedColors(boolean inverted) {
		
		this.inverted = inverted;
		if (inverted == true) {
			invertedMaxSizeImage = Utils.invertImage(maxSizeImage);	
		}
		else {
			// Free up memory when we don't need inverted images
			invertedMaxSizeImage = null;
		}
		
    	rescaleImage(scale);
	}

    /**
     * SwingWorker class that loads the image.  This thread supports being canceled.
     */
    private class LoadImageWorker extends SimpleWorker {
        
		@Override
		protected void doStuff() throws Exception {
			String imageFilename = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		maxSizeImage = Utils.readImage(iconDemoPanel.convertImagePath(imageFilename));
		}

		@Override
		protected void hadSuccess() {
        	if (isCancelled()) return;
    		rescaleImage(scale);    		
		}

		@Override
		protected void hadError(Throwable error) {
			error.printStackTrace();
		}
    }
}
