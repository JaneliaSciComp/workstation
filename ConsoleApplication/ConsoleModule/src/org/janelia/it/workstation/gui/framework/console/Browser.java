package org.janelia.it.workstation.gui.framework.console;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.*;
import org.janelia.it.workstation.gui.dialogs.search.GeneralSearchDialog;
import org.janelia.it.workstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.workstation.gui.framework.outline.*;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.workstation.gui.framework.session_mgr.BrowserModelListenerAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.workstation.gui.framework.viewer.ImageCache;
import org.janelia.it.workstation.shared.util.FreeMemoryWatcher;
import org.janelia.it.workstation.shared.util.PrintableComponent;
import org.janelia.it.workstation.shared.util.PrintableImage;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Collections;
import java.util.List;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:29 PM
 */
public class Browser implements Cloneable {

    private static final Logger log = LoggerFactory.getLogger(Browser.class);

    private static final String BROWSER_POSITION = "BROWSER_POSITION_ON_SCREEN";
    public static final String SEARCH_HISTORY = "SEARCH_HISTORY";
    public static final String ADD_TO_ROOT_HISTORY = "ADD_TO_ROOT_HISTORY";
    private static final String VIEWERS_LINKED = "Browser.ViewersLinked";
    private static final String AUTO_SHARE_TEMPLATE = "Browser.AutoShareTemplate";

    // Used by printing mechanism to ensure capacity.
    public static final String VIEW_OUTLINES = "Outlines Section";
    public static final String VIEW_ONTOLOGY = "Ontology Section";

    public static final String OUTLINE_ONTOLOGY = "Ontology";
    public static final String OUTLINE_LAYERS = "Layers";
    public static final String OUTLINE_SPLIT_PICKER = "Split Picking Tool";

    private static final String MEMORY_EXCEEDED_PRT_SCR_MSG = "Insufficient memory to print screen";
    private static final String MEMORY_EXCEEDED_ADVISORY = "Low Memory";
    private static final int RGB_TYPE_BYTES_PER_PIXEL = 4;
    private static final int PRINT_OVERHEAD_SIZE = 1000000;

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
    
    private PermissionTemplate autoShareTemplate;
    private List<String> searchHistory;
    
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

    private void jbInit(BrowserModel browserModel) throws Exception {

        log.info("Initializing browser...");
        
        this.browserModel = browserModel;
        
        // Initialize workspace
        ModelMgr.getModelMgr().init();
        
        // Load model properties
        this.autoShareTemplate = (PermissionTemplate)SessionMgr.getSessionMgr().getModelProperty(AUTO_SHARE_TEMPLATE);
        this.searchHistory = (List<String>) SessionMgr.getSessionMgr().getModelProperty(SEARCH_HISTORY);
        
        this.viewerManager = new ViewerManager();

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

        browserModel.addBrowserModelListener(new BrowserModelObserver());
        SessionMgr.getSessionMgr().addSessionModelListener(modelListener);

        sessionOutline = new SessionOutline(SessionMgr.getMainFrame());

        entityOutline = new EntityOutline() {
            @Override
            public List<Entity> loadRootList() throws Exception {
            	List<Entity> workspaces = ModelMgr.getModelMgr().getWorkspaces();
                loadedWorkspaces(workspaces);
                return workspaces;
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
        
        //annotationSessionPropertyPanel = new AnnotationSessionPropertyDialog(entityOutline, ontologyOutline);
        
        ontologyOutline.setPreferredSize(new Dimension());

        resetBrowserPosition();
        
        // Collect the final components
        mainPanel.setLayout(layout);
        allPanelsView.setLayout(new BorderLayout());
        mainPanel.add(allPanelsView, "Regular");
        collapsedOutlineView.setLayout(new BorderLayout());
        mainPanel.add(collapsedOutlineView, "Collapsed FileOutline");

        resetView();
        
        log.info("Ready.");
    }
    
    /**
     * Once the workspaces are loaded, we can initialize other UI components.
     * @param workspaces 
     */
    private void loadedWorkspaces(List<Entity> workspaces) {
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                generalSearchConfig = new SearchConfiguration();
                generalSearchConfig.load();
            }

            @Override
            protected void hadSuccess() {
                generalSearchDialog = new GeneralSearchDialog(generalSearchConfig);
                importDialog = new ImportDialog("Import Files");
                patternSearchDialog = new PatternSearchDialog();
                giantFiberSearchDialog = new GiantFiberSearchDialog();
                arbitraryMaskSearchDialog = new MaskSearchDialog();
                screenEvaluationDialog = new ScreenEvaluationDialog();
                maaSearchDialog = new MAASearchDialog();
                dataSetListDialog = new DataSetListDialog();
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
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
            SessionMgr.getSessionMgr().setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
            SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY, searchHistory);

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

    public ImageCache getImageCache() {
        return imageCache;
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

    public void resetView() {
        //openOntologyComponent();
        viewerManager.clearAllViewers();
        viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
        entityDetailsOutline.showNothing();
    }

// TODO: this can probably be deleted, since it's not clear what function it serves that isn't provided by the default RCP behavior. 
//    private void openOntologyComponent() {
//        TopComponent win = WindowLocator.getByName(OntologyViewerTopComponent.COMPONENT_NAME);
//        if (win != null && !win.isOpened()) {
//            Mode propertiesMode = WindowManager.getDefault().findMode("properties");
//            if (propertiesMode != null) {
//                propertiesMode.dockInto(win);
//            }
//        }
//    }

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

    public PermissionTemplate getAutoShareTemplate() {
        return autoShareTemplate;
    }

    public void setAutoShareTemplate(PermissionTemplate autoShareTemplate) {
        this.autoShareTemplate = autoShareTemplate;
        SessionMgr.getSessionMgr().setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }

    public List<String> getSearchHistory() {
        log.trace("Returning current search history: {} ",searchHistory);
        return searchHistory;
    }

    public void setSearchHistory(List<String> searchHistory) {
        log.trace("Saving search history: {} ",searchHistory);
        this.searchHistory = searchHistory;
        SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY, searchHistory);
    }
}
