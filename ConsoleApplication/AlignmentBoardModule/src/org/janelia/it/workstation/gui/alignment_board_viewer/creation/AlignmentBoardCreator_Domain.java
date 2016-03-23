package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.alignment_board.Launcher;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.util.Icons;

import org.janelia.it.workstation.model.domain.AlignmentContext;
import org.janelia.it.workstation.model.domain.AlignmentContextFactory;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;

import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.workstation.nb_action.DomainObjectCreator;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with or without a known sample, to create a new Alignment Board.
 * 
 * @author fosterl
 */
@ServiceProvider(service=DomainObjectCreator.class,path=DomainObjectCreator.LOOKUP_PATH)
public class AlignmentBoardCreator_Domain implements DomainObjectCreator {
    
    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardCreator_Domain.class);
    
    private DomainObject domainObject;
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private Sample sample;
            private DomainObject sampleMember;
            private List<AlignmentContext> contexts;
            
            @Override
            protected void doStuff() throws Exception {
                if (domainObject!=null) {
                    if (domainObject instanceof Sample) {
                        this.contexts = new DomainHelper().getAvailableAlignmentContexts(sample);
                    }
                    else if (domainObject instanceof NeuronFragment) {
                        NeuronFragment nf = (NeuronFragment)domainObject;
                        sample = new DomainHelper().getSampleForNeuron(nf);
                        if (sample == null) {
                            throw new Exception("No sample ancestor found for neuron fragment " + domainObject.getId());
                        }
                        this.contexts = new DomainHelper().getAvailableAlignmentContexts(sample);                        
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
                    
                    private DomainObject newBoard;
                    
                    @Override
                    protected void doStuff() throws Exception {
//                        newBoard = ModelMgr.getModelMgr().createAlignmentBoard(boardName, 
//                                alignmentContext.getAlignmentSpaceName(), alignmentContext.getOpticalResolution(), alignmentContext.getPixelResolution());
//                        AlignmentBoardContext alignmentBoardContext = new AlignmentBoardContext(newBoard);
                        // Presence of a sample member implies that single child of
                        // the sample must be added without its siblings.
//                        if (sampleMember!=null) {
//                            alignmentBoardContext.addNewAlignedEntity(sampleMember);
//                        }
//                        else if (sample!=null) {
//                            alignmentBoardContext.addNewAlignedEntity(sample);
//                        }
                    }
                    
                    @Override
                    protected void hadSuccess() {
                        // Update Tree UI
                        //final EntityOutline entityOutline = browser.getEntityOutline();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                // Need to reflect the selection in the browse panel.
                                //entityOutline.selectEntityByUniqueId(newBoard.getUniqueId());
                                Launcher launcher = new Launcher();
                                launcher.launch(newBoard.getId());
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
    public void useDomainObject(DomainObject e) {
        this.domainObject = e;
        execute();
    }

    @Override
    public boolean isCompatible(DomainObject e) {
        setDomainObject(e);
        if ( e == null ) {
            log.debug("Just nulled-out the rooted entity to ABCreator");
            return true;
        }
        else {
            log.debug("Just UN-Nulled object in ABCreator");            
            // Caching the test sampleEntity, for use in action label.
//            final String entityTypeName = e.getType();
            return e instanceof Sample || e instanceof NeuronFragment;
//                    entityTypeName.equals( EntityConstants.TYPE_SAMPLE )   ||
//                   entityTypeName.equals( EntityConstants.TYPE_NEURON_FRAGMENT );
        }
    }

    @Override
    public String getActionLabel() {
        if ( domainObject == null ) {
            return "  Create New Alignment Board";
        }
        else {
            return "  Open In New Alignment Board";
        }
    }

    /**
     * @param rootedEntity the rootedEntity to set
     */
    private void setDomainObject(DomainObject domainObject) {
        this.domainObject = domainObject;
    }

    /**
     * @return the rootedEntity
     */
    private DomainObject getDomainObject() {
        return domainObject;
    }

}
