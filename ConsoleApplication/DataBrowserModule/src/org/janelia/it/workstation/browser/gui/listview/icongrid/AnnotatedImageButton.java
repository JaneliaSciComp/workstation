package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.gui.support.AnnotationTablePanel;
import org.janelia.it.workstation.browser.gui.support.AnnotationView;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.SelectablePanel;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SelectablePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an
 * ImagesPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotatedImageButton<T,S> extends SelectablePanel {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedImageButton.class);
    
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JPanel mainPanel;
    private final JPanel buttonPanel;
//    private final JPanel annotationPanel;
    private AnnotationView annotationView;
    private boolean wantViewable = false;
    private double aspectRatio;
    private final JCheckBox editMode;
    private List<Annotation> annotations;
    
    protected final T imageObject;
    protected final ImageModel<T,S> imageModel;
    protected final SelectionModel<T,S> selectionModel;
    protected final ImagesPanel<T,S> imagesPanel;
    
    // For drag and drop functionality
    private DragSource source = new DragSource();
    private boolean dragEnabled = false;
    private DomainObjectTransferHandler transferHandler;
    private DragGestureListener dragGestureListener = new DragGestureListener() {
        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            log.info("dragGestureRecognized: {}",dge);
            if (!dragEnabled) {
                return;
            }
            InputEvent inputevent = dge.getTriggerEvent();
            boolean keyDown = false;
            if (inputevent instanceof MouseEvent) {
                MouseEvent mouseevent = (MouseEvent) inputevent;
                if (mouseevent.isShiftDown() || mouseevent.isAltDown() || mouseevent.isAltGraphDown() || mouseevent.isControlDown() || mouseevent.isMetaDown()) {
                    keyDown = true;
                }
            }
            if (!isSelected() && !keyDown) {
                log.info("selecting image: {}",dge);
                selectionModel.select(imageObject, true, true);
            }
            getTransferHandler().exportAsDrag(AnnotatedImageButton.this, dge.getTriggerEvent(), TransferHandler.LINK);
        }
    };

    private boolean titleVisible;

    
    /**
     * Factory method for creating AnnotatedImageButtons. 
     */
    public static <U,S> AnnotatedImageButton<U,S> create(U imageObject, ImageModel<U,S> imageModel, SelectionModel<U,S> selectionModel, ImagesPanel<U,S> imagesPanel) {
        String filepath = imageModel.getImageFilepath(imageObject);
        // The filepath is passed through because it's kind of expensive to calculate. 
        // But it's kind of redundant with the image model, and it doesn't make sense for static icons, 
        // so we could use some refactoring here to make this cleaner. 
        if (filepath != null) {
            return new DynamicImageButton<>(imageObject, imageModel, selectionModel, imagesPanel, filepath);
        }
        else {
            return new StaticImageButton<>(imageObject, imageModel, selectionModel, imagesPanel, filepath);
        }
    }

    @SuppressWarnings("unchecked")
    protected AnnotatedImageButton(T imgObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, final ImagesPanel<T,S> imagesPanel, String filepath) {
        this.imageObject = imgObject;
        this.imageModel = imageModel;
        this.selectionModel = selectionModel;
        this.imagesPanel = imagesPanel;
        
        Boolean disableImageDrag = (Boolean) FrameworkImplProvider.getModelProperty(OptionConstants.DISABLE_IMAGE_DRAG_PROPERTY);
        if (disableImageDrag == null || disableImageDrag == false) {
            if (selectionModel instanceof DomainObjectSelectionModel) {
                dragEnabled = true;
            }
        }
        
        GridBagConstraints c = new GridBagConstraints();
        buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        add(buttonPanel);
        
        titleLabel = new JLabel(" ");
        titleLabel.setFocusable(false);
        titleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(false);
        
        subtitleLabel = new JLabel();
        subtitleLabel.setFocusable(false);
        subtitleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subtitleLabel.setOpaque(false);
        subtitleLabel.setVisible(false);

        editMode = new JCheckBox();
        editMode.setHorizontalAlignment(SwingConstants.LEFT);
        editMode.setVerticalAlignment(SwingConstants.NORTH);
        editMode.setVisible(false);
        editMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                imagesPanel.updateEditSelectModel(imageObject, editMode.isSelected());
            }
        });

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(editMode, c);

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(titleLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(subtitleLabel, c);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        mainPanel.setOpaque(false);

        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        buttonPanel.add(mainPanel, c);
        
        
        // Remove all default mouse listeners except drag gesture recognizer
        for (MouseListener mouseListener : getMouseListeners()) {
            if (!(mouseListener instanceof MouseDragGestureRecognizer)) {
                removeMouseListener(mouseListener);
            }
        }

        // Fix event dispatching so that user can click on the title or the tags and still select the button
        titleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        subtitleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));

        imagesPanel.ensureCorrectAnnotationView(this);

        // If dragging is enabled, add the gesture recognizer to this panel, and all children where dragging can be initiated
        if (dragEnabled) {
            // TODO: this class should not know about DomainObjects
            this.transferHandler = new DomainObjectTransferHandler((ImageModel<DomainObject,Reference>)imageModel, (DomainObjectSelectionModel)selectionModel);
            setDraggable(this);
            setDraggable(titleLabel);
            setDraggable(subtitleLabel);
//            setDraggable(annotationPanel);     
        }
        
        refresh(imageObject);
    }

    protected void setMainComponent(JComponent component) {
        mainPanel.removeAll();
        mainPanel.add(component, BorderLayout.CENTER);
        component.addMouseListener(new MouseForwarder(this, "MainComponent->AnnotatedImageButton"));
        setDraggable(component);
        revalidate();
        repaint();
    }
    
    protected final void setDraggable(JComponent component) {
        source.createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_LINK, dragGestureListener);
        component.setTransferHandler(transferHandler);
    }
    
    public final void refresh(T imageObject) {

        String title = null;
        String subtitle = null;
        if (imageModel!=null) {
            title = imageModel.getImageTitle(imageObject);
            subtitle = imageModel.getImageSubtitle(imageObject);
        }
        else {
            title = imageObject.toString();
        }
        setTitle(title, 100);
        setSubtitle(subtitle, 100);
        setAnnotations(imageModel.getAnnotations(imageObject));
    }

    public void setTitle(String title, int maxWidth) {
        // Subtle font size scaling 
        int fontSize = (int) Math.round(maxWidth * 0.005) + 10;
        Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
        titleLabel.setFont(titleLabelFont);
        titleLabel.setPreferredSize(new Dimension(maxWidth, titleLabel.getFontMetrics(titleLabelFont).getHeight()));
        titleLabel.setText(title);
        titleLabel.setToolTipText(title);
        titleLabel.setVisible(titleVisible);
    }

    public void setSubtitle(String subtitle, int maxWidth) {
        if (StringUtils.isEmpty(subtitle)) {
            subtitleLabel.setVisible(false);
            return;
        }
        // Subtle font size scaling 
        int fontSize = (int) Math.round(maxWidth * 0.003) + 10;
        Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
        subtitleLabel.setFont(titleLabelFont);
        subtitleLabel.setPreferredSize(new Dimension(maxWidth, subtitleLabel.getFontMetrics(titleLabelFont).getHeight()));
        subtitleLabel.setText(subtitle);
        subtitleLabel.setToolTipText(subtitle);
        subtitleLabel.setVisible(titleVisible);
    }

    public synchronized void setTitleVisible(boolean visible) {
        this.titleVisible = visible;
        titleLabel.setVisible(visible);
        subtitleLabel.setVisible(visible & !StringUtils.isEmpty(subtitleLabel.getText()));
    }

    public synchronized void setTagsVisible(boolean visible) {
        ((JPanel) annotationView).setVisible(visible);
    }

    public synchronized AnnotationView getAnnotationView() {
        return annotationView;
    }

    public final synchronized void setAnnotationView(AnnotationView annotationView) {
        if (annotationView==null) throw new IllegalArgumentException("Annotation view cannot be null");
        if (this.annotationView!=null) {
            buttonPanel.remove((JPanel)this.annotationView);
        }
        this.annotationView = annotationView;
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add((JPanel)annotationView, c);
        
        // Fix event dispatching so that user can click on the tags and still select the button
        ((JPanel) annotationView).addMouseListener(new MouseForwarder(this, "JPanel(annotationView)->AnnotatedImageButton"));
        showAnnotations();
    }

    private void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
        showAnnotations();
    }
    
    private void showAnnotations() {
        annotationView.setAnnotations(annotations);
        buttonPanel.revalidate();
    }

    public T getUserObject() {
        return imageObject;
    }

    public void toggleEditMode(boolean mode) {
        // if hiding edit mode, clear out checkbox
        if (!mode) {
            this.setEditModeValue(false);
        }
        editMode.setVisible(mode);
    }

    public void setEditModeValue(boolean selection) {
        editMode.setSelected(selection);
    }

    public boolean getEditModeValue() {
        return editMode.isSelected();
    }

    public synchronized void setImageSize(int maxWidth, int maxHeight) {
        log.trace("setImageSize({}x{})", maxWidth, maxHeight);
        mainPanel.setPreferredSize(new Dimension(maxWidth, maxHeight));
        mainPanel.revalidate();
        mainPanel.repaint();
        setTitle(titleLabel.getText(), maxWidth);
        setSubtitle(subtitleLabel.getText(), maxWidth);
        JPanel annotationPanel = (JPanel) annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
            annotationPanel.setPreferredSize(new Dimension(maxWidth, annotationPanel.getPreferredSize().height));
        }
    }

    public synchronized void resizeTable(int tableHeight) {
        JPanel annotationPanel = (JPanel) annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
            annotationPanel.setPreferredSize(new Dimension(annotationPanel.getPreferredSize().width, tableHeight));
        }
    }

    public void setViewable(boolean wantViewable) {
        this.wantViewable = wantViewable;
    }
    
    public boolean isViewable() {
        return wantViewable;
    }

    protected synchronized void registerAspectRatio(int width, int height) {
        log.trace("registerAspectRatio({}x{})", width, height);
        mainPanel.setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
        double a = (double)width / (double)height;
        if (a != this.aspectRatio) {
            this.aspectRatio = a;
            if (imagesPanel!=null) {
                imagesPanel.registerAspectRatio(a);
            }
        }
    }
}
