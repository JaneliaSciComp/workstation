package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.viewer3d.*;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.Collection;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
public class Snapshot3d extends JPanel {
    // Chosing dimensions of same aspect ratio as expected data.
    private static final Dimension WIDGET_SIZE = new Dimension( 400, 800 );
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

    private void launch( Collection<TextureDataI> textureDatas ) {
        cleanup();

        Mip3d mip3d = new Mip3d();
        mip3d.clear();
                
        Comparator<TextureDataI> comparator = new Comparator<TextureDataI>() {
            @Override
            public int compare(TextureDataI o1, TextureDataI o2) {
                return o1.getHeader().compareTo(o2.getHeader());
            }            
        };
        Collections.sort( new ArrayList(textureDatas), comparator );

        VolumeModel volumeModel = mip3d.getVolumeModel();
        volumeModel.removeAllListeners();
        volumeModel.resetToDefaults();
        SnapshotVolumeBrick brick = new SnapshotVolumeBrick( volumeModel );
        brick.setImageColorModel( imageColorModel );
        if ( textureDatas.size() == 1 ) {
            brick.setPrimaryTextureData(textureDatas.iterator().next());
        }
        else {
            brick.setTextureDatas(textureDatas);
        }
        mip3d.addActor( brick );
        
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
    }

    private void cleanup() {
        // Cleanup old widgets.
        for ( Component c: locallyAddedComponents ) {
            this.remove( c );
            if ( c instanceof Mip3d ) {
                Mip3d mip3d = (Mip3d)c;
                mip3d.setVisible(false);
                mip3d.clear();
            }
        }
        locallyAddedComponents.clear();
        validate();
        repaint();
    }
    
    private class SnapshotWorker extends SimpleWorker {
        private final VolumeSource volumeSource;
        private Collection<TextureDataI> textureDatas;

        public SnapshotWorker( VolumeSource collector ) {
            this.volumeSource = collector;
            textureDatas = Collections.<TextureDataI>synchronizedCollection( new ArrayList<TextureDataI>() );
        }

        @Override
        protected void doStuff() throws Exception {
            volumeAcceptor = new VolumeSource.VolumeAcceptor() {
                @Override
                public void accept(TextureDataI textureData) {                    
                    SnapshotWorker.this.textureDatas.add( textureData );
                }
            };
            try {
                volumeSource.getVolume( volumeAcceptor );
            } catch ( RuntimeException rte ) {
                throw new Exception(rte);
            }
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
            progressMonitor.setNote( "Launching viewer." );
            launch( textureDatas );
        }

        @Override
        protected void hadError(Throwable error) {
            SessionMgr.getSessionMgr().handleException( error );
        }
    }
}
