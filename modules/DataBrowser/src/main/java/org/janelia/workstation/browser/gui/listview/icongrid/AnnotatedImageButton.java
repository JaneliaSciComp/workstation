package org.janelia.workstation.browser.gui.listview.icongrid;

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

import org.janelia.workstation.browser.gui.options.BrowserOptions;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.browser.gui.support.SelectablePanel;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionModel;
import org.janelia.workstation.core.events.selection.SelectionModel;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.common.gui.support.AnnotationView;
import org.janelia.workstation.common.gui.support.MouseForwarder;
import org.janelia.workstation.browser.gui.support.AnnotationTablePanel;
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
    
    // GUI
    protected final JLabel titleLabel;
    protected final JLabel subtitleLabel;
    protected final JPanel mainPanel;
    protected final JPanel buttonPanel;
    protected final JCheckBox editMode;
    protected AnnotationView annotationView;
    
    // Model
    protected final ImageModel<T,S> imageModel;
    protected final SelectionModel<T,S> selectionModel;
    protected T imageObject;
    protected List<Annotation> annotations;

    // State
    protected boolean wantViewable = false;
    protected double aspectRatio;
    protected boolean titleVisible;
    
    // For drag and drop functionality
    protected DragSource source = new DragSource();
    protected boolean dragEnabled = false;
    protected DomainObjectTransferHandler transferHandler;
    protected DragGestureListener dragGestureListener = new DragGestureListener() {
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

    @SuppressWarnings("unchecked")
    protected AnnotatedImageButton(T imgObject, ImageModel<T,S> imageModel, SelectionModel<T,S> selectionModel, String filepath) {
        this.imageObject = imgObject;
        this.imageModel = imageModel;
        this.selectionModel = selectionModel;

        if (!BrowserOptions.getInstance().isDragAndDropDisabled()) {
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
        editMode.setVerticalAlignment(SwingConstants.TOP);
        editMode.setVisible(false);
        editMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                updateEditSelectModel(editMode.isSelected());
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
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(titleLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(subtitleLabel, c);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        mainPanel.setOpaque(false);

        c.gridx = 0;
        c.gridy = 3;
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

        // If dragging is enabled, add the gesture recognizer to this panel, and all children where dragging can be initiated
        if (dragEnabled) {
            // TODO: this class should not know about DomainObjects
            this.transferHandler = new DomainObjectTransferHandler((ImageModel<DomainObject,Reference>)imageModel, (DomainObjectSelectionModel)selectionModel);
            setDraggable(this);
            setDraggable(titleLabel);
            setDraggable(subtitleLabel);     
        }
        
        refresh(imageObject);
    }
    
    /**
     * Subclasses must call this to add their content.
     */
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

        this.imageObject = imageObject;
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
        c.gridy = 4;
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
        if (annotationView!=null) {
            annotationView.setAnnotations(annotations);
            buttonPanel.revalidate();
        }
    }

    public T getUserObject() {
        return imageObject;
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

    /**
     * Override this method to take action when the user clicks the edit mode button.
     * @param select
     */
    public void updateEditSelectModel(boolean select) {
    }
}
