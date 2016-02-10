package org.janelia.it.workstation.gui.browser.gui.hud;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.it.workstation.gui.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.gui.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.ImageCache;
import org.janelia.it.workstation.gui.util.MissingIcon;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persistent heads-up display for a synchronized image.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Hud extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(Hud.class);

    public static final String THREE_D_CONTROL = "3D";

    private static Hud instance;
    
    private final Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final Cursor hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Point pp = new Point();
    
    private DomainObject domainObject;
    private HasFiles fileProvider;
    
    private boolean dirtyEntityFor3D;
    private final JScrollPane scrollPane;
    private final JLabel previewLabel;
    private Mip3d mip3d;

    private ResultSelectionButton resultButton;
    private ImageTypeSelectionButton typeButton;
    private final JMenu rgbMenu = new JMenu("RGB Controls");
    private JCheckBox render3DCheckbox;
    
    private Hud3DController hud3DController;
    private KeyListener keyListener;

    public enum COLOR_CHANNEL {
        RED,
        GREEN,
        BLUE
    }
    
    public static Hud getSingletonInstance() {
        if (instance == null) {
            instance = new Hud();
        }
        return instance;
    }
    
    private Hud() {
        dirtyEntityFor3D = true;
        setModalityType(ModalityType.MODELESS);
        setLayout(new BorderLayout());
        setVisible(false);
        previewLabel = new JLabel(new ImageIcon());
        previewLabel.setFocusable(false);
        previewLabel.setRequestFocusEnabled(false);
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(previewLabel);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            public void mouseDragged(final MouseEvent e) {
                JViewport vport = (JViewport) e.getSource();
                Point cp = e.getPoint();
                Point vp = vport.getViewPosition();
                vp.translate(pp.x - cp.x, pp.y - cp.y);
                previewLabel.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
                pp.setLocation(cp);
            }

            public void mousePressed(MouseEvent e) {
                previewLabel.setCursor(hndCursor);
                pp.setLocation(e.getPoint());
            }

            public void mouseReleased(MouseEvent e) {
                previewLabel.setCursor(defCursor);
                previewLabel.repaint();
            }
        };
        
        scrollPane.getViewport().addMouseMotionListener(mouseAdapter);
        scrollPane.getViewport().addMouseListener(mouseAdapter);
        
        this.add(scrollPane, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.CENTER);
        init3dGui();
        if (mip3d != null) {
            mip3d.setDoubleBuffered(true);
        }
    }

    public void toggleDialog() {
        log.debug("toggleDialog");
        if (isVisible()) {
            hideDialog();
        }
        else {
            if (domainObject != null) {
                render();
            }
        }
    }

    public void hideDialog() {
        log.debug("hideDialog");
        setVisible(false);
    }

    public void setObjectAndToggleDialog(DomainObject domainObject, ResultDescriptor resultDescriptor, String typeName) {
        log.debug("setObjectAndToggleDialog({})",domainObject);
        setObjectAndToggleDialog(domainObject, resultDescriptor, typeName, true);
    }

    public void setObject(DomainObject domainObject, ResultDescriptor resultDescriptor, String typeName) {
        log.debug("setObject({})",domainObject);
        setObjectAndToggleDialog(domainObject, resultDescriptor, typeName, false);
    }

    public void setObjectAndToggleDialog(final DomainObject domainObject, ResultDescriptor resultDescriptor, String typeName, final boolean toggle) {
        this.domainObject = domainObject;
        if (domainObject == null) {
            dirtyEntityFor3D = false;
            if (toggle) {
                toggleDialog();
            }
            return;
        }
        else {
            log.debug("HUD: entity type is {}", domainObject.getType());
        }

        boolean starting = toggle && !isVisible();
        
        ResultDescriptor currResult = starting?resultDescriptor:resultButton.getResultDescriptor();
        String currImageType  = starting?typeName:typeButton.getImageType();
                
        if (currResult==null) {
            currResult = ResultDescriptor.LATEST;
        }
        
        if (currImageType==null) {
            currImageType = FileType.SignalMip.name();
        }
        
        resultButton.setResultDescriptor(currResult);
        resultButton.populate(domainObject);
        
        typeButton.setResultDescriptor(currResult);
        typeButton.setImageType(currImageType);
        typeButton.populate(domainObject);
        
        set3dModeEnabled(getFast3dFile()!=null);

        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            fileProvider = DomainModelViewUtils.getResult(sample, resultButton.getResultDescriptor());
        }
        else if (domainObject instanceof HasFiles) {
            fileProvider = (HasFiles)domainObject;
        }
        
        final String imagePath = DomainUtils.getFilepath(fileProvider, typeButton.getImageType());
        if (imagePath == null) {
            log.info("No image path for {}:{}", domainObject.getName(), domainObject.getId());
            previewLabel.setIcon(new MissingIcon());
            setTitle(domainObject.getName());
        }
        else {
            
            SimpleWorker worker = new SimpleWorker() {

                private BufferedImage image = null;
                
                @Override
                protected void doStuff() throws Exception {
                    
                    ImageCache ic = SessionMgr.getBrowser().getImageCache();
                    if (ic != null) {
                        image = ic.get(imagePath);
                    }

                    // Ensure we have an image and that it is cached.
                    if (image == null) {
                        log.info("Must load image.");
                        final File imageFile = SessionMgr.getCachedFile(imagePath, false);
                        if (imageFile != null) {
                            image = Utils.readImage(imageFile.getAbsolutePath());
                            if (ic != null) {
                                ic.put(imagePath, image);
                            }
                        }
                    }

                    // No image loaded or cached.  Do nada.
                    if (image == null) {
                        log.info("No image read for {}:{}", domainObject.getId(), imagePath);
                    }

                }

                @Override
                protected void hadSuccess() {
                    
                    if (image!=null) {
                        previewLabel.setIcon(image == null ? null : new ImageIcon(image));
                        dirtyEntityFor3D = true;
                        if (render3DCheckbox != null) {
                            render3DCheckbox.setSelected(false);
                            hud3DController.entityUpdate();
                        }
                        setAllColorsOn();
                        setTitle(domainObject.getName());
                        
                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        int width = (int)Math.round((double)screenSize.width*0.9);
                        int height = (int)Math.round((double)screenSize.height*0.9);
                        width = Math.min(image.getWidth()+50, width);
                        height = Math.min(image.getHeight()+100, height);
                        setPreferredSize(new Dimension(width, height));
                        setSize(new Dimension(width, height));
                    }
                    
                    if (toggle) {
                        toggleDialog();
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
                    
            worker.execute();
            
        }
        mip3d.repaint();
    }
    
    public String getFast3dFile() {
        return DomainUtils.getFilepath(fileProvider, FileType.FastStack);
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

    /**
     * This is a controller-callback method to allow or disallow the user from attempting to switch
     * to 3D mode.
     *
     * @param flag T=allow; F=disallow
     */
    public void set3dModeEnabled(boolean flag) {
        if (render3DCheckbox != null) {
            render3DCheckbox.setEnabled(flag);
            if (previewLabel.getIcon() == null) {
                // In this case, no 2D image exists, and the checkbox for 3D is locked at true.
                render3DCheckbox.setEnabled(false);
                render3DCheckbox.setSelected(true);
            }
            else {
                // In this case, 2D exists.  Need to reset the checkbox for 2D use, for now.
                render3DCheckbox.setSelected(false);
            }
        }
    }

    public void handleRenderSelection() {
        boolean is3D = shouldRender3D();
        if (is3D) {
            renderIn3D();
        }
        else {
            this.remove(mip3d);
            this.add(scrollPane, BorderLayout.CENTER);
        }
        rgbMenu.setEnabled(is3D);
        this.validate();
        this.repaint();
    }
    
    private void render() {
        if (shouldRender3D()) {
            renderIn3D();
        }
        packAndShow();
    }

    @Override
    protected void packAndShow() {
        SwingUtilities.updateComponentTreeUI(this);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void renderIn3D() {
        this.remove(scrollPane);
        if (dirtyEntityFor3D) {
            try {
                if (hud3DController != null) {
                    hud3DController.setUiBusyMode();
                    hud3DController.load3d();
                    dirtyEntityFor3D = hud3DController.isDirty();
                }

            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load 3D image.");
                log.error("Failed to load 3D image", ex);
                set3dModeEnabled(false);
                handleRenderSelection();

            }
        }
        else {
            if (hud3DController != null) {
                hud3DController.set3dWidget();
            }
        }
    }

    private void setAllColorsOn() {
        for (Component component : rgbMenu.getMenuComponents()) {
            ((JCheckBoxMenuItem) component).setSelected(true);
        }
        mip3d.toggleRGBValue(COLOR_CHANNEL.RED.ordinal(), true);
        mip3d.toggleRGBValue(COLOR_CHANNEL.GREEN.ordinal(), true);
        mip3d.toggleRGBValue(COLOR_CHANNEL.BLUE.ordinal(), true);
    }

    private void init3dGui() {
        if (!Mip3d.isAvailable()) {
            log.error("Cannot initialize 3D HUD because 3D MIP viewer is not available");
            return;
        }
        try {
            mip3d = new Mip3d();
            mip3d.setResetFirstRedraw(true);
            rgbMenu.setFocusable(false);
            rgbMenu.setRequestFocusEnabled(false);
            JMenuBar menuBar = new JMenuBar();
            menuBar.setFocusable(false);
            menuBar.setRequestFocusEnabled(false);
            JCheckBoxMenuItem redButton = new JCheckBoxMenuItem("Red");
            redButton.setSelected(true);
            redButton.setFocusable(false);
            redButton.setRequestFocusEnabled(false);
            redButton.addActionListener(new MyButtonActionListener(COLOR_CHANNEL.RED));
            rgbMenu.add(redButton);
            JCheckBoxMenuItem blueButton = new JCheckBoxMenuItem("Blue");
            blueButton.setSelected(true);
            blueButton.addActionListener(new MyButtonActionListener(COLOR_CHANNEL.BLUE));
            blueButton.setFocusable(false);
            blueButton.setRequestFocusEnabled(false);
            rgbMenu.add(blueButton);
            JCheckBoxMenuItem greenButton = new JCheckBoxMenuItem("Green");
            greenButton.setSelected(true);
            greenButton.addActionListener(new MyButtonActionListener(COLOR_CHANNEL.GREEN));
            greenButton.setFocusable(false);
            greenButton.setRequestFocusEnabled(false);
            rgbMenu.add(greenButton);
            rgbMenu.setEnabled(false);
            menuBar.add(rgbMenu);

            resultButton = new ResultSelectionButton() {
                @Override
                protected void resultChanged(ResultDescriptor resultDescriptor) {
                    setObjectAndToggleDialog(domainObject, resultDescriptor, typeButton.getImageType(), false);
                }
            };
            typeButton = new ImageTypeSelectionButton() {
                @Override
                protected void imageTypeChanged(String typeName) {
                    setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeName, false);
                }
            };

            JPanel leftSidePanel = new JPanel();
            leftSidePanel.setLayout(new FlowLayout());
            leftSidePanel.add(menuBar);
            leftSidePanel.setFocusable(false);
            leftSidePanel.setRequestFocusEnabled(false);
            leftSidePanel.add(resultButton);
            leftSidePanel.add(typeButton);
            
            hud3DController = new Hud3DController(this, mip3d);
            render3DCheckbox = new JCheckBox(THREE_D_CONTROL);
            render3DCheckbox.setSelected(false); // Always startup as false.
            render3DCheckbox.addActionListener(hud3DController);
            render3DCheckbox.setFont(render3DCheckbox.getFont().deriveFont(9.0f));
            render3DCheckbox.setBorderPainted(false);
            render3DCheckbox.setActionCommand(THREE_D_CONTROL);
            render3DCheckbox.setFocusable(false);
            render3DCheckbox.setRequestFocusEnabled(false);

            JPanel rightSidePanel = new JPanel();
            rightSidePanel.setLayout(new FlowLayout());
            rightSidePanel.add(menuBar);
            rightSidePanel.setFocusable(false);
            rightSidePanel.setRequestFocusEnabled(false);
            rightSidePanel.add(render3DCheckbox);

            JPanel menuLikePanel = new JPanel();
            menuLikePanel.setFocusable(false);
            menuLikePanel.setRequestFocusEnabled(false);
            menuLikePanel.setLayout(new BorderLayout());
            menuLikePanel.add(leftSidePanel, BorderLayout.WEST);
            menuLikePanel.add(rightSidePanel, BorderLayout.EAST);
            add(menuLikePanel, BorderLayout.NORTH);
        }
        catch (Exception ex) {
            // Turn off the 3d capability if exception.
            render3DCheckbox = null;
            log.error("Error initializing 3D HUD", ex);
        }
    }

    private boolean shouldRender3D() {
        boolean rtnVal = render3DCheckbox != null && render3DCheckbox.isEnabled() && render3DCheckbox.isSelected();
        if (!rtnVal) {
            if (hud3DController != null && hud3DController.is3DReady() && (this.previewLabel.getIcon() == null)) {
                rtnVal = true;
            }
        }
        return rtnVal;
    }

    private class MyButtonActionListener implements ActionListener {

        private COLOR_CHANNEL myChannel;

        public MyButtonActionListener(COLOR_CHANNEL channel) {
            myChannel = channel;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            mip3d.toggleRGBValue(myChannel.ordinal(), ((JCheckBoxMenuItem) actionEvent.getSource()).isSelected());
            mip3d.repaint();
        }
    }
}
