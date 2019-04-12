package org.janelia.it.workstation.browser.gui.hud;

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

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.support.ResultSelectionButton;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.model.descriptors.DescriptorUtils;
import org.janelia.it.workstation.browser.util.ImageCache;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.api.FileMgr;
import org.janelia.it.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.workstation.common.gui.support.MissingIcon;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A persistent heads-up display for a synchronized image.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 * @author fosterl
 */
public class Hud extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(Hud.class);

    public static final String THREE_D_CONTROL = "3D";

    private static Hud instance;
    
    private final Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final Cursor hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Point pp = new Point();
    
    // GUI
    private boolean dirtyEntityFor3D;
    private final JScrollPane scrollPane;
    private final JLabel previewLabel;
    private final ResultSelectionButton resultButton;
    private final ImageTypeSelectionButton typeButton;
    private final JMenu rgbMenu = new JMenu("RGB Controls");
    private final JPanel menuLikePanel;
    private JCheckBox render3DCheckbox;
    private Mip3d mip3d;
    private Hud3DController hud3DController;
    private KeyListener keyListener;

    // Current state
    private boolean firstShowing = true;
    private DomainObject domainObject;
    private HasFiles fileProvider;
    private String title;
    private FileType imageType;
    
    public enum COLOR_CHANNEL {
        RED,
        GREEN,
        BLUE
    }
    
    public static boolean isInitialized() {
        return instance != null;
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

        resultButton = new ResultSelectionButton() {
            @Override
            protected void resultChanged(ArtifactDescriptor resultDescriptor) {
                setObjectAndToggleDialog(domainObject, resultDescriptor, typeButton.getImageTypeName(), false, true);
            }
        };
        typeButton = new ImageTypeSelectionButton() {
            @Override
            protected void imageTypeChanged(FileType fileType) {
                setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), fileType.name(), false, true);
            }
        };


        JPanel leftSidePanel = new JPanel();
        leftSidePanel.setLayout(new FlowLayout());
        leftSidePanel.setFocusable(false);
        leftSidePanel.setRequestFocusEnabled(false);
        leftSidePanel.add(resultButton);
        leftSidePanel.add(typeButton);

        menuLikePanel = new JPanel();
        menuLikePanel.setFocusable(false);
        menuLikePanel.setRequestFocusEnabled(false);
        menuLikePanel.setLayout(new BorderLayout());
        menuLikePanel.add(leftSidePanel, BorderLayout.WEST);
        
        add(menuLikePanel, BorderLayout.NORTH);
        
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
            showDialog();
        }
    }

    public void showDialog() {
        log.debug("showDialog");
        if (shouldRender3D()) {
            renderIn3D();
        }
        packAndShow();
    }
    
    public void hideDialog() {
        log.debug("hideDialog");
        setVisible(false);
    }
    
    /**
     * Display a domain object with the possibility of switching between different views of it. 
     * @param domainObject
     * @param resultDescriptor
     * @param typeName
     * @param toggle
     * @param overrideSettings
     */
    public void setObjectAndToggleDialog(final DomainObject domainObject, ArtifactDescriptor resultDescriptor, String typeName, 
            final boolean toggle, boolean overrideSettings) {
        
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

        ArtifactDescriptor currResult = (overrideSettings && resultDescriptor!=null) ? resultDescriptor : resultButton.getResultDescriptor();
        String currImageType  = (overrideSettings && typeName!=null) ? typeName : typeButton.getImageTypeName();

        log.info("setObjectAndToggleDialog - name:{}, toggle:{}, currResult:{}, currImageType:{}",domainObject.getName(),toggle,currResult,currImageType);
        
        if (currResult==null) {
            currResult = ArtifactDescriptor.LATEST;
        }
        
        if (currImageType==null) {
            currImageType = FileType.SignalMip.name();
        }
        
        resultButton.setResultDescriptor(currResult);
        resultButton.populate(domainObject);
        
        typeButton.setResultDescriptor(currResult);
        typeButton.setImageTypeName(currImageType);
        typeButton.populate(domainObject);
        
        imageType = FileType.valueOf(currImageType);

        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            fileProvider = DescriptorUtils.getResult(sample, resultButton.getResultDescriptor());
        }
        else if (domainObject instanceof HasFiles) {
            fileProvider = (HasFiles)domainObject;
        }

        log.info("Using file provider: {}", fileProvider);
        
        final String imagePath = DomainUtils.getFilepath(fileProvider, typeButton.getImageTypeName());
        
        this.title = domainObject.getName();
        setObjectAndToggleDialog(imagePath, toggle, overrideSettings);
    }

    public void setFilepathAndToggleDialog(String imagePath, final boolean toggle, boolean overrideSettings) {
        setFilepathAndToggleDialog(imagePath, null, toggle, overrideSettings);
    }
    
    /**
     * Display an image in the lightbox viewer.
     * @param imagePath
     * @param toggle
     * @param overrideSettings
     */
    public void setFilepathAndToggleDialog(String imagePath, String title, final boolean toggle, boolean overrideSettings) {
        if (title!=null) {
            this.title = title;
        }
        else if (imagePath!=null) {
            this.title = new File(imagePath).getName();
        }
        else {
            this.title = "";
        }
        resultButton.setVisible(false);
        typeButton.setVisible(false);
        setObjectAndToggleDialog(imagePath, toggle, overrideSettings);
    }
    
    private void setObjectAndToggleDialog(String filepath, final boolean toggle, boolean overrideSettings) {
        log.info("setObjectAndToggleDialog({},toggle={},overrideSettings={})",filepath,toggle,overrideSettings);
        
        if (filepath == null) {
            log.info("No image path for {} ({})", title, typeButton.getImageTypeName());
            previewLabel.setIcon(new MissingIcon());

            if (render3DCheckbox != null) {
                render3DCheckbox.setEnabled(false);
                render3DCheckbox.setSelected(false);
            }

            setAllColorsOn();
            setTitle(title);

            if (toggle) {
                toggleDialog();
            }
        }
        else {
            log.info("fast3dFile={}", getFast3dFile());
            set3dModeEnabled(getFast3dFile()!=null);
            
            SimpleWorker worker = new SimpleWorker() {

                private BufferedImage image = null;
                
                @Override
                protected void doStuff() throws Exception {
                    
                    ImageCache ic = DataBrowserMgr.getDataBrowserMgr().getImageCache();
                    if (ic != null) {
                        image = ic.get(filepath);
                    }

                    // Ensure we have an image and that it is cached.
                    if (image == null) {
                        log.debug("Must load image.");
                        final File imageFile = FileMgr.getFileMgr().getFile(filepath, false);
                        if (imageFile != null) {
                            image = Utils.readImage(imageFile.getAbsolutePath());
                            if (ic != null) {
                                ic.put(filepath, image);
                            }
                        }
                    }

                    // No image loaded or cached.  Do nada.
                    if (image == null) {
                        log.info("No image read for {}", filepath);
                    }

                }

                @Override
                protected void hadSuccess() {
                    
                    if (image!=null) {
                        previewLabel.setIcon(image == null ? null : new ImageIcon(image));
                        dirtyEntityFor3D = true;
                        if (render3DCheckbox != null) {
                            render3DCheckbox.setSelected(false);
                            handleRenderSelection();
                        }
                        setAllColorsOn();
                        setTitle(title);
                        
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
                    FrameworkImplProvider.handleException(error);
                }
            };
                    
            worker.execute();
            
        }
        if (mip3d!=null) mip3d.repaint();
    }
    
    public String getFast3dFile() {
        
        FileType type = null;
        if (imageType==FileType.AllMip) {
            type = FileType.AllMovie;
        }
        else if (imageType==FileType.SignalMip) {
            type = FileType.SignalMovie;
        }
        else if (imageType==FileType.ReferenceMip) {
            type = FileType.ReferenceMovie;
        }
        else if (imageType==FileType.Signal1Mip || imageType==FileType.Signal2Mip || imageType==FileType.Signal3Mip) {
            type = FileType.SignalMovie;
        }
        else if (imageType==FileType.RefSignal1Mip || imageType==FileType.RefSignal2Mip || imageType==FileType.RefSignal3Mip) {
            type = FileType.AllMovie;
        }
        
        // Try the preferred movie
        String fastFile = type==null ? null : DomainUtils.getFilepath(fileProvider, type);
        if (fastFile==null) {
            // Try some other options
            fastFile = DomainUtils.getFilepath(fileProvider, FileType.AllMovie);
            if (fastFile==null) {
                fastFile = DomainUtils.getFilepath(fileProvider, FileType.FastStack);
            }
            else {
                // AllMovie sometimes contains an older file which has this bug:
                // Encountered MPEG file {...}_movie.mp4, which has 'dead planes' in Z.  Expected Z was 650.  Number of planes read 0.
                if (fastFile.endsWith("_movie.mp4")) {
                    // Hack to prevent this older data from loading and breaking the HUD
                    fastFile = null;
                }
            }
        }
        
        return fastFile;
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
        }
    }

    public void handleRenderSelection() {
        boolean is3D = shouldRender3D();
        if (is3D) {
            renderIn3D();
        }
        else {
            if (mip3d!=null) this.remove(mip3d);
            this.add(scrollPane, BorderLayout.CENTER);
        }
        rgbMenu.setEnabled(is3D);
        this.validate();
        this.repaint();
    }
    
    @Override
    protected void packAndShow() {
        SwingUtilities.updateComponentTreeUI(this);
        if (firstShowing) {
            setLocationRelativeTo(getParent());
            firstShowing = false;
        }
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
        if (mip3d!=null) {
            mip3d.toggleRGBValue(COLOR_CHANNEL.RED.ordinal(), true);
            mip3d.toggleRGBValue(COLOR_CHANNEL.GREEN.ordinal(), true);
            mip3d.toggleRGBValue(COLOR_CHANNEL.BLUE.ordinal(), true);
        }
    }

    private void init3dGui() {

        SimpleWorker worker = new SimpleWorker() {

            private boolean allow3d = false;
            
            @Override
            protected void doStuff() throws Exception { 
                // The isAvailable() check actually takes a second or two, so that's why we do it in a background thread.
                if (!Mip3d.isAvailable()) {
                    log.error("Cannot initialize 3D HUD because 3D MIP viewer is not available");
                    return;
                }
                mip3d = new Mip3d();
                mip3d.setResetFirstRedraw(true);
                hud3DController = new Hud3DController(Hud.this, mip3d);
                allow3d = true;
            }
            
            @Override
            protected void hadSuccess() {
                
                if (!allow3d) return;
                
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
                
                JPanel rightSidePanel = new JPanel();
                rightSidePanel.setLayout(new FlowLayout());
                rightSidePanel.setFocusable(false);
                rightSidePanel.setRequestFocusEnabled(false);
                rightSidePanel.add(menuBar);
                
                render3DCheckbox = new JCheckBox(THREE_D_CONTROL);
                render3DCheckbox.setSelected(false); // Always startup as false.
                render3DCheckbox.addActionListener(hud3DController);
                render3DCheckbox.setFont(render3DCheckbox.getFont().deriveFont(9.0f));
                render3DCheckbox.setBorderPainted(false);
                render3DCheckbox.setActionCommand(THREE_D_CONTROL);
                render3DCheckbox.setFocusable(false);
                render3DCheckbox.setRequestFocusEnabled(false);
                rightSidePanel.add(render3DCheckbox);
                
                menuLikePanel.add(rightSidePanel, BorderLayout.EAST);
                menuLikePanel.validate();
                menuLikePanel.repaint();
            }
            
            @Override
            protected void hadError(Throwable ex) {
                // Turn off the 3d capability if exception.
                render3DCheckbox = null;
                log.error("Error initializing 3D HUD", ex);
            }
        };
        worker.execute();
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
            if (mip3d!=null) {
                mip3d.toggleRGBValue(myChannel.ordinal(), ((JCheckBoxMenuItem) actionEvent.getSource()).isSelected());
                mip3d.repaint();
            }
        }
    }
}
