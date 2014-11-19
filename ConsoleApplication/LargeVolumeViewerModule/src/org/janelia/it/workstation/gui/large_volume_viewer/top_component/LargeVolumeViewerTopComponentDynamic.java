/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.top_component;

import javax.swing.SwingUtilities;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.it.workstation.gui.passive_3d.Snapshot3DLauncher;
import org.janelia.it.workstation.model.entity.RootedEntity;
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

    protected static final String ACTION = "CTL_LargeVolumeViewerAction=Large Volume Viewer";
    protected static final String WINDOW_NAMER = "CTL_LargeVolumeViewerTopComponent=Large Volume Viewer Tool";
    protected static final String HINT = "HINT_LargeVolumeViewerTopComponent=Examine multi-tile brains.";
    
    protected static Logger logger = LoggerFactory.getLogger( LargeVolumeViewerTopComponent.class );
    private final LargeVolumeViewViewer lvvv = new LargeVolumeViewViewer();

    /*  Apply the settings as follows, to delegate back to this class.
    
    @ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.large_volume_viewer.top_component//LargeVolumeViewer//EN",
        autostore = false
    )
    @TopComponent.Description(
        preferredID = LargeVolumeViewerTopComponentDynamic.LVV_PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
    )
    @TopComponent.Registration(mode = "editor", openAtStartup = false)
    @ActionID(category = "Window", id = "org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent")
    @ActionReference(path = "Menu/Window" //, position = 333 //)
    @TopComponent.OpenActionRegistration(
        displayName = "#CTL_LargeVolumeViewerAction",
        preferredID = LargeVolumeViewerTopComponentDynamic.LVV_PREFERRED_ID
    )
    @Messages({
        ACTION,
        WINDOW_NAMER,
        HINT
    })

    
       Then add a variable of 'this' type to the top component, as 'state'
    
    private final LargeVolumeViewerTopComponentDynamic state = new LargeVolumeViewerTopComponentDynamic();

       Add this method to the TopComponent. 
    
    public void openLargeVolumeViewer( Long entityId ) throws Exception {
        state.load( entityId );
    }

       Make sure the componentClosed method issues state.close()
    
    @Override
    public void componentClosed() {
        jPanel1.remove( state.getLvvv() );
        state.close();
    }


    */
    
    /**
     * @return the lvvv
     */
    protected LargeVolumeViewViewer getLvvv() {
        return lvvv;
    }
    
    protected void load(Long entityId) throws Exception {
        Snapshot3DLauncher.removeStaleViewer();
        RootedEntity rootedEntity = new RootedEntity(
                ModelMgr.getModelMgr().getEntityById(entityId)
        );
        getLvvv().loadEntity(rootedEntity);
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
                logger.error(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

}
