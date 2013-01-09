package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * This is a special viewer to be added to the alignment board viewer panel.
 */
public class AlignmentBoardViewer extends Viewer {

    private Entity alignmentBoard;
    private RootedEntity albRootedEntity;

    private Mip3d mip3d;
    private ModelMgrObserver modelMgrObserver;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);
        setLayout(new BorderLayout());
    }

    public void setAlignmentBoard( RootedEntity albRootedEntity, Entity alignmentBoard ) {
        if (EntityConstants.TYPE_ALIGNMENT_BOARD.equals(alignmentBoard.getEntityType().getName())) {
            this.alignmentBoard = alignmentBoard;
            this.albRootedEntity = albRootedEntity;
            if ( modelMgrObserver != null ) {
                ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
            }
            modelMgrObserver = new ModelMgrListener(this, alignmentBoard);
            ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
            refresh();
        }
        else
            throw new RuntimeException("Invalid entity type for alignment board.");
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
        refresh();
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
        return Arrays.asList( new RootedEntity[]{ albRootedEntity } );
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
        alignmentBoard = null;
        albRootedEntity = null;
        removeAll();
        mip3d = null;
        ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
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

            // First how to find the items?
            List<Entity> displayableList = new ArrayList<Entity>();
            try {
                recursivelyFindDisplayableChildren(displayableList, alignmentBoard);
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException(ex);
            }

            //  Next, speak the volumes.
            mip3d.setClearOnLoad( true );
            final List<String> filenames = new ArrayList<String>();
            EntityFilenameFetcher filenameFetcher = new EntityFilenameFetcher();
            for ( Entity displayable: displayableList ) {
                // Find this displayable entity's file name of interest.
                String filename = filenameFetcher.fetchFilename(
                        displayable,
                        displayable.getEntityType().getName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ?
                                EntityFilenameFetcher.FilenameType.NEURON_FRAGMENT_3d :
                                EntityFilenameFetcher.FilenameType.IMAGE_FAST_3d
                );
                if ( filename != null ) {
                    filenames.add( filename );
                }
            }

            
            // Activate the layers panel for controlling visibility. This code might have to be moved elsewhere. 
            LayersPanel layersPanel = SessionMgr.getBrowser().getLayersPanel();
            layersPanel.showEntities(alignmentBoard.getOrderedChildren());
            SessionMgr.getBrowser().selectRightPanel(layersPanel);
            
            
            SimpleWorker loadWorker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    for ( String filename: filenames ) {
                        mip3d.loadVolume( filename );
                        // After first volume has been loaded, set the unset clear flag, so subsequent
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

            loadWorker.execute();

        }
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void recursivelyFindDisplayableChildren(List<Entity> displayableList, Entity entity) throws Exception {
        entity = ModelMgr.getModelMgr().loadLazyEntity(entity, false);
        for (Entity childEntity: entity.getChildren()) {
            if ( entity.hasChildren() ) {
                recursivelyFindDisplayableChildren(displayableList, childEntity);
            }

            String entityTypeName = childEntity.getEntityType().getName();
            if (
                EntityConstants.TYPE_CURATED_NEURON.equals(entityTypeName)
                        ||
                EntityConstants.TYPE_SAMPLE.equals(entityTypeName)
                        ||
                EntityConstants.TYPE_NEURON_FRAGMENT.equals(entityTypeName)
//                        ||
//                EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(entityTypeName)
               ) {
                logger.info("Adding a child of type " + entityTypeName);
                displayableList.add(childEntity);
            }
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
