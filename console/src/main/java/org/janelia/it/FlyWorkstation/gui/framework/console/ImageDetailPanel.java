package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A panel which displays a single image entity with information about it.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageDetailPanel extends JPanel {

    private final IconDemoPanel iconDemoPanel;
    private JButton indexButton;
    private JButton prevButton;
    private JButton nextButton;
    
    private JLabel zoomLabel;
    private final JPanel imageViewer;
    private final JLabel imageCaption;
    private final JScrollPane scrollPane;
    private final JPanel imagePanel;
    private JComponent imageComponent;
    private final JPanel southernPanel;
    private final AnnotationTagCloudPanel tagPanel;

    private double currImageSizePercent = 1.0;
    
    private SimpleWorker dataWorker;
    private Entity entity;


    public ImageDetailPanel(final IconDemoPanel iconDemoPanel) {

        this.iconDemoPanel = iconDemoPanel;

        setLayout(new BorderLayout());

        add(createToolbar(), BorderLayout.PAGE_START);

        imageViewer = new JPanel();

        imageViewer.setLayout(new BorderLayout());

        imageCaption = new JLabel();
        imageCaption.setHorizontalAlignment(SwingConstants.CENTER);
        imageViewer.add(imageCaption, BorderLayout.NORTH);
        
        imagePanel = new JPanel(new BorderLayout());
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagePanel);

        imageViewer.add(scrollPane, BorderLayout.CENTER);

        this.southernPanel = new JPanel(new BorderLayout());

        final JScrollPane southernScrollPane = new JScrollPane();
        southernScrollPane.setViewportView(southernPanel);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, imageViewer, southernScrollPane);
        splitPane.setResizeWeight(0.85);
        add(splitPane, BorderLayout.CENTER);

        this.tagPanel = new AnnotationTagCloudPanel();

        // Remove the scrollpane's listeners so that mouse wheel events get propagated up
        for (MouseWheelListener l : scrollPane.getMouseWheelListeners()) {
            scrollPane.removeMouseWheelListener(l);
        }

        scrollPane.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                Utils.setOpenedHandCursor(scrollPane);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Utils.setClosedHandCursor(scrollPane);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Utils.setDefaultCursor(scrollPane);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Utils.setOpenedHandCursor(scrollPane);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                iconDemoPanel.requestFocusInWindow();
            }
        });

        southernPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                iconDemoPanel.requestFocusInWindow();
            }
        });

        scrollPane.addMouseMotionListener(new MouseMotionListener() {

            private Point lastPoint;
            private double sensitivity = 1.0;

            @Override
            public void mouseMoved(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {

                int x = e.getX() - lastPoint.x;
                int y = e.getY() - lastPoint.y;
                x *= -1 * sensitivity;
                y *= -1 * sensitivity;

                Point vp = scrollPane.getViewport().getViewPosition();
                vp.translate(x, y);
                if (vp.x < 0) vp.x = 0;
                if (vp.y < 0) vp.y = 0;
                scrollPane.getViewport().setViewPosition(vp);
                
                lastPoint = e.getPoint();

                scrollPane.revalidate();
                scrollPane.repaint();
            }
        });

        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() > 0) {
                	focusEntity(iconDemoPanel.getNextEntity());
                }
                else {
                	focusEntity(iconDemoPanel.getPreviousEntity());
                }
            }
        });
    }

    private void focusEntity(Entity entity) {
        if (entity != null) {
    		AnnotatedImageButton button = iconDemoPanel.getImagesPanel().getButtonByEntityId(entity.getId());
            if (button != null) {
            	button.requestFocus();
            }
        }
    }
    
	private JToolBar createToolbar() {

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(true);
        toolBar.setRollover(true);

        indexButton = new JButton("Back to index");
        indexButton.setToolTipText("Return to the index of images.");
        indexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                iconDemoPanel.reloadAnnotations();
                iconDemoPanel.showAllEntities();
            }
        });
        toolBar.add(indexButton);

        toolBar.addSeparator();

        prevButton = new JButton("Previous");
        prevButton.setToolTipText("Go to the previous image.");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	focusEntity(iconDemoPanel.getPreviousEntity());
            }
        });
        toolBar.add(prevButton);

        nextButton = new JButton("Next");
        nextButton.setToolTipText("Go to the next image.");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	focusEntity(iconDemoPanel.getNextEntity());
            }
        });
        toolBar.add(nextButton);

        toolBar.addSeparator();

        JSlider slider = new JSlider(100, 400, 100);
        slider.setFocusable(false);
        slider.setToolTipText("Image size percentage");
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                double imageSizePercent = (double) source.getValue() / (double) 100;
                currImageSizePercent = imageSizePercent;
                rescaleImage(imageSizePercent);
                zoomLabel.setText((int) source.getValue() + "%");
            }
        });
        toolBar.add(slider);

        zoomLabel = new JLabel();
        zoomLabel.setText("100%");
        toolBar.add(zoomLabel);

        return toolBar;
    }

    public AnnotationTagCloudPanel getTagPanel() {
        return tagPanel;
    }

    public void load(Entity entity) {

    	if (imageComponent!=null) {
    		if (imageComponent instanceof DynamicImagePanel) {
    			((DynamicImagePanel)imageComponent).cancelLoad();
    		}
    		imagePanel.remove(imageComponent);
    	}

        this.entity = entity;
        
        String imageFilename = Utils.getDefaultImageFilePath(entity);
        if (imageFilename!=null) {
            imageComponent = new DynamicImagePanel(PathTranslator.convertImagePath(imageFilename));
            final DynamicImagePanel dynamicImagePanel = ((DynamicImagePanel)imageComponent);
            dynamicImagePanel.setViewable(true, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
			        if (iconDemoPanel.isInverted()) {
			        	dynamicImagePanel.setInvertedColors(true);
			        }
			        else {
			        	dynamicImagePanel.rescaleImage(currImageSizePercent);
			        }
					return null;
				}
			});
        }
        else {
        	imageComponent = new JLabel("No 2D image to display for "+entity.getName());
        	((JLabel)imageComponent).setHorizontalAlignment(SwingConstants.CENTER);
        }
        
        imagePanel.add(imageComponent, BorderLayout.CENTER);

        if (dataWorker != null && !dataWorker.isDone()) {
            dataWorker.cancel(true);
        }

        imageCaption.setText(entity.getName());
        southernPanel.removeAll();
        southernPanel.add(new JLabel(Icons.getLoadingIcon()));
        
        revalidate();
        repaint();
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations) {
    	if (entity == null) return;
    	List<OntologyAnnotation> tags = annotations.getFilteredAnnotationMap().get(entity.getId());
    	tagPanel.setTags(tags);
        southernPanel.removeAll();
        southernPanel.add(tagPanel, BorderLayout.CENTER);
    }

    public void rescaleImage(double scale) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).rescaleImage(scale);
		}
    }

    public void setInvertedColors(boolean inverted) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).setInvertedColors(inverted);
		}
    }
    
    public JButton getIndexButton() {
		return indexButton;
	}

	public JButton getPrevButton() {
		return prevButton;
	}

	public JButton getNextButton() {
		return nextButton;
	}
}
