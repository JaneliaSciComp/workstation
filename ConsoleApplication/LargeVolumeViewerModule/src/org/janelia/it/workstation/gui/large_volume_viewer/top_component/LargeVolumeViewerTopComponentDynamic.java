package org.janelia.it.workstation.gui.large_volume_viewer.top_component;

import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.it.workstation.gui.passive_3d.Snapshot3DLauncher;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The actual "state" of the top component.  Delegate things to here, so that
 * the Design Editor can be free to start over completely, should that prove
 * necessary.  Put differently: this class allows the Top Component's design
 * to vary independently of its object state/behavior.
 * 
 * @author fosterl
 */
public class LargeVolumeViewerTopComponentDynamic {
    public static final String LVV_PREFERRED_ID = "LargeVolumeViewerTopComponent";
    public static final ToolString LVV_LOGSTAMP_ID = new ToolString("LargeVolumeViewer");

    protected static final String ACTION = "CTL_LargeVolumeViewerAction=Large Volume Viewer";
    protected static final String WINDOW_NAMER = "CTL_LargeVolumeViewerTopComponent=Large Volume Viewer";
    protected static final String HINT = "HINT_LargeVolumeViewerTopComponent=Examine multi-tile brains.";
    
    protected static Logger logger = LoggerFactory.getLogger( LargeVolumeViewerTopComponent.class );
    private final LargeVolumeViewViewer lvvv = new LargeVolumeViewViewer();

    /**
     * @return the lvvv
     */
    protected LargeVolumeViewViewer getLvvv() {
        return lvvv;
    }

    public void load(DomainObject domainObject) {
        Snapshot3DLauncher.removeStaleViewer();
        getLvvv().loadDomainObject(domainObject);
    }
    
    protected void close() {
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
                logger.error("Problem closing LVV component group",ex);
            }
        }
    }
}
