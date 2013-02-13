package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.LayersPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorWheelColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
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
        TransferHandler transferHandler = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport supp) {
                // Check for String flavor
                for ( DataFlavor flavor: supp.getDataFlavors() ) {
                    if (!supp.isDataFlavorSupported( flavor )) {
                        return false;
                    }
                }

                // Fetch the drop location
                DropLocation loc = supp.getDropLocation();

                // Return whether we accept the location
                return true;
            }

            @Override
            public boolean importData(TransferSupport supp) {
                if (!canImport(supp)) {
                    return false;
                }

                // Fetch the Transferable and its data
                Transferable t = supp.getTransferable();
                for ( DataFlavor flavor: supp.getDataFlavors() ) {
                    try {
                        Object data = t.getTransferData( flavor );

                        // Fetch the drop location
                        DropLocation loc = supp.getDropLocation();

                        // Do something.
                        if ( data instanceof ArrayList ) {
                            ArrayList list = (ArrayList)data;
                            if ( list.size() > 0 ) {
                                Object firstItem = list.get( 0 );
                                if ( firstItem instanceof Entity ) {
                                    Entity draggedEntity = (Entity)firstItem;
                                    if ( alignmentBoard != null ) {
                                        alignmentBoard.addChildEntity( draggedEntity );
                                    }
                                }
                            }
                        }
                        System.out.println("Accepting import data " + loc);
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                    }
                }

                return true;
            }
        };

        setTransferHandler( transferHandler );

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

    /**
     * Accumulates all data for masking, from the set of files provided, preparing them for
     * injection into th evolume being loaded.
     *
     * @param maskFiles list of all mask files to use against the signal volumes.
     */
    private VolumeMaskBuilder createMaskBuilder(
            List<String> maskFiles, List<FragmentBean> fragments, FileResolver resolver
    ) {

        VolumeMaskBuilder volumeMaskBuilder = null;
        // Build the masking texture info.
        if (maskFiles != null  &&  maskFiles.size() > 0) {
            VolumeMaskBuilder builder = new VolumeMaskBuilder();
            builder.setFragments( fragments );
            for ( String maskFile: maskFiles ) {
                VolumeLoader volumeLoader = new VolumeLoader( resolver );
                volumeLoader.loadVolume(maskFile);
                volumeLoader.populateVolumeAcceptor(builder);
            }
            volumeMaskBuilder = builder;
        }

        return volumeMaskBuilder;
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

        public void setFilenames(
                List<String> signalFilenames,
                List<String> maskFilenames
        ) {
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


            // *** TEMP *** this sets up a test of mapping neuron fragment number vs color.
            ColorMappingI colorMapper = new ColorWheelColorMapping();
            mip3d.setMaskColorMappings( colorMapper.getMapping( alignmentBoardDataBuilder.getFragments() ) );

            FileResolver resolver = new CacheFileResolver();
            for ( String signalFilename: signalFilenames ) {
                List<String> maskFilenamesForSignal = alignmentBoardDataBuilder.getMaskFilenames( signalFilename );
                VolumeMaskBuilder volumeMaskBuilder = createMaskBuilder(
                        maskFilenamesForSignal, alignmentBoardDataBuilder.getFragments( signalFilename ), resolver
                );

                mip3d.loadVolume( signalFilename, volumeMaskBuilder, resolver );
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
