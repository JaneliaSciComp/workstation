package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

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
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ontology.Annotation;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.events.selection.SelectionModel;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationTablePanel;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationTagCloudPanel;
import org.janelia.it.workstation.gui.browser.gui.support.AnnotationView;
import org.janelia.it.workstation.gui.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SelectablePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an
 * ImagesPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotatedImageButton<T,S> extends SelectablePanel implements DragGestureListener {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedImageButton.class);
    
    protected final JLabel titleLabel;
    protected final JLabel subtitleLabel;
    protected final JPanel mainPanel;
    protected final JPanel buttonPanel;
    protected final JLabel loadingLabel;
    protected boolean viewable = false;
    protected AnnotationView<T,S> annotationView;
    protected boolean annotationsLoaded = false;
    protected DragSource source;
    protected double aspectRatio;
    protected final ImagesPanel<T,S> imagesPanel;
    protected final ImageModel<T,S> imageModel;
    protected final SelectionModel<T,S> selectionModel;
    protected final T imageObject;
    protected SimpleWorker annotationLoadingWorker;
    protected final JComponent innerComponent;
    
    /**
     * Factory method for creating AnnotatedImageButtons. 
     */
    public static <U,S> AnnotatedImageButton<U,S> create(U imageObject, ImageModel<U,S> imageModel, SelectionModel<U,S> selectionModel, ImagesPanel<U,S> imagesPanel) {
        if (imageModel.getImageFilepath(imageObject) != null) {
            return new DynamicImageButton<>(imageObject, imageModel, selectionModel, imagesPanel);
        }
        else {
            return new StaticImageButton<>(imageObject, imageModel, selectionModel, imagesPanel);
        }
    }

    protected AnnotatedImageButton(T imageObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, ImagesPanel<T,S> imagesPanel) {

        this.imageObject = imageObject;
        this.imageModel = imageModel;
        this.selectionModel = selectionModel;
        this.imagesPanel = imagesPanel;

        Boolean disableImageDrag = (Boolean) SessionMgr.getSessionMgr().getModelProperty(ViewerSettingsPanel.DISABLE_IMAGE_DRAG_PROPERTY);
        if (disableImageDrag == null || disableImageDrag == false) {
            this.source = new DragSource();
            source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);
            // TODO: this class should not know about DomainObjects
            setTransferHandler(new DomainObjectTransferHandler((ImageModel<DomainObject,DomainObjectId>)imageModel, (DomainObjectSelectionModel)selectionModel) {
                @Override
                public JComponent getDropTargetComponent() {
                    return AnnotatedImageButton.this;
                }
            });
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

        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(titleLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(subtitleLabel, c);

        mainPanel = new JPanel();
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

        setAnnotationView(new AnnotationTagCloudPanel<T,S>());

        mainPanel.removeAll();
        this.innerComponent = init(imageObject, imageModel);
        mainPanel.add(innerComponent);
        
        refresh(imageObject);
    }

    public final void refresh(T imageObject) {

        StringBuilder tsb = new StringBuilder();
        if (imageModel!=null) {
            tsb.append(imageModel.getImageLabel(imageObject));
        }
        else {
            tsb.append(imageObject.toString());
        }
        setTitle(tsb.toString(), 100);
        showAnnotations(imageModel.getAnnotations(imageObject));
    }

    public void setTitle(String title, int maxWidth) {
        // Subtle font size scaling 
        int fontSize = (int) Math.round((double) maxWidth * 0.005) + 10;
        Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
        titleLabel.setFont(titleLabelFont);
        titleLabel.setPreferredSize(new Dimension(maxWidth, titleLabel.getFontMetrics(titleLabelFont).getHeight()));
        titleLabel.setText(title);
        titleLabel.setToolTipText(title);
    }

    public void setSubtitle(String subtitle, int maxWidth) {
        if (StringUtils.isEmpty(subtitle)) {
            subtitleLabel.setVisible(false);
            return;
        }
        // Subtle font size scaling 
        int fontSize = (int) Math.round((double) maxWidth * 0.003) + 10;
        Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
        subtitleLabel.setFont(titleLabelFont);
        subtitleLabel.setPreferredSize(new Dimension(maxWidth, subtitleLabel.getFontMetrics(titleLabelFont).getHeight()));
        subtitleLabel.setText(subtitle);
        subtitleLabel.setToolTipText(subtitle);
        subtitleLabel.setVisible(true);
    }

    public abstract JComponent init(T imageObject, final ImageModel<T,S> imageModel);

    public synchronized void setTitleVisible(boolean visible) {
        titleLabel.setVisible(visible);
        subtitleLabel.setVisible(visible & !StringUtils.isEmpty(subtitleLabel.getText()));
    }

    public synchronized void setTagsVisible(boolean visible) {
        if (annotationView == null) {
            return;
        }
        ((JPanel) annotationView).setVisible(visible);
    }

    public final synchronized void setAnnotationView(AnnotationView<T,S> annotationView) {
        this.annotationView = annotationView;
        // Fix event dispatching so that user can click on the tags and still select the button
        ((JPanel) annotationView).addMouseListener(new MouseForwarder(this, "JPanel(annotationView)->AnnotatedImageButton"));
        if (annotationsLoaded) {
            showAnnotations(annotationView.getAnnotations());
        }
    }

    public synchronized AnnotationView<T,S> getAnnotationView() {
        return annotationView;
    }

    private void showAnnotations(List<Annotation> annotations) {

        annotationView.setAnnotations(annotations);
        annotationView.setSelectionModel(selectionModel);
        annotationView.setImageModel(imageModel);
        
        annotationsLoaded = true;

        buttonPanel.remove(loadingLabel);
        if (annotationView != null) {
            buttonPanel.remove((JPanel) annotationView);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add((JPanel) annotationView, c);

        buttonPanel.revalidate();
    }

    public T getImageObject() {
        return imageObject;
    }

    public synchronized void setImageSize(int maxWidth, int maxHeight) {
        setTitle(titleLabel.getText(), maxWidth);
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

    public abstract void setViewable(boolean wantViewable);

    protected synchronized void registerAspectRatio(double width, double height) {
        double a = width / height;
        if (a != this.aspectRatio) {
            this.aspectRatio = a;
            if (imagesPanel!=null) {
                imagesPanel.registerAspectRatio(a);
            }
        }
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        InputEvent inputevent = dge.getTriggerEvent();
        boolean keyDown = false;
        if (inputevent instanceof MouseEvent) {
            MouseEvent mouseevent = (MouseEvent) inputevent;
            if (mouseevent.isShiftDown() || mouseevent.isAltDown() || mouseevent.isAltGraphDown() || mouseevent.isControlDown() || mouseevent.isMetaDown()) {
                keyDown = true;
            }
        }
        if (!isSelected() && !keyDown) {
            selectionModel.select(imageObject, true);
        }
        getTransferHandler().exportAsDrag(this, dge.getTriggerEvent(), TransferHandler.LINK);
    }
}
