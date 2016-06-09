package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.awt.Component;
import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board_viewer.CompatibilityChecker;
import org.janelia.it.workstation.gui.alignment_board_viewer.LayersPanel;
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
public class AlignmentBoardAppender implements DomainObjectCreator {
    
    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardAppender.class);
    
    private DomainObject domainObject;
    private final DomainHelper domainHelper = new DomainHelper();
    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private Sample sample;
			// The member-of-sample is some domain object that is part of
			// a sample.  It may be a Neuron Fragment or a Volume Image.
			// If it is not given, it means the whole sample is to be added.
            private DomainObject memberOfSample = null;
            
            @Override
            protected void doStuff() throws Exception {
                if (domainObject!=null) {
                    if (domainObject instanceof Sample) {
						this.sample = (Sample)domainObject;
                    }
                    else if (domainObject instanceof NeuronFragment) {
                        NeuronFragment nf = (NeuronFragment)domainObject;
						this.memberOfSample = domainObject;
                        sample = new DomainHelper().getSampleForNeuron(nf);
                        if (sample == null) {
                            throw new Exception("No sample ancestor found for neuron fragment " + domainObject.getId());
                        }
                    }
                }
            }
            
            @Override
            protected void hadSuccess() {
                final AlignmentContext alignmentContext = getAlignmentContext();
                if (sample!=null && (!compatibilityChecker.isSampleCompatible(alignmentContext, sample))) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Sample is not aligned to a compatible alignment space", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                SimpleWorker worker = new SimpleWorker() {
                    
                    private final AlignmentBoard board = getAlignmentBoard();

                    @Override
                    protected void doStuff() throws Exception { 
                        AlignmentBoardContext alignmentBoardContext = new AlignmentBoardContext(board, alignmentContext);
                        // Presence of a sample member implies that single child of
                        // the sample must be added without its siblings.
                        if (memberOfSample!=null) {
                            alignmentBoardContext.addDomainObject(memberOfSample);
                        }
                        else if (sample!=null) {
							log.info("Adding sample {} to alignment board {}, with id={}", sample.getName(), board.getName(), board.getId());
                            alignmentBoardContext.addDomainObject(sample);
                        }
                    }

                    @Override
                    protected void hadSuccess() {
                        // Update Tree UI
                        //final EntityOutline entityOutline = browser.getEntityOutline();
//                        SwingUtilities.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                // Need to reflect the selection in the browse panel.
//                                Launcher launcher = new Launcher();
//								log.info("Re-launching with alignment board {}, with id={}", board.getName(), board.getId());
//                                launcher.launch(board.getId());
//                            }
//                        });
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
        boolean rtnVal = false;
        // Establish: is the Alignment Board Viewer already open?
        AlignmentBoardContext abContext = getABContext();
        setDomainObject(domainObject);
        // Establish: is the domainObject incoming, an appropriate type, with alignment board open?
        if (domainObject != null  &&  abContext != null) {
            AlignmentContext alignmentContext = abContext.getAlignmentContext();
            AlignmentBoard receiver = abContext.getAlignmentBoard();
            if (domainObject instanceof Sample) {
                rtnVal = compatibilityChecker.isSampleCompatible(alignmentContext, (Sample)domainObject);
            }
            else if ( domainObject instanceof NeuronFragment) {
                Sample sample = domainHelper.getSampleForNeuron((NeuronFragment)domainObject);
                rtnVal = compatibilityChecker.isSampleCompatible(alignmentContext, sample);
            }
            else if (domainObject instanceof CompartmentSet) {
                rtnVal = true;
                for (AlignmentBoardItem item: receiver.getChildren()) {
                    if (item.getTarget().getObjectRef().getTargetClassName().equals(CompartmentSet.class.getSimpleName())) {
                        rtnVal = false;
                        break;
                    }
                }
                if (rtnVal) {
                    rtnVal = compatibilityChecker.isCompartmentSetCompatible(alignmentContext, (CompartmentSet)domainObject);
                }
            }
        }

        return rtnVal;
    }

    @Override
    public String getActionLabel() {
        if ( domainObject != null ) {
            AlignmentBoard alignmentBoard = getAlignmentBoard();
            if (alignmentBoard != null) {
                return "  Add to Alignment Board '" + alignmentBoard.getName() + "'";
            }
        }
        return "  Ignore: no alignment board open";
    }

    /**
     * @param domainObject the domain object to set.
     */
    private void setDomainObject(DomainObject domainObject) {
        this.domainObject = domainObject;
    }

    /**
     * @return the object of interest
     */
    private DomainObject getDomainObject() {
        return domainObject;
    }
    
    private AlignmentBoard getAlignmentBoard() {
        AlignmentBoardContext abContext = getABContext();
        if ( abContext != null ) {
            return abContext.getAlignmentBoard();
        }
        return null;
    }
    
    private AlignmentBoardContext getABContext() {
        AlignmentBoardContext rtnVal = null;
        LayersPanel lp = AlignmentBoardMgr.getInstance().getLayersPanel();
        if (lp != null) {
            rtnVal = lp.getAlignmentBoardContext();
        }
        return rtnVal;
    }
    
    private AlignmentContext getAlignmentContext() {
        AlignmentBoardContext abContext = getABContext();
        if (abContext != null) {
            return abContext.getAlignmentContext();
        }
        return null;
    }
    
}
