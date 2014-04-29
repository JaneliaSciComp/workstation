package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.*;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.GeneralSearchDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModelListenerAdapter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.framework.tool_manager.ToolMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ImageCache;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.SliceViewViewer;
import org.janelia.it.FlyWorkstation.gui.top_component.OntologyViewerTopComponent;
import org.janelia.it.FlyWorkstation.shared.util.FreeMemoryWatcher;
import org.janelia.it.FlyWorkstation.shared.util.PrintableComponent;
import org.janelia.it.FlyWorkstation.shared.util.PrintableImage;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.jacs.model.entity.Entity;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.Lookup.Template;
import org.openide.util.lookup.Lookups;
import org.openide.windows.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

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
    
    private static Class menuBarClass;
    private JSplitPane centerLeftHorizontalSplitPane;
    private JSplitPane centerRightHorizontalSplitPane;
    private JSplitPane leftVerticalSplitPane;
    
    private JPanel allPanelsView = new JPanel();
    private JPanel collapsedOutlineView = new JPanel();
    private JPanel mainPanel = new JPanel();
    private ViewerManager viewerManager;
    private final ImageCache imageCache = new ImageCache();
    private CardLayout layout = new CardLayout();
    private JMenuBar menuBar;
    private SessionModelListener modelListener = new MySessionModelListener();

    private float realEstatePercent = .2f;
    private BrowserModel browserModel;
    private BorderLayout borderLayout = new BorderLayout();
    private SessionOutline sessionOutline;
    private EntityOutline entityOutline;
    private EntityDetailsOutline entityDetailsOutline;
    private TaskOutline taskOutline;
    private ToolsMenuModifier toolsMenuModifier;

    private VerticalPanelPicker rightPanel;
    private OntologyOutline ontologyOutline;
    private SplitPickingPanel splitPickingPanel;
        
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
        // NO-Frame enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        
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
    public Browser(/* // NO-Frame int topLeftX, int topLeftY, Dimension size, */ BrowserModel browserModel) {
        // NO-Frame enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        try {
            jbInit(browserModel);
            // NO-Frame setLocation(topLeftX, topLeftY);
            // NO-Frame setSize(size);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    public ImageCache getImageCache() {
    	return imageCache;
    }

    private void jbInit(BrowserModel browserModel) throws Exception {

        viewerManager = new ViewerManager();
        
//        Boolean isViewersLinked = (Boolean)SessionMgr.getSessionMgr().getModelProperty(VIEWERS_LINKED);
//        if (isViewersLinked==null) {
//            isViewersLinked = false;
//            SessionMgr.getSessionMgr().setModelProperty(VIEWERS_LINKED, isViewersLinked);
//        }
        boolean isViewersLinked = false;
        SessionMgr.getSessionMgr().setModelProperty(VIEWERS_LINKED, isViewersLinked);
        viewerManager.setIsViewersLinked(isViewersLinked);
        
        Object useFreeProperty = SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY);
        if (null!=useFreeProperty && useFreeProperty instanceof Boolean) {
            useFreeMemoryViewer((Boolean)useFreeProperty);
        }
        else {
            useFreeMemoryViewer(false);
        }
        // NO-Frame getContentPane().setLayout(borderLayout);
        // NO-Frame setTitle("");

        this.browserModel = browserModel;
        browserModel.addBrowserModelListener(new BrowserModelObserver());
        SessionMgr.getSessionMgr().addSessionModelListener(modelListener);        
        
        sessionOutline = new SessionOutline(SessionMgr.getMainFrame());
		
        entityOutline = new EntityOutline() {
			@Override
			public List<Entity> loadRootList() throws Exception {
				List<Entity> roots = ModelMgr.getModelMgr().getCommonRootEntities();
				Collections.sort(roots, new EntityRootComparator());
				return roots;
			}
		};
		
		entityDetailsOutline = new EntityDetailsOutline();
		
        taskOutline = new TaskOutline(SessionMgr.getMainFrame());
        
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
        // NO-Frame else {
        // NO-Frame     setSize(consolePosition.getBrowserSize());
        // NO-Frame     setLocation(consolePosition.getBrowserLocation());
        // NO-Frame }
        
        splitPickingPanel = new SplitPickingPanel();
        
        rightPanel = new VerticalPanelPicker();
        //rightPanel.addPanel(OUTLINE_ONTOLOGY, Icons.getIcon("page.png"), "Displays an ontology for annotation", ontologyOutline);
        rightPanel.addPanel(OUTLINE_SPLIT_PICKER, Icons.getIcon("page_copy.png"), "Allows for simulation of flyline crosses", splitPickingPanel);
        
        
        Component rightComponent = rightPanel;

        centerRightHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, viewerManager.getViewerContainer(), rightComponent);
        centerRightHorizontalSplitPane.setMinimumSize(new Dimension(0, 0));
        centerRightHorizontalSplitPane.setDividerSize(10);
        centerRightHorizontalSplitPane.setOneTouchExpandable(true);
        centerRightHorizontalSplitPane.setDividerLocation(consolePosition.getHorizontalRightDividerLocation());
        centerRightHorizontalSplitPane.setBorder(BorderFactory.createEmptyBorder());

        centerLeftHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, leftVerticalSplitPane, centerRightHorizontalSplitPane);
        centerLeftHorizontalSplitPane.setMinimumSize(new Dimension(0, 0));
        centerLeftHorizontalSplitPane.setDividerSize(10);
        centerLeftHorizontalSplitPane.setOneTouchExpandable(true);
        centerLeftHorizontalSplitPane.setDividerLocation(consolePosition.getHorizontalLeftDividerLocation());
        centerLeftHorizontalSplitPane.setBorder(BorderFactory.createEmptyBorder());

        // Collect the final components
        mainPanel.setLayout(layout);
        allPanelsView.setLayout(new BorderLayout());
        //allPanelsView.add(centerLeftHorizontalSplitPane, BorderLayout.CENTER);
        // NO-Frame getContentPane().add(statusBar, BorderLayout.SOUTH);
        mainPanel.add(allPanelsView, "Regular");
        collapsedOutlineView.setLayout(new BorderLayout());
        mainPanel.add(collapsedOutlineView, "Collapsed FileOutline");
        // NO-Frame getContentPane().add(mainPanel, BorderLayout.CENTER);

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
        return centerLeftHorizontalSplitPane;
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
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
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

    public Object clone() {
        // NO-Frame java.awt.Point topLeft = this.getLocation();
        // NO-Frame Dimension size = this.getSize();
        BrowserModel newBrowserModel = (BrowserModel) this.browserModel.clone();
        Browser newBrowser = new Browser(newBrowserModel);
        // NO-Frame newBrowser.setTitle(getTitle());
        newBrowser.setBrowserImageIcon(browserImageIcon);
        //newBrowser.setVisible(true);

        return newBrowser;
    }
    
    public void setIconImage( Image image ) {
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

    /**
     * Overriden so we can exit on System Close
     * // NO-Frame 
    protected void processWindowEvent(WindowEvent e) {
        // NO-Frame super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            SessionMgr.getSessionMgr().removeBrowser(this);
        }
    }
    */

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
            // NO-Frame setVisible(false);
            SessionMgr.getSessionMgr().removeSessionModelListener(modelListener);
            
            BrowserPosition position = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);

            if (position == null) {
                position = new BrowserPosition();
            }

            position.setScreenSize(Toolkit.getDefaultToolkit().getScreenSize());
            // NO-Frame position.setBrowserSize(Browser.this.getSize());
            // NO-Frame position.setBrowserLocation(Browser.this.getLocation());
            position.setHorizontalLeftDividerLocation(centerLeftHorizontalSplitPane.getDividerLocation());
            position.setHorizontalRightDividerLocation(centerRightHorizontalSplitPane.getDividerLocation());
            if ( leftVerticalSplitPane != null ) {
                position.setVerticalDividerLocation(leftVerticalSplitPane.getDividerLocation());
            }
            
            SessionMgr.getSessionMgr().setModelProperty(BROWSER_POSITION, position);
            if ( generalSearchDialog != null )  {
                SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY, generalSearchDialog.getSearchHistory());
            }                
            
            // NO-Frame dispose();
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
            log.debug("Model change detected for "+key+", saving user settings");
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
    
    public SplitPickingPanel getSplitPickingPanel() {
        return splitPickingPanel;
    }

    public void setSplitPickingPanel(SplitPickingPanel splitPickingPanel) {
        this.splitPickingPanel = splitPickingPanel;
    }

    public SessionOutline getAnnotationSessionOutline() {
        return sessionOutline;
    }

    public TaskOutline getTaskOutline() {
		return taskOutline;
	}

	public AnnotationSessionPropertyDialog getAnnotationSessionPropertyDialog() {
        return annotationSessionPropertyPanel;
    }

    public RunNeuronSeparationDialog getRunNeuronSeparationDialog() {
		return runNeuronSeparationDialog;
	}

    public ImportDialog getImportDialog(){
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

    public void toggleViewComponentState(String viewComponentKey) {
        // TODO The layout needs to be much nicer.  See IntelliJ layouts, with perhaps the frame menu name still visible
        if (VIEW_OUTLINES.equals(viewComponentKey)) {
            centerLeftHorizontalSplitPane.getLeftComponent().setVisible(!centerLeftHorizontalSplitPane.getLeftComponent().isVisible());
            centerLeftHorizontalSplitPane.setDividerLocation(centerLeftHorizontalSplitPane.getLastDividerLocation());
        }
        else if (VIEW_ONTOLOGY.equals(viewComponentKey)) {
            centerRightHorizontalSplitPane.getRightComponent().setVisible(!centerRightHorizontalSplitPane.getRightComponent().isVisible());
            centerRightHorizontalSplitPane.setDividerLocation(centerRightHorizontalSplitPane.getLastDividerLocation());
        }
    }

    public JSplitPane getCenterRightHorizontalSplitPane() {
		return centerRightHorizontalSplitPane;
	}
    
    public void setPerspective(Perspective perspective) {
        log.info("Setting perspective: {}",perspective);
        switch (perspective) {
        case SplitPicker:
            selectRightPanel(OUTLINE_SPLIT_PICKER);
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
            break;
        case AnnotationSession:
            openOntologyComponent();
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
        break;
            case TaskMonitoring:
                openOntologyComponent();
                viewerManager.clearAllViewers();
                break;
        case SliceViewer:
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), SliceViewViewer.class);
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
        if (SystemInfo.isMac && lafName!=null && lafName.contains("synthetica")) {
            offsetY=20;
        }
        
        position.setBrowserLocation(new Point(0, offsetY));
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        position.setScreenSize(screenSize);
        position.setBrowserSize(new Dimension(screenSize.width, screenSize.height-offsetY));
        
        SessionMgr.getSessionMgr().setModelProperty(BROWSER_POSITION, position);

        // NO-Frame setSize(position.getBrowserSize());
        // NO-Frame setLocation(position.getBrowserLocation());
        
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
        TopComponent win = WindowManager.getDefault().findTopComponent(OntologyOutline.ONTOLOGY_COMPONENT_NAME);
        if (! win.isOpened() ) {
            Mode propertiesMode = WindowManager.getDefault().findMode("properties");
            if ( propertiesMode != null ) {
                propertiesMode.dockInto( win );
            }
        }
    }

}
