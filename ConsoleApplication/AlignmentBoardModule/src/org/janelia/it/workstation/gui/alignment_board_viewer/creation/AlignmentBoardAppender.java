package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.awt.Component;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.compartments.Compartment;
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
import org.janelia.it.workstation.nb_action.DomainObjectAppender;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with some selected object(s), while an alignment board is open,
 * to add to that board.  This is the NG alternative to the old "Add to
 * Alignment Board" functionality supported via Baseball Cards.
 * 
 * @author fosterl
 */
@ServiceProvider(service=DomainObjectAppender.class,path=DomainObjectAppender.LOOKUP_PATH)
public class AlignmentBoardAppender implements DomainObjectAppender {
    
	private static final String INCOMPATIBLE_MSG = "Sample is not aligned to a compatible alignment space";
    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardAppender.class);
    
    private List<DomainObject> domainObjects;
    private final DomainHelper domainHelper = new DomainHelper();
    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
    
    public void execute() {

        final Component mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
			// NOTE: to support adding references, a new menu item might
			// be added in future: add this sample's reference channel to
			// open alignment board.  LLF
			//
            
            @Override
            protected void doStuff() throws Exception {
                if (domainObjects!=null  &&  domainObjects.size() > 0) {
					for (DomainObject domainObject: domainObjects) {
						if (domainObject instanceof Sample) {
							Sample sample = (Sample)domainObject;
							if (!compatibilityChecker.isSampleCompatible(getAlignmentContext(), sample)) {
								throw new Exception(INCOMPATIBLE_MSG);
							}
						}
						else if (domainObject instanceof CompartmentSet) {
							// Not sure this can happen: Only if users delete a compartment set, and then add it back
							// before reopening??
							CompartmentSet compartmentSet = (CompartmentSet)domainObject;
							if (!compatibilityChecker.isCompartmentSetCompatible(getAlignmentContext(), compartmentSet)) {
								throw new Exception(INCOMPATIBLE_MSG);
							}
						}
						else if (domainObject instanceof NeuronFragment) {
							NeuronFragment nf = (NeuronFragment) domainObject;
							Sample sample = domainHelper.getSampleForNeuron(nf);
							if (sample == null) {
								throw new Exception("No sample ancestor found for neuron fragment " + domainObject.getId());
							}
							if (!compatibilityChecker.isFragmentCompatible(getAlignmentContext(), sample, nf)) {
								throw new Exception(INCOMPATIBLE_MSG);
							}
						}
						else if (domainObject instanceof Compartment) {
							// Not sure this can happen.
							Compartment compartment = (Compartment) domainObject;
							CompartmentSet compartmentSet = compartment.getParent();
							if (compartmentSet == null) {
								throw new Exception("No compartment-set ancestor found for compartment " + domainObject.getId());
							}
							if (!compatibilityChecker.isCompartmentSetCompatible(getAlignmentContext(), compartmentSet)) {
								throw new Exception(INCOMPATIBLE_MSG);
							}
						}
					}
                }
            }
            
            @Override
            protected void hadSuccess() {
                SimpleWorker worker = new SimpleWorker() {
                    
                    @Override
                    protected void doStuff() throws Exception {                         
                        domainHelper.saveAlignmentBoardAsync(getAlignmentBoard());
                        AlignmentBoardContext alignmentBoardContext = getABContext();
                        alignmentBoardContext.addDomainObjects(domainObjects);
                    }

                    @Override
                    protected void hadSuccess() {
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
    public void useDomainObjects(List<DomainObject> e) {
        this.domainObjects = e;
        execute();
    }

    @Override
    public boolean isCompatible(List<DomainObject> domainObjects) {
        try {
            boolean rtnVal = false;
            // Establish: is the Alignment Board Viewer already open?
            AlignmentBoardContext abContext = getABContext();
            setDomainObjects(domainObjects);
            // Establish: is the domainObject incoming, an appropriate type, with alignment board open?
            if (domainObjects != null && abContext != null) {
                AlignmentContext alignmentContext = abContext.getAlignmentContext();
                AlignmentBoard receiver = abContext.getAlignmentBoard();
                for (DomainObject domainObject : domainObjects) {
                    if (domainObject instanceof Sample) {
                        rtnVal = compatibilityChecker.isSampleCompatible(alignmentContext, (Sample) domainObject);
                    } else if (domainObject instanceof NeuronFragment) {
                        final NeuronFragment neuronFragment = (NeuronFragment) domainObject;
                        Sample sample = domainHelper.getSampleForNeuron(neuronFragment);
                        rtnVal = compatibilityChecker.isFragmentCompatible(alignmentContext, sample, neuronFragment);
                    } else if (domainObject instanceof CompartmentSet) {
                        rtnVal = true;
                        for (AlignmentBoardItem item : receiver.getChildren()) {
                            // Only one compartment set may be added.
                            if (domainHelper.isCompartmentSet(item)) {
                                rtnVal = false;
                                break;
                            }
                        }
                        if (rtnVal) {
                            rtnVal = compatibilityChecker.isCompartmentSetCompatible(alignmentContext, (CompartmentSet) domainObject);
                        }
                    }
                }
            }

            return rtnVal;
        } catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            return false;
        }
    }

    @Override
    public String getActionLabel() {
        if ( domainObjects != null ) {
            AlignmentBoard alignmentBoard = getAlignmentBoard();
            if (alignmentBoard != null) {
                return "  Add to Alignment Board '" + alignmentBoard.getName() + "'";
            }
        }
        return "  Ignore: no alignment board open";
    }

    /**
     * @param domainObjects the domain object to set.
     */
    private void setDomainObjects(List<DomainObject> domainObjects) {
        this.domainObjects = domainObjects;
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
