package org.janelia.workstation.gui.large_volume_viewer.top_component;

import java.awt.BorderLayout;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.google.common.eventbus.Subscribe;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.janelia.workstation.gui.passive_3d.Snapshot3DLauncher;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
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
    private final LargeVolumeViewViewer lvvv = new LargeVolumeViewViewer();

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
        establishLookups();
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
        Snapshot3DLauncher.removeStaleViewer();
        getLvvv().loadDomainObject(domainObject);
        initialObjectReference = Reference.createFor(domainObject);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        jPanel1.add(lvvv, BorderLayout.CENTER );
        Events.getInstance().registerOnEventBus(this);
        Events.getInstance().registerOnEventBus(lvvv);
    }

    @Override
    public void componentClosed() {
        jPanel1.remove(lvvv);
        Events.getInstance().unregisterOnEventBus(this);
        Events.getInstance().unregisterOnEventBus(lvvv);
        closeGroup();
    }

    protected void closeGroup() {
        Runnable runnable = new Runnable() {
            public void run() {
                lvvv.close();
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
    
    public LargeVolumeViewViewer getLvvv() {
        return lvvv;
    }
    
    public AnnotationManager getAnnotationMgr() {
        return lvvv.hasQuadViewUi() ? lvvv.getQuadViewUi().getAnnotationMgr() : null;
    }

    /**
     * A safe way to ask if editing functions (such as Actions) should be enabled. Returns false if the quad view has not yet been initialized.
     */
    public boolean editsAllowed() {
        return lvvv.hasQuadViewUi() && lvvv.getQuadViewUi().getAnnotationMgr().editsAllowed();
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
        
        if (lvvv != null && lvvv.getQuadViewUi()!=null) {
            Vec3 focus = lvvv.getQuadViewUi().getCameraFocus();
            if (focus != null) {
                String viewFocus = focus.x()+","+focus.y()+","+focus.z();
                log.info("Writing state: viewFocus={}",viewFocus);
                p.setProperty("viewFocus", viewFocus);
            }
            
            // Save view zoom
            double zoom = lvvv.getQuadViewUi().getPixelsPerSceneUnit();
            log.info("Writing state: viewZoom={}",zoom);
            p.setProperty("viewZoom", ""+zoom);
        }
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
            initialObjectReference = Reference.createFor(objectStrRef);

            String viewFocusStr = p.getProperty("viewFocus");
            log.info("Reading state: viewFocus={}",viewFocusStr);
            final Vec3 viewFocus = parseVec3(viewFocusStr);

            String viewZoomStr = p.getProperty("viewZoom");
            log.info("Reading state: viewZoom={}",viewZoomStr);
            final Double viewZoom = parseDouble(viewZoomStr);
            
            if (viewFocus!=null && viewZoom!=null) {
                getLvvv().setInitialViewFocus(viewFocus, viewZoom);
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    log.info("Saving initial reference -> {}", initialObjectReference);
                    if (AccessManager.loggedIn()) {
                        loadInitialState();
                    } else {
                        // Not logged in yet, wait for a SessionStartEvent
                    }
                }
            });
        }
    }

    @Subscribe
    public void sessionStarted(SessionStartEvent event) {
        log.debug("Session started, loading initial state");
        loadInitialState();
    }

    private synchronized void loadInitialState() {
        if (initialObjectReference != null) {

            SimpleWorker worker = new SimpleWorker() {
                DomainObject domainObject = null;

                @Override
                protected void doStuff() throws Exception {
                    TiledMicroscopeDomainMgr tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                    if (TmSample.class.getSimpleName().equals(initialObjectReference.getTargetClassName())) {
                        domainObject = tmDomainMgr.getSample(initialObjectReference.getTargetId());
                    } else if (TmWorkspace.class.getSimpleName().equals(initialObjectReference.getTargetClassName())) {
                        domainObject = tmDomainMgr.getWorkspace(initialObjectReference.getTargetId());
                    } else {
                        log.error("State object is unsupported by the LVV: {}", initialObjectReference);
                    }
                }

                @Override
                protected void hadSuccess() {
                    if (domainObject != null) {
                        openLargeVolumeViewer(domainObject);
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    // Squelch this error because the user does not need to know this failed. They can just re-open the sample manually.
                    log.error("Error loading last open object: {}", initialObjectReference, error);
                }
            };
            worker.execute();
        }
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
    
    private void establishLookups() {
        // Use Lookup to communicate sample location and camera position
        // TODO: separate data source from current view details
        LargeVolumeViewerLocationProvider locProvider = 
                new LargeVolumeViewerLocationProvider(lvvv);
        content.add(locProvider);
        associateLookup(new AbstractLookup(content));
    }
    
    private NeuronSet currNeurons = null;
    public void registerNeurons(NeuronSet neurons) {
        // Use Lookup to communicate neuron reconstructions.
        // Based on tutorial at https://platform.netbeans.org/tutorials/74/nbm-selection-1.html
        if (currNeurons!=null) {
            content.remove(currNeurons);
        }
        content.add(neurons);
        this.currNeurons = neurons;
    }
}
