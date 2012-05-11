package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.LoadRequestStatusObserverAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestStatus;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.AnnotationSessionPropertyDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.PatternSearchDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.RunNeuronSeparationDialog;
import org.janelia.it.FlyWorkstation.gui.dialogs.search.GeneralSearchDialog;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.search.SearchToolbar;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModelListenerAdapter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ImageCache;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerSplitPanel;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.JOutlookBar;
import org.janelia.it.FlyWorkstation.gui.util.JOutlookBar2;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.FreeMemoryWatcher;
import org.janelia.it.FlyWorkstation.shared.util.PrintableComponent;
import org.janelia.it.FlyWorkstation.shared.util.PrintableImage;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:29 PM
 */
public class Browser extends JFrame implements Cloneable {
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
    public static final String BAR_SESSIONS = "Sessions";
    public static final String BAR_TASKS = "Services";

    private static String MEMORY_EXCEEDED_PRT_SCR_MSG = "Insufficient memory to print screen";
    private static String MEMORY_EXCEEDED_ADVISORY = "Low Memory";
    private static int RGB_TYPE_BYTES_PER_PIXEL = 4;
    private static int PRINT_OVERHEAD_SIZE = 1000000;
    private static Class menuBarClass;
    private JSplitPane jSplitPaneRightVertical;
    private JSplitPane centerLeftHorizontalSplitPane;
    private JSplitPane centerRightHorizontalSplitPane;
    private JSplitPane jSplitPaneBottom;
    private JSplitPane jSplitPaneMain;
    private JPanel allPanelsView = new JPanel();
    private JPanel collapsedOutlineView = new JPanel();
    private JPanel mainPanel = new JPanel();
    private ViewerSplitPanel viewerPanel;
    private final ImageCache imageCache = new ImageCache();
    private CardLayout layout = new CardLayout();
    private JMenuBar menuBar;
    private SearchToolbar searchToolbar;
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
    private TaskOutline taskOutline;
    private OntologyOutline ontologyOutline;
    private AnnotationSessionPropertyDialog annotationSessionPropertyPanel;
    private RunNeuronSeparationDialog runNeuronSeparationDialog;
    private GeneralSearchDialog searchDialog;
    private PatternSearchDialog patternSearchDialog;
    private String mostRecentFileOutlinePath;
    private JTabbedPane icsTabPane = new JTabbedPane();
    private int rightDividerLocation;
    private int jSplitPaneRightVerticalSplitLocation;
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
    
    
    /**
     * Center Window, use passed realEstatePercent (0-1.0, where 1.0 is 100% of the screen)
     */
    public Browser(float realEstatePercent, BrowserModel browserModel) {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        this.realEstatePercent = realEstatePercent;

        viewerPanel = new ViewerSplitPanel();
        viewerPanel.setMainViewer(new IconDemoPanel(viewerPanel, EntitySelectionModel.CATEGORY_MAIN_VIEW));
        
        try {
            jbInit(browserModel);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    public Viewer showSecViewer() {
    	Viewer secViewer = viewerPanel.getSecViewer();
		if (secViewer==null) {
			secViewer = new IconDemoPanel(SessionMgr.getBrowser().getViewersPanel(), EntitySelectionModel.CATEGORY_SEC_VIEW);
			viewerPanel.setSecViewer(secViewer);
		}
		return secViewer;
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

        if (menuBarClass == null) {
            menuBar = new ConsoleMenuBar(this);
        }
        else {
//            menuBar = (JMenuBar) menuBarClass.getConstructor(new Class[] { this.getClass() }).newInstance(new Object[] { this });
            Constructor[] cons = menuBarClass.getConstructors();
            for (Constructor con : cons) {
//                System.out.println(con.toString());
            }
            menuBar = (JMenuBar) menuBarClass.getConstructor(new Class[]{this.getClass()}).newInstance(new Object[]{this});
        }

        setJMenuBar(menuBar);
        
        
//        viewerPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        searchToolbar = new SearchToolbar();
        usingSplashPanel = true;
//        subBrowserTabPane = new SubBrowser(browserModel);
//        fileOutline = new FileOutline(this);
        sessionOutline = new SessionOutline(this);
        // todo We should probably pass the user info to the server rather than filter the complete list on the console.
		
        entityOutline = new EntityOutline() {
			@Override
			public List<Entity> loadRootList() {
				List<Entity> rootList = ModelMgr.getModelMgr().getUserCommonRootEntitiesByTypeName(EntityConstants.TYPE_FOLDER);
				rootList.addAll(ModelMgr.getModelMgr().getSystemCommonRootEntitiesByTypeName(EntityConstants.TYPE_FOLDER));
				return rootList;
			}
		};
		
        taskOutline = new TaskOutline(this);
        
        ontologyOutline = new OntologyOutline();
        
        annotationSessionPropertyPanel = new AnnotationSessionPropertyDialog(entityOutline, ontologyOutline);
        runNeuronSeparationDialog = new RunNeuronSeparationDialog();
        searchDialog = new GeneralSearchDialog();

        List<String> searchHistory = (List<String>) SessionMgr.getSessionMgr().getModelProperty(SEARCH_HISTORY);
        searchDialog.setSearchHistory(searchHistory);

        patternSearchDialog = new PatternSearchDialog();
        
        ontologyOutline.setPreferredSize(new Dimension());
//        icsTabPane = new ICSTabPane(this);

        outlookBar = new JOutlookBar2();
        outlookBar.addBar(BAR_DATA, Icons.getIcon("folders_explorer_medium.png"), entityOutline);
        outlookBar.addBar(BAR_SESSIONS, Icons.getIcon("cart_medium.png"), sessionOutline);
        outlookBar.addBar(BAR_TASKS, Icons.getIcon("cog_medium.png"), taskOutline);
//        outlookBar.addBar("Files", fileOutline);
//        outlookBar.setVisibleBarByName(Browser.BAR_PUBLIC_DATA);
        
        outlookBar.addPropertyChangeListener("visibleBar", new PropertyChangeListener() {
        	public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        		JComponent comp = outlookBar.getBar(outlookBar.getVisibleBarName());
				
				// clear the main viewer 
				final IconDemoPanel panel = (IconDemoPanel)Browser.this.getMainViewer();
				panel.clear();
				
				// refresh the outline being selected
        		if (comp instanceof Refreshable) {
        			((Refreshable)comp).refresh();
        		}
        		
        		// clear annotation session whenever the user moves away from the session outline
        		if (!(comp instanceof SessionOutline)) {
        			ModelMgr.getModelMgr().setCurrentAnnotationSession(null);
        		}
            }
        });
          
        BrowserPosition consolePosition = (BrowserPosition) SessionMgr.getSessionMgr().getModelProperty(BROWSER_POSITION);
        if (null == consolePosition) {
            consolePosition = getNewBrowserPosition();
        }

        VerticalPanelPicker rightPanel = new VerticalPanelPicker();
        rightPanel.addPanel(Icons.getIcon("page.png"), "Ontology", "Displays an ontology for annotation", ontologyOutline);
        rightPanel.addPanel(Icons.getIcon("page_copy.png"), "Split Picking Tool", "Allows for simulation of flyline crosses", new SplitPickingPanel());
        
        centerRightHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, viewerPanel, rightPanel);
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

        searchToolbar.setVisible(false);

        // Collect the final components
        mainPanel.setLayout(layout);
        allPanelsView.setLayout(new BorderLayout());
        allPanelsView.add(searchToolbar, BorderLayout.NORTH);
        allPanelsView.add(centerLeftHorizontalSplitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        mainPanel.add(allPanelsView, "Regular");
        collapsedOutlineView.setLayout(new BorderLayout());
        mainPanel.add(collapsedOutlineView, "Collapsed FileOutline");
        getContentPane().add(mainPanel, BorderLayout.CENTER);

//        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
//			@Override
//			public void entitySelected(String category, String entityId, boolean clearAll) {
//				updateStatusBar();
//			}
//			@Override
//			public void entityDeselected(String category, String entityId) {
//				updateStatusBar();
//			}
//        });
        
        // Run this later so that the Browser has finished initializing by the time it runs
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		        entityOutline.showLoadingIndicator();

		        final SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

		            private List<Entity> rootList;
		        	
		            protected void doStuff() throws Exception {
		            	rootList = entityOutline.loadRootList();
		            }

		            protected void hadSuccess() {
		                entityOutline.init(rootList);
		            }

		            protected void hadError(Throwable error) {
		                error.printStackTrace();
		                JOptionPane.showMessageDialog(mainPanel, "Error loading data outlines", "Data Load Error", JOptionPane.ERROR_MESSAGE);
		                entityOutline.showNothing();
		            }

		        };
		        entityOutlineLoadingWorker.execute();
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

        if (jSplitPaneRightVertical != null) {
            jSplitPaneRightVertical.removeAll();
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
//                    descriptionObserver.setEntity(newSelection);
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
            SessionMgr.getSessionMgr().setModelProperty(SEARCH_HISTORY, searchDialog.getSearchHistory());
            
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


    // TODO: this was refactored but clients just cast the result to IconDemoPanel for now. At some point we need to 
    // go through every usage of this method and make sure the cast to IconDemoPanel is appropriate, or if additional
    // checks need to be made.
    public Viewer getActiveViewer() {
        return viewerPanel.getActiveViewer();
    }
    
    public Viewer getMainViewer() {
        return viewerPanel.getMainViewer();
    }
    
    public Viewer getSecViewer() {
        return viewerPanel.getSecViewer();
    }
    
    public ViewerSplitPanel getViewersPanel() {
    	return viewerPanel;
    }
    
    public Viewer getViewerForCategory(String category) {
    	if (getMainViewer().getSelectionCategory().equals(category)) {
    		return getMainViewer();
    	}
    	else if (getSecViewer()!=null && getSecViewer().getSelectionCategory().equals(category)) {
    		return getSecViewer();
    	}
    	else {
    		throw new IllegalArgumentException("Unknown viewer category: "+category);
    	}
    }

    public Refreshable getActiveOutline() {
    	return (Refreshable)outlookBar.getVisibleBarComponent();
    }

    public EntityOutline getEntityOutline() {
        return entityOutline;
    }
    
    public OntologyOutline getOntologyOutline() {
        return ontologyOutline;
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

    public PatternSearchDialog getPatternSearchDialog() {
        return patternSearchDialog;
    }

	public GeneralSearchDialog getSearchDialog() {
		return searchDialog;
	}

	public String getMostRecentFileOutlinePath() {
        return mostRecentFileOutlinePath;
    }

    public void toggleViewComponentState(String viewComponentKey) {
        // todo The layout needs to be much nicer.  See IntelliJ layouts, with perhaps the component menu name still visible
        if (VIEW_SEARCH.equals(viewComponentKey)) {
            searchToolbar.setVisible(!searchToolbar.isVisible());
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
}
