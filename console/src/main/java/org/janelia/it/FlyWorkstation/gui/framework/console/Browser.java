package org.janelia.it.FlyWorkstation.gui.framework.console;

import com.google.common.collect.ComparisonChain;
import org.janelia.it.FlyWorkstation.api.entity_model.access.LoadRequestStatusObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestStatus;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.*;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.GeneralSearchDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.SearchConfiguration;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModelListenerAdapter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ImageCache;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardViewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.LayersPanel;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.JOutlookBar;
import org.janelia.it.FlyWorkstation.gui.util.JOutlookBar2;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.FreeMemoryWatcher;
import org.janelia.it.FlyWorkstation.shared.util.PrintableComponent;
import org.janelia.it.FlyWorkstation.shared.util.PrintableImage;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:29 PM
 */
public class Browser extends JFrame implements Cloneable {
    
    private static final Logger log = LoggerFactory.getLogger(Browser.class);
    
    //    private static Hashtable editorTypeToConstructorRegistry = new MultiHash();
//    private static Hashtable editorClassToSubEditorClassRegistry =
//            new MultiHash();
//    private static Hashtable editorConstructorToNameRegistry = new Hashtable();
//    private static Hashtable editorClassToNameRegistry = new Hashtable();
//    private static Hashtable editorNameToConstructorRegistry = new Hashtable();
//    private static Hashtable editorNameToTypeRegistry = new Hashtable();
//    private static Map typeToDefaultEditorName = new HashMap();
    private static String BROWSER_POSITION = "BROWSER_POSITION_ON_SCREEN";
	private static String SEARCH_HISTORY = "SEARCH_HISTORY";

    // Used by printing mechanism to ensure capacity.
    public static final String VIEW_SEARCH = "Search Toolbar";
    public static final String VIEW_OUTLINES = "Outlines Section";
    public static final String VIEW_ONTOLOGY = "Ontology Section";
    
    public static final String BAR_DATA = "Data";
    public static final String BAR_SAMPLES = "Samples";
    public static final String BAR_SESSIONS = "Sessions";
    public static final String BAR_TASKS = "Services";

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
    private JSplitPane jSplitPaneBottom;
    private JSplitPane jSplitPaneMain;
    private JPanel allPanelsView = new JPanel();
    private JPanel collapsedOutlineView = new JPanel();
    private JPanel mainPanel = new JPanel();
    private ViewerManager viewerManager;
    private final ImageCache imageCache = new ImageCache();
    private CardLayout layout = new CardLayout();
    private JMenuBar menuBar;
//    private SearchToolbar searchToolbar;
    private JTabbedPane subBrowserTabPane = new JTabbedPane();
    private ArrayList browserObservers = new ArrayList();
    private SessionModelListener modelListener = new MySessionModelListener();
    private DescriptionObserver descriptionObserver = new DescriptionObserver();

    private float realEstatePercent = .2f;
    private BrowserModel browserModel;
    private BorderLayout borderLayout = new BorderLayout();
    //    private Editor masterEditor;
//    private Editor subEditor;
    private JOutlookBar outlookBar;
    //    private FileOutline fileOutline;
    private SessionOutline sessionOutline;
    private EntityOutline entityOutline;
    private EntityWrapperOutline entityWrapperOutline;
    private TaskOutline taskOutline;

    private VerticalPanelPicker rightPanel;
    private OntologyOutline ontologyOutline;
    private LayersPanel layersPanel;
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
    private String mostRecentFileOutlinePath;
    private JTabbedPane icsTabPane = new JTabbedPane();
    private int rightDividerLocation;
    private JMenu editorMenu;
    private boolean showSubEditorWhenAvailable = true;
    private StatusBar statusBar = new StatusBar();
    private String title;
    private ImageIcon browserImageIcon;
    private PageFormat pageFormat;
    private boolean outlineIsCollapsed = false;
    private boolean usingSplashPanel = true;
    private boolean isDrillingDownToSelectedEntity = false;
    private String currentAnnotationSessionTaskId;
    private MaskSearchDialog arbitraryMaskSearchDialog;
    private CellCounterDialog runCellCounterDialog;


    /**
     * Center Window, use passed realEstatePercent (0-1.0, where 1.0 is 100% of the screen)
     */
    public Browser(float realEstatePercent, BrowserModel browserModel) {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        this.realEstatePercent = realEstatePercent;

        viewerManager = new ViewerManager();
        
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
    public Browser(int topLeftX, int topLeftY, Dimension size, BrowserModel browserModel) {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        try {
            jbInit(browserModel);
            setLocation(topLeftX, topLeftY);
            setSize(size);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    public ImageCache getImageCache() {
    	return imageCache;
    }

    static public void setMenuBarClass(Class aMenuBarClass) {
        menuBarClass = aMenuBarClass;
    }

    // todo Remove access to the menu bar
    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    public void addBrowserObserver(BrowserObserver browserObserver) {
        browserObservers.add(browserObserver);
    }

    public void removeBrowserObserver(BrowserObserver browserObserver) {
        browserObservers.remove(browserObserver);
    }

    public void closeAllViews() {
//        if (masterEditor == null) {
//            return;
//        }
//
//        JPanel rightPanel = getFormattedSplashPanel();
//        usingSplashPanel = true;
//        masterEditor.dispose();
//        masterEditor = null;
//        postMasterEditorChanged(null, false);
//        resetMasterEditorMenus();

//        int location = centerLeftHorizontalSplitPane.getDividerLocation();
//        centerLeftHorizontalSplitPane.setRightComponent(rightPanel);
//        centerLeftHorizontalSplitPane.setDividerLocation(location);
    }

    private void jbInit(BrowserModel browserModel) throws Exception {
//        showSubEditorWhenAvailable = ((Boolean) SessionMgr.getSessionMgr()
//                                                          .getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)).booleanValue();

        Object useFreeProperty = SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY);
        if (null!=useFreeProperty && useFreeProperty instanceof Boolean) {
            useFreeMemoryViewer((Boolean)useFreeProperty);
        }
        else {
            useFreeMemoryViewer(false);
        }
        getContentPane().setLayout(borderLayout);
        setTitle("");

        this.browserModel = browserModel;
        browserModel.addBrowserModelListener(new BrowserModelObserver());
        SessionMgr.getSessionMgr().addSessionModelListener(modelListener);        
        
//        viewerPanel.setBorder(BorderFactory.createLineBorder(Color.black));
//        searchToolbar = new SearchToolbar();
        usingSplashPanel = true;
//        subBrowserTabPane = new SubBrowser(browserModel);
//        fileOutline = new FileOutline(this);
        sessionOutline = new SessionOutline(this);
        // todo We should probably pass the user info to the server rather than filter the complete list on the console.
		
        entityOutline = new EntityOutline() {
			@Override
			public List<Entity> loadRootList() throws Exception {
				List<Entity> roots = ModelMgr.getModelMgr().getCommonRootEntities();
				Collections.sort(roots, new Comparator<Entity>(){
					public int compare(Entity o1, Entity o2) {
						return ComparisonChain.start()
							.compareTrueFirst(ModelMgrUtils.isOwner(o1), ModelMgrUtils.isOwner(o2))
							.compare(o1.getOwnerKey(), o2.getOwnerKey())
							.compare(o1.getId(), o2.getId()).result();
					}
				});

				return roots;
			}
		};
		
        entityWrapperOutline = new EntityWrapperOutline() {
            @Override
            public List<EntityWrapper> loadRootList() throws Exception {
                List<Entity> roots = ModelMgr.getModelMgr().getCommonRootEntities();
                Collections.sort(roots, new Comparator<Entity>(){
                    public int compare(Entity o1, Entity o2) {
                        return ComparisonChain.start()
                            .compareTrueFirst(ModelMgrUtils.isOwner(o1), ModelMgrUtils.isOwner(o2))
                            .compare(o1.getOwnerKey(), o2.getOwnerKey())
                            .compare(o1.getId(), o2.getId()).result();
                    }
                });
                List<EntityWrapper> wrappers = new ArrayList<EntityWrapper>();
                for(Entity rootEntity : roots) {
                    EntityData ed = new EntityData();
                    ed.setChildEntity(rootEntity);
                    RootedEntity rootedEntity = new RootedEntity("/e_"+rootEntity.getId(), ed);
                    wrappers.add(EntityWrapperFactory.wrap(rootedEntity));
                }
                return wrappers;
            }
        };
		
        taskOutline = new TaskOutline(this);
        
        ontologyOutline = new OntologyOutline();
        
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
        runCellCounterDialog = new CellCounterDialog();
        screenEvaluationDialog = new ScreenEvaluationDialog(this);
        maaSearchDialog = new MAASearchDialog(this);
        dataSetListDialog = new DataSetListDialog();
        
        ontologyOutline.setPreferredSize(new Dimension());

        outlookBar = new JOutlookBar2();
        outlookBar.addBar(BAR_DATA, Icons.getIcon("folders_explorer_medium.png"), entityOutline);
        outlookBar.addBar(BAR_SAMPLES, Icons.getIcon("folders_explorer_medium.png"), entityWrapperOutline);
        outlookBar.addBar(BAR_SESSIONS, Icons.getIcon("cart_medium.png"), sessionOutline);
        outlookBar.addBar(BAR_TASKS, Icons.getIcon("cog_medium.png"), taskOutline);
        
        outlookBar.addPropertyChangeListener("visibleBar", new PropertyChangeListener() {
        	public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

        	    log.info("Changing viewable outline to: "+outlookBar.getVisibleBarName());
                
        	    Integer oldValue = (Integer)propertyChangeEvent.getOldValue();
        	    String oldBarName = oldValue>=0 ? outlookBar.getBarNames().get(oldValue) : null;
        	    
        	    // Set the perspective
        	    String n = outlookBar.getVisibleBarName();
        	    if (n.equals(BAR_DATA)) {
        	        setPerspective(Perspective.ImageBrowser);
        	    } 
        	    else if (n.equals(BAR_SAMPLES)) {
        	        setPerspective(Perspective.AlignmentBoard);
        	    }
        	    else if (n.equals(BAR_SESSIONS)) {
        	        setPerspective(Perspective.AnnotationSession);
        	    }
        	    else if (n.equals(BAR_TASKS)) {
        	        setPerspective(Perspective.TaskMonitoring);
        	    }
        	    else {
        	        log.warn("Unrecognized bar: "+n);
        	    }
				
        	    // Activate the outline, and deactivate all others
				for(String bar : outlookBar.getBarNames()) {
				    JComponent comp = outlookBar.getBar(bar);
                    if (!(comp instanceof ActivatableView)) continue;
				    if (bar.equals(outlookBar.getVisibleBarName())) {
				        ((ActivatableView)comp).activate();
				    }
				    else if (bar.equals(oldBarName)){
	                    ((ActivatableView)comp).deactivate();    
				    }
				}
				
        		// clear annotation session whenever the user moves away from the session outline
				JComponent activeComp = outlookBar.getBar(outlookBar.getVisibleBarName());
        		if (!(activeComp instanceof SessionOutline)) {
        			ModelMgr.getModelMgr().setCurrentAnnotationSession(null);
        		}
            }
        });
          
        BrowserPosition consolePosition = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);
        if (null == consolePosition) {
            consolePosition = getNewBrowserPosition();
        }
        
        layersPanel = new LayersPanel();
        splitPickingPanel = new SplitPickingPanel();
        
        rightPanel = new VerticalPanelPicker();
        rightPanel.addPanel(OUTLINE_ONTOLOGY, Icons.getIcon("page.png"), "Displays an ontology for annotation", ontologyOutline);
        rightPanel.addPanel(OUTLINE_LAYERS, Icons.getIcon("palette.png"), "Adjust alignment board layers", layersPanel);
        rightPanel.addPanel(OUTLINE_SPLIT_PICKER, Icons.getIcon("page_copy.png"), "Allows for simulation of flyline crosses", splitPickingPanel);
        
        
        Component rightComponent = rightPanel;

        centerRightHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, viewerManager.getViewerContainer(), rightComponent);
        centerRightHorizontalSplitPane.setMinimumSize(new Dimension(200, 0));
        centerRightHorizontalSplitPane.setOpaque(true);
        centerRightHorizontalSplitPane.setDividerSize(10);
        centerRightHorizontalSplitPane.setOneTouchExpandable(true);
        centerRightHorizontalSplitPane.setDividerLocation(consolePosition.getHorizontalRightDividerLocation());
        centerRightHorizontalSplitPane.setBorder(BorderFactory.createEmptyBorder());
        
        centerLeftHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, outlookBar, centerRightHorizontalSplitPane);
        centerLeftHorizontalSplitPane.setMinimumSize(new Dimension(400, 0));
        centerLeftHorizontalSplitPane.setOneTouchExpandable(true);
        centerLeftHorizontalSplitPane.setDividerLocation(consolePosition.getHorizontalLeftDividerLocation());
        centerLeftHorizontalSplitPane.setBorder(BorderFactory.createEmptyBorder());
        
        setSize(consolePosition.getBrowserSize());
        setLocation(consolePosition.getBrowserLocation());

//        searchToolbar.setVisible(false);

        if (menuBarClass == null) {
            menuBar = new ConsoleMenuBar(this);
        }
        else {
            menuBar = (JMenuBar) menuBarClass.getConstructor(new Class[]{this.getClass()}).newInstance(new Object[]{this});
        }
        setJMenuBar(menuBar);
        
        // Collect the final components
        mainPanel.setLayout(layout);
        allPanelsView.setLayout(new BorderLayout());
//        allPanelsView.add(searchToolbar, BorderLayout.NORTH);
        allPanelsView.add(centerLeftHorizontalSplitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        mainPanel.add(allPanelsView, "Regular");
        collapsedOutlineView.setLayout(new BorderLayout());
        mainPanel.add(collapsedOutlineView, "Collapsed FileOutline");
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        // Run this later so that the Browser has finished initializing by the time it runs
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			    setPerspective(Perspective.ImageBrowser);
//
//			    // Temporary code for testing with a single alignment board
//			    // TODO: REMOVE ALL THIS
//			    setPerspective(Perspective.AlignmentBoard);
//
//			    SimpleWorker worker = new SimpleWorker() {
//
//			        RootedEntity ab;
//
//                    @Override
//                    protected void doStuff() throws Exception {
//                        Entity cr = ModelMgr.getModelMgr().getCommonRootEntityByName(LayersPanel.ALIGNMENT_BOARDS_FOLDER);
//                        if (cr==null) {
//                            throw new IllegalStateException("Cannot find test AB folder");
//                        }
//                        RootedEntity rootedEntity = new RootedEntity(cr);
//                        ModelMgr.getModelMgr().loadLazyEntity(cr, false);
//                        ab = rootedEntity.getChildOfType(EntityConstants.TYPE_ALIGNMENT_BOARD);
//                    }
//
//                    @Override
//                    protected void hadSuccess() {
//                        SessionMgr.getBrowser().getLayersPanel().openAlignmentBoard(ab.getEntityId());
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                        SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//
//                worker.execute();
			}
        });
    }

//	private void updateStatusBar() {
//		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
//		int s = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_MAIN_VIEW).size() + 
//			esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_SEC_VIEW).size();
//		statusBar.setDescription(s+" entities selected");
//	}
	
    ///////// Browser Controller section////////////
//    static public void registerEditorForType(Class type, Class editor,
//                                             String editorName,
//                                             boolean defaultEditorForType)
//                                      throws Exception {
//        if (validateEditorClass(editor)) {
//            if (defaultEditorForType) {
//                if (typeToDefaultEditorName.containsKey(type)) {
//                    throw new Exception("Editor " +
//                                        typeToDefaultEditorName.get(type) +
//                                        " is already the default for type " +
//                                        type);
//                }
//
//                typeToDefaultEditorName.put(type, editorName);
//            }
//
//            Constructor con = editor.getConstructor(
//                                      new Class[] { Browser.class, Boolean.class });
//            Vector tmpVec = (Vector) editorTypeToConstructorRegistry.get(type);
//
//            if ((tmpVec != null) && tmpVec.contains(con)) {
//                return; //Check for existance of mapping
//            }
//
//            editorTypeToConstructorRegistry.put(type, con);
//            editorNameToConstructorRegistry.put(editorName, con);
//            editorConstructorToNameRegistry.put(con, editorName);
//            editorClassToNameRegistry.put(editor, editorName);
//            editorNameToTypeRegistry.put(editorName, type);
//        } else {
//            throw new Exception("Class passed for Editor (" +
//                                editor.getName() +
//                                ")is not acceptable (see debug screen)");
//        }
//    }
//
//    static public void registerSubEditorForMainEditor(Class mainEditor,
//                                                      Class subEditor)
//                                               throws Exception {
//        if (validateSubEditorClass(subEditor)) {
//            editorClassToSubEditorClassRegistry.put(mainEditor,
//                                                    subEditor.getConstructor(
//                                                            new Class[] {
//                Browser.class, Boolean.class
//            }));
//        } else {
//            throw new Exception("Class passed for SubEditor (" +
//                                subEditor.getName() +
//                                ") is not acceptable (see debug screen)");
//        }
//    }
//
//    public void drillDownToEntityUsingDefaultEditor(GenomicEntity entity) {
//        if (entity == null) {
//            return;
//        }
//
//        String editorName = (String) typeToDefaultEditorName.get(
//                                    entity.getClass());
//
//        if (editorName != null) {
//            drillDownToEntityUsingThisEditor(editorName, entity);
//        }
//    }
//
//    public void drillDownToEntityUsingThisEditor(String editorName,
//                                                 GenomicEntity entity) {
//		// If there is no entity selected, or we are already drilling, do nothing and return.
//		//System.out.println("Drilling to entity");
//        if (entity == null || isDrillingDownToSelectedEntity) {
//            return;
//        }
//
//		// Set the drilling flag to prevent multiple calls.
//		isDrillingDownToSelectedEntity = true;
//
//        //Validate editor sent against the lastselection type
//        Class modelClass = (Class) editorNameToTypeRegistry.get(editorName);
//
//        if (modelClass != entity.getClass()) {
//            return;
//        }
//
//        //throw new IllegalArgumentException ("ERROR! - EditorName sent cannot edit the last selection");
//        //dispose old editor
//        if ((masterEditor != null) &&
//                !editorName.equals(editorClassToNameRegistry.get(
//                                           masterEditor.getClass()))) {
//            masterEditor.dispose();
//            masterEditor = null;
//        }
//
//        browserModel.setMasterEditorEntity(entity);
//
//        //instanciate the new editor
//        if (masterEditor == null) {
//            setMasterEditorPrivate(constructEditorForEditorName(editorName,
//                                                                true));
//        }
//
//		// Turn off the drilling flag
//		isDrillingDownToSelectedEntity = false;
//
//		this.doLayout();
//		this.repaint();
//		//System.out.println("No longer drilling to entity");
//    }

//    public void printPageSetup() {
//        if (pageFormat == null) {
//            pageFormat = new PageFormat();
//            pageFormat.setOrientation(PageFormat.LANDSCAPE);
//        }
//
//        PrinterJob printJob = PrinterJob.getPrinterJob();
//        pageFormat = printJob.pageDialog(pageFormat);
//        pageFormat = printJob.validatePage(pageFormat);
//    }
//

    /**
     * Print the screen state by first creating a buffered image.
     */
    public void printBrowser() {
        // For now, use whole thing.
        Component component = this;

        // Ensure sufficient resources.
        long requiredSize = (long) (RGB_TYPE_BYTES_PER_PIXEL * component.getWidth() * component.getHeight()) + (long) PRINT_OVERHEAD_SIZE;

        if (requiredSize > FreeMemoryWatcher.getFreeMemoryWatcher().getFreeMemory()) {
            JOptionPane.showMessageDialog(this, MEMORY_EXCEEDED_PRT_SCR_MSG, MEMORY_EXCEEDED_ADVISORY, JOptionPane.ERROR_MESSAGE);

            return;
        } // Memory capacity WOULD be exceeded.

        PrinterJob printJob = PrinterJob.getPrinterJob();

        if (pageFormat == null) {
            pageFormat = new PageFormat();
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
            pageFormat = printJob.validatePage(pageFormat);
        } // Must create a page format.

        if (printJob.printDialog()) {
            // Get a buffered image of the component.
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(component.getWidth(), component.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            component.paint(graphics);

            // Wrap buffered image in a component, and print that.
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

        Component comp;
        comp = this;
//        if (master) {
//            comp = (Component) masterEditor;
//        } else {
//            comp = (Component) subEditor;
//        }

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
//
//    void setMasterEditor(String editorName) {
//        if ((masterEditor != null) &&
//                editorName.equals(editorClassToNameRegistry.get(
//                                          masterEditor.getClass()))) {
//            return; //if editor already showing, do nothing
//        }
//
//        Editor editor = null;
//
//        try {
//            editor = (Editor) ((Constructor) (this.editorNameToConstructorRegistry.get(
//                                       editorName))).newInstance(
//                             new Object[] { this, new Boolean(true) });
//        } catch (Exception ex) {
//            System.out.println(
//                    "ERROR! - Object selected whose editor cannot be instanciated.  Object Type: " +
//                    browserModel.getCurrentSelection().getClass() +
//                    " Editor Type: " + editor);
//
//            try {
//                SessionMgr.getSessionMgr().handleException(ex);
//            } catch (Exception ex1) {
//                ex.printStackTrace();
//            }
//        }
//
//        try {
//            setMasterEditorPrivate(editor);
//        } catch (Exception ex) {
//            System.out.println(
//                    "ERROR! - Object selected whose editor cannot be set in setmasterEditor.  Object Type: " +
//                    browserModel.getCurrentSelection().getClass());
//
//            try {
//                SessionMgr.getSessionMgr().handleException(ex);
//            } catch (Exception ex1) {
//                ex.printStackTrace();
//            }
//        }
//
//        if (editorMenu != null) {
//            ((EditorMenuFactory.EditorMenu) editorMenu).setDisabled(editorName);
//        }
//    }
//
//    ///////// End Browser Controller section/////////
//
//    /**
//    * This method can be called by the master Browser when it wants to change it's
//    * menus.  This will force a re-poll of the master console menus and redraw of
//    * the menuBar.
//    */
//    public void resetMasterEditorMenus() {
//        postEditorSpecificMenusChanged();
//    }

    /**
     * Set the Editor in the right pane.  The editor must be a sub-class of
     * java.awt.Component at some level, or an exception will be thrown.
     */
//    public void setMasterEditor(Editor editor) throws Exception {
//        if (validateEditorClass(editor.getClass())) {
//            setMasterEditorPrivate(editor);
//        } else {
//            throw new Exception("Editor passed to setMasterEditor is invalid");
//        }
//    }
//

    /**
     * @return BrowserModel The browserModel for this instance of the console.
     */
    public BrowserModel getBrowserModel() {
        return browserModel;
    }

    //    public Editor getMasterEditor() {
//        return masterEditor;
//    }
//
//    public Hashtable getEditorNameToConstructorRegistry() {
//        return editorNameToConstructorRegistry;
//    }
//
    public Object clone() {
        java.awt.Point topLeft = this.getLocation();
        Dimension size = this.getSize();
        BrowserModel newBrowserModel = (BrowserModel) this.browserModel.clone();
        Browser newBrowser = new Browser(topLeft.x + 25, topLeft.y + 25, size, newBrowserModel);
        newBrowser.setTitle(title);
        newBrowser.setBrowserImageIcon(browserImageIcon);
        newBrowser.setVisible(true);

        return newBrowser;
    }

    public void setTitle(String title) {
        this.title = title;
        super.setTitle(title);
    }

    private void setFullTitle(String infoSource) {
        if (infoSource != null) {
            super.setTitle(title + " - " + infoSource);
        }
    }

    public void setBrowserImageIcon(ImageIcon _browserImageIcon) {
        if (_browserImageIcon != null) {
            this.browserImageIcon = _browserImageIcon;
            this.setIconImage(browserImageIcon.getImage());
        }
    }

    /**
     * Overriden so we can exit on System Close
     */
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            SessionMgr.getSessionMgr().removeBrowser(this);
        }
    }

//    static public boolean validateEditorClass(Class editor) {
//        Class[] interfaces = editor.getInterfaces();
//        boolean editorSupported = false;
//
//        for (int i = 0; i < interfaces.length; i++) {
//            if (interfaces[i] == client.gui.framework.roles.Editor.class) {
//                editorSupported = true;
//
//                break;
//            }
//        }
//
//        if (!editorSupported) {
//            System.out.println("ERROR! - Editor passed (" + editor +
//                               ")is not a client.gui.framework.session_mgr.Editor!");
//
//            return false;
//        }
//
//        Class testClass = editor;
//
//        while ((testClass != java.lang.Object.class) &&
//               (testClass != java.awt.Component.class)) {
//            testClass = testClass.getSuperclass();
//        }
//
//        if (testClass == java.lang.Object.class) {
//            System.out.println("ERROR! - Editor passed (" + editor +
//                               ") is not a java.awt.Component!");
//
//            return false;
//        }
//
//        try {
//            editor.getConstructor(new Class[] { Browser.class, Boolean.class });
//        } catch (NoSuchMethodException nsme) {
//            System.out.println("ERROR! - Editor passed (" + editor +
//                               ") does not have a constructor that takes a client.gui.framework.console.Browser!");
//
//            return false;
//        }
//
//        return true;
//    }
//
//    static public boolean validateSubEditorClass(Class editor) {
//        boolean editorSupported = false;
//        Class[] interfaces = editor.getInterfaces();
//
//        for (int i = 0; i < interfaces.length; i++) {
//            if (interfaces[i] == client.gui.framework.roles.SubEditor.class) {
//                editorSupported = true;
//
//                break;
//            }
//        }
//
//        Class superClass = editor.getSuperclass();
//
//        while (!editorSupported && (superClass != null)) {
//            interfaces = superClass.getInterfaces();
//
//            for (int i = 0; i < interfaces.length; i++) {
//                if (interfaces[i] == client.gui.framework.roles.SubEditor.class) {
//                    editorSupported = true;
//
//                    break;
//                }
//            }
//
//            superClass = superClass.getSuperclass();
//        }
//
//        if (!editorSupported) {
//            System.out.println("ERROR! - SubEditor passed (" + editor +
//                               ")is not a client.gui.framework.session_mgr.SubEditor!");
//
//            return false;
//        }
//
//        Class testClass = editor;
//
//        while ((testClass != java.lang.Object.class) &&
//               (testClass != java.awt.Component.class)) {
//            testClass = testClass.getSuperclass();
//        }
//
//        if (testClass == java.lang.Object.class) {
//            System.out.println("ERROR! - SubEditor passed (" + editor +
//                               ") is not a java.awt.Component!");
//
//            return false;
//        }
//
//        try {
//            editor.getConstructor(new Class[] { Browser.class, Boolean.class });
//        } catch (NoSuchMethodException nsme) {
//            System.out.println("ERROR! - SubEditor passed (" + editor +
//                               ") does not have a constructor that takes a client.gui.framework.console.Browser!");
//
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//    * This method can be called directly from inside the class to bypass the check on the editor.
//    *
//    */
//    private void setMasterEditorPrivate(Editor editor) {
//        if ((masterEditor != null) &&
//                !masterEditor.getName().equals(editor.getName())) {
//            masterEditor.dispose();
//        }
//
//        masterEditor = editor;
//        resetMasterEditorMenus();
//    }

    public boolean isOutlineCollapsed() {
        return outlineIsCollapsed;
    }

    public void setView(boolean isOutlineCollapsed) {
        if ((outlineIsCollapsed == isOutlineCollapsed) && !usingSplashPanel) {
            return;
        }

        // Set the fileOutline state
        outlineIsCollapsed = isOutlineCollapsed;

        // Do not allow multiple calls if the thing already exists in that state.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        //allPanelsView is the default view, it is the view with all the four components.
        //collapsedOutlineView is the view where the fileOutline is collapsed and the top half of the
        //panel contains the main editor.
        //these two views can be selected from the View Menu. They share common components so upon
        //any one selection reparenting of the components to their respective split panes has to
        //occur
        if (!outlineIsCollapsed) {
            clearDefaultView();

            if (jSplitPaneBottom != null) {
                jSplitPaneBottom.removeAll();
            }

            if (jSplitPaneMain != null) {
                jSplitPaneMain.removeAll();
            }

            collapsedOutlineView.removeAll();

            BrowserPosition browserPosition = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(this.BROWSER_POSITION);

            centerLeftHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            centerLeftHorizontalSplitPane.setMinimumSize(new Dimension(0, 0));

            //also add back the components on the Left vertical pane which got removed
            //when View 2 was showing
            if (centerLeftHorizontalSplitPane.getParent() == null) {
                allPanelsView.add(centerLeftHorizontalSplitPane, BorderLayout.CENTER);
            }

            layout.first(mainPanel);

            //this will handle the right pane of the Split Pane in view1. If display subeditor property is on
            //it will add the master editor and the subeditor and if it is off then it will add only the master
            //editor on the right pane
//            if (masterEditor != null) {
//                usingSplashPanel = false;
//                handleSubEditor();
//            } else {
//                usingSplashPanel = true;
//                centerLeftHorizontalSplitPane.setRightComponent(
//                        getFormattedSplashPanel());
//            }

//            if (browserPosition == null) {
//                centerLeftHorizontalSplitPane.setDividerLocation(
//                        (int) (screenSize.width * realEstatePercent * (1 - MAIN_VIEWER_PANE_HORIZONTAL_PERCENT)));
//            } else {
//                centerLeftHorizontalSplitPane.setDividerLocation(
//                        browserPosition.getHorizontalLeftDividerLocation());
//            }
        }
        else {
            clearDefaultView();

            if (jSplitPaneBottom != null) {
                jSplitPaneBottom.removeAll();
            }

            if (jSplitPaneMain != null) {
                jSplitPaneMain.removeAll();
            }

            jSplitPaneMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            jSplitPaneMain.setMinimumSize(new Dimension(0, 0));

//            if (masterEditor != null) {
//                jSplitPaneMain.setTopComponent((Component) masterEditor);
//            } else {
//                jSplitPaneMain.setTopComponent(getFormattedSplashPanel());
//                usingSplashPanel = true;
//                jSplitPaneMain.setBottomComponent(icsTabPane);
//            }

            jSplitPaneBottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            jSplitPaneBottom.setMinimumSize(new Dimension(0, 0));

//            handleSubEditor();
            jSplitPaneBottom.setOneTouchExpandable(true);

            if (jSplitPaneMain.getParent() == null) {
                collapsedOutlineView.add(jSplitPaneMain, BorderLayout.CENTER);
            }

            layout.last(mainPanel);
        }

        //  Packing seems to prevent the grey window issue!!!!
        //  Pack forces all components to be displayable, and validates them
        this.pack();
        this.doLayout();
        this.repaint();
    }

    private void clearDefaultView() {
        if (centerLeftHorizontalSplitPane != null) {
            centerLeftHorizontalSplitPane.removeAll();

            //centerLeftHorizontalSplitPane.repaint();
        }

        allPanelsView.removeAll();
    }

//    private void handleSubEditor() {
//        if (masterEditor == null) {
//            return;
//        }
//
//        if ((subBrowserTabPane.getComponents().length != 0) &&
//                showSubEditorWhenAvailable) {
//            addSubEditorSplitPaneToBottomRight(subBrowserTabPane);
//        } else if ((subBrowserTabPane.getComponents().length != 0) &&
//                       !showSubEditorWhenAvailable) {
//            subBrowserTabPane.removeAll();
//            removeSubEditorSplitPane();
//        } else if ((subBrowserTabPane.getComponents().length == 0) &&
//                       !showSubEditorWhenAvailable) {
//            removeSubEditorSplitPane();
//        } else if ((subBrowserTabPane.getComponents().length == 0) &&
//                       showSubEditorWhenAvailable) {
//            Vector subEditorConstructors = (Vector) editorClassToSubEditorClassRegistry.get(
//                                                   masterEditor.getClass());
//
//
//            //Dispose current components
//            subBrowserTabPane.removeAll();
//
//            if (subEditorConstructors == null) {
//                removeSubEditorSplitPane();
//
//                return;
//            }
//
//            Component component;
//            Constructor constructor = null;
//            subEditor = null;
//
//            try {
//                for (Enumeration e = subEditorConstructors.elements();
//                     e.hasMoreElements();) {
//                    constructor = (Constructor) e.nextElement();
//                    component = (Component) constructor.newInstance(
//                                        new Object[] { this, new Boolean(false) });
//
//                    if (subEditor == null) {
//                        subEditor = (Editor) component;
//                    }
//
//                    if (component.getName() == null) {
//                        component.setName(
//                                "No Name Found, Please call setName on your Editor!");
//                    }
//
//                    subBrowserTabPane.add(component);
//                }
//
//                addSubEditorSplitPaneToBottomRight(subBrowserTabPane);
//            } catch (Exception ex) {
//                System.out.println(
//                        "ERROR! - Object selected whose subEditor cannot be instanciated. SubEditor Type: " +
//                        constructor.getDeclaringClass());
//                SessionMgr.getSessionMgr().handleException(ex);
//            }
//        }
//    }
//
//    private void removeSubEditorSplitPane() {
//        if (!outlineIsCollapsed) {
//            int location = centerLeftHorizontalSplitPane.getDividerLocation();
//
//            if (jSplitPaneRightVertical != null) {
//                jSplitPaneRightVerticalSplitLocation = jSplitPaneRightVertical.getDividerLocation();
//
//                centerLeftHorizontalSplitPane.remove(jSplitPaneRightVertical);
//            }
//
//            centerLeftHorizontalSplitPane.setRightComponent((Component) masterEditor);
//            centerLeftHorizontalSplitPane.setDividerLocation(location);
//        } else {
//            jSplitPaneBottom.removeAll();
//
//            int location = jSplitPaneMain.getDividerLocation();
//
//            jSplitPaneMain.setBottomComponent(icsTabPane);
//            jSplitPaneMain.setDividerLocation(location);
//        }
//    }
//
//    /**
//     * This method adds the subeditor pane in the default manner.
//     * That is in the bottom right corner of the Frame
//     */
//    private void addSubEditorSplitPaneToBottomRight(Component subEditorTabPane) {
//        if (!outlineIsCollapsed) {
//            jSplitPaneRightVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
//                                                     false,
//                                                     (Component) masterEditor,
//                                                     subBrowserTabPane);
//            jSplitPaneRightVertical.setMinimumSize(new Dimension(0,0));
//            jSplitPaneRightVertical.setDividerLocation(
//                    jSplitPaneRightVerticalSplitLocation);
//            jSplitPaneRightVertical.setOneTouchExpandable(true);
//            jSplitPaneRightVertical.setDividerSize(10);
//
//            centerLeftHorizontalSplitPane.setRightComponent(jSplitPaneRightVertical);
//        } else {
//            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//            int location = jSplitPaneMain.getDividerLocation();
//
//            jSplitPaneBottom.setLeftComponent(icsTabPane);
//            jSplitPaneBottom.setRightComponent(subBrowserTabPane);
//            jSplitPaneBottom.setOneTouchExpandable(true);
//            jSplitPaneBottom.setDividerLocation(
//                    (int) (screenSize.width * realEstatePercent * .25f));
//            jSplitPaneBottom.setDividerSize(10);
//
//            jSplitPaneMain.setBottomComponent(jSplitPaneBottom);
//            jSplitPaneMain.setDividerLocation(location);
//        }
//    }

    private void useFreeMemoryViewer(boolean use) {
        statusBar.useFreeMemoryViewer(false);
        //validate();
    }

//    private boolean subEditorsExistForEditor(Editor editor) {
//        return editorClassToSubEditorClassRegistry.get(editor.getClass()) != null;
//    }
//
//    private boolean isSubEditorShowing() {
//        return jSplitPaneRightVertical != null;
//    }

    //    private Editor constructEditorForEditorName(String editorName,
//                                                boolean masterEditor) {
//        Editor editor = null;
//
//        try {
//            editor = (Editor) ((Constructor) editorNameToConstructorRegistry.get(
//                                       editorName)).newInstance(
//                             new Object[] { this, new Boolean(masterEditor) });
//        } catch (Exception ex) {
//            System.out.println(
//                    "ERROR! - Editor cannot be instanciated. Editor: " +
//                    editorName);
//
//            try {
//                SessionMgr.getSessionMgr().handleException(ex);
//            } catch (Exception ex1) {
//                ex.printStackTrace();
//            }
//        }
//
//        return editor;
//    }
//
    private void postNumOpenBrowsersChanged() {
        for (int i = 0; i < browserObservers.size(); i++) {
            ((BrowserObserver) browserObservers.get(i)).openBrowserCountChanged(SessionMgr.getSessionMgr().getNumberOfOpenBrowsers());
        }
    }

    public MaskSearchDialog getMaskSearchDialog() {
        return arbitraryMaskSearchDialog;
    }

    public CellCounterDialog getRunCellCounterDialog() {
        return runCellCounterDialog;
    }

//    private void postEditorSpecificMenusChanged() {
//        for (int i = 0; i < browserObservers.size(); i++) {
//            if (masterEditor == null) {
//                ((BrowserObserver) browserObservers.elementAt(i)).editorSpecificMenusChanged(
//                        null);
//            } else {
//                ((BrowserObserver) browserObservers.elementAt(i)).editorSpecificMenusChanged(
//                        masterEditor.getMenus());
//            }
//        }
//    }
//
//    private void postMasterEditorChanged(String newMasterEditor,
//                                         boolean subEditorAvailable) {
//        for (int i = 0; i < browserObservers.size(); i++) {
//            ((BrowserObserver) browserObservers.elementAt(i)).masterEditorChanged(
//                    newMasterEditor, subEditorAvailable);
//        }
//    }

    private class BrowserModelObserver extends BrowserModelListenerAdapter {
        public void browserCurrentSelectionChanged(Entity newSelection) {
            if (newSelection != null) {
//                LoadRequestStatus lrs = newSelection.loadPropertiesBackground();
//
//                if (lrs.getLoadRequestState()
//                       .equals(LoadRequestStatus.COMPLETE)) {
//                    String description = "Entity";
//
////                    if (newSelection instanceof Feature) {
////                        description = "[" +
////                                      ((Feature) newSelection).getEnvironment() +
////                                      "]";
////                        description = description +
////                                      newSelection.getDescriptiveText();
////                    } else {
////                        description = newSelection.getDescriptiveText();
////                    }
//
//                    statusBar.setDescription("Current Selection: " +
//                                             description);
//                } else {
//                    descriptionObserver.setEntityAndToggleDialog(newSelection);
//                    lrs.addLoadRequestStatusObserver(descriptionObserver, true);
//                    statusBar.setDescription(
//                            "Current Selection: Loading Properties");
//                }
            }
            else {
                statusBar.setDescription("");
            }
        }

        public void browserClosing() {
            setVisible(false);
            SessionMgr.getSessionMgr().removeSessionModelListener(modelListener);

            BrowserPosition position = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);

            if (position == null) {
                position = new BrowserPosition();
            }

            position.setScreenSize(Toolkit.getDefaultToolkit().getScreenSize());
            position.setBrowserSize(Browser.this.getSize());
            position.setBrowserLocation(Browser.this.getLocation());
//            position.setVerticalDividerLocation(
//                    dataSplitPaneVertical.getDividerLocation());
            position.setHorizontalLeftDividerLocation(centerLeftHorizontalSplitPane.getDividerLocation());
            position.setHorizontalRightDividerLocation(centerRightHorizontalSplitPane.getDividerLocation());
            
            SessionMgr.getSessionMgr().setModelProperty(BROWSER_POSITION, position);
            SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY, generalSearchDialog.getSearchHistory());
            
            dispose();
        }
    }

    class DescriptionObserver extends LoadRequestStatusObserverAdapter {
        private Entity ge;

        public void setEntity(Entity ge) {
            this.ge = ge;
        }

        public void stateChanged(LoadRequestStatus loadRequestStatus, LoadRequestState newState) {
            if (newState == LoadRequestStatus.COMPLETE) {
                String description = "Test";

//                if (ge instanceof Entity) {
//                    description = "[" + ((Entity) ge).getEnvironment() +
//                                  "]";
//                    description = description + ge.getDescriptiveText();
//                } else {
//                    description = ge.getDescriptiveText();
//                }

                statusBar.setDescription("Current Selection: " + description);
                loadRequestStatus.removeLoadRequestStatusObserver(this);
            }
        }
    }

    class MySessionModelListener implements SessionModelListener {
        public void browserAdded(BrowserModel browserModel) {
            postNumOpenBrowsersChanged();
        }

        public void browserRemoved(BrowserModel browserModel) {
            postNumOpenBrowsersChanged();
        }

        public void sessionWillExit() {
        }

        public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
//            if (key.equals(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)) {
//                showSubEditorWhenAvailable = ((Boolean) SessionMgr.getSessionMgr()
//                                                                  .getModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY)).booleanValue();
//                handleSubEditor();
//            }

            if (key.equals(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY)) {
                useFreeMemoryViewer(((Boolean) newValue).booleanValue());
            }
        }
    }

    public ViewerManager getViewerManager() {
		return viewerManager;
	}

    public Refreshable getActiveOutline() {
    	return (Refreshable)outlookBar.getVisibleBarComponent();
    }

    public EntityOutline getEntityOutline() {
        return entityOutline;
    }
    
    public EntityWrapperOutline getEntityWrapperOutline() {
        return entityWrapperOutline;
    }
    
    public OntologyOutline getOntologyOutline() {
        return ontologyOutline;
    }

    public void selectRightPanel(String panelName) {
        rightPanel.showPanel(panelName);
    }
    
    public LayersPanel getLayersPanel() {
        return layersPanel;
    }

    public void setLayersPanel(LayersPanel layersPanel) {
        this.layersPanel = layersPanel;
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

	public String getMostRecentFileOutlinePath() {
        return mostRecentFileOutlinePath;
    }

    public void toggleViewComponentState(String viewComponentKey) {
        // todo The layout needs to be much nicer.  See IntelliJ layouts, with perhaps the component menu name still visible
        if (VIEW_SEARCH.equals(viewComponentKey)) {
//            searchToolbar.setVisible(!searchToolbar.isVisible());
        }
        else if (VIEW_OUTLINES.equals(viewComponentKey)) {
            centerLeftHorizontalSplitPane.getLeftComponent().setVisible(!centerLeftHorizontalSplitPane.getLeftComponent().isVisible());
            centerLeftHorizontalSplitPane.setDividerLocation(centerLeftHorizontalSplitPane.getLastDividerLocation());
        }
        else if (VIEW_ONTOLOGY.equals(viewComponentKey)) {
            centerRightHorizontalSplitPane.getRightComponent().setVisible(!centerRightHorizontalSplitPane.getRightComponent().isVisible());
            centerRightHorizontalSplitPane.setDividerLocation(centerRightHorizontalSplitPane.getLastDividerLocation());
        }
    }

    public void setMostRecentFileOutlinePath(String mostRecentFileOutlinePath) {
        this.mostRecentFileOutlinePath = mostRecentFileOutlinePath;
        this.currentAnnotationSessionTaskId = null;
//        sessionOutline.clearSelection();
        File tmpFile = new File(mostRecentFileOutlinePath);
        if (tmpFile.exists()) {
//            viewerPanel.loadImageEntities(getFiles(mostRecentFileOutlinePath));
        }
    }
    
    public JSplitPane getCenterRightHorizontalSplitPane() {
		return centerRightHorizontalSplitPane;
	}

	/**
     * TODO: put this in the FileOutline
     *
     * @param pathToData
     * @return
     */
    public List<File> getFiles(String pathToData) {

        List<File> files = new ArrayList<File>();
        File tmpFile = new File(pathToData);
        if (tmpFile.isDirectory()) {
            File[] childImageFiles = tmpFile.listFiles(new FilenameFilter() {
                public boolean accept(File file, String s) {
                    // TODO: Need a whole mechanism to categorize the files and editors used for them.
                    return s.endsWith(".tif");
                }
            });
            Collections.addAll(files, childImageFiles);
        }
        else if (tmpFile.isFile()) {
            files.add(tmpFile);
        }

        return files;
    }

//    public void setAnnotationSessionChanged(String returnSessionTask) {
//        currentAnnotationSessionTaskId = returnSessionTask;
//        this.mostRecentFileOutlinePath = null;
////        fileOutline.clearSelection();
//        sessionOutline.rebuildDataModel();
//        sessionOutline.selectSession(currentAnnotationSessionTaskId);
//    }

    public BrowserPosition getNewBrowserPosition() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        BrowserPosition position = new BrowserPosition();
        position.setScreenSize(screenSize);
        position.setBrowserSize(screenSize);
        position.setBrowserLocation(new Point(0, 0));
        position.setHorizontalLeftDividerLocation(400);
        position.setHorizontalRightDividerLocation(1100);
        position.setVerticalDividerLocation(800);
        return position;
    }

    public JOutlookBar getOutlookBar() {
        return outlookBar;
    }
    
    public void setPerspective(Perspective perspective) {
        log.info("Setting perspective: {}",perspective);
        switch (perspective) {
        case AlignmentBoard:
            outlookBar.setVisibleBarByName(Browser.BAR_SAMPLES);
            selectRightPanel(OUTLINE_LAYERS);
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), AlignmentBoardViewer.class);
            break;
        case SplitPicker:
            outlookBar.setVisibleBarByName(Browser.BAR_DATA);
            selectRightPanel(OUTLINE_SPLIT_PICKER);
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
            break;
        case AnnotationSession:
            outlookBar.setVisibleBarByName(Browser.BAR_SESSIONS);
            selectRightPanel(OUTLINE_ONTOLOGY);
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
            break;
        case TaskMonitoring:
            outlookBar.setVisibleBarByName(Browser.BAR_TASKS);
            selectRightPanel(OUTLINE_ONTOLOGY);
            viewerManager.clearAllViewers();
            break;
        case ImageBrowser:
        default:
            outlookBar.setVisibleBarByName(Browser.BAR_DATA);
            selectRightPanel(OUTLINE_ONTOLOGY);
            viewerManager.clearAllViewers();
            viewerManager.ensureViewerClass(viewerManager.getMainViewerPane(), IconDemoPanel.class);
        }
    }

    public void resetWindow() {
        int offsetY = 0;
        String lafName = (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.DISPLAY_LOOK_AND_FEEL);
        if (SystemInfo.isMac && lafName!=null && lafName.contains("synthetica")) {
            offsetY=20;
        }
        BrowserPosition consolePosition = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);
        consolePosition.setBrowserLocation(new Point(0, offsetY));
        SessionMgr.getSessionMgr().setModelProperty(BROWSER_POSITION, consolePosition);        
        setSize(consolePosition.getBrowserSize());
        setLocation(consolePosition.getBrowserLocation());
    }

}
