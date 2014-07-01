package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.awt.Component;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.Launcher;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.outline.EntityOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.model.domain.AlignmentContext;
import org.janelia.it.workstation.model.domain.AlignmentContextFactory;
import org.janelia.it.workstation.model.domain.EntityWrapperFactory;
import org.janelia.it.workstation.model.domain.Sample;
import org.janelia.it.workstation.model.domain.EntityWrapper;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.workstation.nb_action.EntityWrapperCreator;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.model.domain.Neuron;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with or without a known sample, to create a new Alignment Board.
 * 
 * @author fosterl
 */
@ServiceProvider(service=EntityWrapperCreator.class,path=EntityWrapperCreator.LOOKUP_PATH)
public class AlignmentBoardCreator implements EntityWrapperCreator {
    
    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardCreator.class);
    
    private RootedEntity rootedEntity;
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();
        final Browser browser = SessionMgr.getBrowser();

        SimpleWorker worker = new SimpleWorker() {
            
            private Sample sample;
            private EntityWrapper sampleMember;
            private List<AlignmentContext> contexts;
            
            @Override
            protected void doStuff() throws Exception {
                if (rootedEntity!=null) {
                    if (rootedEntity.getType().equals(EntityConstants.TYPE_SAMPLE) ) {
                        this.sample = (Sample) EntityWrapperFactory.wrap(getRootedEntity());
                        this.contexts = sample.getAvailableAlignmentContexts();
                    }
                    else {                        
                        // Verify we have the sample ancestor.
                        Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(rootedEntity.getEntity(), EntityConstants.TYPE_SAMPLE);
                        if (sampleEntity == null) {
                            throw new Exception("No sample ancestor found for neuron fragment " + rootedEntity.getId());
                        }
                        this.sample = (Sample) EntityWrapperFactory.wrap(new RootedEntity(sampleEntity));
                        this.sampleMember = EntityWrapperFactory.wrap(getRootedEntity());
                        this.sampleMember.setParent(sample);

                        // The grandparent entity here will be an aligned neuron separation.
                        // The parent of the sample member will be a neuron fragment collection.
                        String alignmentSpaceName = null;
                        String opticalRes = null;
                        String pixelRes = null;
                        Entity neuronSeparation = ModelMgr.getModelMgr().getAncestorWithType(rootedEntity.getEntity(), EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                        if ( neuronSeparation != null ) {
                            RootedEntity rootedNS = new RootedEntity( neuronSeparation );
                            alignmentSpaceName = rootedNS.getValueByAttributeName( EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE );
                            opticalRes = rootedNS.getValueByAttributeName( EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION );
                            pixelRes = rootedNS.getValueByAttributeName( EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION );
                            if ( alignmentSpaceName == null ) {
                                // Need to resolve by comparing with all sample contexts.
                                List<AlignmentContext> sampleContexts = sample.getAvailableAlignmentContexts();
                                for ( AlignmentContext context: sampleContexts ) {
                                    if ( context.getOpticalResolution().equals(opticalRes)  &&  
                                         context.getPixelResolution().equals(pixelRes) ) {
                                        if ( contexts != null ) {
                                            // Avoid chosing when multiple contexts match.
                                            contexts = null;
                                            break;
                                        }
                                        this.contexts = Collections.singletonList( context );
                                    }
                                }
                                if ( contexts == null ) {
                                    this.contexts = sampleContexts;
                                    log.warn("Failed to find neuron or ref's alignment space.  Showing user whole list from sample.");
                                }
                            }
                            else {
                                this.contexts = Collections.singletonList(
                                    new AlignmentContext( alignmentSpaceName, opticalRes, pixelRes )
                                );
                            }
                        }
                        
                    }
                }
                else {
                    this.contexts = new AlignmentContextFactory().getAllAlignmentContexts();
                }
            }
            
            @Override
            protected void hadSuccess() {
                
                if (sample!=null && contexts.isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Sample is not aligned to a compatible alignment space", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Pick an alignment context for the new board
                AlignmentContext values[] = new AlignmentContext[contexts.size()];
                contexts.toArray(values);
                final AlignmentContext alignmentContext = (AlignmentContext)JOptionPane.showInputDialog(mainFrame, "Choose an alignment space for this alignment board", 
                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE, Icons.getIcon("folder_graphite_palette.png"), 
                        values, values[0]);
                if (alignmentContext==null) return;
                
                // Pick a name for the new board
                final String boardName = (String) JOptionPane.showInputDialog(mainFrame, "Board Name:\n",
                        "Create Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if (StringUtils.isEmpty(boardName)) return;
                
                SimpleWorker worker = new SimpleWorker() {
                    
                    private RootedEntity newBoard;
                    
                    @Override
                    protected void doStuff() throws Exception {
                        newBoard = ModelMgr.getModelMgr().createAlignmentBoard(boardName, 
                                alignmentContext.getAlignmentSpaceName(), alignmentContext.getOpticalResolution(), alignmentContext.getPixelResolution());
                        AlignmentBoardContext alignmentBoardContext = new AlignmentBoardContext(newBoard);
                        // Presence of a sample member implies that single child of
                        // the sample must be added without its siblings.
                        if (sampleMember!=null) {
                            alignmentBoardContext.addNewAlignedEntity(sampleMember);
                        }
                        else if (sample!=null) {
                            alignmentBoardContext.addNewAlignedEntity(sample);
                        }
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // Update Tree UI
                        final EntityOutline entityOutline = browser.getEntityOutline();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                entityOutline.selectEntityByUniqueId(newBoard.getUniqueId());
                                Launcher launcher = new Launcher();
                                launcher.launch(newBoard.getEntityId());
                            }
                        });
                    }
                    
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Preparing alignment board...", ""));
                worker.execute();
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Finding alignments...", ""));
        worker.execute();
    }

    @Override
    public void wrapEntity(RootedEntity e) {
        this.rootedEntity = (RootedEntity)e;
        execute();
    }

    @Override
    public boolean isCompatible(RootedEntity e) {
        setRootedEntity(e);
        if ( e == null ) {
            log.debug("Just nulled-out the rooted entity to ABCreator");
            return true;
        }
        else {
            log.debug("Just UN-Nulled rooted entity in ABCreator");            
            // Caching the test sampleEntity, for use in action label.
            final String entityTypeName = e.getEntity().getEntityTypeName();
            return entityTypeName.equals( EntityConstants.TYPE_SAMPLE )   ||
                   entityTypeName.equals( EntityConstants.TYPE_NEURON_FRAGMENT );
        }
    }

    @Override
    public String getActionLabel() {
        if ( rootedEntity == null ) {
            return "  Create New Alignment Board";
        }
        else {
            return "  Open In New Alignment Board";
        }
    }

    /**
     * @param rootedEntity the rootedEntity to set
     */
    private void setRootedEntity(RootedEntity rootedEntity) {
        this.rootedEntity = rootedEntity;
    }

    /**
     * @return the rootedEntity
     */
    private RootedEntity getRootedEntity() {
        return rootedEntity;
    }

}
