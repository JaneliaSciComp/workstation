package org.janelia.workstation.browser.gui.hud;

import org.apache.commons.io.FilenameUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromImageAction;
import org.janelia.workstation.browser.gui.colordepth.CreateMaskFromSampleAction;
import org.janelia.workstation.browser.gui.support.ImageTypeSelectionButton;
import org.janelia.workstation.browser.gui.support.ResultSelectionButton;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.MissingIcon;
import org.janelia.workstation.core.api.FileMgr;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.janelia.workstation.core.model.descriptors.DescriptorUtils;
import org.janelia.workstation.core.util.ImageCache;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * A persistent heads-up display for a synchronized image.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 * @author fosterl
 */
public class Hud extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(Hud.class);

    // Input Handling
    private final Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final Cursor hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Point pp = new Point();
    private KeyListener keyListener;
    
    // GUI
    private final JScrollPane scrollPane;
    private final JLabel previewLabel;
    private final ResultSelectionButton resultButton;
    private final ImageTypeSelectionButton typeButton;
    private final JButton colorDepthMaskButton;
    private final JPanel menuLikePanel;

    // Current state
    private boolean resetPosition = true;
    private DomainObject domainObject;
    private HasFiles fileProvider;
    private String title;
    private FileType imageType;

    public static boolean isInitialized() {
        return instance != null;
    }

    private static Hud instance;
    public static Hud getSingletonInstance() {
        if (instance == null) {
            instance = new Hud();
        }
        return instance;
    }
    
    private Hud() {

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

        colorDepthMaskButton = new JButton("Create Mask for Color Depth Search");
        colorDepthMaskButton.setVisible(false);
        colorDepthMaskButton.setFocusable(false);
        colorDepthMaskButton.setRequestFocusEnabled(false);
        colorDepthMaskButton.addActionListener(e -> {
            if (domainObject instanceof Sample) {
                CreateMaskFromSampleAction action = new CreateMaskFromSampleAction((Sample) domainObject, resultButton.getResultDescriptor(), imageType.name());
                hideDialog();
                action.actionPerformed(e);
            }
            else if (domainObject instanceof ColorDepthImage) {
                CreateMaskFromImageAction action = new CreateMaskFromImageAction((ColorDepthImage) domainObject);
                hideDialog();
                action.actionPerformed(e);
            }
            else if (domainObject instanceof ColorDepthMask) {
                CreateMaskFromImageAction action = new CreateMaskFromImageAction((ColorDepthMask) domainObject);
                hideDialog();
                action.actionPerformed(e);
            }
            else {
                throw new IllegalStateException("Cannot extract color depth MIP from "+domainObject);
            }
        });

        JPanel leftSidePanel = new JPanel();
        leftSidePanel.setLayout(new FlowLayout());
        leftSidePanel.setFocusable(false);
        leftSidePanel.setRequestFocusEnabled(false);
        leftSidePanel.add(resultButton);
        leftSidePanel.add(typeButton);
        leftSidePanel.add(colorDepthMaskButton);

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
            colorDepthMaskButton.setVisible(
                               imageType == FileType.ColorDepthMip1
                            || imageType == FileType.ColorDepthMip2
                            || imageType == FileType.ColorDepthMip3
                            || imageType == FileType.ColorDepthMip4);
        }
        else if (domainObject instanceof HasFiles) {
            fileProvider = (HasFiles)domainObject;
            colorDepthMaskButton.setVisible(domainObject instanceof ColorDepthImage || domainObject instanceof ColorDepthMask);
        }

        log.info("Using file provider: {}", fileProvider);
        final String imagePath = DomainUtils.getFilepath(fileProvider, typeButton.getImageTypeName());
        this.title = domainObject.getName();

        setObjectAndToggleDialog(imagePath, toggle, overrideSettings);
    }

    /**
     * Display a file from a HasFiles interface, with UI to select the file type.
     * @param hasFiles
     * @param typeName
     * @param toggle
     * @param overrideSettings
     */
    public void setObjectAndToggleDialog(final HasFiles hasFiles, String typeName,
                                         final boolean toggle, boolean overrideSettings) {

        log.info("setObjectAndToggleDialog - hasFiles, typeName:{}, toggle:{}, overrideSettings:{}",typeName,toggle,overrideSettings);

        this.fileProvider = hasFiles;
        log.info("Using file provider: {}", fileProvider);
        final String imagePath = DomainUtils.getFilepath(fileProvider, typeButton.getImageTypeName());

        String currImageType  = (overrideSettings && typeName!=null) ? typeName : typeButton.getImageTypeName();
        if (currImageType==null) {
            currImageType = FileType.FirstAvailable2d.name();
        }

        typeButton.setResultDescriptor(null);
        typeButton.setImageTypeName(currImageType);
        typeButton.populate(domainObject);

        this.imageType = FileType.valueOf(currImageType);

        resultButton.setVisible(false);
        typeButton.setVisible(true);
        setObjectAndToggleDialog(imagePath, toggle, overrideSettings);
    }

    /**
     * Display just an image with no UI.
     * @param filepath
     * @param toggle
     * @param overrideSettings
     */
    public void setFilepathAndToggleDialog(String filepath, String title, final boolean toggle, boolean overrideSettings) {
        log.info("setObjectAndToggleDialog({},title,toggle={},overrideSettings={})",filepath,title,toggle,overrideSettings);
        if (title!=null) {
            this.title = title;
        }
        else if (filepath!=null) {
            this.title = new File(filepath).getName();
        }
        else {
            this.title = "";
        }
        resultButton.setVisible(false);
        typeButton.setVisible(false);
        setObjectAndToggleDialog(filepath, toggle, overrideSettings);
    }
    
    private void setObjectAndToggleDialog(String filepath, final boolean toggle, boolean overrideSettings) {
        log.info("setObjectAndToggleDialog({},toggle={},overrideSettings={})",filepath,toggle,overrideSettings);
        
        if (filepath == null) {
            log.info("No image path for {} ({})", title, typeButton.getImageTypeName());
            previewLabel.setIcon(new MissingIcon());

            setTitle(title);

            if (toggle) {
                toggleDialog();
            }
            else {
                pack();
            }
        }
        else {
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
                        try {
                            log.debug("Must load image.");
                            try (InputStream imageStream = FileMgr.getFileMgr().openFileInputStream(filepath, false)) {
                                image = Utils.readImageFromInputStream(imageStream, FilenameUtils.getExtension(filepath));
                            }
                            if (ic != null) {
                                ic.put(filepath, image);
                            }
                        }
                        catch (FileNotFoundException e) {
                            log.debug("Could not find file: "+filepath, e);
                        }
                    }

                    // No image loaded or cached.  Do nada.
                    if (image == null) {
                        log.info("No image read for {}", filepath);
                    }

                }

                @Override
                protected void hadSuccess() {
                    setTitle(title);

                    int imageWidth, imageHeight;
                    if (image != null) {
                        previewLabel.setIcon(new ImageIcon(image));
                        imageWidth = image.getWidth();
                        imageHeight = image.getHeight();
                    }
                    else {
                        MissingIcon icon = new MissingIcon();
                        previewLabel.setIcon(icon);
                        imageWidth = icon.getIconWidth();
                        imageHeight = icon.getIconHeight();
                    }

                    // Pack to get sizes
                    pack();

                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
                    log.trace("Got screen insets: {}", scnMax);
                    // Available size of the screen
                    int width = screenSize.width - scnMax.left - scnMax.right;
                    int height = screenSize.height - scnMax.bottom - scnMax.top;

                    // Use less than available, just in case there are borders or padding
                    width = (int)Math.round((double)width * 0.98);
                    height = (int)Math.round((double)height * 0.98);
                    log.info("Available screen size: {}x{}", width, height);

                    int padding = 8;
                    int windowTitleHeight = 35;
                    log.info("Image size: {}x{}", imageWidth, imageHeight);
                    log.debug("  windowTitleHeight: {}", windowTitleHeight);
                    log.debug("  menuLikePanel height: {}", menuLikePanel.getPreferredSize().height);

                    int availableWidth = width - padding;
                    int availableHeight = height - padding - windowTitleHeight - menuLikePanel.getPreferredSize().height;
                    log.debug("Available image size: {}x{}", availableWidth, availableHeight);

                    int scrollPaneWidth = Math.min(imageWidth + padding, availableWidth);
                    int scrollPaneHeight = Math.min(imageHeight + padding, availableHeight);

                    Dimension currentSize = scrollPane.getPreferredSize();
                    if (currentSize.width != scrollPaneWidth || currentSize.height != scrollPaneHeight) {
                        resetPosition = true;
                    }

                    log.info("Setting scroll pane size: {}x{}", scrollPaneWidth, scrollPaneHeight);
                    scrollPane.setPreferredSize(new Dimension(scrollPaneWidth, scrollPaneHeight));

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
}
