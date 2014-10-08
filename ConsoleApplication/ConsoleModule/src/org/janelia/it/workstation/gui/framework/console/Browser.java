package org.janelia.it.workstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.AnnotationSessionPropertyDialog;
import org.janelia.it.workstation.gui.dialogs.DataSetListDialog;
import org.janelia.it.workstation.gui.dialogs.GiantFiberSearchDialog;
import org.janelia.it.workstation.gui.dialogs.ImportDialog;
import org.janelia.it.workstation.gui.dialogs.MAASearchDialog;
import org.janelia.it.workstation.gui.dialogs.MaskSearchDialog;
import org.janelia.it.workstation.gui.dialogs.PatternSearchDialog;
import org.janelia.it.workstation.gui.dialogs.RunNeuronSeparationDialog;
import org.janelia.it.workstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.workstation.gui.dialogs.search.GeneralSearchDialog;
import org.janelia.it.workstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.workstation.gui.framework.outline.EntityDetailsOutline;
import org.janelia.it.workstation.gui.framework.outline.EntityOutline;
import org.janelia.it.workstation.gui.framework.outline.EntityRootComparator;
import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.outline.SessionOutline;
import org.janelia.it.workstation.gui.framework.outline.VerticalPanelPicker;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModelListenerAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.workstation.gui.framework.viewer.ImageCache;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.FreeMemoryWatcher;
import org.janelia.it.workstation.shared.util.PrintableComponent;
import org.janelia.it.workstation.shared.util.PrintableImage;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:29 PM
 */
public class Browser implements Cloneable {

    private static final Logger log = LoggerFactory.getLogger(Browser.class);

    private static String BROWSER_POSITION = "BROWSER_POSITION_ON_SCREEN";
    public static String SEARCH_HISTORY = "SEARCH_HISTORY";
    private static String VIEWERS_LINKED = "Browser.ViewersLinked";

    // Used by printing mechanism to ensure capacity.
    public static final String VIEW_OUTLINES = "Outlines Section";
    public static final String VIEW_ONTOLOGY = "Ontology Section";

    public static final String OUTLINE_ONTOLOGY = "Ontology";
    public static final String OUTLINE_LAYERS = "Layers";
    public static final String OUTLINE_SPLIT_PICKER = "Split Picking Tool";

    private static String MEMORY_EXCEEDED_PRT_SCR_MSG = "Insufficient memory to print screen";
    private static String MEMORY_EXCEEDED_ADVISORY = "Low Memory";
    private static int RGB_TYPE_BYTES_PER_PIXEL = 4;
    private static int PRINT_OVERHEAD_SIZE = 1000000;

    private JPanel allPanelsView = new JPanel();
    private JPanel collapsedOutlineView = new JPanel();
    private JPanel mainPanel = new JPanel();
    private ViewerManager viewerManager;
    private final ImageCache imageCache = new ImageCache();
    private CardLayout layout = new CardLayout();
    private SessionModelListener modelListener = new MySessionModelListener();

    private BrowserModel browserModel;
    private SessionOutline sessionOutline;
    private EntityOutline entityOutline;
    private EntityDetailsOutline entityDetailsOutline;
    private ToolsMenuModifier toolsMenuModifier;

    private VerticalPanelPicker rightPanel;
    private OntologyOutline ontologyOutline;

    private AnnotationSessionPropertyDialog annotationSessionPropertyPanel;
    private ImportDialog importDialog;
    private RunNeuronSeparationDialog runNeuronSeparationDialog;
    private SearchConfiguration generalSearchConfig;
    private GeneralSearchDialog generalSearchDialog;
    private PatternSearchDialog patternSearchDialog;
    private GiantFiberSearchDialog giantFiberSearchDialog;
    private ScreenEvaluationDialog screenEvaluationDialog;
    private MAASearchDialog maaSearchDialog;
    private DataSetListDialog dataSetListDialog;
    private StatusBar statusBar = new StatusBar();
    private ImageIcon browserImageIcon;
    private Image iconImage;
    private PageFormat pageFormat;
    private MaskSearchDialog arbitraryMaskSearchDialog;

    /**
     * Center Window, use passed realEstatePercent (0-1.0, where 1.0 is 100% of the screen)
     */
    public Browser(float realEstatePercent, BrowserModel browserModel) {
        try {
            jbInit(browserModel);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    /**
     * Use given coordinates of the top left point and passed realEstatePercent (0-1.0).
     * THis constructor is used only by the clone method
     */
    public Browser(BrowserModel browserModel) {
        try {
            jbInit(browserModel);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

    private void jbInit(BrowserModel browserModel) throws Exception {

        // Initialize workspace
        ModelMgr.getModelMgr().init();
        
        viewerManager = new ViewerManager();

        boolean isViewersLinked = false;
        SessionMgr.getSessionMgr().setModelProperty(VIEWERS_LINKED, isViewersLinked);
        viewerManager.setIsViewersLinked(isViewersLinked);

        Object useFreeProperty = SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY);
        if (null != useFreeProperty && useFreeProperty instanceof Boolean) {
            useFreeMemoryViewer((Boolean) useFreeProperty);
        }
        else {
            useFreeMemoryViewer(false);
        }

        this.browserModel = browserModel;
        browserModel.addBrowserModelListener(new BrowserModelObserver());
        SessionMgr.getSessionMgr().addSessionModelListener(modelListener);

        sessionOutline = new SessionOutline(SessionMgr.getMainFrame());

        entityOutline = new EntityOutline() {
            @Override
            public List<Entity> loadRootList() throws Exception {
            	return ModelMgr.getModelMgr().getWorkspaces();
            }
        };

        entityDetailsOutline = new EntityDetailsOutline();

        ontologyOutline = new OntologyOutline() {
            @Override
            public List<Entity> loadRootList() throws Exception {
                List<Entity> roots = ModelMgr.getModelMgr().getOntologyRootEntities();
                Collections.sort(roots, new EntityRootComparator());
                return roots;
            }
        };

        annotationSessionPropertyPanel = new AnnotationSessionPropertyDialog(entityOutline, ontologyOutline);
        importDialog = new ImportDialog("Import Files");
        runNeuronSeparationDialog = new RunNeuronSeparationDialog();

        generalSearchConfig = new SearchConfiguration();
        generalSearchConfig.load();
        generalSearchDialog = new GeneralSearchDialog(generalSearchConfig);

        List<String> searchHistory = (List<String>) SessionMgr.getSessionMgr().getModelProperty(SEARCH_HISTORY);
        generalSearchDialog.setSearchHistory(searchHistory);

        patternSearchDialog = new PatternSearchDialog();
        giantFiberSearchDialog = new GiantFiberSearchDialog();
        arbitraryMaskSearchDialog = new MaskSearchDialog();
        screenEvaluationDialog = new ScreenEvaluationDialog();
        maaSearchDialog = new MAASearchDialog(this);
        dataSetListDialog = new DataSetListDialog();

        ontologyOutline.setPreferredSize(new Dimension());

        BrowserPosition consolePosition = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);
        if (null == consolePosition) {
            consolePosition = resetBrowserPosition();
        }

        // Collect the final components
        mainPanel.setLayout(layout);
        allPanelsView.setLayout(new BorderLayout());
        mainPanel.add(allPanelsView, "Regular");
        collapsedOutlineView.setLayout(new BorderLayout());
        mainPanel.add(collapsedOutlineView, "Collapsed FileOutline");

        // Run this later so that the Browser has finished initializing by the time it runs
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                entityDetailsOutline.activate();
                entityOutline.activate();
                // Ontology outline is activated by setting the perspective:
                setPerspective(Perspective.ImageBrowser);
            }
        });
    }

    public JComponent getMainComponent() {
        return viewerManager.getViewerContainer();
    }

    /**
     * Print the screen state by first creating a buffered image.
     */
    public void printBrowser() {
        // For now, use whole thing.
        JFrame frame = SessionMgr.getMainFrame();

        // Ensure sufficient resources.
        long requiredSize = (long) (RGB_TYPE_BYTES_PER_PIXEL * frame.getWidth() * frame.getHeight()) + (long) PRINT_OVERHEAD_SIZE;

        if (requiredSize > FreeMemoryWatcher.getFreeMemoryWatcher().getFreeMemory()) {
            JOptionPane.showMessageDialog(frame, MEMORY_EXCEEDED_PRT_SCR_MSG, MEMORY_EXCEEDED_ADVISORY, JOptionPane.ERROR_MESSAGE);

            return;
        } // Memory capacity WOULD be exceeded.

        PrinterJob printJob = PrinterJob.getPrinterJob();

        if (pageFormat == null) {
            pageFormat = new PageFormat();
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
            pageFormat = printJob.validatePage(pageFormat);
        } // Must create a page format.

        if (printJob.printDialog()) {
            // Get a buffered image of the frame.
            BufferedImage bufferedImage = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            frame.paint(graphics);

            // Wrap buffered image in a frame, and print that.
            PrintableImage printableImage = new PrintableImage(bufferedImage);
            printJob.setPrintable(printableImage, pageFormat);

            try {
                printJob.print();
            } // Print the job.
            catch (Exception ex) {
                ex.printStackTrace();
                //SessionMgr.getSessionMgr().handleException(ex);
            } // Got exception.

            printableImage.setVisible(false);
            printableImage = null;
        } // User says: "Go"
    } // End method: printBrowser

    public void printEditor(boolean master) {
        PrinterJob printJob = PrinterJob.getPrinterJob();

        if (pageFormat == null) {
            pageFormat = new PageFormat();
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
        }

        Component comp = SessionMgr.getMainFrame();

        if (printJob.printDialog()) {
            printJob.setPrintable(new PrintableComponent(comp), pageFormat);
            try {
                printJob.print();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @return BrowserModel The browserModel for this instance of the console.
     */
    public BrowserModel getBrowserModel() {
        return browserModel;
    }

    public void setIconImage(Image image) {
        this.iconImage = image;
    }

    public Image getIconImage() {
        return iconImage;
    }

    public void setBrowserImageIcon(ImageIcon _browserImageIcon) {
        if (_browserImageIcon != null) {
            this.browserImageIcon = _browserImageIcon;
            this.setIconImage(browserImageIcon.getImage());
        }
    }

    private void useFreeMemoryViewer(boolean use) {
        statusBar.useFreeMemoryViewer(false);
    }

    public MaskSearchDialog getMaskSearchDialog() {
        return arbitraryMaskSearchDialog;
    }

    public void supportMenuProcessing() {
        toolsMenuModifier = new ToolsMenuModifier();
        toolsMenuModifier.rebuildMenu();
        new CredentialSynchronizer().synchronize(this);
    }

    private class BrowserModelObserver extends BrowserModelListenerAdapter {

        @Override
        public void browserCurrentSelectionChanged(Entity newSelection) {
            if (newSelection != null) {
                statusBar.setDescription("Current Selection: " + newSelection.getName());
            }
            else {
                statusBar.setDescription("");
            }
        }

        @Override
        public void browserClosing() {
            SessionMgr.getSessionMgr().removeSessionModelListener(modelListener);

            BrowserPosition position = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);

            if (position == null) {
                position = new BrowserPosition();
            }

            SessionMgr.getSessionMgr().setModelProperty(BROWSER_POSITION, position);
            if (generalSearchDialog != null) {
                SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY, generalSearchDialog.getSearchHistory());
            }

        }
    }

    class MySessionModelListener implements SessionModelListener {

        public void browserAdded(BrowserModel browserModel) {
        }

        public void browserRemoved(BrowserModel browserModel) {
        }

        public void sessionWillExit() {
        }

        public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
            if (key.equals(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY)) {
                useFreeMemoryViewer(((Boolean) newValue).booleanValue());
            }
            log.debug("Model change detected for " + key + ", saving user settings");
            SessionMgr.getSessionMgr().saveUserSettings();
        }

    }

    public ViewerManager getViewerManager() {
        return viewerManager;
    }

    public EntityOutline getEntityOutline() {
        return entityOutline;
    }

    public EntityDetailsOutline getEntityDetailsOutline() {
        return entityDetailsOutline;
    }

    public OntologyOutline getOntologyOutline() {
        return ontologyOutline;
    }

    public void selectRightPanel(String panelName) {
        rightPanel.showPanel(panelName);
    }

    public SessionOutline getAnnotationSessionOutline() {
        return sessionOutline;
    }

    public AnnotationSessionPropertyDialog getAnnotationSessionPropertyDialog() {
        return annotationSessionPropertyPanel;
    }

    public RunNeuronSeparationDialog getRunNeuronSeparationDialog() {
        return runNeuronSeparationDialog;
    }

    public ImportDialog getImportDialog() {
        return importDialog;
    }

    public PatternSearchDialog getPatternSearchDialog() {
        return patternSearchDialog;
    }

    public GiantFiberSearchDialog getGiantFiberSearchDialog() {
        return giantFiberSearchDialog;
    }

    public ScreenEvaluationDialog getScreenEvaluationDialog() {
        return screenEvaluationDialog;
    }

    public MAASearchDialog getMAASearchDialog() {
        return maaSearchDialog;
    }

    public DataSetListDialog getDataSetListDialog() {
        return dataSetListDialog;
    }

    public SearchConfiguration getGeneralSearchConfig() {
        return generalSearchConfig;
    }

    public GeneralSearchDialog getGeneralSearchDialog() {
        return generalSearchDialog;
    }

    public void setPerspective(Perspective perspective) {
        log.info("Setting perspective: {}", perspective);
        switch (perspective) {
            case TaskMonitoring:
                openOntologyComponent();
                viewerManager.clearAllViewers();
                break;
            case LargeVolumeViewer:
                viewerManager.clearAllViewers();
                viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), LargeVolumeViewViewer.class);
                break;
            case ImageBrowser:
            default:
                openOntologyComponent();
                viewerManager.clearAllViewers();
                viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
        }
    }

    public BrowserPosition resetBrowserPosition() {

        BrowserPosition position = new BrowserPosition();
        position.setHorizontalLeftDividerLocation(400);
        position.setHorizontalRightDividerLocation(1100);
        position.setVerticalDividerLocation(800);

        int offsetY = 0;
        String lafName = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_LOOK_AND_FEEL);
        if (SystemInfo.isMac && lafName != null && lafName.contains("synthetica")) {
            offsetY = 20;
        }

        position.setBrowserLocation(new Point(0, offsetY));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        position.setScreenSize(screenSize);
        position.setBrowserSize(new Dimension(screenSize.width, screenSize.height - offsetY));

        SessionMgr.getSessionMgr().setModelProperty(BROWSER_POSITION, position);

        return position;
    }

    public boolean isViewersLinked() {
        return viewerManager.isViewersLinked();
    }

    public void setIsViewersLinked(boolean isViewersLinked) {
        viewerManager.setIsViewersLinked(isViewersLinked);
        SessionMgr.getSessionMgr().setModelProperty(VIEWERS_LINKED, isViewersLinked);
    }

    private void openOntologyComponent() {
        TopComponent win = WindowLocator.getByName(OntologyOutline.ONTOLOGY_COMPONENT_NAME);
        if (win != null && !win.isOpened()) {
            Mode propertiesMode = WindowManager.getDefault().findMode("properties");
            if (propertiesMode != null) {
                propertiesMode.dockInto(win);
            }
        }
    }

}
