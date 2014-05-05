package org.janelia.it.FlyWorkstation.gui.passive_3d;

import org.janelia.it.FlyWorkstation.gui.dialogs.ModalDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.static_view.RGBExcludableVolumeBrick;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeBrickFactory;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeBrickI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.loader.texture.TextureDataI;

import java.awt.*;

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

    public Snapshot3d() {
        super();
    }

    /**
     * Launching consists of making a load worker, and then executing that.
     *
     * @param volumeSource for getting the data.
     */
    public void launch( VolumeSource volumeSource) {
        SnapshotWorker loadWorker = new SnapshotWorker( volumeSource );
        loadWorker.execute();
    }

    private void launch( TextureDataI textureData ) {
        Mip3d mip3d = new Mip3d();
        mip3d.clear();
        VolumeBrickFactory factory = new VolumeBrickFactory() {
            @Override
            public VolumeBrickI getVolumeBrick(VolumeModel model) {
                return new RGBExcludableVolumeBrick( model );
            }
            @Override
            public VolumeBrickI getVolumeBrick(VolumeModel model, TextureDataI maskTextureData, TextureDataI renderMapTextureData ) {
                return null; // Trivial case.
            }
        };
        mip3d.setVolume(factory, textureData);
        this.setPreferredSize( size );
        this.setMinimumSize( size );
        this.setLayout(new BorderLayout());
        this.add( mip3d, BorderLayout.CENTER );

        packAndShow();
    }

    private class SnapshotWorker extends SimpleWorker {
        private VolumeSource volumeSource;

        public SnapshotWorker( VolumeSource collector ) {
            this.volumeSource = collector;
        }

        @Override
        protected void doStuff() throws Exception {
            volumeAcceptor = new VolumeSource.VolumeAcceptor() {
                @Override
                public void accept(TextureDataI textureData) {
                    launch( textureData );
                }
            };
            volumeSource.getVolume( volumeAcceptor );
        }

        @Override
        protected void hadSuccess() {
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }
    }
}
