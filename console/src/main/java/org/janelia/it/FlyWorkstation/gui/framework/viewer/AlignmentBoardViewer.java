package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.LayersPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.AlignmentBoardDataBuilder;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows alignment board relevant entities in 3D.
 */
public class AlignmentBoardViewer extends Viewer {

    private final AlignmentBoardDataBuilder alignmentBoardDataBuilder = new AlignmentBoardDataBuilder();
    private Entity alignmentBoard;
    private RootedEntity albRootedEntity;

    private Mip3d mip3d;
    private ABLoadWorker loadWorker;
    private ModelMgrObserver modelMgrObserver;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);
        setLayout(new BorderLayout());
    }

    @Override
    public void clear() {
        // TODO watch for problems, here.
        refresh();
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        //this.updateUI();
        revalidate();
        repaint();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        alignmentBoard = rootedEntity.getEntity();
        if (loadWorker != null) {
            loadWorker.disregard();
            loadWorker.cancel( true );
        }
        loadWorker = new ABLoadWorker();
        refresh();

        // Listen for further changes, so can refresh again later.
        modelMgrObserver = new ModelMgrListener( this, alignmentBoard );
        ModelMgr.getModelMgr().addModelMgrObserver( modelMgrObserver );
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {
        loadEntity(rootedEntity);
        try {
            if ( success != null )
                success.call();
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    @Override
    public List<RootedEntity> getRootedEntities() {
        return Arrays.asList( albRootedEntity );
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public RootedEntity getRootedEntityById(String uniqueId) {
        return albRootedEntity;
    }

    @Override
    public Entity getEntityById(String id) {
        return alignmentBoard;
    }

    @Override
    public void close() {
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        if (loadWorker != null) {
            loadWorker.disregard();
        }
        alignmentBoard = null;
        albRootedEntity = null;
        removeAll();
        mip3d = null;
    }

    @Override
    public void refresh() {
        logger.info("Refresh called.");

        if (alignmentBoard != null) {
            showLoadingIndicator();

            if ( mip3d == null ) {
                mip3d = new Mip3d();
            }

            mip3d.refresh();

            mip3d.setClearOnLoad( true );

            // Here, should load volumes, for all the different items given.

            // Activate the layers panel for controlling visibility. This code might have to be moved elsewhere.
            LayersPanel layersPanel = SessionMgr.getBrowser().getLayersPanel();
            layersPanel.showEntities(alignmentBoard.getOrderedChildren());
            SessionMgr.getBrowser().selectRightPanel(layersPanel);

            alignmentBoardDataBuilder.setAlignmentBoard( alignmentBoard );
            List<String> signalFilenames = alignmentBoardDataBuilder.getSignalFilenames();
            List<String> maskFilenames = alignmentBoardDataBuilder.getMaskFilenames();

            loadWorker.setFilenames( signalFilenames, maskFilenames );
            loadWorker.execute();

        }

    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    //------------------------------Inner Classes
    /** Listens for changes to the child-set of the heard-entity. */
    public static class ModelMgrListener extends ModelMgrAdapter {
        private Entity heardEntity;
        private AlignmentBoardViewer viewer;
        ModelMgrListener( AlignmentBoardViewer viewer, Entity e ) {
            heardEntity = e;
            this.viewer = viewer;
        }

        @Override
        public void entityChildrenChanged(long entityId) {
            if (heardEntity.getId() == entityId) {
                viewer.refresh();
            }
        }
    }

    public class ABLoadWorker extends SimpleWorker {
        private List<String> maskFilenames;
        private List<String> signalFilenames;

        public void setFilenames( List<String> signalFilenames, List<String> maskFilenames ) {
            this.signalFilenames = signalFilenames;
            this.maskFilenames = maskFilenames;
        }

        @Override
        protected void doStuff() throws Exception {
            if ( signalFilenames == null  ||  maskFilenames == null ) {
                return;
            }
            else if ( signalFilenames.size() == 0 ) {
                mip3d.clear();
                return;
            }

            FileResolver resolver = new CacheFileResolver();
            mip3d.setMaskFiles(maskFilenames, resolver);

            // *** TEMP *** this sets up a test of mapping neuron fragment number vs color.
            Map<Integer,byte[]> maskMappings = new HashMap<Integer,byte[]>();
//for (int i=0; i < 65535; i++) {
//    maskMappings.put(i, new byte[]{ (byte)0xff, (byte)0, (byte)0xff });
//}

            maskMappings.put( 15, new byte[] { (byte)0x00, (byte)0x00, (byte)0xff } );
            maskMappings.put( 22, new byte[] { (byte)0x00, (byte)0xff, (byte)0x00 } );
            maskMappings.put( 41, new byte[] { (byte)0xff, (byte)0x00, (byte)0x00 } );
            mip3d.setMaskColorMappings( maskMappings );

            for ( String signalFilename: signalFilenames ) {
                mip3d.loadVolume( signalFilename, resolver);
                // After first volume has been loaded, unset clear flag, so subsequent
                // ones are overloaded.
                mip3d.setClearOnLoad(false);
            }
        }

        @Override
        protected void hadSuccess() {
            // Add this last.  "show-loading" removes it.  This way, it is shown only
            // when it becomes un-busy.
            AlignmentBoardViewer.this.removeAll();
            add(mip3d, BorderLayout.CENTER);

            revalidate();
            repaint();
        }

        @Override
        protected void hadError(Throwable error) {
            AlignmentBoardViewer.this.removeAll();
            revalidate();
            repaint();
            SessionMgr.getSessionMgr().handleException( error );
        }
    };

}
