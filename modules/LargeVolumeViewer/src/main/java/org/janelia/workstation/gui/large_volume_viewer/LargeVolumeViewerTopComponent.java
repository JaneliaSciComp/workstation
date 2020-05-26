package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.*;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.google.common.eventbus.Subscribe;

import org.janelia.workstation.common.gui.support.WindowLocator;
import org.janelia.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent;
import org.janelia.workstation.gui.large_volume_viewer.controller.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonController;
import org.janelia.workstation.integration.activity_logging.ToolString;
import org.janelia.workstation.geom.Vec3;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.gui.large_volume_viewer.top_component//LargeVolumeViewer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = LargeVolumeViewerTopComponent.LVV_PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "LargeVolumeViewerTopComponent")
@ActionReference(path = "Menu/Window/Large Volume Viewer", position = 100)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_LargeVolumeViewerAction",
        preferredID = LargeVolumeViewerTopComponent.LVV_PREFERRED_ID
)
@Messages({
    "CTL_LargeVolumeViewerAction=Large Volume Viewer",
    "CTL_LargeVolumeViewerTopComponent=Large Volume Viewer",
    "HINT_LargeVolumeViewerTopComponent=Examine multi-tile brains."
})
public final class LargeVolumeViewerTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(LargeVolumeViewerTopComponent.class);

    public static final String LVV_PREFERRED_ID = "LargeVolumeViewerTopComponent";
    public static final String TC_VERSION = "1.0";
    
    public static final ToolString LVV_LOGSTAMP_ID = new ToolString("LargeVolumeViewer");

    private static boolean restoreStateOnOpen = true;
    
    private final InstanceContent content = new InstanceContent();
    private Vec3 initialViewFocus;
    private Double initialZoom;
    private QuadViewUi viewUI;

    // Initial State
    private Reference initialObjectReference;

    static {
        // So popup menu shows over GLCanvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    }
    
    public static void setRestoreStateOnOpen(boolean value) {
        // A hack to ensure that if the LVV is opened by the user after app launch it does not restore its previous state
        restoreStateOnOpen = value;
    }
    
    public static final LargeVolumeViewerTopComponent getInstance() {
        return (LargeVolumeViewerTopComponent)WindowManager.getDefault().findTopComponent(LVV_PREFERRED_ID);
    }
    
    public LargeVolumeViewerTopComponent() {
        initComponents();
        setName(Bundle.CTL_LargeVolumeViewerTopComponent());
        setToolTipText(Bundle.HINT_LargeVolumeViewerTopComponent());
    }

    public DomainObject getCurrent() {
        return getLookup().lookup(DomainObject.class);
    }

    public boolean setCurrent(DomainObject domainObject) {
        DomainObject curr = getCurrent();
        if (curr!=null) {
            if (domainObject!=null && domainObject.equals(curr)) {
                return false;
            }
            content.remove(curr);
        }
        if (domainObject!=null) {
            content.add(domainObject);
        }
        return true;
    }
    
    public void openLargeVolumeViewer(DomainObject domainObject) {
        log.info("openLargeVolumeViewer({})",domainObject);
        setCurrent(domainObject);
        initialObjectReference = Reference.createFor(domainObject);
    }

    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();

        jPanel1.setLayout(new java.awt.BorderLayout());
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }

    private javax.swing.JPanel jPanel1;

    @Override
    public void componentOpened() {
        if (viewUI==null)
            initialize();
    }

    @Override
    public void componentClosed() {
        jPanel1.remove(viewUI);
        closeGroup();
    }

    protected void closeGroup() {
        Runnable runnable = new Runnable() {
            public void run() {
                TopComponentGroup tcg = WindowManager.getDefault().findTopComponentGroup(
                        "large_volume_viewer_plugin"
                );
                if (tcg != null) {
                    tcg.close();
                }
            }
        };
        if ( SwingUtilities.isEventDispatchThread() ) {
            runnable.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait( runnable );
            } catch ( Exception ex ) {
                log.error("Problem closing LVV component group",ex);
            }
        }
    }

    public void initialize() {
        if ( viewUI == null ) {
            // trying to diagnost how this can be null later
            viewUI =  QuadViewUiProvider.createQuadViewUi(FrameworkAccess.getMainFrame(), false);
        }
        removeAll();
        viewUI.setVisible(true);
        add((Component) viewUI);

        // Repaint the skeleton
        SkeletonController.getInstance().skeletonChanged(true);

        revalidate();
        repaint();

        // Need to popup the skeletal viewer.
        AnnotationSkeletalViewTopComponent asvtc =
                (AnnotationSkeletalViewTopComponent) WindowLocator.getByName(
                        AnnotationSkeletalViewTopComponent.PREFERRED_ID
                );
        if (asvtc != null) {
            asvtc.revalidate();
            asvtc.repaint();
        }

        if (initialViewFocus!=null) {
            log.info("Setting initial camera focus: {}", initialViewFocus);
            viewUI.setCameraFocus(initialViewFocus);
            initialViewFocus = null;
        }
        if (initialZoom!=null) {
            log.info("Setting initial zoom: {}", initialZoom);
            viewUI.setPixelsPerSceneUnit(initialZoom);
            initialZoom = null;
        }
    }

    void writeProperties(java.util.Properties p) {
        
        p.setProperty("version", TC_VERSION);
        
        // Save currently open object
        
        DomainObject current = getCurrent();
        if (current!=null) {
            String objectRef = Reference.createFor(current).toString();
            log.info("Writing state: objectRef={}",objectRef);
            p.setProperty("objectRef", objectRef);
        }
        else {
            p.remove("objectRef");
        }
        
        // Save view focus
        Vec3 focus = viewUI.getCameraFocus();
        if (focus != null) {
            String viewFocus = focus.x()+","+focus.y()+","+focus.z();
            log.info("Writing state: viewFocus={}",viewFocus);
            p.setProperty("viewFocus", viewFocus);
        }

        // Save view zoom
        double zoom =viewUI.getPixelsPerSceneUnit();
        log.info("Writing state: viewZoom={}",zoom);
        p.setProperty("viewZoom", ""+zoom);
    }

    void readProperties(java.util.Properties p) {

        if (!restoreStateOnOpen) return;
        
        if (!ApplicationPanel.isLoadLastObject()) {
            return;
        }
        
        String version = p.getProperty("version");
        if (!TC_VERSION.equals(version)) return;
        
        String objectStrRef = p.getProperty("objectRef");
        log.info("Reading state: objectRef={}",objectStrRef);
        
        if (!StringUtils.isEmpty(objectStrRef)) {
            String viewFocusStr = p.getProperty("viewFocus");
            log.info("Reading state: viewFocus={}",viewFocusStr);
            final Vec3 viewFocus = parseVec3(viewFocusStr);

            String viewZoomStr = p.getProperty("viewZoom");
            log.info("Reading state: viewZoom={}",viewZoomStr);
            final Double viewZoom = parseDouble(viewZoomStr);
            
            if (viewFocus!=null && viewZoom!=null) {
                setInitialViewFocus(viewFocus, viewZoom);
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    log.info("Saving initial reference -> {}", initialObjectReference);
                }
            });
        }
    }

    private void setInitialViewFocus(Vec3 initialViewFocus, Double initialZoom) {
        this.initialViewFocus = initialViewFocus;
        this.initialZoom = initialZoom;
    }

    @Subscribe
    public void sessionStarted(SessionStartEvent event) {
        log.debug("Session started, loading initial state");
    }

    private Vec3 parseVec3(String vecStr) {
        try {
            if (!StringUtils.isBlank(vecStr)) {
                String[] vecArr = vecStr.split(",");
                Double x = new Double(vecArr[0]);
                Double y = new Double(vecArr[1]);
                Double z = new Double(vecArr[2]);
                return new Vec3(x, y, z);
            }
        }
        catch (Exception e) {
            log.warn("Could not parse vector: "+vecStr);
        }
        return null;
    }
    
    private Double parseDouble(String doubleStr) {
        try {
            if (doubleStr!=null) {
                return new Double(doubleStr);
            }
        }
        catch (NumberFormatException e) {
            log.warn("Could not parse double: "+doubleStr);
        }
        return null;
    }

    public QuadViewUi getQuadViewUi() {
        return viewUI;
    }

    public AnnotationManager getAnnotationMgr() {
        return viewUI.getAnnotationMgr();
    }
}
