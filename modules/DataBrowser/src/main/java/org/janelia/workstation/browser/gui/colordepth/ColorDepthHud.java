package org.janelia.workstation.browser.gui.colordepth;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.LayerUI;

import net.miginfocom.swing.MigLayout;
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
import org.janelia.workstation.core.filecache.URLProxy;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final int CROSSHAIR_OFFSET = 4;
    private static final int CROSSHAIR_SIZE = 5;

    // Input Handling
    private final Point pp = new Point();
    private KeyListener keyListener;

    // GUI
    private final JPanel headPanel;
    private final JScrollPane scrollPane1;
    private final JLabel previewLabel1;
    private final JScrollPane scrollPane2;
    private final JLabel previewLabel2;
    private final JLayer<JScrollPane> scrollLayer1;
    private final JLayer<JScrollPane> scrollLayer2;

    // Current state
    private boolean firstShowing = true;
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

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(scrollLayer1);
        mainPanel.add(scrollLayer2);

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

    public void showDialog() {
        log.debug("showDialog");
        packAndShow();
    }

    public void hideDialog() {
        log.debug("hideDialog");
        setVisible(false);
    }

    @Override
    protected void packAndShow() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
        if (firstShowing) {
            setLocationRelativeTo(getParent());
            firstShowing = false;
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
            private BufferedImage image1;
            private BufferedImage image2;

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
                URLProxy imageFileURL1 = FileMgr.getFileMgr().getURL(mask.getFilepath(), true);
                this.image1 = Utils.readImage(imageFileURL1);
                URLProxy imageFileURL2 = FileMgr.getFileMgr().getURL(image.getFilepath(), true);
                this.image2 = Utils.readImage(imageFileURL2);
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

                previewLabel1.setIcon(image1 == null ? null : new ImageIcon(image1));
                previewLabel2.setIcon(image2 == null ? null : new ImageIcon(image2));

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int width = (int)Math.round((double)screenSize.width*(0.9/2));
                int height = (int)Math.round((double)screenSize.height*0.9);
                width = Math.min(image1.getWidth(), width);
                height = Math.min(image1.getHeight(), height);

                scrollPane1.setSize(new Dimension(width, height));
                scrollPane2.setSize(new Dimension(width, height));

                if (toggle) {
                    toggleDialog();
                }
                else {
                    pack();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        worker.execute();
    }


}
