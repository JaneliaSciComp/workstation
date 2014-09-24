package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.*;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.Collection;
import java.util.ArrayList;
import javax.swing.JLabel;
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/31/13
 * Time: 3:25 PM
 *
 * This popup will give users a snapshot volume.  Very simple viewer, relatively speaking.
 */
public class Snapshot3d extends ModalDialog {
    // Choosing initial width > height as workaround to the reset-focus problem.
    private static final Dimension WIDGET_SIZE = new Dimension( 650, 600 );
    private VolumeSource.VolumeAcceptor volumeAcceptor;
    private Collection<Component> locallyAddedComponents = new ArrayList<>();
    private ImageColorModel imageColorModel;
    private IndeterminateNoteProgressMonitor monitor;
    private String labelText;

    private static Snapshot3d snapshotInstance;
    private final Logger logger = LoggerFactory.getLogger(Snapshot3d.class);
    
    public static Snapshot3d getInstance() {
        if ( snapshotInstance == null ) {
            snapshotInstance = new Snapshot3d();
        }
        return snapshotInstance;
    }
    
    private Snapshot3d() {
        super();
        super.setModal( false );
    }

    public void setImageColorModel( ImageColorModel imageColorModel ) {
        this.imageColorModel = imageColorModel;
        this.imageColorModel.getColorModelChangedSignal().addObserver(
                new Observer() {
                    public void update( Observable target, Object obj ) {
                        Snapshot3d.this.validate();
                        Snapshot3d.this.repaint();
                    }
                }
        );
    }
    
    public void setLoadProgressMonitor( IndeterminateNoteProgressMonitor monitor ) {
        this.monitor = monitor;
    }

    public void setLabelText( String labelText ) {
        this.labelText = labelText;
    }
    
    public IndeterminateNoteProgressMonitor getMonitor() {
        return monitor;
    }
    
    /**
     * Launching consists of making a load worker, and then executing that.
     *
     * @param volumeSource for getting the data.
     */
    public void launch( MonitoredVolumeSource volumeSource ) {
        SnapshotWorker loadWorker = new SnapshotWorker( volumeSource );
        if ( getMonitor() == null ) {
            setLoadProgressMonitor( new IndeterminateNoteProgressMonitor(SessionMgr.getMainFrame(), "Fetching tiles", volumeSource.getInfo()) );
        }
        loadWorker.setProgressMonitor( getMonitor() );
        volumeSource.setProgressMonitor( getMonitor() );        
        loadWorker.execute();
    }

    private void launch( TextureDataI textureData ) {
        cleanup();

        Mip3d mip3d = new Mip3d();
        mip3d.clear();
        VolumeBrickFactory factory = new VolumeBrickFactory() {
            @Override
            public VolumeBrickI getVolumeBrick(VolumeModel model) {
                SnapshotVolumeBrick svb = new SnapshotVolumeBrick( model );
                svb.setImageColorModel( imageColorModel );
                return svb;
            }
            @Override
            public VolumeBrickI getVolumeBrick(VolumeModel model, TextureDataI maskTextureData, TextureDataI renderMapTextureData ) {
                return null; // Trivial case.
            }
        };

        VolumeBrickActorBuilder actorBuilder = new VolumeBrickActorBuilder();
        GLActor actor = actorBuilder.buildVolumeBrickActor(mip3d.getVolumeModel(), factory, textureData);
        if ( actor != null ) {
            mip3d.addActor(actor);
        }
        else {
            logger.error("Failed to create volume brick for {}.", textureData.getFilename());
        }
        
        locallyAddedComponents.add( mip3d );
        this.setPreferredSize( WIDGET_SIZE );
        this.setMinimumSize( WIDGET_SIZE );
        this.setLayout(new BorderLayout());
        if ( labelText != null ) {
            final JLabel label = new JLabel( labelText );
            locallyAddedComponents.add( label );
            this.add( label, BorderLayout.SOUTH );
        }
        this.add( mip3d, BorderLayout.CENTER );

        packAndShow();
    }

    private void cleanup() {
        // Cleanup old widgets.
        for ( Component c: locallyAddedComponents ) {
            this.remove( c );
        }
        locallyAddedComponents.clear();
    }

    private class SnapshotWorker extends SimpleWorker {
        private final VolumeSource volumeSource;
        private TextureDataI textureData;

        public SnapshotWorker( VolumeSource collector ) {
            this.volumeSource = collector;
        }

        @Override
        protected void doStuff() throws Exception {
            volumeAcceptor = new VolumeSource.VolumeAcceptor() {
                @Override
                public void accept(TextureDataI textureData) {
                    SnapshotWorker.this.textureData = textureData;
                }
            };
            volumeSource.getVolume( volumeAcceptor );
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (progressMonitor == null) {
                return;
            }
            if ("progress".equals(e.getPropertyName())) {
                int progress = (Integer) e.getNewValue();
                progressMonitor.setProgress(progress);
                if (progressMonitor.isCanceled()) {
                    super.cancel(true);
                }
            }
        }

        @Override
        protected void hadSuccess() {
            launch( textureData );
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }
    }
}
