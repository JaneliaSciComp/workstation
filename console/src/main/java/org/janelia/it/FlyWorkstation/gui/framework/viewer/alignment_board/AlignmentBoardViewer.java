package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityWrapperTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.AlignmentBoardSettingsDialog;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.RangeSlider;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableDataSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.ABContextDataSource;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
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
public class AlignmentBoardViewer extends Viewer implements AlignmentBoardControllable {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardViewer.class);
    private static final String GEO_SEARCH_TOOLTIP = "<html>" +
            "Adjust the X, Y, and Z sliders, to leave only the template volume of <br>" +
            "search lit.  This template volume will then be used to derive the coordinates, <br>" +
            "to search other specimens and present the resulting overlappoing volume." +
            "</html>";

    private Entity alignmentBoard;
    private RootedEntity albRootedEntity;

    private Mip3d mip3d;
    private RenderablesLoadWorker loadWorker;
    private RangeSlider xSlider;
    private RangeSlider ySlider;
    private RangeSlider zSlider;
    private JPanel wrapperPanel;

    private ModelMgrObserver modelMgrObserver;
    private RenderMappingI renderMapping;
    private BrainGlow brainGlow;
    private AlignmentBoardSettingsDialog settings;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);

        renderMapping = new ConfigurableColorMapping();
        setLayout(new BorderLayout());
        ModelMgr.getModelMgr().registerOnEventBus(this);
        
        setTransferHandler(new EntityWrapperTransferHandler() {
            @Override
            public JComponent getDropTargetComponent() {
                return AlignmentBoardViewer.this;
            }
        });
    }

    @Override
    public void clear() {
        clearObserver();
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
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
                mip3d = createMip3d();
            }

            mip3d.refresh();
        }
    }

    @Override
    public void totalRefresh() {
        refresh();
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
            
            if ( itemEntity instanceof Sample  &&  alignedItem.isVisible() ) {
            
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
                    log.debug("  neurons:");
                    for(Neuron neuron : sample.getNeuronSet()) {
                        log.debug("    "+neuron.getName()+" (mask index = "+neuron.getMaskIndex()+")");
                        log.debug("    * mask: "+neuron.getMask3dImageFilepath());
                        log.debug("    * chan: "+neuron.getChan3dImageFilepath());
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

            // Changing the render mapping values.
            this.updateRendering( abContext );

        }
        else {
            this.updateBoard( abContext );
        }
    }

    //---------------------------------------IMPLEMNTATION of AlignmentBoardControllable
    @Override
    public void clearDisplay() {
        mip3d.clear();
    }

    /**
     * Callback from loader threads to control loading information.
     *
     * @param signalTexture for the signal
     * @param maskTexture for the mask
     */
    @Override
    public void loadVolume( TextureDataI signalTexture, TextureDataI maskTexture ) {

        if ( ! mip3d.setVolume(
                signalTexture, maskTexture, renderMapping, (float)AlignmentBoardSettingsDialog.DEFAULT_GAMMA
        ) ) {
            logger.error( "Failed to load volume to mip3d." );
        }
        else {
            xSlider.setMaximum( signalTexture.getSx() );
            ySlider.setMaximum( signalTexture.getSy() );
            zSlider.setMaximum( signalTexture.getSz() );

            xSlider.setValue( 0 );
            xSlider.setUpperValue( signalTexture.getSx() );
            ySlider.setValue( 0 );
            ySlider.setUpperValue( signalTexture.getSy() );
            zSlider.setValue( 0 );
            zSlider.setUpperValue( signalTexture.getSz() );

            ChangeListener listener = new SliderChangeListener( xSlider, ySlider, zSlider, mip3d );

            xSlider.addChangeListener( listener );
            ySlider.addChangeListener( listener );
            zSlider.addChangeListener( listener );
        }

    }

    @Override
    public void displayReady() {
        mip3d.refresh();

        // Strip any "show-loading" off the viewer.
        removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        add(wrapperPanel, BorderLayout.CENTER);

    }

    @Override
    public void loadCompletion( boolean successful, boolean loadFiles, Throwable error ) {
        if ( successful ) {
            revalidate();
            repaint();

            if ( loadFiles ) {
                mip3d.refresh();
            }
            else {
                mip3d.refreshRendering();
            }

        }
        else {
            removeAll();
            revalidate();
            repaint();
            SessionMgr.getSessionMgr().handleException(error);
        }

    }

    //---------------------------------------HELPERS
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

        // TEMP
        //if ( brainGlow != null ) {
        //    brainGlow.isRunning = false;
        //} // TEMP

        if (context != null) {
            showLoadingIndicator();

            if ( mip3d == null ) {
                mip3d = createMip3d();
                wrapperPanel = createWrapperPanel( mip3d );
            }

            mip3d.refresh();

            // Here, should load volumes, for all the different items given.
            //loadWorker = new ABLoadWorker( this, context, mip3d );
            loadWorker = new RenderablesLoadWorker(
                    new ABContextDataSource( context ), renderMapping, this, settings.getAlignmentBoardSettings()
            );
            loadWorker.execute();

        }

        // TEMP
        //brainGlow = new BrainGlow();
        //brainGlow.start();  // TEMP
    }

    /**
     * Build out the Mip3D object for rendering all.  Make listeners on it so the viewer change its data
     * as needed.
     */
    private Mip3d createMip3d() {
        Mip3d rtnVal = new Mip3d();
        settings = new AlignmentBoardSettingsDialog( rtnVal );
        settings.setDownSampleRate( AlignmentBoardSettingsDialog.DEFAULT_DOWNSAMPLE_RATE );
        settings.addSettingsListener(
                new AlignmentBoardSettingsListener( rtnVal, this )
        );

        rtnVal.addMenuAction( settings.getLaunchAction() );
        return rtnVal;
    }

    private JPanel createWrapperPanel( Mip3d mip3d ) {
        JPanel rtnVal = new JPanel();
        rtnVal.setLayout( new BorderLayout() );
        xSlider = new RangeSlider( 0, 100 );  // Dummy starting ranges.
        ySlider = new RangeSlider( 0, 100 );
        zSlider = new RangeSlider( 0, 100 );
        xSlider.setBorder( new TitledBorder( "Selection X Bounds" ) );
        ySlider.setBorder( new TitledBorder( "Selection Y Bounds" ) );
        zSlider.setBorder( new TitledBorder( "Selection Z Bounds" ) );
        JButton searchButton = new JButton( "Geometric Search" );
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout( new GridLayout( 1, 4 ) );
        sliderPanel.add( xSlider );
        sliderPanel.add( ySlider );
        sliderPanel.add( zSlider );
        sliderPanel.add( searchButton );
        sliderPanel.setToolTipText(GEO_SEARCH_TOOLTIP);
        searchButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent ae ) {
                logger.info(
                        "Selection covers (X): " + xSlider.getValue() + ".." + xSlider.getUpperValue() +
                        " and (Y): " + ySlider.getValue() + ".." + ySlider.getUpperValue() +
                        " and (Z): " + zSlider.getValue() + ".." + zSlider.getUpperValue()
                );
                writeBackVolumeSelection();

            }
        });
        rtnVal.add( mip3d, BorderLayout.CENTER );
        rtnVal.add( sliderPanel, BorderLayout.SOUTH );
        return rtnVal;
    }

    /**
     * This is called when the board visibility or coloring has been change.
     */
    private void updateRendering( AlignmentBoardContext context ) {
        logger.info("Update-rendering called.");

        if (context != null) {
            // Here, simply make the rendering change.
            // Here, should load volumes, for all the different items given.

            //loadWorker = new ABLoadWorker( this, context, mip3d, renderMappings );
            loadWorker = new RenderablesLoadWorker(
                    new ABContextDataSource(context), renderMapping, this, settings.getAlignmentBoardSettings()
            );
            loadWorker.setLoadFilesFlag( Boolean.FALSE );
            loadWorker.execute();

        }

    }

    /** This control-callback writes the user's selected volume to a file on disk. */
    private void writeBackVolumeSelection() {
        Map<Integer,byte[]> renderableIdVsRenderMethod = renderMapping.getMapping();

        // Determine whether the user has slid the sliders out of default position.
        boolean partialVolumeConstraints = false;
        if ( xSlider.getValue() != 0  ||  ySlider.getValue() != 0  ||  zSlider.getValue() != 0 ) {
            partialVolumeConstraints = true;
        }
        else if ( xSlider.getUpperValue() != xSlider.getMaximum()  ||
                  ySlider.getUpperValue() != ySlider.getMaximum()  ||
                  zSlider.getUpperValue() != zSlider.getMaximum() ) {
            partialVolumeConstraints = true;
        }

        final float[] cropCoords =
                partialVolumeConstraints ?
                        getCropCoords( new RangeSlider[] { xSlider, ySlider, zSlider } ) :
                        null;

        // Convert crop coords back into full-range values, and invert any downsample.
        if ( cropCoords != null ) {
            double downSampleRate = settings.getDownsampleRate();
            cropCoords[ 0 ] = (float)(xSlider.getMaximum() * downSampleRate * cropCoords[ 0 ]);
            cropCoords[ 1 ] = (float)(xSlider.getMaximum() * downSampleRate * cropCoords[ 1 ]);

            cropCoords[ 2 ] = (float)(ySlider.getMaximum() * downSampleRate * cropCoords[ 2 ]);
            cropCoords[ 3 ] = (float)(ySlider.getMaximum() * downSampleRate * cropCoords[ 3 ]);

            cropCoords[ 4 ] = (float)(zSlider.getMaximum() * downSampleRate * cropCoords[ 4 ]);
            cropCoords[ 5 ] = (float)(zSlider.getMaximum() * downSampleRate * cropCoords[ 5 ]);
        }

        ABContextDataSource dataSource = new ABContextDataSource(
                SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext()
        );

        Collection<MaskChanRenderableData> searchDatas = new ArrayList<MaskChanRenderableData>();
        for ( MaskChanRenderableData data: dataSource.getRenderableDatas() ) {
            byte[] rendition = renderableIdVsRenderMethod.get( data.getBean().getTranslatedNum() );
            if ( rendition != null  &&  rendition[ 3 ] != RenderMappingI.NON_RENDERING ) {
                searchDatas.add( data );
            }
        }

        VolumeSearchLoadWorker.Callback callback = new VolumeSearchLoadWorker.Callback() {
            @Override
            public void loadSucceeded() {
            }

            @Override
            public void loadFailed(Throwable ex) {
                ex.printStackTrace();
                SessionMgr.getSessionMgr().handleException( ex );
            }

            @Override
            public void loadVolume(TextureDataI texture) {
                byte[] textureBytes = texture.getTextureData();

                Map<Byte,Integer> byteValToCount = new HashMap<Byte,Integer>();
                for ( int i = 0; i < textureBytes.length; i ++ ) {
                    Integer oldVal = byteValToCount.get( textureBytes[ i ] );
                    if ( oldVal == null ) {
                        oldVal = new Integer( 0 );
                    }
                    byteValToCount.put( textureBytes[i], ++oldVal );
                }

                for ( Byte b: byteValToCount.keySet() ) {
                    System.out.println("Value " + b + " appears " + byteValToCount.get( b ) + " times.");
                }
            }
        };

        VolumeSearchLoadWorker volumeSearchLoadWorker = new VolumeSearchLoadWorker(
                searchDatas, cropCoords, callback
        );
        volumeSearchLoadWorker.execute();
    }

    private static float[] getCropCoords( RangeSlider[] sliders) {
        float[] cropCoords = new float[ 6 ];
        for ( int i = 0; i < 3; i++ ) {
            cropCoords[ i * 2 ] = (float)sliders[ i ].getValue() / (float)sliders[ i ].getMaximum();
            cropCoords[ i * 2 + 1 ] = (float)sliders[ i ].getUpperValue() / (float)sliders[ i ].getMaximum();
        }
        return cropCoords;
    }

    //------------------------------Inner Classes
    public static class AlignmentBoardSettingsListener implements AlignmentBoardSettingsDialog.SettingsListener {
        private Mip3d mip3d;
        private AlignmentBoardViewer viewer;
        public AlignmentBoardSettingsListener( Mip3d mip3d, AlignmentBoardViewer viewer ) {
            this.mip3d = mip3d;
            this.viewer = viewer;
        }
        @Override
        public void setBrightness(double brightness) {
            mip3d.setGamma( (float)brightness );
        }

        @Override
        public void updateSettings() {
            AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
            viewer.updateBoard(context);
        }
    }

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

    // Add listener to update display.
    public static class SliderChangeListener implements ChangeListener {
        private RangeSlider[] sliders;
        private Mip3d mip3d;

        public SliderChangeListener( RangeSlider xSlider, RangeSlider ySlider, RangeSlider zSlider, Mip3d mip3d ) {
            this.sliders = new RangeSlider[] {
                xSlider, ySlider, zSlider
            };
            this.mip3d = mip3d;
        }

        public void stateChanged(ChangeEvent e) {
            float[] cropCoords = getCropCoords( sliders );
            mip3d.setCropCoords( cropCoords );
        }

    }

    /** An experiment in animating the view.  If ever used, should be moved elsewhere. */
    public class BrainGlow extends Thread {
        private float gamma = 0.0f;
        public boolean isRunning = true;

        public BrainGlow() {
            super.start();
        }

        @Override
        public void run() {
            while ( isRunning ) {
                long curTime = System.currentTimeMillis() % 50L;
                if ( curTime > 25 ) {
                    curTime = 50 - curTime;
                }
                gamma = 0.75f + (curTime / 100.0f);
                mip3d.setGamma( gamma );
                try {
                    Thread.sleep( 60 );
                } catch ( Exception ex ) {
                    break;
                }
            }
        }
    }

}
