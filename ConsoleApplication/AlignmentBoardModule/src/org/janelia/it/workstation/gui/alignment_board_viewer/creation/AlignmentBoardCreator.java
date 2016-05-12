package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.workstation.gui.alignment_board.Launcher;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;

import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;

import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
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
public class AlignmentBoardCreator implements DomainObjectCreator {
    
    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardCreator.class);
    
    private DomainObject domainObject;
    private String objective;
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private Sample sample;
            private DomainObject sampleMember;
            private List<AlignmentContext> contexts;
            
            @Override
            protected void doStuff() throws Exception {
                // Initially, there will never be a domain object.
                // Worry more about this later.
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
                    this.contexts = new DomainHelper().getAllAlignmentContexts();
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
                DisplayWrapper values[] = formatContexts(contexts);
                final DisplayWrapper displayWrapper = (DisplayWrapper)JOptionPane.showInputDialog(mainFrame, "Choose an alignment space for this alignment board", 
                        "Choose alignment space", JOptionPane.QUESTION_MESSAGE, Icons.getIcon("folder_graphite_palette.png"), 
                        values, values[0]);                
                if (displayWrapper==null) return;
                final AlignmentContext alignmentContext = displayWrapper.getContext();
                
                // Pick a name for the new board
                final String boardName = (String) JOptionPane.showInputDialog(mainFrame, "Board Name:\n",
                        "Create Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
                if (StringUtils.isEmpty(boardName)) return;
                
                SimpleWorker worker = new SimpleWorker() {
                    
                    private DomainObject newBoard;
                    
                    @Override
                    protected void doStuff() throws Exception { 
                        AlignmentBoard board = new AlignmentBoard();                        
                        board.setAlignmentSpace(alignmentContext.getAlignmentSpace());
                        board.setImageSize(alignmentContext.getImageSize());
                        board.setOpticalResolution(alignmentContext.getOpticalResolution());
                        board.setName(boardName);                        
                        newBoard = board;
                        
                        AlignmentBoardContext alignmentBoardContext = new AlignmentBoardContext(board, alignmentContext);
                        // Presence of a sample member implies that single child of
                        // the sample must be added without its siblings.
                        if (sampleMember!=null) {
                            alignmentBoardContext.addDomainObject(sampleMember, objective);
                        }
                        else if (sample!=null) {
                            alignmentBoardContext.addDomainObject(sample, objective);
                        }
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
    public boolean isCompatible(DomainObject domainObject) {
        setDomainObject(domainObject, "");
        if ( domainObject == null ) {
            log.debug("Just nulled-out the domain object to ABCreator");
            return true;
        }
        else {
            log.debug("Just UN-Nulled object in ABCreator");            
            return domainObject instanceof Sample || domainObject instanceof NeuronFragment  ||  domainObject instanceof CompartmentSet;            
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
     * @param domainObject the domain object to set.
     */
    private void setDomainObject(DomainObject domainObject, String objective) {
        this.domainObject = domainObject;
        this.objective = objective;
    }

    /**
     * @return the object of interest
     */
    private DomainObject getDomainObject() {
        return domainObject;
    }
    
    private DisplayWrapper[] formatContexts(List<AlignmentContext> contexts) {
        DisplayWrapper[] values = new DisplayWrapper[contexts.size()];
        int i = 0;
        for (AlignmentContext context: contexts) {
            values[ i++ ] = new DisplayWrapper(context, String.format("%s: %s %s", context.getAlignmentSpace(), context.getImageSize(), context.getOpticalResolution()));
        }
        
        return values;
    }

    private static class DisplayWrapper {
        private AlignmentContext context;
        private String description;

        public DisplayWrapper(AlignmentContext context, String description) {
            this.context = context;
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
        
        public AlignmentContext getContext() {
            return context;
        }
    }
    
}
