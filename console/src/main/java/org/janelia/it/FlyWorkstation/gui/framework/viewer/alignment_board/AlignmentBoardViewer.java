package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.*;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.ArtifactType;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.Channels;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.Size;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows alignment board relevant entities in 3D.
 */
public class AlignmentBoardViewer extends Viewer {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardViewer.class);
    
    private Entity alignmentBoard;
    private RootedEntity albRootedEntity;

    private Mip3d mip3d;
    private ABLoadWorker loadWorker;
    private ModelMgrObserver modelMgrObserver;
    private RenderMappingI renderMapping;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);
        renderMapping = new ConfigurableColorMapping();
        setLayout(new BorderLayout());
        ModelMgr.getModelMgr().registerOnEventBus(this);
    }

    @Override
    public void clear() {
        clearObserver();
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
        Entity newEntity = rootedEntity.getEntity();
        if ( ! newEntity.equals( alignmentBoard ) ) {
            // Stop any existing load, to free up the A-board.
            if (loadWorker != null) {
                loadWorker.disregard();
                loadWorker.cancel( true );
            }

            deleteAll();
        }
        alignmentBoard = newEntity;

        setTransferHandler( new ABTransferHandler( alignmentBoard ) );
        refresh();

        // Listen for further changes, so can refresh again later.
        establishObserver();
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
        logger.info("Closing");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
        deleteAll();
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

            // Here, should load volumes, for all the different items given.
//
//            loadWorker = new ABLoadWorker( this, context, mip3d, getRenderMapping() );
//            loadWorker.execute();

        }
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    /**
     * The "controller" should set this color mapping value onto this viewer.  Creating this requires
     * being able to create something that can map colors against renderable objects.
     *
     * @return the color mapping set for this viewer.
     */
    public RenderMappingI getRenderMapping() {
        return renderMapping;
    }

    public void setRenderMapping(RenderMappingI renderMapping) {
        this.renderMapping = renderMapping;
    }

    @Subscribe
    public void handleBoardOpened(AlignmentBoardOpenEvent event) {
        
        AlignmentBoardContext abContext = event.getAlignmentBoardContext();

        log.debug("Alignment board opened: "+abContext.getName());
        log.debug("* Alignment space: "+abContext.getAlignmentContext().getAlignmentSpaceName());
        log.debug("* Optical resolution: "+abContext.getAlignmentContext().getOpticalResolution());
        log.debug("* Pixel resolution: "+abContext.getAlignmentContext().getPixelResolution());
        
        for(AlignedItem alignedItem : abContext.getAlignedItems()) {
        
            EntityWrapper itemEntity = alignedItem.getItemWrapper();
            
            if (itemEntity instanceof Sample) {
            
                Sample sample = (Sample)itemEntity;
                
                log.debug("  Sample: "+sample.getName());
                log.debug("  * 3d image: "+sample.get3dImageFilepath());
                log.debug("  * fast 3d image: "+sample.getFast3dImageFilepath());
                
                if (sample.getChildren()==null) {
                    log.warn("  Sample children not loaded");
                }
                if (sample.getNeuronSet()==null) {
                    log.warn("  Sample neurons not loaded");
                }
                
                MaskedVolume vol = sample.getMaskedVolume();
                if (vol!=null) {
                    log.debug("    original separation volumes:");
                    log.debug("    * reference vol: "+vol.getReferenceVolumePath());
                    log.debug("    * signal vol: "+vol.getSignalVolumePath());
                    log.debug("    * signal label: "+vol.getSignalLabelPath());
                    
                    log.debug("    fast load 8-bit volumes:");
                    log.debug("    * fast signal: "+vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, Size.Full, Channels.All, true));
                    log.debug("    * fast label: "+vol.getFastVolumePath(ArtifactType.ConsolidatedLabel, Size.Full, Channels.All, true));
                    log.info("    * fast reference: "+vol.getFastVolumePath(ArtifactType.Reference, Size.Full, Channels.All, true));
    
                    log.debug("    subsampled volumes:");
                    for(Size size : Size.values()) {
                        log.debug("    * "+size+"/signal: "+vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, size, Channels.All, true));
                        log.debug("    * "+size+"/label: "+vol.getFastVolumePath(ArtifactType.ConsolidatedLabel, size, Channels.All, true));
                        log.info("    * "+size+"/reference: "+vol.getFastVolumePath(ArtifactType.Reference, size, Channels.All, true));
                    }
    
                    log.debug("    mpeg4 volumes:");
                    for(Size size : Size.values()) {
                        for(Channels channels : Channels.values()) {
                            log.debug("    * "+size+"/"+channels+" signal: "+vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, size, channels, false));
                        }
                        log.info("    * "+size+"/reference: "+vol.getFastVolumePath(ArtifactType.Reference, size, Channels.All, false));
                    }
                    
                    log.debug("  metadata files:");
                    for(Size size : Size.values()) {
                        log.debug("  * signal metadata: "+vol.getFastMetadataPath(ArtifactType.ConsolidatedSignal, size));
                        log.debug("  * reference metadata: "+vol.getFastMetadataPath(ArtifactType.Reference, size));
                    }
                }
                
                if (sample.getNeuronSet()!=null) {
                    for(Neuron neuron : sample.getNeuronSet()) {
                        log.debug("  Neuron: "+neuron.getName()+" (mask index = "+neuron.getMaskIndex()+")");
                    }
                }

            }
            else {
                log.error("Cannot handle entites of type: "+itemEntity.getType());    
            }

        }

        // The true update!
        this.updateBoard( abContext );
    }
    
    @Subscribe 
    public void printItemChanged(AlignmentBoardItemChangeEvent event) {

        AlignmentBoardContext abContext = event.getAlignmentBoardContext();

        AlignedItem alignedItem = event.getAlignedItem();

        log.info("* Change Type: "+event.getChangeType());
        log.info("* Item Alias: "+alignedItem.getName());
        log.info("* Item Name: "+alignedItem.getItemWrapper().getName());
        log.info("* Item Visibility: "+alignedItem.isVisible());
        log.info("* Item Color: "+alignedItem.getColor()+" (hex="+alignedItem.getColorHex()+")");

        log.info("Item changed on alignment context: "+abContext.getName());
        log.info("* Item: "+event.getAlignedItem().getName());
        log.info("* Change Type: "+event.getChangeType());    
    }

    @Subscribe 
    public void handleItemChanged(AlignmentBoardItemChangeEvent event) {
        AlignmentBoardContext abContext = event.getAlignmentBoardContext();
        if ( event.getChangeType().equals( AlignmentBoardItemChangeEvent.ChangeType.VisibilityChange )  ||
             event.getChangeType().equals( AlignmentBoardItemChangeEvent.ChangeType.ColorChange ) ) {

        }
        else {
            this.updateBoard( abContext );
        }
    }

    private void establishObserver() {
        modelMgrObserver = new ModelMgrListener( this, alignmentBoard );
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
    }

    private void deleteAll() {
        clearObserver();
        if (loadWorker != null) {
            loadWorker.disregard();
        }
        alignmentBoard = null;
        albRootedEntity = null;
        removeAll();
        mip3d = null;
    }

    private void clearObserver() {
        if ( modelMgrObserver != null ) {
            ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        }
    }

    /**
     * This is called when the board data has been updated.
     */
    private void updateBoard( AlignmentBoardContext context ) {
        logger.info("Update-board called.");

        if (context != null) {
            showLoadingIndicator();

            if ( mip3d == null ) {
                mip3d = new Mip3d();
            }

            mip3d.refresh();

            // Here, should load volumes, for all the different items given.
            loadWorker = new ABLoadWorker( this, context, mip3d, getRenderMapping() );
            loadWorker.execute();

        }

    }

    /**
     * This is called when the board visibility or coloring has been change.
     */
    private void updateRendering( AlignmentBoardContext context ) {
        logger.info("Update-rendering called.");

        if (context != null) {
//            showLoadingIndicator();

//            if ( mip3d == null ) {
//                mip3d = new Mip3d();
//            }

//            mip3d.refresh();

            // Here, simply make the rendering change.
            // Here, should load volumes, for all the different items given.

            loadWorker = new ABLoadWorker( this, context, mip3d, getRenderMapping() );
            loadWorker.execute();

        }

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

}
