
package org.janelia.it.workstation.gui.framework.viewer;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.List;

import javax.swing.*;

import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A DynamicImagePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an
 * ImagesPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotatedImageButton extends JPanel implements DragGestureListener {

    protected final JLabel titleLabel;
    protected final JLabel subtitleLabel;
    protected final JPanel mainPanel;
    protected final JPanel buttonPanel;
    protected final JLabel loadingLabel;
    protected boolean viewable = false;
    protected AnnotationView annotationView;
    protected boolean annotationsLoaded = false;
    protected DragSource source;
    protected double aspectRatio;
    protected final IconPanel iconPanel;
    protected final org.janelia.it.workstation.model.entity.RootedEntity rootedEntity;
    protected SimpleWorker annotationLoadingWorker;

    private static BufferedImage normalBorderImage;
    private static BufferedImage selectedBorderImage;
    private static Color normalBackground;
    private static Color selectedBackground;

    static {
        // TODO: this should be done whenever the L&F changes, but currently we need to restart anyway, 
        // so it doesn't matter until that is fixed

        String normalBorder = "border_normal.png";
        String selectedBorder = "border_selected.png";
        normalBackground = new Color(241, 241, 241);
        selectedBackground = new Color(203, 203, 203);

        if (org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().isDarkLook()) {
            normalBorder = "border_dark_normal.png";
            selectedBorder = "border_dark_selected.png";
            normalBackground = null;
            selectedBackground = null;
        }

        try {
            normalBorderImage = Utils.toBufferedImage(Utils.getClasspathImage(normalBorder).getImage());
            selectedBorderImage = Utils.toBufferedImage(Utils.getClasspathImage(selectedBorder).getImage());
        }
        catch (FileNotFoundException e) {
            org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public AnnotatedImageButton(final org.janelia.it.workstation.model.entity.RootedEntity rootedEntity, final IconPanel iconPanel) {

        normalBackground = getBackground();
        setBackground(normalBackground);

        this.iconPanel = iconPanel;
        this.rootedEntity = rootedEntity;

        Boolean disableImageDrag = (Boolean) org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().getModelProperty(org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel.DISABLE_IMAGE_DRAG_PROPERTY);
        if (disableImageDrag == null || disableImageDrag == false) {
            this.source = new DragSource();
            source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);

            setTransferHandler(new org.janelia.it.workstation.gui.framework.outline.EntityTransferHandler() {
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
        loadingLabel.setIcon(org.janelia.it.workstation.gui.util.Icons.getLoadingIcon());
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

        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add(loadingLabel, c);

        // Remove all default mouse listeners except drag gesture recognizer
        for (MouseListener mouseListener : getMouseListeners()) {
            if (!(mouseListener instanceof MouseDragGestureRecognizer)) {
                removeMouseListener(mouseListener);
            }
        }

        // Fix event dispatching so that user can click on the title or the tags and still select the button
        titleLabel.addMouseListener(new org.janelia.it.workstation.gui.util.MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        subtitleLabel.addMouseListener(new org.janelia.it.workstation.gui.util.MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));

        refresh(rootedEntity);
    }

    public final void refresh(org.janelia.it.workstation.model.entity.RootedEntity rootedEntity) {

        mainPanel.removeAll();

        Entity entity = rootedEntity.getEntity();

        StringBuilder tsb = new StringBuilder();
        tsb.append(entity.getName());

        String splitPart = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
        if (splitPart != null) {
            tsb.append(" (").append(splitPart).append(")");
        }

        StringBuilder ssb = new StringBuilder();

        String crossLabel = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CROSS_LABEL);
        if (crossLabel != null) {
            ssb.append("Cross label: ").append(crossLabel);
        }
        else {
            final Entity rep = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
            if (rep != null) {
                if (EntityUtils.isInitialized(rep)) {
                    ssb.append("Represented by ").append(rep.getName());
                }
                else {
                    SimpleWorker worker = new SimpleWorker() {

                        private Entity loadedRep;

                        @Override
                        protected void doStuff() throws Exception {
                            loadedRep = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntityById(rep.getId());
                        }

                        @Override
                        protected void hadSuccess() {
                            String subtitle = "Represented by " + loadedRep.getName();
                            setSubtitle(subtitle, 100);
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
                        }

                    };
                    worker.execute();
                }
            }
        }

        setTitle(tsb.toString(), 100);
        if (ssb.length() > 0) {
            setSubtitle(ssb.toString(), 100);
        }

        mainPanel.add(init(rootedEntity));
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

    public abstract JComponent init(org.janelia.it.workstation.model.entity.RootedEntity rootedEntity);

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

    public synchronized void setAnnotationView(AnnotationView annotationView) {
        this.annotationView = annotationView;
        // Fix event dispatching so that user can click on the tags and still select the button
        ((JPanel) annotationView).addMouseListener(new org.janelia.it.workstation.gui.util.MouseForwarder(this, "JPanel(annotationView)->AnnotatedImageButton"));
        if (annotationsLoaded) {
            showAnnotations(annotationView.getAnnotations());
        }
    }

    public synchronized AnnotationView getAnnotationView() {
        return annotationView;
    }

    public synchronized void showAnnotations(List<OntologyAnnotation> annotations) {

        annotationView.setAnnotations(annotations);
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

    public org.janelia.it.workstation.model.entity.RootedEntity getRootedEntity() {
        return rootedEntity;
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

    public void setInvertedColors(boolean inverted) {
    }

    public void setViewable(boolean wantViewable) {
    }

    public synchronized void registerAspectRatio(double width, double height) {
        double a = width / height;
        if (a != this.aspectRatio) {
            iconPanel.getImagesPanel().registerAspectRatio(a);
        }
        this.aspectRatio = a;
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
            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(iconPanel.getSelectionCategory(), rootedEntity.getId(), true);
        }
        getTransferHandler().exportAsDrag(this, dge.getTriggerEvent(), TransferHandler.LINK);
    }

    public IconPanel getIconPanel() {
        return iconPanel;
    }

    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;

        if (selected) {
            if (selectedBackground != null) {
                setBackground(selectedBackground);
            }
        }
        else {
            if (normalBackground != null) {
                setBackground(normalBackground);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        BufferedImage borderImage = selected ? selectedBorderImage : normalBorderImage;
        if (borderImage == null) {
            return;
        }

        int b = 10; // border width
        int w = getWidth();
        int h = getHeight();
        int iw = borderImage.getWidth();
        int ih = borderImage.getHeight();

        g.drawImage(borderImage, 0, 0, b, b, 0, 0, b, b, null); // top left
        g.drawImage(borderImage, w - b, 0, w, b, iw - b, 0, iw, b, null); // top right
        g.drawImage(borderImage, 0, h - b, b, h, 0, ih - b, b, ih, null); // bottom right
        g.drawImage(borderImage, w - b, h - b, w, h, iw - b, ih - b, iw, ih, null); // bottom left

        g.drawImage(borderImage, b, 0, w - b, b, b, 0, iw - b, b, null); // top
        g.drawImage(borderImage, 0, b, b, h - b, 0, b, b, ih - b, null); // left
        g.drawImage(borderImage, b, h - b, w - b, h, b, ih - b, iw - b, ih, null); // bottom
        g.drawImage(borderImage, w - b, b, w, h - b, iw - b, b, iw, ih - b, null); // right
    }
}
