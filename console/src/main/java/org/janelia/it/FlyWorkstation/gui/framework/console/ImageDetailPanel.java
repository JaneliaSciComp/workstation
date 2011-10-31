package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.List;

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

    private JLabel zoomLabel;
    private final JPanel imageViewer;
    private final JLabel imageCaption;
    private final JLabel imageLabel;
    private final JPanel southernPanel;
    private final AnnotationTagCloudPanel tagPanel;

    private SimpleWorker imageWorker;
    private SimpleWorker dataWorker;
    private Entity entity;

    private BufferedImage maxSizeImage;
    private BufferedImage invertedMaxSizeImage;

    private boolean inverted;
    private double scale = 1.0d;

    public ImageDetailPanel(final IconDemoPanel iconDemoPanel) {

        this.iconDemoPanel = iconDemoPanel;

        setLayout(new BorderLayout());

        add(createToolbar(), BorderLayout.PAGE_START);

        imageViewer = new JPanel();

        imageViewer.setLayout(new BorderLayout());

        imageCaption = new JLabel();
        imageCaption.setHorizontalAlignment(SwingConstants.CENTER);
        imageViewer.add(imageCaption, BorderLayout.NORTH);

        this.imageLabel = new JLabel((ImageIcon) Icons.getLoadingIcon());
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagePanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

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
                    iconDemoPanel.nextEntity();
                }
                else {
                    iconDemoPanel.previousEntity();
                }
            }
        });
    }

    private JToolBar createToolbar() {

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(true);
        toolBar.setRollover(true);

        final JButton indexButton = new JButton("Back to index");
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

        final JButton prevButton = new JButton("Previous");
        prevButton.setToolTipText("Go to the previous image.");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                iconDemoPanel.previousEntity();
            }
        });
        toolBar.add(prevButton);

        final JButton nextButton = new JButton("Next");
        nextButton.setToolTipText("Go to the next image.");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                iconDemoPanel.nextEntity();
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

        if (imageWorker != null && !imageWorker.isDone()) {
            imageWorker.cancel(true);
        }

        if (dataWorker != null && !dataWorker.isDone()) {
            dataWorker.cancel(true);
        }

        this.entity = entity;

        imageCaption.setText(entity.getName());
        southernPanel.removeAll();
        southernPanel.add(new JLabel(Icons.getLoadingIcon()));

        imageLabel.setIcon(Icons.getLoadingIcon());

        imageWorker = new LoadImageWorker();
        imageWorker.execute();
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
        if (maxSizeImage == null) return;
        BufferedImage image = Utils.getScaledImageIcon(inverted ? invertedMaxSizeImage : maxSizeImage, scale);
        imageLabel.setIcon(new ImageIcon(image));
        this.scale = scale;
        
		revalidate();
		repaint();
    }

    public void setInvertedColors(boolean inverted) {

        this.inverted = inverted;
        if (inverted == true && maxSizeImage != null) {
            invertedMaxSizeImage = Utils.invertImage(maxSizeImage);
        }
        else {
            // Free up memory when we don't need inverted images
            invertedMaxSizeImage = null;
        }

        rescaleImage(scale);
    }

    public synchronized BufferedImage getMaxSizeImage() {
		return maxSizeImage;
	}

	public synchronized void setMaxSizeImage(BufferedImage maxSizeImage) {
		this.maxSizeImage = maxSizeImage;
	}

	/**
     * SwingWorker class that loads the image.  This thread supports being canceled.
     */
    private class LoadImageWorker extends SimpleWorker {

        @Override
        protected void doStuff() throws Exception {
            String imageFilename = Utils.getDefaultImageFilePath(entity);
            setMaxSizeImage(Utils.readImage(PathTranslator.convertImagePath(imageFilename)));
        }

        @Override
        protected void hadSuccess() {
            if (isCancelled()) return;
            setInvertedColors(iconDemoPanel.isInverted());
        }

        @Override
        protected void hadError(Throwable error) {

            imageLabel.setForeground(Color.red);
            imageLabel.setIcon(Icons.getMissingIcon());
            imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
            imageLabel.setHorizontalTextPosition(JLabel.CENTER);

            if (error instanceof FileNotFoundException) {
                imageLabel.setText("File not found");
            }
            else {
                error.printStackTrace();
                imageLabel.setText("Image could not be loaded");
            }

            // TODO: set read-only mode
        }
    }
}
