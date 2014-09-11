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
import org.janelia.it.workstation.gui.large_volume_viewer.ImageColorModel;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 7/31/13
 * Time: 3:25 PM
 *
 * This popup will give users a snapshot volume.  Very simply viewer, relatively speaking.
 */
public class Snapshot3d extends ModalDialog {
    private Dimension size = new Dimension( 600, 600 );
    private VolumeSource.VolumeAcceptor volumeAcceptor;
    private ImageColorModel imageColorModel;
    private Logger logger = LoggerFactory.getLogger(Snapshot3d.class);
    
    public Snapshot3d() {
        super();
    }

    public void setImageColorModel( ImageColorModel imageColorModel ) {
        this.imageColorModel = imageColorModel;
    }
    
    /**
     * Launching consists of making a load worker, and then executing that.
     *
     * @param volumeSource for getting the data.
     */
    public void launch( VolumeSource volumeSource) {
        SnapshotWorker loadWorker = new SnapshotWorker( volumeSource );
        loadWorker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getMainFrame(), "Fetching data", ""));
        loadWorker.execute();
    }

    private void launch( TextureDataI textureData ) {
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

        this.setPreferredSize( size );
        this.setMinimumSize( size );
        this.setLayout(new BorderLayout());
        this.add( mip3d, BorderLayout.CENTER );

        packAndShow();
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
        protected void hadSuccess() {
            launch( textureData );
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }
    }
}
