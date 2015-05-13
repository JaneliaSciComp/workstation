package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import org.janelia.it.workstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.gui.support.SelectablePanel;

/**
 * A DynamicImagePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an
 * ImagesPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotatedImageButton<T> extends SelectablePanel implements DragGestureListener {

    protected final JLabel titleLabel;
    protected final JLabel subtitleLabel;
    protected final JPanel mainPanel;
    protected final JPanel buttonPanel;
    protected final JLabel loadingLabel;
    protected boolean viewable = false;
//    protected AnnotationView annotationView;
    protected boolean annotationsLoaded = false;
    protected DragSource source;
    protected double aspectRatio;
    protected final IconPanel iconPanel;
    protected final T imageObject;
    protected SimpleWorker annotationLoadingWorker;
    
    /**
     * Factory method for creating AnnotatedImageButtons. 
     * @param <U>
     * @param imageObject
     * @param filepath
     * @param iconPanel
     * @return 
     */
    public static <U> AnnotatedImageButton<U> create(U imageObject, String filepath, IconPanel iconPanel) {
        if (filepath != null) {
            return new DynamicImageButton(imageObject, iconPanel);
        }
        else {
            return new StaticImageButton(imageObject, iconPanel);
        }
    }

    protected AnnotatedImageButton(final T imageObject, final IconPanel iconPanel) {

        this.iconPanel = iconPanel;
        this.imageObject = imageObject;

        Boolean disableImageDrag = (Boolean) SessionMgr.getSessionMgr().getModelProperty(ViewerSettingsPanel.DISABLE_IMAGE_DRAG_PROPERTY);
        if (disableImageDrag == null || disableImageDrag == false) {
            this.source = new DragSource();
            source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);

            setTransferHandler(new EntityTransferHandler() {
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

//        c.gridx = 0;
//        c.gridy = 3;
//        c.fill = GridBagConstraints.BOTH;
//        c.anchor = GridBagConstraints.PAGE_START;
//        c.weighty = 1;
//        buttonPanel.add(loadingLabel, c);

        // Remove all default mouse listeners except drag gesture recognizer
        for (MouseListener mouseListener : getMouseListeners()) {
            if (!(mouseListener instanceof MouseDragGestureRecognizer)) {
                removeMouseListener(mouseListener);
            }
        }

        // Fix event dispatching so that user can click on the title or the tags and still select the button
        titleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        subtitleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));

        refresh(imageObject);
    }

    public final void refresh(T imageObject) {

        mainPanel.removeAll();

        StringBuilder tsb = new StringBuilder();
        if (iconPanel!=null) {
            tsb.append(iconPanel.getImageLabel(imageObject));
        }
        else {
            tsb.append(imageObject.toString());
        }
        
//        String splitPart = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
//        if (splitPart != null) {
//            tsb.append(" (").append(splitPart).append(")");
//        }
//
//        StringBuilder ssb = new StringBuilder();
//
//        String crossLabel = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CROSS_LABEL);
//        if (crossLabel != null) {
//            ssb.append("Cross label: ").append(crossLabel);
//        }
//        else {
//            final Entity rep = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
//            if (rep != null) {
//                if (EntityUtils.isInitialized(rep)) {
//                    ssb.append("Represented by ").append(rep.getName());
//                }
//                else {
//                    SimpleWorker worker = new SimpleWorker() {
//
//                        private Entity loadedRep;
//
//                        @Override
//                        protected void doStuff() throws Exception {
//                            loadedRep = ModelMgr.getModelMgr().getEntityById(rep.getId());
//                        }
//
//                        @Override
//                        protected void hadSuccess() {
//                            String subtitle = "Represented by " + loadedRep.getName();
//                            setSubtitle(subtitle, 100);
//                        }
//
//                        @Override
//                        protected void hadError(Throwable error) {
//                            SessionMgr.getSessionMgr().handleException(error);
//                        }
//
//                    };
//                    worker.execute();
//                }
//            }
//        }
//
        setTitle(tsb.toString(), 100);
//        if (ssb.length() > 0) {
//            setSubtitle(ssb.toString(), 100);
//        }

        mainPanel.add(init(imageObject));
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

    public abstract JComponent init(T imageObject);

    public synchronized void setTitleVisible(boolean visible) {
        titleLabel.setVisible(visible);
        subtitleLabel.setVisible(visible & !StringUtils.isEmpty(subtitleLabel.getText()));
    }

    public synchronized void setTagsVisible(boolean visible) {
//        if (annotationView == null) {
//            return;
//        }
//        ((JPanel) annotationView).setVisible(visible);
    }

//    public synchronized void setAnnotationView(AnnotationView annotationView) {
//        this.annotationView = annotationView;
//        // Fix event dispatching so that user can click on the tags and still select the button
//        ((JPanel) annotationView).addMouseListener(new MouseForwarder(this, "JPanel(annotationView)->AnnotatedImageButton"));
//        if (annotationsLoaded) {
//            showAnnotations(annotationView.getAnnotations());
//        }
//    }
//
//    public synchronized AnnotationView getAnnotationView() {
//        return annotationView;
//    }
//
//    public synchronized void showAnnotations(List<OntologyAnnotation> annotations) {
//
//        annotationView.setAnnotations(annotations);
//        annotationsLoaded = true;
//
//        buttonPanel.remove(loadingLabel);
//        if (annotationView != null) {
//            buttonPanel.remove((JPanel) annotationView);
//        }
//        GridBagConstraints c = new GridBagConstraints();
//        c.gridx = 0;
//        c.gridy = 3;
//        c.fill = GridBagConstraints.BOTH;
//        c.anchor = GridBagConstraints.PAGE_START;
//        c.weighty = 1;
//        buttonPanel.add((JPanel) annotationView, c);
//
//        buttonPanel.revalidate();
//    }

    public T getImageObject() {
        return imageObject;
    }

    public synchronized void setImageSize(int maxWidth, int maxHeight) {
        setTitle(titleLabel.getText(), maxWidth);
//        JPanel annotationPanel = (JPanel) annotationView;
//        if (annotationView instanceof AnnotationTablePanel) {
//            annotationPanel.setPreferredSize(new Dimension(maxWidth, annotationPanel.getPreferredSize().height));
//        }
    }

    public synchronized void resizeTable(int tableHeight) {
//        JPanel annotationPanel = (JPanel) annotationView;
//        if (annotationView instanceof AnnotationTablePanel) {
//            annotationPanel.setPreferredSize(new Dimension(annotationPanel.getPreferredSize().width, tableHeight));
//        }
    }

    public void setViewable(boolean wantViewable) {
    }

    protected synchronized void registerAspectRatio(double width, double height) {
        double a = width / height;
        if (a != this.aspectRatio) {
            this.aspectRatio = a;
            if (iconPanel!=null) {
                iconPanel.registerAspectRatio(a);
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
//        if (!isSelected() && !keyDown) {
//            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(iconPanel.getSelectionCategory(), rootedEntity.getId(), true);
//        }
        getTransferHandler().exportAsDrag(this, dge.getTriggerEvent(), TransferHandler.LINK);
    }
}
