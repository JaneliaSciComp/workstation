package org.janelia.workstation.colordepth;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;
import org.janelia.model.domain.enums.SplitHalfType;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.support.AnnotationTagCloudPanel;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Special heads-up display for viewing color depth search results alongside the search mask.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthHud extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthHud.class);

    private static ColorDepthHud instance;
    public static ColorDepthHud getSingletonInstance() {
        if (instance == null) {
            instance = new ColorDepthHud();
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private static final int CROSSHAIR_OFFSET = 5;
    private static final int CROSSHAIR_SIZE = 7;

    // Input Handling
    private final Point pp = new Point();
    private KeyListener keyListener;

    // GUI
    private final JPanel headPanel;
    private final JPanel imagePanel1;
    private final JPanel imagePanel2;
    private final JPanel checkboxPanel1;
    private final JPanel checkboxPanel2;
    private final JCheckBox mirrorCheckbox1;
    private final JCheckBox mirrorCheckbox2;
    private final JScrollPane scrollPane1;
    private final JLabel previewLabel1;
    private final JScrollPane scrollPane2;
    private final JLabel previewLabel2;
    private final JLayer<JScrollPane> scrollLayer1;
    private final JLayer<JScrollPane> scrollLayer2;
    private BufferedImage image1;
    private BufferedImage image2;

    // Current state
    private boolean resetPosition = true;
    private Point point;

    class MouseCursorLayerUI extends LayerUI<JScrollPane> {

        @Override
        public void paint(Graphics g, JComponent c) {

            super.paint(g, c);
            if (point != null) {
                // Draw red crosshair
                int o = CROSSHAIR_OFFSET;
                int s = CROSSHAIR_SIZE;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.red);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(point.x-s-o, point.y, point.x-o, point.y);
                g2.drawLine(point.x+s+o, point.y, point.x+o, point.y);
                g2.drawLine(point.x, point.y-s-o, point.x, point.y-o);
                g2.drawLine(point.x, point.y+s+o, point.x, point.y+o);
                g2.dispose();
            }
        }
    }

    private ColorDepthHud() {

        setModalityType(Dialog.ModalityType.MODELESS);
        setLayout(new BorderLayout());
        setVisible(false);

        previewLabel1 = new JLabel(new ImageIcon());
        previewLabel1.setFocusable(false);
        previewLabel1.setRequestFocusEnabled(false);

        scrollPane1 = new JScrollPane();
        scrollPane1.setViewportView(previewLabel1);
        scrollLayer1 = new JLayer<>(scrollPane1, new MouseCursorLayerUI());

        previewLabel2 = new JLabel(new ImageIcon());
        previewLabel2.setFocusable(false);
        previewLabel2.setRequestFocusEnabled(false);

        scrollPane2 = new JScrollPane();
        scrollPane2.setViewportView(previewLabel2);
        scrollLayer2 = new JLayer<>(scrollPane2, new MouseCursorLayerUI());

        // Borrowed from https://stackoverflow.com/questions/1984071/how-to-hide-cursor-in-a-swing-application
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
                new Point(0, 0), "blank cursor");
        scrollPane1.setCursor(blankCursor);
        scrollPane2.setCursor(blankCursor);

        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                point = null;
                scrollLayer1.repaint();
                scrollLayer2.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                point = e.getPoint();
                scrollLayer1.repaint();
                scrollLayer2.repaint();
            }

            public void mouseDragged(final MouseEvent e) {
                // Move location
                JViewport vport = (JViewport) e.getSource();
                Point cp = e.getPoint();
                Point vp = vport.getViewPosition();
                vp.translate(pp.x - cp.x, pp.y - cp.y);
                previewLabel1.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
                previewLabel2.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
                pp.setLocation(cp);
                // Update cursor
                point = e.getPoint();
                scrollLayer1.repaint();
                scrollLayer2.repaint();
            }

            public void mousePressed(MouseEvent e) {
                pp.setLocation(e.getPoint());
            }
        };

        scrollPane1.getViewport().addMouseMotionListener(mouseAdapter);
        scrollPane1.getViewport().addMouseListener(mouseAdapter);
        scrollPane2.getViewport().addMouseMotionListener(mouseAdapter);
        scrollPane2.getViewport().addMouseListener(mouseAdapter);

        headPanel = new JPanel();
        headPanel.setLayout(new MigLayout("gap 50, fillx, wrap 2"));
        add(headPanel, BorderLayout.NORTH);

        mirrorCheckbox1 = new JCheckBox("Mirror");
        mirrorCheckbox1.setFocusable(false);
        mirrorCheckbox1.addItemListener((e) -> updateMaskImage());

        mirrorCheckbox2 = new JCheckBox("Mirror");
        mirrorCheckbox2.setFocusable(false);
        mirrorCheckbox2.addItemListener((e) -> updateResultImage());

        checkboxPanel1 = new JPanel();
        checkboxPanel1.setLayout(new BorderLayout());
        checkboxPanel1.add(mirrorCheckbox1, BorderLayout.WEST);

        checkboxPanel2 = new JPanel();
        checkboxPanel2.setLayout(new BorderLayout());
        checkboxPanel2.add(mirrorCheckbox2, BorderLayout.WEST);

        imagePanel1 = new JPanel();
        imagePanel1.setLayout(new BorderLayout());
        imagePanel1.add(checkboxPanel1, BorderLayout.NORTH);
        imagePanel1.add(scrollLayer1, BorderLayout.CENTER);

        imagePanel2 = new JPanel();
        imagePanel2.setLayout(new BorderLayout());
        imagePanel2.add(checkboxPanel2, BorderLayout.NORTH);
        imagePanel2.add(scrollLayer2, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(imagePanel1);
        mainPanel.add(imagePanel2);

        add(mainPanel, BorderLayout.CENTER);

        // Default key listener should close this window
        setKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                if (KeymapUtil.isModifier(e)) {
                    return;
                }

                if (e.getID() != KeyEvent.KEY_PRESSED) {
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    hideDialog();
                    e.consume();
                }
            }
        });
    }

    /**
     * The HUD should only have one listener at a time. Clients should use this
     * method instead of addKeyListener.
     * @param keyListener
     */
    public void setKeyListener(KeyListener keyListener) {
        if (this.keyListener == keyListener) return;
        this.keyListener = keyListener;
        for(KeyListener l : getKeyListeners()) {
            removeKeyListener(l);
        }
        if (keyListener!=null) {
            addKeyListener(keyListener);
        }
    }

    public void toggleDialog() {
        log.debug("toggleDialog");
        if (isVisible()) {
            hideDialog();
        }
        else {
            showDialog();
        }
    }

    public void hideDialog() {
        log.debug("hideDialog");
        setVisible(false);
    }

    public void showDialog() {
        log.debug("showDialog");
        packAndShow();
    }

    @Override
    protected void packAndShow() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
        if (resetPosition) {
            log.info("Resetting HUD location");
            setLocationRelativeTo(getParent());
            resetPosition = false;
        }
        setVisible(true);
    }

    public void setObjectAndToggleDialog(ColorDepthMatch match, ColorDepthResultImageModel imageModel, boolean toggle) {

        SimpleWorker worker = new SimpleWorker()     {

            private String title;
            private String subtitle;
            private List<Annotation> annotations;
            private ColorDepthImage image;
            private Sample sample;

            @Override
            protected void doStuff() throws Exception {

                // Get information from the image model
                ColorDepthMask mask = imageModel.getMask();
                annotations = imageModel.getAnnotations(match);
                title = imageModel.getImageTitle(match);
                subtitle = imageModel.getImageSubtitle(match);

                // Load related objects
                image = DomainMgr.getDomainMgr().getModel().getDomainObject(match.getImageRef());
                log.debug("sampleRef: {}", image.getSampleRef());
                if (image.getSampleRef()!=null) {
                    sample = DomainMgr.getDomainMgr().getModel().getDomainObject(image.getSampleRef());
                }
                log.debug("sample: {}", sample);

                // Load images
                try (InputStream imageStream = FileMgr.getFileMgr().openFileInputStream(mask.getFilepath(), false)) {
                    image1 = Utils.readImageFromInputStream(imageStream, FilenameUtils.getExtension(mask.getFilepath()));
                }
                catch (FileNotFoundException e) {
                    log.warn("Mask image not found: {}", mask.getFilepath());
                }
                try (InputStream imageStream = FileMgr.getFileMgr().openFileInputStream(image.getFilepath(), false)) {
                    image2 = Utils.readImageFromInputStream(imageStream, FilenameUtils.getExtension(image.getFilepath()));
                }
                catch (FileNotFoundException e) {
                    log.warn("Result image not found: {}", image.getFilepath());
                }
            }

            @Override
            protected void hadSuccess() {

                AnnotationTagCloudPanel annotationPanel = new AnnotationTagCloudPanel() {
                    @Override
                    protected JPopupMenu getPopupMenu(Annotation annotation) {
                        if (annotation.getName().equals(SplitHalfType.AD.getName())
                                || annotation.getName().equals(SplitHalfType.DBD.getName())) {
                            SplitHalfContextMenu menu = new SplitHalfContextMenu(
                                    imageModel, match, SplitHalfType.valueOf(annotation.getName()));
                            menu.addMenuItems();
                            return menu;
                        }
                        return null;
                    }
                };
                annotationPanel.setAnnotations(annotations);

                JPanel titlePanel = new JPanel();
                titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
                titlePanel.add(new JLabel(title));
                titlePanel.add(annotationPanel);

                headPanel.removeAll();
                headPanel.add(titlePanel, "al left center");
                headPanel.add(new JLabel(subtitle), "al right center");
                headPanel.updateUI();

                updateMaskImage();
                updateResultImage();

                int imageWidth, imageHeight;
                if (image1 != null) {
                    imageWidth = image1.getWidth();
                    imageHeight = image1.getHeight();
                }
                else if (image2 != null) {
                    imageWidth = image2.getWidth();
                    imageHeight = image2.getHeight();
                }
                else {
                    imageWidth = imageHeight = 100;
                }

                // Pack to get panel sizes
                pack();

                // Get size of the screen
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
                log.trace("Got screen insets: {}", scnMax);
                int width = screenSize.width - scnMax.left - scnMax.right;
                int height = screenSize.height - scnMax.bottom - scnMax.top;

                // Use less than available, just in case. On Windows, sizing the dialog larger than the screen can
                // result in the panel not rendering after it's toggled off and on.
                width = (int)Math.round((double)width * 0.98);
                height = (int)Math.round((double)height * 0.98);
                log.info("Available screen size: {}x{}", width, height);

                int padding = 8;
                int windowTitleHeight = 35;
                log.info("Image size: {}x{}", imageWidth, imageHeight);
                log.debug("  imageHeight: {}", imageHeight);
                log.debug("  windowTitleHeight: {}", windowTitleHeight);
                log.debug("  headPanel height: {}", headPanel.getPreferredSize().height);
                log.debug("  checkboxPanel1.height: {}", checkboxPanel1.getPreferredSize().height);

                int availableWidth = width/2 - padding;
                int availableHeight = height - padding - windowTitleHeight
                        - headPanel.getPreferredSize().height -  checkboxPanel1.getPreferredSize().height;
                log.debug("Available image size: {}x{}", availableWidth, availableHeight);

                int scrollPaneWidth = Math.min(imageWidth + padding, availableWidth);
                int scrollPaneHeight = Math.min(imageHeight + padding, availableHeight);

                Dimension currentSize = scrollPane1.getPreferredSize();
                if (currentSize.width != scrollPaneWidth || currentSize.height != scrollPaneHeight) {
                    resetPosition = true;
                }

                log.info("Setting scroll pane size: {}x{}", scrollPaneWidth, scrollPaneHeight);
                scrollPane1.setPreferredSize(new Dimension(scrollPaneWidth, scrollPaneHeight));
                scrollPane2.setPreferredSize(new Dimension(scrollPaneWidth, scrollPaneHeight));

                if (toggle) {
                    toggleDialog();
                }
                else {
                    pack();
                    revalidate();
                    repaint();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }

    private void updateMaskImage() {
        if (image1 != null) {
            if (mirrorCheckbox1.isSelected()) {
                previewLabel1.setIcon(new ImageIcon(mirror(image1)));
            }
            else {
                previewLabel1.setIcon(new ImageIcon(image1));
            }
        }
        else {
            previewLabel1.setIcon(null);
        }
    }

    private void updateResultImage() {
        if (image1 != null) {
            if (mirrorCheckbox2.isSelected()) {
                previewLabel2.setIcon(new ImageIcon(mirror(image2)));
            }
            else {
                previewLabel2.setIcon(new ImageIcon(image2));
            }
        }
        else {
            previewLabel2.setIcon(null);
        }
    }

    /**
     * Flip the given image horizontally and return the result
     * @param image
     * @return
     */
    private BufferedImage mirror(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-image.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(image, null);
    }

}
